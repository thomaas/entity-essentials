/**
 * Entity Essentials -- A Component-based Entity System
 *
 * Copyright (C) 2017 Elmar Schug <elmar.schug@jayware.org>,
 *                    Markus Neubauer <markus.neubauer@jayware.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jayware.e2.assembly.impl;


import org.jayware.e2.assembly.api.TreeManager;
import org.jayware.e2.assembly.api.TreeNode;
import org.jayware.e2.context.api.Context;
import org.jayware.e2.context.api.ContextProvider;
import org.jayware.e2.entity.api.EntityRef;
import org.jayware.e2.entity.api.InvalidEntityRefException;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;


public class TreeManagerTest
{
    private TreeManager testee;

    private Context testContext;

    private @Mock TreeNode testNode;

    private @Mock EntityRef testEntityRef;
    private @Mock EntityRef testNodeEntityRef;

    @BeforeMethod
    public void setUp()
    {
        initMocks(this);

        testee = new TreeManagerImpl();

        testContext = ContextProvider.getInstance().createContext();

        for (EntityRef ref : asList(testEntityRef, testNodeEntityRef))
        {
            when(ref.isValid()).thenReturn(true);
            when(ref.isInvalid()).thenReturn(false);
            when(ref.getContext()).thenReturn(testContext);
            when(ref.belongsTo(testContext)).thenReturn(true);
        }

        when(testNode.getNodeRef()).thenReturn(testNodeEntityRef);
        when(testNode.getContext()).thenReturn(testContext);
    }

    @Test
    public void test_createTreeNode_ReturnsNotNull()
    {
        assertThat(testee.createTreeNodeFor(testEntityRef)).isNotNull();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test_createTreeNode_ThrowsIllegalArgumentExceptionIfPassedRefIsNull()
    {
        testee.createTreeNodeFor(null);
    }

    @Test(expectedExceptions = InvalidEntityRefException.class)
    public void test_createTreeNode_ThrowsInvalidEntityRefExceptionIfPassedRefIsInvalid()
    {
        final EntityRef ref = mock(EntityRef.class);
        when(ref.isValid()).thenReturn(false);
        when(ref.isInvalid()).thenReturn(true);
        when(ref.getContext()).thenReturn(testContext);

        testee.createTreeNodeFor(ref);
    }

    @Test
    public void test_deleteTreeNode()
    {
        testee.deleteTreeNode(testNode);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test_deleteTreeNode_ThrowsIllegalArgumentExceptionIfPassedTreeNodeIsNull()
    {
        testee.deleteTreeNode(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test_findChildrenOf_Throws_IllegalArgumentException_if_null_is_passed()
    {
        testee.findChildrenOf(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test_queryChildrenOf_Throws_IllegalArgumentException_if_null_is_passed()
    {
        testee.queryChildrenOf(null);
    }
}
