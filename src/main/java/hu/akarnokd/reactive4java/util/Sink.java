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

import hu.akarnokd.reactive4java.base.Observer;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

/**
 * Base class for implementing operators via lightweight sink that can be closed
 * to prevent events to flow to the wrapped observer.
 * @author akarnokd, 2013.01.16.
 * @since 0.97
 * @param <T> the element type
 */
public class Sink<T> implements Closeable {
    /** The reference to the observer. */
    protected final AtomicReference<Observer<? super T>> observer;
    /** The reference to the cancel handler. */
    protected final AtomicReference<Closeable> cancel;
    /**
     * Constructor.
     * @param observer the observer to wrap
     * @param cancel the cancel handler to wrap
     */
    public Sink(@Nonnull Observer<? super T> observer, @Nonnull Closeable cancel) {
        this.observer = new AtomicReference<Observer<? super T>>(observer);
        this.cancel = new AtomicReference<Closeable>(cancel);
    }

    @Override
    public void close() throws IOException {
        observer.set(ObserverAdapter.INSTANCE);
        Closeable c = cancel.getAndSet(null);
        if (c != null) {
            c.close();
        }
    }
    /** Convenience method to close this sink and suppress its exceptions. */
    protected void closeSilently() {
        Closeables.closeSilently(this);
    }
    /**
     * @return creates a new event forwarder for this sink.
     */
    public Observer<T> getForwarder() {
        return new Observer<T>() {

            @Override
            public void error(@Nonnull Throwable ex) {
                observer.get().error(ex);
                Closeables.closeSilently(Sink.this);
            }

            @Override
            public void finish() {
                observer.get().finish();
                Closeables.closeSilently(Sink.this);
            }

            @Override
            public void next(T value) {
                observer.get().next(value);
            }
            
        };
    }
}
