/**
 * Entity Essentials -- A Component-based Entity System
 *
 * Copyright (C) 2015 Elmar Schug <elmar.schug@jayware.org>,
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
package org.jayware.e2.component.impl;

import org.jayware.e2.component.api.AbstractComponent;
import org.jayware.e2.component.api.Aspect;
import org.jayware.e2.component.api.AspectEvent.AspectGainedEvent;
import org.jayware.e2.component.api.AspectEvent.AspectLostEvent;
import org.jayware.e2.component.api.Component;
import org.jayware.e2.component.api.ComponentEvent.AddComponentEvent;
import org.jayware.e2.component.api.ComponentEvent.ComponentAddedEvent;
import org.jayware.e2.component.api.ComponentEvent.ComponentCreatedEvent;
import org.jayware.e2.component.api.ComponentEvent.ComponentPreparedEvent;
import org.jayware.e2.component.api.ComponentEvent.ComponentPulledEvent;
import org.jayware.e2.component.api.ComponentEvent.ComponentPushedEvent;
import org.jayware.e2.component.api.ComponentEvent.ComponentRemovedEvent;
import org.jayware.e2.component.api.ComponentEvent.CreateComponentEvent;
import org.jayware.e2.component.api.ComponentEvent.PrepareComponentEvent;
import org.jayware.e2.component.api.ComponentEvent.PullComponentEvent;
import org.jayware.e2.component.api.ComponentEvent.PushComponentEvent;
import org.jayware.e2.component.api.ComponentEvent.RemoveComponentEvent;
import org.jayware.e2.component.api.ComponentFactory;
import org.jayware.e2.component.api.ComponentNotFoundException;
import org.jayware.e2.context.api.Context;
import org.jayware.e2.context.api.Disposable;
import org.jayware.e2.entity.api.EntityEvent.EntityDeletedEvent;
import org.jayware.e2.entity.api.EntityRef;
import org.jayware.e2.event.api.Event;
import org.jayware.e2.event.api.EventManager;
import org.jayware.e2.event.api.Handle;
import org.jayware.e2.event.api.Param;
import org.jayware.e2.event.api.Query;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.jayware.e2.component.api.Aspect.aspect;
import static org.jayware.e2.component.api.AspectEvent.NewAspectParam;
import static org.jayware.e2.component.api.AspectEvent.OldAspectParam;
import static org.jayware.e2.component.api.ComponentEvent.ComponentChangeEvent.ComponentParam;
import static org.jayware.e2.component.api.ComponentEvent.ComponentPulledEvent.OldComponentParam;
import static org.jayware.e2.component.api.ComponentEvent.ComponentTypeParam;
import static org.jayware.e2.entity.api.EntityEvent.EntityChangedEvent.EntityRefParam;
import static org.jayware.e2.entity.api.EntityEvent.EntityPathParam;
import static org.jayware.e2.event.api.EventType.RootEvent.ContextParam;
import static org.jayware.e2.event.api.Parameters.param;
import static org.jayware.e2.event.api.Presence.Optional;


public class ComponentStore
implements Disposable
{
    private final Context myContext;

    private final EventManager myEventManager;

    private final ComponentFactory myComponentFactory;

    private final Map<String, Class<? extends Component>> myComponentClassMap;
    private final Map<Class<? extends Component>, Map<EntityRef, Component>> myComponentDatabase;

    private final ReadWriteLock myReadWriteLock = new ReentrantReadWriteLock();
    private final Lock myReadLock = myReadWriteLock.readLock();
    private final Lock myWriteLock = myReadWriteLock.writeLock();

    public ComponentStore(Context context)
    {
        myContext = context;
        myEventManager = myContext.getService(EventManager.class);
        myComponentFactory = myContext.getService(ComponentFactory.class);

        myComponentClassMap = new HashMap<>();
        myComponentDatabase = new HashMap<>();

        myEventManager.subscribe(context, this);
    }

    public void prepareComponent(Class<? extends Component> component)
    {
        myEventManager.send(PrepareComponentEvent.class,
            param(ContextParam, myContext),
            param(ComponentTypeParam, component)
        );
    }

    public <T extends Component> T addComponent(EntityRef ref, Class<T> component)
    {
        if (!hasComponent(ref, component))
        {
            myEventManager.send(AddComponentEvent.class,
                param(ContextParam, myContext),
                param(EntityRefParam, ref),
                param(EntityPathParam, ref.getPath()),
                param(ComponentTypeParam, component)
            );
        }

        return getComponent(ref, component);
    }

    public <T extends Component> void removeComponent(EntityRef ref, Class<T> component)
    {
        myEventManager.send(RemoveComponentEvent.class,
            param(ContextParam, myContext),
            param(EntityRefParam, ref),
            param(EntityPathParam, ref.getPath()),
            param(ComponentTypeParam, component)
        );
    }

    public <T extends Component> T getComponent(EntityRef ref, Class<T> component)
    {
        myReadLock.lock();
        try
        {
            final T instance = findComponent(ref, component);

            if (instance == null)
            {
                throw new ComponentNotFoundException(ref, component);
            }

            return instance;
        }
        finally
        {
            myReadLock.unlock();
        }
    }

    public <T extends Component> Collection<T> getComponents(EntityRef ref)
    {
        myReadLock.lock();
        try
        {
            final Set<T> components = new HashSet<>();

            for (Map<EntityRef, Component> refComponentMap : myComponentDatabase.values())
            {
                final AbstractComponent component = (AbstractComponent) refComponentMap.get(ref);
                if (component != null)
                {
                    components.add(component.copy());
                }
            }

            return components;
        }
        finally
        {
            myReadLock.unlock();
        }
    }

    public Aspect getAspect(EntityRef ref)
    {
        myReadLock.lock();
        try
        {
            final Set<Class<? extends Component>> components = new HashSet<>();

            for (Map<EntityRef, Component> refComponentMap : myComponentDatabase.values())
            {
                final AbstractComponent component = (AbstractComponent) refComponentMap.get(ref);
                if (component != null)
                {
                    components.add(component.type());
                }
            }

            return aspect(components);
        }
        finally
        {
            myReadLock.unlock();
        }
    }

    public <T extends Component> T findComponent(EntityRef ref, Class<T> type)
    {
        myReadLock.lock();
        try
        {
                AbstractComponent instance = getComponentFromDatabase(ref, type);

                if (instance != null)
                {
                    return (T) instance.copy();
                }

            return null;
        }
        finally
        {
            myReadLock.unlock();
        }
    }

    public <T extends Component> void pullComponent(EntityRef ref, T component)
    {
        final Class<? extends Component> type = ((AbstractComponent) component).type();

        if (!hasComponent(ref, type))
        {
            throw new ComponentNotFoundException(ref, type);
        }

        myEventManager.send(PullComponentEvent.class,
            param(ContextParam, myContext),
            param(EntityRefParam, ref),
            param(EntityPathParam, ref.getPath()),
            param(ComponentParam, component),
            param(ComponentTypeParam, component.type())
        );
    }

    public <T extends Component> void pushComponent(EntityRef ref, T component)
    {
        final Class<? extends Component> type = ((AbstractComponent) component).type();

        if (!hasComponent(ref, type))
        {
            throw new ComponentNotFoundException(ref, type);
        }

        firePushComponentEvent(ref, component);
    }

    public boolean hasComponent(EntityRef ref, Class<? extends Component> component)
    {
        final Map<EntityRef, Component> row;

        myReadLock.lock();
        try
        {
            row = myComponentDatabase.get(component);
            return row != null && row.containsKey(ref);
        }
        finally
        {
            myReadLock.unlock();
        }
    }

    public boolean hasComponents(EntityRef ref, Collection<Class<? extends Component>> components)
    {
        Map<EntityRef, Component> row;

        myReadLock.lock();
        try
        {
            for (Class<? extends Component> component : components)
            {
                row = myComponentDatabase.get(component);

                if (row == null || !row.containsKey(ref))
                {
                    return false;
                }
            }

            return true;
        }
        finally
        {
            myReadLock.unlock();
        }
    }

    public int numberOfComponents(EntityRef ref)
    {
        int result = 0;

        myReadLock.lock();
        try
        {
            for (Map<EntityRef, Component> row : myComponentDatabase.values())
            {
                if (row.containsKey(ref))
                {
                    ++result;
                }
            }

            return result;
        }
        finally
        {
            myReadLock.unlock();
        }
    }

    public Set<Class<? extends Component>> getComponentClasses()
    {
        myReadLock.lock();
        try
        {
            return new HashSet<>(myComponentClassMap.values());
        }
        finally
        {
            myReadLock.unlock();
        }
    }

    public Class<? extends Component> getComponentClassByName(String name)
    {
        myReadLock.lock();
        try
        {
            return myComponentClassMap.get(name);
        }
        finally
        {
            myReadLock.unlock();
        }
    }

    @Handle(EntityDeletedEvent.class)
    public void handleEntityDeletedEvent(@Param(EntityRefParam) EntityRef ref)
    {
        myWriteLock.lock();
        try
        {
            for (Component component : getComponents(ref))
            {
                Map<EntityRef, Component> row = myComponentDatabase.get(component.type());

                if (row != null)
                {
                    final Component instance = row.get(ref);

                    if (instance != null)
                    {
                        row.remove(ref);
                    }
                }
            }
        }
        finally
        {
            myWriteLock.unlock();
        }
    }

    @Handle(PrepareComponentEvent.class)
    public void handlePrepareComponentEvent(@Param(ComponentTypeParam) Class<? extends Component> componentType)
    {
        boolean fireEvents = false;

        myWriteLock.lock();
        try
        {
            if (!myComponentClassMap.containsKey(componentType.getName()))
            {
                myComponentFactory.prepareComponent(componentType);
                myComponentClassMap.put(componentType.getName(), componentType);
            }
        }
        finally
        {
            myWriteLock.unlock();
        }

        if (fireEvents)
        {
            fireComponentPreparedEvent(componentType);
        }
    }

    @Handle(CreateComponentEvent.class)
    public void handleCreateComponentEvent(Event event, @Param(ComponentTypeParam) Class<? extends Component> type)
    {
        if (event.isQuery())
        {
            final Component result = myComponentFactory.createComponent(type).newInstance(myContext);

            fireComponentCreatedEvent(result);

            ((Query) event).result(ComponentParam, result);
        }
    }

    @Handle(AddComponentEvent.class)
    public void handleAddComponentEvent(@Param(EntityRefParam) EntityRef ref,
                                        @Param(ComponentTypeParam) Class<? extends Component> componentType,
                                        @Param(value = ComponentParam, presence = Optional) Component component)
    {
        Map<EntityRef, Component> row;
        Component instance;
        Aspect oldAspect = null;
        Aspect newAspect = null;
        boolean fireEvents = false;
        boolean firePushEvent = false;

        myWriteLock.lock();
        try
        {
            if (!myComponentClassMap.containsKey(componentType.getName()))
            {
                prepareComponent(componentType);
            }

            row = myComponentDatabase.get(componentType);

            if (row == null)
            {
                row = new HashMap<>();
                myComponentDatabase.put(componentType, row);
            }

            instance = row.get(ref);

            if (instance == null)
            {
                oldAspect = getAspect(ref);
                newAspect = oldAspect.add(componentType);

                final AbstractComponent newComponent = (AbstractComponent) myComponentFactory.createComponent(componentType).newInstance(myContext);

                if (component != null)
                {
                    newComponent.copy(component);
                }

                row.put(ref, newComponent);
                fireEvents = true;
            }
            else
            {
                firePushEvent = component != null;
            }
        }
        finally
        {
            myWriteLock.unlock();
        }

        if (fireEvents)
        {
            fireComponentAddedEvent(ref, componentType);
            fireAspectGainedEvent(ref, newAspect, oldAspect);
        }
        else if (firePushEvent)
        {
            firePushComponentEvent(ref, component);
        }
    }

    @Handle(RemoveComponentEvent.class)
    public void handleRemoveComponentEvent(@Param(EntityRefParam) EntityRef ref,
                                           @Param(ComponentTypeParam) Class<? extends Component> componentType)
    {
        final Map<EntityRef, Component> row;

        final Component instance;
        Aspect oldAspect = null;
        Aspect newAspect = null;
        boolean fireEvents = false;

        myWriteLock.lock();
        try
        {
            row = myComponentDatabase.get(componentType);

            if (row != null)
            {
                instance = row.get(ref);

                if (instance != null)
                {
                    oldAspect = getAspect(ref);
                    newAspect = oldAspect.remove(componentType);

                    row.remove(ref);

                    fireEvents = true;
                }
            }
        }
        finally
        {
            myWriteLock.unlock();
        }

        if (fireEvents)
        {
            fireComponentRemovedEvent(ref, componentType);
            fireAspectLostEvent(ref, newAspect, oldAspect);
        }
    }

    @Handle(PullComponentEvent.class)
    public void handlePullComponentEvent(@Param(EntityRefParam) EntityRef ref,
                                         @Param(ComponentParam) Component component)
    {
        final AbstractComponent newComponent = (AbstractComponent) component;
        final Map<EntityRef, Component> row;
        final AbstractComponent instance;
        Component oldComponent = null;
        boolean fireEvents = false;

        myWriteLock.lock();
        try
        {
                instance = getComponentFromDatabase(ref, component.type());

                if (instance != null)
                {
                    oldComponent = newComponent.copy();
                    newComponent.copy(instance);

                    fireEvents = true;
                }
        }
        finally
        {
            myWriteLock.unlock();
        }

        if (fireEvents)
        {
            fireComponentPulledEvent(ref, newComponent, oldComponent);
        }
    }

    @Handle(PushComponentEvent.class)
    public void handlePushComponentEvent(@Param(EntityRefParam) EntityRef ref,
                                         @Param(ComponentParam) Component newComponent)
    {
        final AbstractComponent instance;
        Component oldComponent = null;
        boolean fireEvents = false;

        myWriteLock.lock();
        try
        {
            instance = getComponentFromDatabase(ref, newComponent.type());

            if (instance != null)
            {
                oldComponent = instance.copy();
                instance.copy(newComponent);

                fireEvents = true;
            }
        }
        finally
        {
            myWriteLock.unlock();
        }

        if (fireEvents)
        {
            fireComponentPushedEvent(ref, newComponent, oldComponent);
        }
    }

    @Override
    public void dispose(Context context)
    {
        myWriteLock.lock();
        try
        {
            myComponentClassMap.clear();
            for (Map<EntityRef, Component> row : myComponentDatabase.values())
            {
                row.clear();
            }
            myComponentDatabase.clear();
        }
        finally
        {
            myWriteLock.unlock();
        }
    }

    private <T extends Component> AbstractComponent getComponentFromDatabase(EntityRef ref, Class<T> type)
    {
        final Map<EntityRef, Component> row = myComponentDatabase.get(type);

        if (row != null)
        {
            return (AbstractComponent) row.get(ref);
        }

        return null;
    }

    private void fireComponentPreparedEvent(Class<? extends Component> type)
    {
        myEventManager.post(ComponentPreparedEvent.class,
            param(ContextParam, myContext),
            param(ComponentTypeParam, type)
        );
    }

    private void fireComponentCreatedEvent(Component component)
    {
        myEventManager.post(ComponentCreatedEvent.class,
            param(ContextParam, myContext),
            param(ComponentTypeParam, component.type()),
            param(ComponentParam, component)
        );
    }

    private void fireComponentAddedEvent(EntityRef ref, Class<? extends Component> type)
    {
        myEventManager.post(ComponentAddedEvent.class,
            param(ContextParam, myContext),
            param(EntityRefParam, ref),
            param(EntityPathParam, ref.getPath()),
            param(ComponentTypeParam, type)
        );
    }

    private void fireComponentRemovedEvent(EntityRef ref, Class<? extends Component> type)
    {
        myEventManager.post(ComponentRemovedEvent.class,
            param(ContextParam, myContext),
            param(EntityRefParam, ref),
            param(EntityPathParam, ref.getPath()),
            param(ComponentTypeParam, type)
        );
    }

    private void fireComponentPulledEvent(EntityRef ref, Component newComponent, Component oldComponent)
    {
        myEventManager.post(ComponentPulledEvent.class,
            param(ContextParam, myContext),
            param(EntityRefParam, ref),
            param(EntityPathParam, ref.getPath()),
            param(ComponentTypeParam, newComponent.type()),
            param(ComponentParam, newComponent),
            param(OldComponentParam, oldComponent)
        );
    }

    private void firePushComponentEvent(EntityRef ref, Component component)
    {
        myEventManager.send(PushComponentEvent.class,
            param(ContextParam, myContext),
            param(EntityRefParam, ref),
            param(EntityPathParam, ref.getPath()),
            param(ComponentParam, component),
            param(ComponentTypeParam, component.type())
        );
    }

    private void fireComponentPushedEvent(EntityRef ref, Component newComponent, Component oldComponent)
    {
        myEventManager.post(ComponentPushedEvent.class,
            param(ContextParam, myContext),
            param(ContextParam, myContext),
            param(EntityRefParam, ref), param(EntityPathParam, ref.getPath()),
            param(ComponentTypeParam, newComponent.type()),
            param(ComponentParam, newComponent),
            param(OldComponentParam, oldComponent)
        );
    }

    private void fireAspectGainedEvent(EntityRef ref, Aspect newAspect, Aspect oldAspect)
    {
        myEventManager.post(AspectGainedEvent.class,
            param(ContextParam, myContext),
            param(EntityPathParam, ref.getPath()),
            param(EntityRefParam, ref),
            param(NewAspectParam, newAspect),
            param(OldAspectParam, oldAspect)
        );
    }

    private void fireAspectLostEvent(EntityRef ref, Aspect newAspect, Aspect oldAspect)
    {
        myEventManager.post(AspectLostEvent.class,
            param(ContextParam, myContext),
            param(EntityPathParam, ref.getPath()),
            param(EntityRefParam, ref),
            param(NewAspectParam, newAspect),
            param(OldAspectParam, oldAspect)
        );
    }
}
