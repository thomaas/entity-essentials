/**
 * Entity Essentials -- A Component-based Entity System
 *
 * Copyright (C) 2016 Elmar Schug <elmar.schug@jayware.org>,
 *                    Markus Neubauer <markus.neubauer@jayware.org>
 *
 *     This file is part of Entity Essentials.
 *
 *     Entity Essentials is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public License
 *     as published by the Free Software Foundation, either version 3 of
 *     the License, or any later version.
 *
 *     Entity Essentials is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jayware.e2.assembly.impl;


import org.jayware.e2.assembly.api.TreeEvent;
import org.jayware.e2.assembly.api.TreeEvent.AddChildNodeEvent;
import org.jayware.e2.assembly.api.TreeEvent.ChildNodeAddedEvent;
import org.jayware.e2.assembly.api.TreeEvent.ChildNodeRemovedEvent;
import org.jayware.e2.assembly.api.TreeEvent.CreateTreeNodeEvent;
import org.jayware.e2.assembly.api.TreeEvent.DeleteTreeNodeEvent;
import org.jayware.e2.assembly.api.TreeEvent.DeletingTreeNodeEvent;
import org.jayware.e2.assembly.api.TreeEvent.FindChildrenQuery;
import org.jayware.e2.assembly.api.TreeEvent.RemoveChildNodeEvent;
import org.jayware.e2.assembly.api.TreeEvent.TreeNodeCreatedEvent;
import org.jayware.e2.assembly.api.TreeNode;
import org.jayware.e2.assembly.api.components.TreeNodeComponent;
import org.jayware.e2.component.api.ComponentManager;
import org.jayware.e2.context.api.Context;
import org.jayware.e2.context.api.Contextual;
import org.jayware.e2.context.api.Disposable;
import org.jayware.e2.entity.api.EntityManager;
import org.jayware.e2.entity.api.EntityPath;
import org.jayware.e2.entity.api.EntityRef;
import org.jayware.e2.entity.api.InvalidEntityRefException;
import org.jayware.e2.event.api.Event;
import org.jayware.e2.event.api.EventManager;
import org.jayware.e2.event.api.Handle;
import org.jayware.e2.event.api.Param;
import org.jayware.e2.event.api.Presence;
import org.jayware.e2.event.api.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Arrays.asList;
import static java.util.Arrays.copyOf;
import static org.jayware.e2.assembly.api.TreeEvent.ChildNodeRemovedEvent.ParentNodeParam;
import static org.jayware.e2.assembly.api.TreeEvent.FindChildrenQuery.ChildrenParam;
import static org.jayware.e2.assembly.api.TreeEvent.NodeParam;
import static org.jayware.e2.assembly.api.TreeEvent.NodePendantParam;
import static org.jayware.e2.event.api.EventType.RootEvent.ContextParam;
import static org.jayware.e2.event.api.Parameters.param;
import static org.jayware.e2.event.api.Presence.Optional;


public class TreeHub
implements Disposable
{
    private static final double CHILD_ARRAY_FRAGMENTATION_THRESHOLD = 0.5;

    private final Context myContext;
    private final EntityManager myEntityManager;
    private final ComponentManager myComponentManager;
    private final EventManager myEventManager;

    private final Map<EntityRef, TreeNode> myLookupTable;

    TreeHub(Context context)
    {
        myContext = context;
        myEntityManager = myContext.getService(EntityManager.class);
        myComponentManager = myContext.getService(ComponentManager.class);
        myEventManager = myContext.getService(EventManager.class);

        myLookupTable = new ConcurrentHashMap<>();

        myEventManager.subscribe(myContext, this);
    }

    @Handle(CreateTreeNodeEvent.class)
    public void handleCreateTreeNodeEvent(Event event, @Param(NodePendantParam) EntityRef pendant)
    {
        final EntityRef ref = myEntityManager.createEntity(myContext);
        final TreeNodeComponent component = myComponentManager.addComponent(ref, TreeNodeComponent.class);
        final TreeNode treeNode;

        component.setPendant(pendant);
        component.pushTo(ref);

        treeNode = new TreeNodeImpl(ref);

        fireTreeNodeCreatedEvent(treeNode, pendant);

        if (event.isQuery())
        {
            ((Query) event).result(NodeParam, treeNode);
        }
    }

    @Handle(DeleteTreeNodeEvent.class)
    public void handleDeleteTreeNodeEvent(@Param(NodeParam) TreeNode node)
    {
        fireDeletingTreeNodeEvent(node);

        final EntityRef nodeRef = node.getNodeRef();
        final TreeNodeComponent nodeComponent = myComponentManager.getComponent(nodeRef, TreeNodeComponent.class);

        if (nodeComponent.getParent() != null)
        {
            fireRemoveChildEvent(node);
        }

        if (nodeComponent.getChildren() != null)
        {
            for (EntityRef childRef : nodeComponent.getChildren())
            {
                if (childRef != null)
                {
                    fireDeleteTreeNodeEvent(myLookupTable.get(childRef));
                }
            }
        }

        myLookupTable.remove(nodeRef);
        myEntityManager.deleteEntity(nodeRef);

        fireTreeNodeDeletedEvent(node);
    }

    @Handle(AddChildNodeEvent.class)
    public void handleAddChildNodeEvent(@Param(ParentNodeParam) TreeNode parent, @Param(NodeParam) TreeNode child)
    {
        final EntityRef parentNodeRef = parent.getNodeRef();
        final EntityRef childNodeRef = child.getNodeRef();

        final TreeNodeComponent parentComponent = myComponentManager.getComponent(parentNodeRef, TreeNodeComponent.class);
        final TreeNodeComponent childComponent = myComponentManager.getComponent(childNodeRef, TreeNodeComponent.class);

        if (childComponent.getParent() != null)
        {
            fireRemoveChildEvent(child);
            childComponent.pullFrom(childNodeRef);
        }

        final List<EntityRef> children = new ArrayList<>();

        if (parentComponent.getChildren() != null)
        {
            children.addAll(asList(parentComponent.getChildren()));
        }

        children.add(childNodeRef);

        parentComponent.setChildren(children.toArray(new EntityRef[children.size()]));
        parentComponent.pushTo(parentNodeRef);

        childComponent.setParent(parentNodeRef);
        childComponent.pushTo(childNodeRef);

        fireChildNodeAddedEvent(parent, child);
    }

    @Handle(RemoveChildNodeEvent.class)
    public void handleRemoveChildNodeEvent(@Param(NodeParam) TreeNode child)
    {
        final EntityRef childRef;
        final EntityRef parentRef;
        final TreeNodeComponent parentComponent;
        final TreeNodeComponent childComponent;

        childRef = child.getNodeRef();
        childComponent = myComponentManager.getComponent(childRef, TreeNodeComponent.class);

        if (childComponent.getParent() != null)
        {
            final TreeNode parent = child.getParent();
            List<EntityRef> children;

            parentRef = parent.getNodeRef();
            parentComponent = myComponentManager.getComponent(parentRef, TreeNodeComponent.class);

            children = asList(parentComponent.getChildren());
            children.remove(childRef);
            childComponent.pushTo(childRef);

            childComponent.setParent(null);
            parentComponent.pushTo(parentRef);

            fireChildNodeRemovedEvent(parent, child);
        }
    }

    @Handle(FindChildrenQuery.class)
    public void handleFindChildrenQuery(Query query, @Param(NodeParam) TreeNode node, @Param(value = ChildrenParam, presence = Optional) List<TreeNode> children)
    {
        final TreeNodeComponent nodeComponent;

        if (children == null)
        {
            children = new ArrayList<>();
        }
        else
        {
            children.clear();
        }

        nodeComponent = myComponentManager.getComponent(node.getNodeRef(), TreeNodeComponent.class);

        for (EntityRef entityRef : nodeComponent.getChildren())
        {
            children.add(myLookupTable.get(entityRef));
        }

        query.result(ChildrenParam, children);
    }

    @Override
    public void dispose(Context context)
    {
        myEventManager.unsubscribe(context, this);
    }

    private void fireTreeNodeCreatedEvent(TreeNode treeNode, EntityRef pendant)
    {
        myEventManager.post(
            TreeNodeCreatedEvent.class,
            param(ContextParam, myContext),
            param(NodeParam, treeNode),
            param(NodePendantParam, pendant)
        );
    }

    private void fireDeleteTreeNodeEvent(TreeNode node)
    {
        myEventManager.send(
            DeleteTreeNodeEvent.class,
            param(ContextParam, myContext),
            param(NodeParam, node)
        );
    }

    private void fireDeletingTreeNodeEvent(TreeNode node)
    {
        myEventManager.send(
            DeletingTreeNodeEvent.class,
            param(ContextParam, myContext),
            param(NodeParam, node),
            param(NodePendantParam, node.getPendantRef())
        );
    }

    private void fireTreeNodeDeletedEvent(TreeNode node)
    {
        myEventManager.send(
            TreeEvent.TreeNodeDeletedEvent.class,
            param(ContextParam, myContext),
            param(NodeParam, node),
            param(NodePendantParam, node.getPendantRef())
        );
    }

    private void fireAddChildNodeEvent(TreeNode parent, TreeNode child)
    {
        myEventManager.send(
            AddChildNodeEvent.class,
            param(ContextParam, myContext),
            param(ParentNodeParam, parent),
            param(NodeParam, child)
        );
    }

    private void fireChildNodeAddedEvent(TreeNode parent, TreeNode child)
    {
        myEventManager.post(
            ChildNodeAddedEvent.class,
            param(ContextParam, myContext),
            param(ParentNodeParam, parent),
            param(NodeParam, child)
        );
    }

    private void fireRemoveChildEvent(TreeNode node)
    {
        myEventManager.send(
            RemoveChildNodeEvent.class,
            param(ContextParam, myContext),
            param(NodeParam, node)
        );
    }

    private void fireChildNodeRemovedEvent(TreeNode parent, TreeNode child)
    {
        myEventManager.post(
            ChildNodeRemovedEvent.class,
            param(ContextParam, myContext),
            param(ParentNodeParam, parent),
            param(NodeParam, child)
        );
    }

    public class TreeNodeImpl
    implements TreeNode
    {
        private final EntityRef myNodeRef;
        private final TreeNodeComponent myNodeComponent;

        private final EntityRef myPendantRef;

        public TreeNodeImpl(EntityRef ref)
        {
            myNodeRef = ref;
            myNodeComponent = myComponentManager.getComponent(myNodeRef, TreeNodeComponent.class);
            myPendantRef = myNodeComponent.getPendant();

            myLookupTable.put(ref, this);
        }

        @Override
        public TreeNode getParent()
        {
            myComponentManager.pullComponent(myNodeRef, myNodeComponent);
            return myLookupTable.get(myNodeComponent.getParent());
        }

        @Override
        public boolean hasParent()
        {
            myComponentManager.pullComponent(myNodeRef, myNodeComponent);
            return myNodeComponent.getParent() != null;
        }

        @Override
        public void addChild(TreeNode node)
        {
            fireAddChildNodeEvent(this, node);
        }

        @Override
        public void removeChild(TreeNode node)
        {
            fireRemoveChildEvent(node);
        }

        @Override
        public List<TreeNode> children()
        {
            final List<TreeNode> result = new ArrayList<>();
            myComponentManager.pullComponent(myNodeRef, myNodeComponent);

            if (myNodeComponent.getChildren() != null)
            {
                for (EntityRef ref : myNodeComponent.getChildren())
                {
                    result.add(myLookupTable.get(ref));
                }
            }

            return result;
        }

        @Override
        public EntityRef getNodeRef()
        {
            return myNodeRef;
        }

        @Override
        public EntityRef getPendantRef()
        {
            return myPendantRef;
        }

        @Override
        public String getId()
        throws InvalidEntityRefException
        {
            return myPendantRef.getId();
        }

        @Override
        public EntityPath getPath()
        throws InvalidEntityRefException
        {
            throw new UnsupportedOperationException("TreeNodeImpl.getPath");
        }

        @Override
        public boolean isValid()
        {
            return myPendantRef.isValid();
        }

        @Override
        public boolean isInvalid()
        {
            return myPendantRef.isInvalid();
        }

        @Override
        public Context getContext()
        {
            return myPendantRef.getContext();
        }

        @Override
        public boolean belongsTo(Context context)
        {
            return myPendantRef.belongsTo(context);
        }

        @Override
        public boolean belongsTo(Contextual contextual)
        {
            return myPendantRef.belongsTo(contextual);
        }
    }
}
