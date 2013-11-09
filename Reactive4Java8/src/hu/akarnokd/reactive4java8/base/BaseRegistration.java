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
    private final Lock lock;
    /** The completion indicator. */
    protected boolean done;
    /**
     * Constructor, initializes the lock object.
     */
    public BaseRegistration() {
        lock = new ReentrantLock();
    }
    /**
     * Call a supplier while holding a lock.
     * @param <T> the returned value type
     * @param supplier the supplier
     * @return the returned value
     */
    protected <T> T sync(Supplier<? extends T> supplier) {
        lock.lock();
        try {
            return supplier.get();
        } finally {
            lock.unlock();
        }        
    }
    /**
     * Call a supplier while holding a lock.
     * @param supplier the supplier
     * @return the returned value
     */
    protected boolean sync(BooleanSupplier supplier) {
        lock.lock();
        try {
            return supplier.getAsBoolean();
        } finally {
            lock.unlock();
        }        
    }
    /**
     * Call a supplier while holding a lock.
     * @param supplier the supplier
     * @return the returned value
     */
    protected int sync(IntSupplier supplier) {
        lock.lock();
        try {
            return supplier.getAsInt();
        } finally {
            lock.unlock();
        }        
    }
    /**
     * Execute the runnable while holding a lock.
     * @param run the runnable to execute
     */
    protected void sync(Runnable run) {
        lock.lock();
        try {
        
        } finally {
            lock.unlock();
        }        
    }
    public boolean isClosed() {
        return sync(() -> done);
    }

}
