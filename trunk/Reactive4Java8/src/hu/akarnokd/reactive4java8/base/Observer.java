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
 * Base interface for observing a stream of values.
 * <p>reactive4java note: the interface has default empty
 * implementation for the error() and finish methods
 * to allow simple lambda expressions to register with
 * the {@link Observable} implementations.</p>
 * @author akarnokd, 2013.11.08
 * @param <T> the value type to be observed
 */
@FunctionalInterface
public interface Observer<T> {
    /**
     * Receive a value.
     * <p>Exceptions thrown by the method is forwarded to
     * the error() method.</p>
     * @param value the value received
     */
    void next(T value);
    /**
     * Receive an exception.
     * @param t a throwable exception
     */
    default void error(Throwable t) { }
    /**
     * Receive a completion signal.
     */
    default void finish() { }
}
