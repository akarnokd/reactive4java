/*
 * Copyright 2013 akarnokd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hu.akarnokd.reactive4java8.util;

/**
 * A simple reference of a double value.
 * @author akarnokd, 2013.11.09.
 */
public class DoubleRef {
    /** The value. */
    public double value;
    public static DoubleRef of(double initial) {
        DoubleRef r = new DoubleRef();
        r.value = initial;
        return r;
    }
}
