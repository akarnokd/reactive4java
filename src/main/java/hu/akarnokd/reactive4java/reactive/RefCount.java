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
package hu.akarnokd.reactive4java.reactive;

import hu.akarnokd.reactive4java.base.ConnectableObservable;
import hu.akarnokd.reactive4java.base.MultiIOException;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;
import hu.akarnokd.reactive4java.util.R4JConfigManager;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

/**
 * Returns an observable sequence which 
 * connects to the source for the first registered 
 * party and stays connected to the source
 * as long as there is at least one registered party to it.
 * @param <T> the element type
 * @since 0.97
 * @author akarnokd, 2013.01.11.
 */
public class RefCount<T> implements Observable<T> {
    /** The lock. */
    @Nonnull 
    protected final Lock lock;
    /** The source . */
    @Nonnull 
    protected final ConnectableObservable<? extends T> source;
    /** The active connection. */
    @GuardedBy("lock")
    protected Closeable connection;
    /** The registration count. */
    @GuardedBy("lock")
    protected int count;
    /**
     * Constructor with a default fair reentrant lock.
     * @param source the source of Ts
     */
    public RefCount(ConnectableObservable<? extends T> source) {
        this(source, new ReentrantLock(R4JConfigManager.get().useFairLocks()));
    }
    /**
     * Constructor.
     * @param source the source of Ts
     * @param lock the lock
     */
    public RefCount(ConnectableObservable<? extends T> source, Lock lock) {
        this.lock = lock;
        this.source = source;
    }
    @Override
    @Nonnull
    public Closeable register(@Nonnull final Observer<? super T> observer) {
        final Closeable c = source.register(observer);
        lock.lock();
        try {
            if (++count == 1) {
                connection = source.connect();
            }
        } finally {
            lock.unlock();
        }
        return new Closeable() {
            @Override
            public void close() throws IOException {
                MultiIOException ex = null;
                try {
                    c.close();
                } catch (IOException exc) {
                    ex = MultiIOException.createOrAdd(ex, exc);
                }
                lock.lock();
                try {
                    if (--count == 0) {
                        connection.close();
                        connection = null;
                    }
                } catch (IOException exc) {
                    ex = MultiIOException.createOrAdd(ex, exc);
                } finally {
                    lock.unlock();
                }
                if (ex != null) {
                    throw ex;
                }
            }
        };
    }
}
