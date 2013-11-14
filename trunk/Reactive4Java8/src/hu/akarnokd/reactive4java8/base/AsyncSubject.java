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

package hu.akarnokd.reactive4java8.base;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents the result (last value or exception) of an asynchronous operation
 * delivered through observation.
 * @author karnok
 */
public final class AsyncSubject<T> implements Subject<T, T> {
    /** The synchronizer. */
    private LockSync ls;
    /** A value has been received. */
    private boolean hasValue;
    /** The subject has completed. */
    private boolean done;
    /** The value received. */
    private T value;
    /** The exception received. */
    private Throwable exception;
    /** The registered observers. */
    private Map<Registration, Observer<? super T>> observers = new LinkedHashMap<>();
    /** Done indicator. */
    private final CountDownLatch doneLatch = new CountDownLatch(1);
    /** Default constructor. Uses a non-fair reentrant lock. */
    public AsyncSubject() {
        this(new ReentrantLock());
    }
    /**
     * Constructor, takes a shared lock.
     * @param sharedLock 
     */
    public AsyncSubject(Lock sharedLock) {
        ls = new LockSync(sharedLock);
    }
    @Override
    public void error(Throwable t) {
        Iterable<Observer<? super T>> oss = ls.sync(() -> {
            if (!done) {
                done = true;
                exception = t;
                List<Observer<? super T>> r = new LinkedList<>(observers.values());
                observers = new LinkedHashMap<>();
                doneLatch.countDown();
                return r;
            }
            return Collections.emptyList();
        });
        oss.forEach(o -> o.error(t));
    }
    @Override
    public void finish() {
        Iterable<Observer<? super T>> oss = null;
        boolean has = false;
        T val = null;
        ls.lock();
        try {
            if (!done) {
                done = true;
                oss = new LinkedList<>(observers.values());
                observers = new LinkedHashMap<>();
                has = hasValue;
                val = value;
                doneLatch.countDown();
            }
        } finally {
            ls.unlock();
        }
        if (oss != null) {
            for (Observer<? super T> o : oss) {
                if (has) {
                    o.next(val);
                }
                o.finish();
            }
        }
    }

    @Override
    public void next(T value) {
        ls.sync(() -> {
            if (!done) {
                this.hasValue = true;
                this.value = value;
            }
        });
    }
    /**
     * Unregister a given registration token.
     * @param token 
     */
    protected void unregister(Registration token) {
        ls.sync(() -> {
            observers.remove(token);
        });
    }
    @Override
    public Registration register(Observer<? super T> observer) {
        T val = null;
        boolean has = false;
        Throwable exc = null;

        ls.lock();
        try {
            if (!done) {
                Registration reg = new Registration() {
                    @Override
                    public void close() {
                        unregister(this);
                    }
                };
                observers.put(reg, observer);
                return reg;
            }
            has = hasValue;
            exc = exception;
            val = value;
        } finally {
            ls.unlock();
        }
        if (exc != null) {
            observer.error(exc);
        } else {
            if (has) {
                try {
                    observer.next(val);
                    observer.finish();
                } catch (Throwable t) {
                    observer.error(t);
                }
            } else {
                    observer.finish();
            }
        }
        return Registration.EMPTY;
    }
    /** @return Has the subject finished? */
    public boolean isDone() {
        return ls.sync(() -> done);
    }
    /**
     * Returns the observed value as optional or throws
     * the exception.
     * @return 
     */
    private Optional<T> getOptionalInternal() {
        ls.lock();
        try {
            if (!done) {
                throw new IllegalStateException();
            }
            if (exception != null) {
                if (exception instanceof RuntimeException) {
                    throw (RuntimeException)exception;
                }
                throw new RuntimeException(exception);
            }
            if (hasValue) {
                return Optional.of(value);
            }
            return Optional.empty();
        } finally {
            ls.unlock();
        }
    }
    /**
     * Returns the observed value as optional or throws
     * the exception.
     * @return 
     */
    private T getInternal() {
        ls.lock();
        try {
            if (!done) {
                throw new IllegalStateException();
            }
            if (exception != null) {
                if (exception instanceof RuntimeException) {
                    throw (RuntimeException)exception;
                }
                throw new RuntimeException(exception);
            }
            if (hasValue) {
                return value;
            }
            throw new NoSuchElementException();
        } finally {
            ls.unlock();
        }
    }
    /**
     * Waits indefinitely to receive an optional value or exception.
     * @return
     * @throws InterruptedException 
     */
    public Optional<T> getOptional() throws InterruptedException {
        doneLatch.await();
        return getOptionalInternal();
    }
    /**
     * Waits the given amount of time to receive an optional value or exception.
     * @param time
     * @param unit
     * @return
     * @throws InterruptedException 
     */
    public Optional<T> getOptional(long time, TimeUnit unit) throws InterruptedException, TimeoutException {
        if (doneLatch.await(time, unit)) {
            return getOptionalInternal();
        }
        throw new TimeoutException();
    }
    /**
     * Waits indefinitely to receive a value or exception.
     * <p>If no value was received, a NoSuchElementException is thrown.</p>
     * @return
     * @throws InterruptedException 
     */
    public T get() throws InterruptedException {
        doneLatch.await();
        return getInternal();
    }
    /**
     * Waits the specified amount of time to receive a value or error,
     * or throws a TimeoutException.
     * @param time
     * @param unit
     * @return
     * @throws InterruptedException
     * @throws TimeoutException 
     */
    public T get(long time, TimeUnit unit) throws InterruptedException, TimeoutException {
        if (doneLatch.await(time, unit)) {
            return getInternal();
        }
        throw new TimeoutException();
    }
}
