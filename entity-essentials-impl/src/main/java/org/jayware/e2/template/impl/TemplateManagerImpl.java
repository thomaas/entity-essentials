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
package org.jayware.e2.template.impl;

import org.jayware.e2.component.api.AbstractComponent;
import org.jayware.e2.component.api.Component;
import org.jayware.e2.component.api.ComponentManager;
import org.jayware.e2.context.api.Context;
import org.jayware.e2.entity.api.EntityRef;
import org.jayware.e2.template.api.ComponentTemplate;
import org.jayware.e2.template.api.ExportException;
import org.jayware.e2.template.api.ImportException;
import org.jayware.e2.template.api.PropertyTemplate;
import org.jayware.e2.template.api.TemplateManager;
import org.jayware.e2.template.api.TemplateProvider;

import java.util.List;

import static org.jayware.e2.context.api.Preconditions.checkContextNotNullAndNotDisposed;
import static org.jayware.e2.entity.api.Preconditions.checkRefNotNullAndValid;
import static org.jayware.e2.util.Preconditions.checkNotNull;


public class TemplateManagerImpl
implements TemplateManager
{
    @Override
    public <T extends ComponentTemplate> T exportComponent(Component component, TemplateProvider provider)
    throws IllegalArgumentException, IllegalStateException, ExportException
    {
        checkNotNull(component, "Component to export mustn't be null!");
        checkNotNull(provider, "To export a Component the TemplateProvider mustn't be null!");

        try
        {
            final ComponentTemplate result = provider.createComponentTemplate(component.type());
            final AbstractComponent internal = (AbstractComponent) component;

            result.setType(component.type());

            final List<String> names = internal.getPropertyNames();
            final List<Class> types = internal.getPropertyTypes();

            for (int i = 0; i < names.size(); ++i)
            {
                final String name = names.get(i);
                final Class type = types.get(i);

                PropertyTemplate propertyTemplate = provider.createPropertyTemplate(Object.class);
                propertyTemplate.setName(name);
                propertyTemplate.setType(type);
                propertyTemplate.setValue(internal.get(name));
                result.properties().add(propertyTemplate);
            }

            return (T) result;
        }
        catch (Exception e)
        {
            throw new ExportException(e);
        }
    }

    @Override
    public <T extends ComponentTemplate> T exportComponent(EntityRef ref, Class<? extends Component> type, TemplateProvider provider)
    throws IllegalArgumentException, IllegalStateException, ExportException
    {
        checkRefNotNullAndValid(ref);
        checkNotNull(type, "To export a Component the type mustn't be null!");
        checkNotNull(provider, "To export a Component the TemplateProvider mustn't be null!");

        final Component component;

        try
        {
            final Context context = ref.getContext();
            final ComponentManager componentManager = context.getService(ComponentManager.class);
            component = componentManager.getComponent(ref, type);
        }
        catch (Exception e)
        {
            throw new ExportException(e);
        }

        return exportComponent(component, provider);
    }

    @Override
    public <T extends ComponentTemplate> Component importComponent(Context context, T template)
    throws IllegalArgumentException, IllegalStateException, ImportException
    {
        checkContextNotNullAndNotDisposed(context);
        checkNotNull(template, "To import a Component the template mustn't be null!");

        try
        {
            final ComponentManager componentManager = context.getService(ComponentManager.class);
            final AbstractComponent component = (AbstractComponent) componentManager.createComponent(context, template.getType());

            final List<PropertyTemplate> properties = template.properties();
            for (PropertyTemplate property : properties)
            {
                component.set(property.getName(), property.getValue());
            }

            return component;
        }
        catch (Exception e)
        {
            throw new ImportException(e);
        }
    }

    @Override
    public <T extends ComponentTemplate> Component importComponent(EntityRef ref, T template)
    throws IllegalArgumentException, IllegalStateException, ImportException
    {
        checkRefNotNullAndValid(ref);
        checkNotNull(template, "To import a Component the template mustn't be null!");

        final Component component = importComponent(ref.getContext(), template);

        try
        {
            component.addTo(ref);
            return component;
        }
        catch (Exception e)
        {
            throw new ImportException(e);
        }
    }
}
