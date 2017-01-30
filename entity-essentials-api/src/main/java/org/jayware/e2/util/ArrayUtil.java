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
package org.jayware.e2.util;

import java.lang.reflect.Array;
import java.util.Arrays;


public class ArrayUtil
{
    public static <T> T[] newArray(T[] reference, int length)
    {
        Class<?> type = reference.getClass().getComponentType();
        T[] result = (T[]) Array.newInstance(type, length);
        return result;
    }

    public static <T> T[] concat(T element, T[] array)
    {
        T[] result = newArray(array, array.length + 1);
        result[0] = element;
        System.arraycopy(array, 0, result, 1, array.length);
        return result;
    }

    public static <T> T[] concat(T[] array, T element)
    {
        T[] result = Arrays.copyOf(array, array.length + 1);
        result[array.length] = element;
        return result;
    }
}
