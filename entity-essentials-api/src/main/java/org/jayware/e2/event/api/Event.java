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
package org.jayware.e2.event.api;


import java.util.UUID;


/**
 * Base <code>Event</code>.
 *
 * @see EventManager
 * @see EventFilter
 * @see Handle
 * @see Parameters
 */
public interface Event
{
    /**
     * Returns the Id of this {@link Event}.
     *
     * @return a {@link UUID} representing this {@link Event}'s Id.
     */
    UUID getId();

    /**
     * Returns the {@link EventType} of this {@link Event}.
     *
     * @return this {@link Event}'s {@link EventType}.
     */
    Class<? extends EventType> getType();

    /**
     * Returns whether this {@link Event} matches the specified {@link EventType}.
     * <p>
     * An {@link Event} matches a specific {@link EventType} if the {@link Event}'s {@link EventType} is assignable
     * to the specific one. The {@link Class#isAssignableFrom(Class)} operation is used to determine if the specified
     * {@link Class} is either the same as, or is a superclass/superinterface of the class of this {@link Event}'s
     * {@link EventType}.
     *
     * @param type a {@link EventType}.
     *
     * @return <code>true</code> if this {@link Event} matches the specified {@link EventType},
     *         otherwise <code>false</code>.
     */
    boolean matches(Class<? extends EventType> type);

    /**
     * Returns the value of the parameter with the specified name or <code>null</code>.
     *
     * @param name the name of the parameter.
     * @param <V> the type of the parameter.
     *
     * @return the value of the parameter or <code>null</code> if the value is <code>null</code>
     *         or this {@link Event} does not have a parameter with the specified name.
     */
    <V> V getParameter(String name);

    /**
     * Returns whether this {@link Event} carries a parameter with the specified name.
     *
     * @param name the name of the paramter.
     *
     * @return <code>true</code> if this {@link Event} carries a parameter with the specified name,
     *         otherwise <code>false</code>.
     */
    boolean hasParameter(String name);

    /**
     * Returns {@link ReadOnlyParameters}.
     *
     * @return {@link ReadOnlyParameters}
     */
    ReadOnlyParameters getParameters();

    /**
     * Returns whether this {@link Event} is a {@link Query}.
     *
     * <b>Note:</b> This operation returns the opposite of {@link Event#isNotQuery()}
     *
     * @return <code>true</code> if this {@link Event} is a {@link Query}, otherwise <code>false</code>.
     */
    boolean isQuery();

    /**
     * Returns whether this {@link Event} is <u>not</u> a {@link Query}.
     *
     * <b>Note:</b> This operation returns the opposite of {@link Event#isQuery()} ()}
     *
     * @return <code>true</code> if this {@link Event} isn't a {@link Query}, otherwise <code>false</code>.
     */
    boolean isNotQuery();
}