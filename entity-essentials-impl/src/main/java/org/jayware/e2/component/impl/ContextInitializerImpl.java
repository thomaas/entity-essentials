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
package org.jayware.e2.component.impl;

import org.jayware.e2.component.api.ComponentFactory;
import org.jayware.e2.context.api.Context;
import org.jayware.e2.context.api.ContextInitializer;

import static org.jayware.e2.component.impl.ComponentManagerImpl.COMPONENT_FACTORY;
import static org.jayware.e2.component.impl.ComponentManagerImpl.COMPONENT_STORE;
import static org.jayware.e2.component.impl.ComponentManagerImpl.PROPERTY_ADAPTER_PROVIDER;


public class ContextInitializerImpl
implements ContextInitializer
{
    @Override
    public void initialize(Context context)
    {
        final ComponentFactoryImpl componentFactory = new ComponentFactoryImpl();
        context.put(COMPONENT_FACTORY, componentFactory);
        context.put(ComponentFactory.class, componentFactory);
        context.put(COMPONENT_STORE, new ComponentStore(context));
        context.put(PROPERTY_ADAPTER_PROVIDER, new ComponentPropertyAdapterProviderImpl());
    }
}
