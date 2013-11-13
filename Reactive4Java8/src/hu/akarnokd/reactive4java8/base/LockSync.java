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

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Helper class to execute methods and functions while holding
 * a lock object.
 * <p>Remark: something like this might be in Java 8 somewhere?</p>
 * @author akarnokd, 2013.11.09.
 */
public final class LockSync {
    /** The lock object. */
    private final Lock lock;
    /** Constructor, creates a reentrant non-fair lock. */
    public LockSync() {
        lock = new ReentrantLock();
    }
    /**
     * Constructor, takes the given shared lock.
     * @param sharedLock the shared lock
     */
    public LockSync(Lock sharedLock) {
        lock = Objects.requireNonNull(sharedLock);
    }
    /**
     * Executes the runnable action while holding the lock.
     * @param run 
     */
    public void sync(Runnable run) {
        lock.lock();
        try {
            run.run();
        } finally {
            lock.unlock();
        }
    }
    /**
     * Calls the given supplier while holding the lock and
     * returns its value.
     * @param <T>
     * @param supplier
     * @return 
     */
    public <T> T sync(Supplier<? extends T> supplier) {
        lock.lock();
        try {
            return supplier.get();
        } finally {
            lock.unlock();
        }        
    }
    /**
     * Calls the given supplier while holding the lock and
     * returns its value.
     * @param supplier
     * @return 
     */
    public int sync(IntSupplier supplier) {
        lock.lock();
        try {
            return supplier.getAsInt();
        } finally {
            lock.unlock();
        }        
    }
    /**
     * Calls the given supplier while holding the lock and
     * returns its value.
     * @param supplier
     * @return 
     */
    public boolean sync(BooleanSupplier supplier) {
        lock.lock();
        try {
            return supplier.getAsBoolean();
        } finally {
            lock.unlock();
        }        
    }
    /**
     * Calls the given supplier while holding the lock and
     * returns its value.
     * @param supplier
     * @return 
     */
    public long sync(LongSupplier supplier) {
        lock.lock();
        try {
            return supplier.getAsLong();
        } finally {
            lock.unlock();
        }        
    }
    /**
     * Calls the given supplier while holding the lock and
     * returns its value.
     * @param supplier
     * @return 
     */
    public double sync(DoubleSupplier supplier) {
        lock.lock();
        try {
            return supplier.getAsDouble();
        } finally {
            lock.unlock();
        }        
    }
}
