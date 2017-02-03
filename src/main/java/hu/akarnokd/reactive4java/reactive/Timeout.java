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

import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;
import hu.akarnokd.reactive4java.base.Scheduler;
import hu.akarnokd.reactive4java.util.Closeables;
import hu.akarnokd.reactive4java.util.CompositeCloseable;
import hu.akarnokd.reactive4java.util.DefaultObserverEx;
import hu.akarnokd.reactive4java.util.DefaultRunnable;
import hu.akarnokd.reactive4java.util.Observers;
import hu.akarnokd.reactive4java.util.R4JConfigManager;
import hu.akarnokd.reactive4java.util.SingleCloseable;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

/**
 * Helper class for Reactive.timeout operations.
 * @author akarnokd, 2013.01.17.
 * @since 0.97
 */
public final class Timeout {
    /** Helper class. */
    private Timeout() { }
    /**
     * Applies a timeout for each element in the source sequence.
     * If an element times out, the source sequence is switched
     * to the other source.
     * @author akarnokd, 2013.01.17.
     * @param <T> the element tye
     */
    public static class Switch<T> implements Observable<T> {
        /** */
        protected final Observable<? extends T> source;
        /** */
        protected final long time;
        /** */
        protected final TimeUnit unit;
        /** */
        protected final Scheduler pool;
        /** */
        protected final Observable<? extends T> other;

        /**
         * Constructor.
         * @param source the source sequence
         * @param time the timeout for each element
         * @param unit the tim unit
         * @param other the other stream to switch to in case of timeout
         * @param pool the scheduler for the timed operations
         */
        public Switch(
                Observable<? extends T> source, 
                long time, 
                TimeUnit unit,
                Observable<? extends T> other, 
                Scheduler pool) {
            this.source = source;
            this.time = time;
            this.unit = unit;
            this.other = other;
            this.pool = pool;
        }

        @Override
        @Nonnull 
        public Closeable register(@Nonnull final Observer<? super T> observer) {
            final SingleCloseable second = new SingleCloseable();
            final CompositeCloseable c = new CompositeCloseable();
            DefaultObserverEx<T> obs = new DefaultObserverEx<T>(true) {
                @Override
                protected void onError(@Nonnull Throwable ex) {
                    observer.error(ex);
                }

                @Override
                protected void onFinish() {
                    observer.finish();
                }

                @Override
                protected void onNext(T value) {
                    remove("timer");
                    observer.next(value);
                    registerTimer();
                }
                @Override
                protected void onRegister() {
                    registerTimer();
                }
                /**
                 * Register the timer that when fired, switches to the second
                 * observable sequence
                 */
                private void registerTimer() {
                    add("timer", pool.schedule(new DefaultRunnable(lock) {
                        @Override
                        public void onRun() {
                            if (!cancelled()) {
                                second.set(Observers.registerSafe(other, observer));
                                close();
                            }
                        }
                    }, time, unit));
                }
            };
            c.add(obs, second);
            obs.registerWith(source);
            return c;
        }
    }
    /**
     * Applies a timeout to each element in the source sequence,
     * starting with the timeout from the firsTimeout observabe,
     * then, for each element a separate window is opened in the
     * form of observable sequence. If any of these window observables
     * fire next or finish, the sequence is switched to the other
     * observable.
     * @author akarnokd, 2013.01.17.
     * @param <T> the source and result element type
     * @param <U> the initial timeout element type, irrelevant
     * @param <V> the per-element timeout type, irrelevant
     */
    public static class ByObservables<T, U, V> implements Observable<T> {
        /** */
        private Observable<? extends T> source;
        /** */
        private Observable<U> firstTimeout;
        /** */
        private Func1<? super T, ? extends Observable<V>> timeoutSelector;
        /** */
        private Observable<? extends T> other;
        /**
         * Constructor.
         * @param source the source sequence
         * @param firstTimeout the timeout observable
         * @param timeoutSelector the selector for timeouts for each element
         * @param other the other source to switch to
         */
        public ByObservables(
                Observable<? extends T> source,
                Observable<U> firstTimeout,
                Func1<? super T, ? extends Observable<V>> timeoutSelector,
                Observable<? extends T> other
        ) {
            this.source = source;
            this.firstTimeout = firstTimeout;
            this.timeoutSelector = timeoutSelector;
            this.other = other;
            
        }
        @Override
        @Nonnull
        public Closeable register(final Observer<? super T> observer) {
            final Lock lock = new ReentrantLock(R4JConfigManager.get().useFairLocks());
            final SingleCloseable firstClose = new SingleCloseable();
            final CompositeCloseable c = new CompositeCloseable();
            
            final DefaultObserverEx<T> obs = new DefaultObserverEx<T>(lock, true) {
                /** Is this the first event? */
                @GuardedBy("lock")
                boolean first = true;
                @Override
                protected void onNext(T value) {
                    if (first) {
                        first = false;
                        Closeables.closeSilently(first);
                        
                    }
                    remove("timeout");
                    observer.next(value);
                    
                    Observable<V> duration = null;
                    try {
                        duration = timeoutSelector.invoke(value);
                    } catch (Throwable t) {
                        observer.error(t);
                        close();
                        return;
                    }
                    final Closeable parent = this;
                    add("timeout", Observers.registerSafe(duration, new DefaultObserverEx<V>(lock, true) {
                        @Override
                        protected void onNext(V value) {
                            timeout();
                            close();
                        }

                        @Override
                        protected void onError(Throwable ex) {
                            innerError(ex);
                        }

                        @Override
                        protected void onFinish() {
                            timeout();
                        } 
                        protected void timeout() {
                            c.add(Observers.registerSafe(other, observer));
                            Closeables.closeSilently(parent);
                        }
                    }));

                }
                /**
                 * Propagate the inner errors.
                 * @param ex the exception
                 */
                protected void innerError(Throwable ex) {
                    error(ex);
                }

                @Override
                protected void onError(Throwable ex) {
                    observer.error(ex);

                    Closeables.closeSilently(first);
                }

                @Override
                protected void onFinish() {
                    observer.finish();
                    
                    Closeables.closeSilently(first);
                }
            };
            obs.registerWith(source);
            
            DefaultObserverEx<U> firstObs = new DefaultObserverEx<U>(lock, true) {

                @Override
                protected void onNext(U value) {
                    timeout();
                    close();
                }

                @Override
                protected void onError(Throwable ex) {
                    observer.error(ex);
                    Closeables.closeSilently(obs);
                }

                @Override
                protected void onFinish() {
                    timeout();
                }
                /** The timeout action. */
                public void timeout() {
                    c.add(Observers.registerSafe(other, observer));
                    Closeables.closeSilently(obs);
                }
                @Override
                protected void onClose() {
                    firstClose.closeSilently();
                }
            };
            
            firstClose.set(Observers.registerSafe(firstTimeout, firstObs));
            
            c.add(firstClose, obs);
            
            return c;
        }
    }
}
