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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Base registration with a lock and done indicator.
 * @author akarnokd, 2013.11.09.
 */
public abstract class BaseRegistration implements Registration {
    protected final LockSync ls;
    /** The completion indicator. */
    protected boolean done;
    /**
     * Constructor, initializes the lock object.
     */
    public BaseRegistration() {
        ls = new LockSync();
    }
    public boolean isClosed() {
        return ls.sync(() -> done);
    }

}
