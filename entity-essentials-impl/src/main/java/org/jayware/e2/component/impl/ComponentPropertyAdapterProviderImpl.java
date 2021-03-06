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

import org.jayware.e2.component.api.ComponentPropertyAdapter;
import org.jayware.e2.component.api.ComponentPropertyAdapterInstantiationException;
import org.jayware.e2.component.api.ComponentPropertyAdapterProvider;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.jayware.e2.util.Preconditions.checkNotNull;
import static org.jayware.e2.util.TypeUtil.getTypeName;


public class ComponentPropertyAdapterProviderImpl
implements ComponentPropertyAdapterProvider
{
    private final Map<Class<?>, ComponentPropertyAdapter<?>> myPropertyAdapterMap;

    public ComponentPropertyAdapterProviderImpl()
    {
        myPropertyAdapterMap = new ConcurrentHashMap<Class<?>, ComponentPropertyAdapter<?>>();
    }

    @Override
    public void registerPropertyAdapter(Class<? extends ComponentPropertyAdapter> adapterClass)
    throws ComponentPropertyAdapterInstantiationException
    {
        checkNotNull(adapterClass);

        try
        {
            final Class adaptedType = resolveAdaptedType(adapterClass);
            final ComponentPropertyAdapter adapterInstance = adapterClass.newInstance();

            if (!myPropertyAdapterMap.containsKey(adaptedType))
            {
                myPropertyAdapterMap.put(adaptedType, adapterInstance);
            }
        }
        catch (Exception e)
        {
            throw new ComponentPropertyAdapterInstantiationException(e);
        }
    }

    @Override
    public void unregisterPropertyAdapter(Class<? extends ComponentPropertyAdapter> adapterClass)
    {
        checkNotNull(adapterClass);
        myPropertyAdapterMap.remove(resolveAdaptedType(adapterClass));
    }

    @Override
    public <T> ComponentPropertyAdapter<T> getAdapterFor(Class<T> type)
    {
        return (ComponentPropertyAdapter<T>) myPropertyAdapterMap.get(type);
    }

    private Class resolveAdaptedType(Class<? extends ComponentPropertyAdapter> adapterClass)
    {
        for (Type type : adapterClass.getGenericInterfaces())
        {
            if (type instanceof ParameterizedType)
            {
                ParameterizedType parameterizedType = (ParameterizedType) type;

                if (getTypeName(parameterizedType.getRawType()).equals(getTypeName(ComponentPropertyAdapter.class)))
                {
                    return (Class) parameterizedType.getActualTypeArguments()[0];
                }
            }
        }

        throw new IllegalArgumentException();
    }
}
