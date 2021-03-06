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
package org.jayware.e2.component.api;

import org.jayware.e2.context.api.Context;
import org.jayware.e2.entity.api.Entity;
import org.jayware.e2.entity.api.EntityRef;
import org.jayware.e2.util.ObjectUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;
import static java.util.Collections.addAll;
import static org.jayware.e2.entity.api.Preconditions.checkRefNotNullAndValid;
import static org.jayware.e2.util.ObjectUtil.equal;
import static org.jayware.e2.util.Preconditions.checkNotNull;


public class Aspect
{
    /**
     * Matches always any {@link Entity}.
     */
    public static final Aspect ANY = new Aspect()
    {
        public boolean matches(EntityRef ref)
        {
            return true;
        }
    };

    /**
     * Matches {@link Entity Entities} which doesn't have any {@link Component}.
     */
    public static final Aspect EMPTY = new Aspect()
    {
        public boolean matches(EntityRef ref)
        {
            checkRefNotNullAndValid(ref);

            final ComponentManager componentManager = ref.getContext().getService(ComponentManager.class);
            return componentManager.getNumberOfComponents(ref) == 0;
        }
    };

    protected final Set<Class<? extends Component>> myIntersectionSet;
    protected final Set<Class<? extends Component>> myUnificationSet;
    protected final Set<Class<? extends Component>> myDifferenceSet;

    protected final String myToString;

    protected Aspect()
    {
        myIntersectionSet = Collections.<Class<? extends Component>>emptySet();
        myUnificationSet = Collections.<Class<? extends Component>>emptySet();
        myDifferenceSet = Collections.<Class<? extends Component>>emptySet();

        myToString = toString(this);
    }

    protected Aspect(Set<Class<? extends Component>> intersectionSet, Set<Class<? extends Component>> unificationSet, Set<Class<? extends Component>> differenceSet)
    {
        this.myIntersectionSet = Collections.<Class<? extends Component>>unmodifiableSet(new HashSet<Class<? extends Component>>(intersectionSet));
        this.myUnificationSet = Collections.<Class<? extends Component>>unmodifiableSet(new HashSet<Class<? extends Component>>(unificationSet));
        this.myDifferenceSet = Collections.<Class<? extends Component>>unmodifiableSet(new HashSet<Class<? extends Component>>(differenceSet));

        myToString = toString(this);
    }

    public static Aspect aspect()
    {
        return EMPTY;
    }

    public static Aspect aspect(Class<? extends Component>... components)
    {
        return aspect(combine(Collections.<Class<? extends Component>>emptySet(), components), Collections.<Class<? extends Component>>emptySet(), Collections.<Class<? extends Component>>emptySet());
    }

    public static Aspect aspect(Set<Class<? extends Component>> components)
    {
        return aspect(components, Collections.<Class<? extends Component>>emptySet(), Collections.<Class<? extends Component>>emptySet());
    }

    public static Aspect aspect(Set<Class<? extends Component>> allOf, Set<Class<? extends Component>> oneOf, Set<Class<? extends Component>> noneOf)
    {
        return new Aspect(allOf, oneOf, noneOf);
    }

    public Aspect withAllOf(Class<? extends Component>... components) throws IllegalArgumentException, IllegalAspectException
    {
        checkNotNull(components, "Components mustn't be null!");

        return checked(new Aspect(combine(myIntersectionSet, components), myUnificationSet, myDifferenceSet));
    }

    public Aspect withAllOf(Collection<Class<? extends Component>> components) throws IllegalArgumentException, IllegalAspectException
    {
        checkNotNull(components, "Components mustn't be null!");

        return checked(new Aspect(combine(myIntersectionSet, components), myUnificationSet, myDifferenceSet));
    }

    public Aspect withOneOf(Class<? extends Component>... components) throws IllegalArgumentException, IllegalAspectException
    {
        checkNotNull(components, "Components mustn't be null!");

        return checked(new Aspect(myIntersectionSet, combine(myUnificationSet, components), myDifferenceSet));
    }

    public Aspect withOneOf(Collection<Class<? extends Component>> components) throws IllegalArgumentException, IllegalAspectException
    {
        checkNotNull(components, "Components mustn't be null!");

        return checked(new Aspect(myIntersectionSet, combine(myUnificationSet, components), myDifferenceSet));
    }

    public Aspect withNoneOf(Class<? extends Component>... components) throws IllegalArgumentException, IllegalAspectException
    {
        checkNotNull(components, "Components mustn't be null!");

        return checked(new Aspect(myIntersectionSet, myUnificationSet, combine(myDifferenceSet, components)));
    }

    public Aspect withNoneOf(Collection<Class<? extends Component>> components) throws IllegalArgumentException, IllegalAspectException
    {
        checkNotNull(components, "Components mustn't be null!");

        return checked(new Aspect(myIntersectionSet, myUnificationSet, combine(myDifferenceSet, components)));
    }

    public Aspect and(Aspect other) throws IllegalArgumentException, IllegalAspectException
    {
        return checked(aspect(combine(getIntersectionSet(), other.getIntersectionSet()), combine(getUnificationSet(), other.getUnificationSet()), combine(getDifferenceSet(), other.getDifferenceSet())));
    }

    public boolean matches(EntityRef ref)
    {
        checkRefNotNullAndValid(ref);

        final Context context = ref.getContext();
        final ComponentManager componentManager = context.getService(ComponentManager.class);

        int matchesAllOf = 0;
        int matchesOneOf = 0;
        boolean matchesNoneOf = true;

        for (Class<? extends Component> type : componentManager.getComponentTypes(ref))
        {
            if (myDifferenceSet.contains(type))
            {
                matchesNoneOf = false;
                break;
            }

            if (matchesAllOf < myIntersectionSet.size() && myIntersectionSet.contains(type))
            {
                ++matchesAllOf;
            }

            if (matchesOneOf == 0 && myUnificationSet.contains(type))
            {
                ++matchesOneOf;
            }
        }

        return matchesNoneOf && matchesAllOf == myIntersectionSet.size() && (matchesOneOf > 0 || myUnificationSet.isEmpty());
    }

