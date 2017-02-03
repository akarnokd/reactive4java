/*
 * Copyright 2011-2013 David Karnok
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
package hu.akarnokd.reactive4java.test;

import hu.akarnokd.reactive4java.base.Observer;
import hu.akarnokd.reactive4java.base.Option;
import hu.akarnokd.reactive4java.base.Timestamped;

import java.util.List;

/**
 * An {@link Observer} that records timestamped events for testing purposes.
 * @param <T> the type of the notification values
 * @author Denes Harmath, 2012.09.22.
 */
public interface TestableObserver<T> extends Observer<T> {

    /**
     * @return the recorded timestamped events
     */
    List<Timestamped<? extends Option<T>>> getEvents();

}
