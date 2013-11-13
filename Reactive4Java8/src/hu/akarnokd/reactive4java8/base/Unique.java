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
package hu.akarnokd.reactive4java8.base;

/**
 * A single-element container that doesn't delegate
 * its equals and hashCode methods to the underlying value, 
 * instead, it is equal only to itself and has a hashCode
 * as such.
 * @author akarnokd, 2013.01.14.
 * @since 0.97
 * @param <T> the contained item type
 */
public final class Unique<T> {
        /** The value. */
        private final T value;
        /**
         * Constructor.
         * @param value the contained value
         */
        private Unique(T value) {
                this.value = value;
        }
        /**
         * Factory method to construct a new unique instance.
         * @param <U> the contained item type
         * @param value the value to contain
         * @return the unique instance
         */
        public static <U> Unique<U> of(U value) {
                return new Unique<>(value);
        }
        /**
         * @return the contained value
         */
        public T get() {
                return value;
        }
}