    public Set<Class<? extends Component>> getIntersectionSet()
    {
        return myIntersectionSet;
    }

    public Set<Class<? extends Component>> getUnificationSet()
    {
        return myUnificationSet;
    }

    public Set<Class<? extends Component>> getDifferenceSet()
    {
        return myDifferenceSet;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!(obj instanceof Aspect))
        {
            return false;
        }

        final Aspect other = (Aspect) obj;
        return equal(getIntersectionSet(), other.getIntersectionSet()) &&
               equal(getUnificationSet(), other.getUnificationSet()) &&
               equal(getDifferenceSet(), other.getDifferenceSet());
    }

    @Override
    public int hashCode()
    {
        return ObjectUtil.hashCode(getIntersectionSet(), getUnificationSet(), getDifferenceSet());
    }

    @Override
    public String toString()
    {
        return myToString;
    }

    public static String toString(Aspect aspect)
    {
        final StringBuilder stringBuilder = new StringBuilder("Aspect ");

        stringBuilder.append("{");

        if (!aspect.myIntersectionSet.isEmpty())
        {
            stringBuilder.append(" allOf: [");

            for (Iterator<Class<? extends Component>> iterator = aspect.myIntersectionSet.iterator(); iterator.hasNext(); )
            {
                final Class<? extends Component> component = iterator.next();
                stringBuilder.append(component.getSimpleName());

                if (iterator.hasNext())
                {
                    stringBuilder.append(", ");
                }
            }

            stringBuilder.append("]");
        }

        if (!aspect.myUnificationSet.isEmpty())
        {
            stringBuilder.append(" oneOf: [");

            for (Iterator<Class<? extends Component>> iterator = aspect.myUnificationSet.iterator(); iterator.hasNext(); )
            {
                final Class<? extends Component> component = iterator.next();
                stringBuilder.append(component.getSimpleName());

                if (iterator.hasNext())
                {
                    stringBuilder.append(", ");
                }
            }

            stringBuilder.append("]");
        }

        if (!aspect.myDifferenceSet.isEmpty())
        {
            stringBuilder.append(" noneOf: [");

            for (Iterator<Class<? extends Component>> iterator = aspect.myDifferenceSet.iterator(); iterator.hasNext(); )
            {
                final Class<? extends Component> component = iterator.next();
                stringBuilder.append(component.getSimpleName());

                if (iterator.hasNext())
                {
                    stringBuilder.append(", ");
                }
            }

            stringBuilder.append("]");
        }

        stringBuilder.append("}");

        return stringBuilder.toString();
    }

    protected static Aspect checked(final Aspect aspect) throws IllegalAspectException
    {
        final Set<Class<? extends Component>> temp = new HashSet<Class<? extends Component>>();

        temp.addAll(aspect.getIntersectionSet());
        temp.retainAll(aspect.getUnificationSet());

        if (!temp.isEmpty())
        {
            failCheck(aspect, temp, "intersection", "unification");
        }

        temp.addAll(aspect.getIntersectionSet());
        temp.retainAll(aspect.getDifferenceSet());

        if (!temp.isEmpty())
        {
            failCheck(aspect, temp, "intersection", "difference");
        }

        temp.addAll(aspect.getUnificationSet());
        temp.retainAll(aspect.getIntersectionSet());

        if (!temp.isEmpty())
        {
            failCheck(aspect, temp, "unification", "intersection");
        }

        temp.addAll(aspect.getUnificationSet());
        temp.retainAll(aspect.getDifferenceSet());

        if (!temp.isEmpty())
        {
            failCheck(aspect, temp, "unification", "difference");
        }

        temp.addAll(aspect.getDifferenceSet());
        temp.retainAll(aspect.getIntersectionSet());

        if (!temp.isEmpty())
        {
            failCheck(aspect, temp, "difference", "intersection");
        }

        temp.addAll(aspect.getDifferenceSet());
        temp.retainAll(aspect.getUnificationSet());

        if (!temp.isEmpty())
        {
            failCheck(aspect, temp, "difference", "unification");
        }

        return aspect;
    }

    private static void failCheck(Aspect aspect, Set<Class<? extends Component>> temp, Object... args)
    {
        final String messageTemplate = "\t    The component %s is part of the %s and the %s set!";

        final StringBuffer message = new StringBuffer();
        for (Class<? extends Component> aClass : temp)
        {
            message.append(format(messageTemplate, aClass.getSimpleName(), args[0], args[1]) + "\n");
        }

        throw new IllegalAspectException(aspect, message.toString());
    }

    protected static Set<Class<? extends Component>> combine(Collection<Class<? extends Component>> set, Class<? extends Component>[] components)
    {
        final Set<Class<? extends Component>> result = new HashSet<Class<? extends Component>>(set);
        addAll(result, components);
        return result;
    }

    protected static Set<Class<? extends Component>> combine(Collection<Class<? extends Component>> a, Collection<Class<? extends Component>> b)
    {
        final Set<Class<? extends Component>> result = new HashSet<Class<? extends Component>>(a);
        result.addAll(b);
        return result;
    }

    protected static Collection<String> collectNames(Collection<Class<? extends Component>> classes)
    {
        final List<String> result = new ArrayList<String>();

        for (Class aClass : classes)
        {
            result.add(aClass.getSimpleName());
        }

        return result;
    }
}