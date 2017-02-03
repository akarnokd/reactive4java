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
import hu.akarnokd.reactive4java.util.DefaultObserver;
import hu.akarnokd.reactive4java.util.DefaultObserverEx;
import hu.akarnokd.reactive4java.util.DefaultRunnable;
import hu.akarnokd.reactive4java.util.Observers;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * Helper class for Reactive.throttle operators.
 * @author akarnokd, 2013.01.16.
 * @since 0.97
 */
public final class Throttle {
    /** Helper class. */
    private Throttle() { }
    /**
     * Creates and observable which fires the last value
     * from source when the given timespan elapsed without a new
     * value occurring from the source. 
     * <p>It is basically how Content Assistant
     * popup works after the user pauses in its typing.</p>
     * @param <T> the value type
     * @author akarnokd, 2013.01.16.
     */
    public static class ByTime<T> implements Observable<T> {
        /** */
        private final Observable<? extends T> source;
        /** */
        private final long delay;
        /** */
        private final TimeUnit unit;
        /** */
        private final Scheduler pool;
        /**
         * Constructor.
         * @param source the source sequence
         * @param delay the delay time
         * @param unit the time unit
         * @param pool the scheduler pool for the timed operatons
         */
        public ByTime(
                @Nonnull final Observable<? extends T> source,
                final long delay,
                @Nonnull final TimeUnit unit,
                @Nonnull final Scheduler pool
        ) {
            this.source = source;
            this.delay = delay;
            this.unit = unit;
            this.pool = pool;
            
        }
        @Override
        @Nonnull 
        public Closeable register(@Nonnull final Observer<? super T> observer) {
            final DefaultObserverEx<T> obs = new DefaultObserverEx<T>(true) {
                /** The last seen value. */
                T last;
                /** The closeable. */
                Closeable c;
                /** The timeout action. */
                final DefaultRunnable r = new DefaultRunnable(lock) {
                    @Override
                    public void onRun() {
                        if (!cancelled()) {
                            observer.next(last);
                        }
                    }
                };
                @Override
                protected void onClose() {
                    Closeables.closeSilently(c);
                }
                @Override
                public void onError(@Nonnull Throwable ex) {
                    observer.error(ex);
                }

                @Override
                public void onFinish() {
                    observer.finish();
                }
                @Override
                public void onNext(T value) {
                    last = value;
                    Closeables.closeSilently(c);
                    c = pool.schedule(r, delay, unit);
                }
            };
            return obs.registerWith(source);
        }
    }
    /**
     * Fires the last event from the source observable if
     * no events are fired during a selector-returned observable window.
     * <p>Exception semantics: exceptions from the source and windows
     * are forwarded immediately and the sequence is terminated.</p>
     * <p>The window close is triggered by either a next or finish event.</p>
     * @author akarnokd, 2013.01.17.
     * @param <T> the source and result element type
     * @param <U> the window observable's type, irrelevant
     */
    public static class ByObservable<T, U> implements Observable<T> {
        /** */
        protected final Observable<? extends T> source;
        /** */
        protected final Func1<? super T, ? extends Observable<U>> durationSelector;
        /**
         * Constructor.
         * @param source the source sequence
         * @param durationSelector the duration selector.
         */
        public ByObservable(
                Observable<? extends T> source,
                Func1<? super T, ? extends Observable<U>> durationSelector
        ) {
            this.source = source;
            this.durationSelector = durationSelector;
        }
        @Override
        @Nonnull
        public Closeable register(final Observer<? super T> observer) {
            DefaultObserverEx<T> obs = new DefaultObserverEx<T>() {

                @Override
                protected void onNext(final T value) {
                    Observable<U> duration = null;
                    try {
                        duration = durationSelector.invoke(value);
                    } catch (Throwable t) {
                        error(t);
                        return;
                    }
                    add("delay", Observers.registerSafe(duration, new DefaultObserver<U>(lock, true) {
                        @Override
                        protected void onNext(U v) {
                            observer.next(value);
                            remove("delay");
                        }

                        @Override
                        protected void onError(Throwable ex) {
                            innerError(ex);
                        }

                        @Override
                        protected void onFinish() {
                            observer.next(value);
                        }
                        
                    }));
                }
                /** Called from the inner observers. */
                protected void innerError(Throwable ex) {
                    error(ex);
                }
                @Override
                protected void onError(Throwable ex) {
                    observer.error(ex);
                }

                @Override
                protected void onFinish() {
                    observer.finish();
                }
                
            };
            return obs.registerWith(source);
        }
        
    }
}
