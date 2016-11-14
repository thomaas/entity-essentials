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
package org.jayware.e2.event.impl;

import org.jayware.e2.event.api.EventType;
import org.jayware.e2.event.api.Query;
import org.jayware.e2.event.api.ReadOnlyParameters;
import org.jayware.e2.util.Key;

import java.util.UUID;


public class QueryWrapper
implements Query
{
    private final Query myQuery;
    private final QueryResultSet myResult;

    QueryWrapper(Query query, QueryResultSet result)
    {
        myQuery = query;
        myResult = result;
    }

    @Override
    public <V> void result(String name, V value)
    {
        myResult.put(name, value);
    }

    @Override
    public <V> void result(Key<V> key, V value)
    {
        myResult.put(key, value);
    }

    @Override
    public UUID getId()
    {
        return myQuery.getId();
    }

    @Override
    public Class<? extends EventType> getType()
    {
        return myQuery.getType();
    }

    @Override
    public boolean matches(Class<? extends EventType> type)
    {
        return myQuery.matches(type);
    }

    @Override
    public <V> V getParameter(String parameter)
    {
        return myQuery.getParameter(parameter);
    }

    @Override
    public boolean hasParameter(String parameter)
    {
        return myQuery.hasParameter(parameter);
    }

    @Override
    public ReadOnlyParameters getParameters()
    {
        return myQuery.getParameters();
    }

    @Override
    public boolean isQuery()
    {
        return true;
    }

    public QueryResultSet getResult()
    {
        return myResult;
    }
}