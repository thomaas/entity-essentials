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
package org.jayware.e2.context.impl;

import com.googlecode.concurentlocks.ReadWriteUpdateLock;
import com.googlecode.concurentlocks.ReentrantReadWriteUpdateLock;
import org.jayware.e2.context.api.Context;
import org.jayware.e2.context.api.Disposable;
import org.jayware.e2.context.api.ServiceProvider;
import org.jayware.e2.context.api.ServiceUnavailableException;
import org.jayware.e2.util.Key;
import org.jayware.e2.util.ObjectUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;

import static org.jayware.e2.util.Key.createKey;
import static org.jayware.e2.util.Preconditions.checkNotNull;


public class ContextImpl
implements Context
{
    private final UUID myContextId;

    private final AtomicReference<Context> myContextState = new AtomicReference<Context>();

    public ContextImpl(ServiceProvider serviceProvider)
    {
        myContextId = UUID.randomUUID();
        myContextState.set(new DefaultContext(serviceProvider));
    }

    @Override
    public UUID getId()
    {
        return myContextId;
    }

    @Override
    public void dispose()
    {
        myContextState.get().dispose();
    }

    @Override
    public boolean isDisposed()
    {
        return myContextState.get().isDisposed();
    }

    @Override
    public <T> void put(Key<T> key, T value)
    {
        myContextState.get().put(key, value);
    }

    @Override
    public <T, I extends T> void put(Class<T> type, I value)
    {
        myContextState.get().put(type, value);
    }

    @Override
    public <T> boolean putIfAbsent(Key<T> key, T value)
    {
        return myContextState.get().putIfAbsent(key, value);
    }

    @Override
    public <T> boolean putIfAbsent(Key<T> key, ValueProvider<T> valueProvider)
    {
        return myContextState.get().putIfAbsent(key, valueProvider);
    }

    @Override
    public <T> void remove(Key<T> key)
    {
        myContextState.get().remove(key);
    }

    @Override
    public <T> T get(Key<T> key)
    {
        return myContextState.get().get(key);
    }

    @Override
    public <T> T get(Key<T> key, T defaultValue)
    {
        return myContextState.get().get(key, defaultValue);
    }

    @Override
    public boolean contains(Key key)
    {
        return myContextState.get().contains(key);
    }

    @Override
    public <S> S getService(Class<? extends S> service)
    {
        return myContextState.get().getService(service);
    }

    @Override
    public <S> S findService(Class<? extends S> service)
    {
        return myContextState.get().findService(service);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        final ContextImpl context = (ContextImpl) o;
        return ObjectUtil.equal(myContextId, context.myContextId);
    }

    @Override
    public int hashCode()
    {
        return ObjectUtil.hashCode(myContextId);
    }

    @Override
    public String toString()
    {
        final StringBuffer sb = new StringBuffer("ContextImpl{");
        sb.append("myContextId=").append(myContextId);
        sb.append(", myContextState=").append(myContextState);
        sb.append('}');
        return sb.toString();
    }

    private class DefaultContext
    implements Context
    {
        private final ServiceProvider myServiceProvider;

        private final ReadWriteUpdateLock myLock = new ReentrantReadWriteUpdateLock();
        private final Lock myReadLock = myLock.readLock();
        private final Lock myWriteLock = myLock.writeLock();
        private final Lock myUpdateLock = myLock.updateLock();

        private final Map<Key, Object> myMap;
        private boolean isDisposing = false;

        public DefaultContext(ServiceProvider serviceProvider)
        {
            myServiceProvider = serviceProvider;
            myMap = new HashMap<Key, Object>();
        }

        @Override
        public UUID getId()
        {
            return myContextId;
        }

        @Override
        public void dispose()
        {
            myWriteLock.lock();
            try
            {
                if (!isDisposing)
                {
                    isDisposing = true;

                    final HashSet toDispose = new HashSet(myMap.values());

                    myWriteLock.unlock();

                    for (Object obj : toDispose)
                    {
                        if (obj instanceof Disposable)
                        {
                            ((Disposable) obj).dispose(ContextImpl.this);
                        }
                    }

                    myContextState.set(new DisposedContext());

                    myWriteLock.lock();
                    myMap.clear();
                }
            }
            finally
            {
                myWriteLock.unlock();
            }
        }

        @Override
        public boolean isDisposed()
        {
            return false;
        }

        @Override
        public <T> void put(Key<T> key, T value)
        {
            checkNotNull(key, "Key mustn't be null!");
            myWriteLock.lock();
            try
            {
                checkDisposing();
                myMap.put(key, value);
            }
            finally
            {
                myWriteLock.unlock();
            }
        }

        @Override
        public <T, I extends T> void put(Class<T> type, I value)
        {
            checkNotNull(type, "Key mustn't be null!");
            put(createKey(type.getName()), value);
        }

        @Override
        public <T> boolean putIfAbsent(Key<T> key, T value)
        {
            checkNotNull(key, "Key mustn't be null!");

            myWriteLock.lock();
            try
            {
                if (!myMap.containsKey(key))
                {
                    checkDisposing();
                    myMap.put(key, value);
                    return true;
                }

                return false;
            }
            finally
            {
                myWriteLock.unlock();
            }
        }

        @Override
        public <T> boolean putIfAbsent(Key<T> key, ValueProvider<T> valueProvider)
        {
            checkNotNull(key, "Key mustn't be null!");
            checkNotNull(valueProvider, "ValueProvider mustn't be null!");

            myWriteLock.lock();
            try
            {
                if (!myMap.containsKey(key))
                {
                    checkDisposing();
                    myMap.put(key, valueProvider.provide(ContextImpl.this));
                    return true;
                }

                return false;
            }
            finally
            {
                myWriteLock.unlock();
            }
        }

        @Override
        public <T> void remove(Key<T> key)
        {
            checkNotNull(key, "Key mustn't be null!");
            myUpdateLock.lock();
            try
            {
                checkDisposing();
                myMap.remove(key);
            }
            finally
            {
                myUpdateLock.unlock();
            }
        }

        @Override
        public <T> T get(Key<T> key)
        {
            checkNotNull(key, "Key mustn't be null!");
            myUpdateLock.lock();
            try
            {
                return (T) myMap.get(key);
            }
            finally
            {
                myUpdateLock.unlock();
            }
        }

        @Override
        public <T> T get(Key<T> key, T defaultValue)
        {
            checkNotNull(key, "Key mustn't be null!");
            myUpdateLock.lock();
            try
            {
                final T value = (T) myMap.get(key);
                return value != null ? value : defaultValue;
            }
            finally
            {
                myUpdateLock.unlock();
            }
        }

        @Override
        public boolean contains(Key key)
        {
            myUpdateLock.lock();
            try
            {
                return myMap.containsKey(key);
            }
            finally
            {
                myUpdateLock.unlock();
            }
        }

        @Override
        public <S> S getService(Class<? extends S> service)
        {
            final S instance = findService(service);

            if (instance == null)
            {
                throw new ServiceUnavailableException(service);
            }

            return instance;
        }

        @Override
        public <S> S findService(Class<? extends S> service)
        {
            myUpdateLock.lock();
            try
            {
                Object instance = myMap.get(createKey(service.getName()));

                if (instance == null || !service.isAssignableFrom(instance.getClass()))
                {
                    instance = myServiceProvider.findService(service);
                    myWriteLock.lock();
                    try
                    {
                        myMap.put(createKey(service.getName()), instance);
                    }
                    finally
                    {
                        myWriteLock.unlock();
                    }
                }

                return (S) instance;
            }
            finally
            {
                myUpdateLock.unlock();
            }
        }

        private void checkDisposing()
        {
            if (isDisposing)
            {
                throw new IllegalStateException("Context is goning to be disposed!");
            }
        }
    }

    private class DisposedContext
    implements Context
    {
        @Override
        public UUID getId()
        {
            return myContextId;
        }

        @Override
        public void dispose()
        {
        }

        @Override
        public boolean isDisposed()
        {
            return true;
        }

        @Override
        public <T> void put(Key<T> key, T value)
        {
            throw new IllegalStateException("Context is disposed!");
        }

        @Override
        public <T, I extends T> void put(Class<T> type, I value)
        {
            throw new IllegalStateException("Context is disposed!");
        }

        @Override
        public <T> boolean putIfAbsent(Key<T> key, T value)
        {
            throw new IllegalStateException("Context is disposed!");
        }

        @Override
        public <T> boolean putIfAbsent(Key<T> key, ValueProvider<T> valueProvider)
        {
            throw new IllegalStateException("Context is disposed!");
        }

        @Override
        public <T> void remove(Key<T> key)
        {
            throw new IllegalStateException("Context is disposed!");
        }

        @Override
        public <T> T get(Key<T> key)
        {
            throw new IllegalStateException("Context is disposed!");
        }

        @Override
        public <T> T get(Key<T> key, T defaultValue)
        {
            throw new IllegalStateException("Context is disposed!");
        }

        @Override
        public boolean contains(Key key)
        {
            throw new IllegalStateException("Context is disposed!");
        }

        @Override
        public <S> S getService(Class<? extends S> service)
        {
            throw new IllegalStateException("No services available. Context is disposed!");

        }

        @Override
        public <S> S findService(Class<? extends S> service)
        {
            throw new IllegalStateException("No services available. Context is disposed!");
        }
    }
}