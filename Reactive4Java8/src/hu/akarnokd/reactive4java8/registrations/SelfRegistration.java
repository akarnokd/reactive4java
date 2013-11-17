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

package hu.akarnokd.reactive4java8.registrations;

import hu.akarnokd.reactive4java8.Registration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Registration which calls a method once it is closed.
 * @author akarnokd, 2013.11.16.
 */
public final class SelfRegistration implements Registration {
    /** The callback for the unregistration. */
    private final AtomicReference<Consumer<? super Registration>> unregister;
    /**
     * Constructor, takes an unregister callback.
     * @param unregister 
     */
    public SelfRegistration(Consumer<? super Registration> unregister) {
        this.unregister = new AtomicReference<>(Objects.requireNonNull(unregister));
    }
    @Override
    public void close() {
        Consumer<? super Registration> c = unregister.getAndSet(null);
        if (c != null) {
            c.accept(this);
        }
    }
    
}
