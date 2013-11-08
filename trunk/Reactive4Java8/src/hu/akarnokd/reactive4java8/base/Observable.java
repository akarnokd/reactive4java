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
 * Defines provider for push-based value streaming.
 * <p>reactive4java notes:
 * <ul>
 * <li>the interface defines the most
 * common operations which were previously in the Reactive
 * utility class.</li>
 * <li>The register() method now returns a {@link Registration}
 * instance instead of a {@link java.io.Closeable} since most cancel
 * operations don't throw IOException, but the previous library was
 * limited to Java 6 constructs.</li>
 * </ul></p>
 * @author akarnokd, 2013.11.08
 * @param <T> the type of the vales to be streamed
 */
@FunctionalInterface
public interface Observable<T> {
    /**
     * Registers an observer with this observable.
     * @param observer the observer to register
     * @return the registration which can be used to deregister
     * the observer.
     */
    Registration register(Observer<? super T> observer);
}
