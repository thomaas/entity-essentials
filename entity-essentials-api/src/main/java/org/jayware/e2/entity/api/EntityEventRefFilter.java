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
package org.jayware.e2.entity.api;

import org.jayware.e2.context.api.Context;
import org.jayware.e2.event.api.Event;
import org.jayware.e2.event.api.EventFilter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.jayware.e2.entity.api.EntityEvent.EntityRefParam;
import static org.jayware.e2.util.Preconditions.checkNotNull;


/**
 * This {@link EventFilter} accepts {@link Event}s of the {@link EntityEvent} type which carry an {@link EntityRef}
 * packed as {@link EntityEvent#EntityRefParam} which is contained in the include-set and <u>not</u> contained in the
 * exclude set.
 * <p>
 * <b>Note:</b> Instances of this class are <u>immutable</u>. Each subsequent call to one of the include/exclude
 * operations will return a <u>new</u> instance of an {@link EntityEventRefFilter}. The recommended way to create an
 * {@link EntityEventRefFilter} is to use its API in a fluent manner like:
 *<pre>
 * {@code EntityEventRefFilter filter = filterEntities().include(refA, refB).exclude(refC);}
 * </pre>
 */
public class EntityEventRefFilter
implements EventFilter
{
    private final Set<EntityRef> myIncludes;
    private final Set<EntityRef> myExcludes;

    private EntityEventRefFilter(Collection<EntityRef> includes, Collection<EntityRef> excludes)
    {
        myIncludes = new HashSet<EntityRef>(includes);
        myExcludes = new HashSet<EntityRef>(excludes);
    }

    /**
     * Creates an {@link EntityEventRefFilter} with the specified {@link EntityRef}s as include-set.
     *
     * @param refs a var-arg of {@link EntityRef} or <code>null</code>.
     *
     * @return an {@link EntityEventRefFilter}.
     */
    public static EntityEventRefFilter filterEntities(EntityRef... refs)
    {
        final Collection<EntityRef> includes;

        if (refs == null)
        {
            includes = Collections.<EntityRef>emptyList();
        }
        else
        {
            includes = Arrays.<EntityRef>asList(refs);
        }

        return new EntityEventRefFilter(includes, Collections.<EntityRef>emptyList());
    }

    /**
     * Creates an {@link EntityEventRefFilter} which additionally includes the specified {@link EntityRef}s.
     *
     * @param refs a var-arg of {@link EntityRef} <u>not</u> <code>null</code>.
     *
     * @return an {@link EntityEventRefFilter}.
     *
     * @throws IllegalArgumentException if the specified var-arg is <code>null</code>.
     */
    public EntityEventRefFilter include(EntityRef... refs) throws IllegalArgumentException
    {
        checkNotNull(refs);

        return new EntityEventRefFilter(combine(myIncludes, refs), myExcludes);
    }

    /**
     * Creates an {@link EntityEventRefFilter} which additionally excludes the specified {@link EntityRef}s.
     *
     * @param refs a var-arg of {@link EntityRef} <u>not</u> <code>null</code>.
     *
     * @return an {@link EntityEventRefFilter}.
     *
     * @throws IllegalArgumentException if the specified var-arg is <code>null</code>.
     */
    public EntityEventRefFilter exclude(EntityRef... refs) throws IllegalArgumentException
    {
        checkNotNull(refs);

        return new EntityEventRefFilter(myIncludes, combine(myExcludes, refs));
    }

    @Override
    public boolean accepts(Context context, Event event)
    {
        if (EntityEvent.class.isAssignableFrom(event.getType()))
        {
            final EntityRef ref = event.getParameter(EntityRefParam);

            if (ref != null)
            {
                if (myExcludes.isEmpty())
                {
                    return myIncludes.contains(ref);
                }
                else
                {
                    if (myIncludes.isEmpty())
                    {
                        return !myExcludes.contains(ref);
                    }
                    else
                    {
                        return myIncludes.contains(ref) && !myExcludes.contains(ref);
                    }
                }
            }
        }

        return false;
    }

    private static Collection<EntityRef> combine(Collection<EntityRef> collection, EntityRef[] refs)
    {
        final Set<EntityRef> includes = new HashSet<EntityRef>();
        includes.addAll(collection);
        includes.addAll(Arrays.<EntityRef>asList(refs));

        return includes;
    }
}
