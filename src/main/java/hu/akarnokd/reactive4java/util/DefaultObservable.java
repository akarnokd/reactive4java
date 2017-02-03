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

package hu.akarnokd.reactive4java.util;

import hu.akarnokd.reactive4java.base.CloseableObservable;
import hu.akarnokd.reactive4java.base.Observer;
import hu.akarnokd.reactive4java.base.Subject;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

/**
 * An observable + observer implementation which keeps track of the registered observers and
 * common methods which dispatch events to all registered observers.
 * <p>The implementation is thread safe: all default methods may be invoked from any thread.
 * Could be used as a default implementation to convert between Observable and other event-listener type pattern.</p>
 * <p>The observer keeps track of the registered observer count and when
 * it reaches zero again, the last deregister will invoke the <code>close()</code> method.</p>
 * <p>Override <code>close()</code> method if you need some resource cleanup. Note that this may happen on any thread.</p>
 * <p>The constructor lets you customize whether automatically deregister every observer after an error or finish message
 * is relayed.</p>
 * @author akarnokd, 2011.01.29.
 * @param <T> the element type of the observable.
 */
public class DefaultObservable<T> implements Subject<T, T>, CloseableObservable<T> {
    /** Atomically keeps track of the registered/deregistered observer count. */
    private final AtomicInteger count = new AtomicInteger();
    /** The map of the active observers. */
    private final ConcurrentMap<Closeable, Observer<? super T>> observers = new ConcurrentHashMap<Closeable, Observer<? super T>>();
    /** Unregister all on error? */
    private final boolean unregisterOnError;
    /** Unregister all on error? */
    private final boolean unregisterOnFinish;
    /**
     * Default constructor. All observers will be unregistered upon error() or finish().
     */
    public DefaultObservable() {
        this(true, true);
    }
    /**
     * Constructor with the option to set the unregistration policies.
     * @param unregisterOnFinish unregister all observers on a finish() call?
     * @param unregisterOnError unregister all observers on an error() call?
     */
    public DefaultObservable(boolean unregisterOnFinish, boolean unregisterOnError) {
        this.unregisterOnFinish = unregisterOnFinish;
        this.unregisterOnError = unregisterOnError;
    }
    @Override
    public void error(@Nonnull Throwable ex) {
        if (unregisterOnError) {
            for (Map.Entry<Closeable, Observer<? super T>> os : observers.entrySet()) {
                os.getValue().error(ex);
                unregister(os.getKey());
            }
        } else {
            for (Observer<? super T> os : observers.values()) {
                os.error(ex);
            }
        }
    }

    @Override
    public void finish() {
        if (unregisterOnFinish) {
            for (Map.Entry<Closeable, Observer<? super T>> os : observers.entrySet()) {
                os.getValue().finish();
                unregister(os.getKey());
            }
        } else {
            for (Observer<? super T> os : observers.values()) {
                os.finish();
            }
        }
    }

    @Override
    public void next(T value) {
        for (Observer<? super T> os : observers.values()) {
            os.next(value);
        }
    }

    @Override
    @Nonnull
    public Closeable register(@Nonnull final Observer<? super T> observer) {
        // FIXME allow multiple registrations for the same observer instance?!
        final Closeable handler = new Closeable() {
            @Override
            public void close() throws IOException {
                unregister(this);
            }
        };
        count.incrementAndGet();
        observers.put(handler,  observer);
        return handler;
    }
    /**
     * Unregister the observer belonging to the given handler.
     * A handler can only be unregistered once
     * @param handler the observer's handler
     */
    protected void unregister(final Closeable handler) {
        if (observers.remove(handler) != null) {
            if (count.decrementAndGet() == 0) {
                close();
            }
        }
    }
    @Override
    public void close() {
        // no operation
    }
    /**
     * @return Returns the current observer count.
     */
    public int getObserverCount() {
        return count.get();
    }
}
