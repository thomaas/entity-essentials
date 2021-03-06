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


/**
 * EventDispatcher
 *
 * @since 1.0
 *
 * @see Event
 * @see EventManager
 */
public interface EventDispatcher
{
    /**
	 * Dispatches the specified {@link Event} to the specified target.
     *
	 * @param event an {@link Event}
	 * @param target a target.
	 */
	void dispatch(Event event, Object target);

    /**
     * Returns whether this {@link EventDispatcher} accepts the specified {@link EventType}.
     *
     * @param type an {@link EventType}'s {@link Class}
     *
     * @return true if this {@link EventDispatcher} accepts the specified {@link EventType}, otherwise false.
     */
    boolean accepts(Class<? extends EventType> type);
}