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

package hu.akarnokd.reactive4java.base;

import java.io.Closeable;

import javax.annotation.Nonnull;


/**
 * Defines a provider of primitive long
 * values for push-based streaming.
 * @author akarnokd
 * @since 0.97
 */
public interface LongObservable {
    /**
     * Registers an observer for the notification of Ts.
     * @param observer the observer of Ts or any supertype of it
     * @return the way of deregister the observer from this provider
     */
    @Nonnull 
    Closeable register(@Nonnull LongObserver observer);
}
