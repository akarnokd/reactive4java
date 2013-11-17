/*
 * Copyright 2013 karnok.
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

package hu.akarnokd.reactive4java8.observables;

import hu.akarnokd.reactive4java8.Registration;
import hu.akarnokd.reactive4java8.Observable;

/**
 * An observable which can be connected, reconnected or disconnected to a source observable. 
 * @author akarnokd, 2013.11.14.
 * @param <T> the element type
 */
public interface ConnectableObservable<T> extends Observable<T> {
        /**
         * Connects the wrapper to its source. All registered observers
         * will receive values while this connection is active. Closing
         * the connection will stop the observers from receiving events,
         * but they won't be unregistered.
         * Connecting to an already connected source is a no-op.
         * @return the handler to close the connection
         */
        Registration connect();
}