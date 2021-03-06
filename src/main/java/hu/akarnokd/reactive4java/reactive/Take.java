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
import hu.akarnokd.reactive4java.base.Func2;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;
import hu.akarnokd.reactive4java.base.Scheduler;
import hu.akarnokd.reactive4java.base.TimeInterval;
import hu.akarnokd.reactive4java.util.CircularBuffer;
import hu.akarnokd.reactive4java.util.CompositeCloseable;
import hu.akarnokd.reactive4java.util.DefaultObserverEx;
import hu.akarnokd.reactive4java.util.DefaultRunnable;
import hu.akarnokd.reactive4java.util.R4JConfigManager;
import hu.akarnokd.reactive4java.util.Schedulers;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;

/**
 * Helper class for take like operators.
 * @author akarnokd, 2013.01.14.
 * @since 0.97
 */
public final class Take {
    /** Helper class. */
    private Take() { }
    /**
     * Creates an observable which takes values from source until
     * the predicate returns false for the current element, then skips the remaining values.
     * @param <T> the element type
     * @author akarnokd, 2013.01.14.
     */
    public static class While<T> implements Observable<T> {
        /** */
        protected final Observable<? extends T> source;
        /** */
        protected final Func1<? super T, Boolean> predicate;

        /**
         * Constructor.
         * @param source the source of Ts
         * @param predicate the predicate
         */
        public While(Observable<? extends T> source,
                Func1<? super T, Boolean> predicate) {
            this.source = source;
            this.predicate = predicate;
        }

        @Override
        @Nonnull 
        public Closeable register(@Nonnull final Observer<? super T> observer) {
            DefaultObserverEx<T> obs = new DefaultObserverEx<T>(true) {
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
                    if (predicate.invoke(value)) {
                        observer.next(value);
                    } else {
                        observer.finish();
                        close();
                    }
                }
            };
            return obs.registerWith(source);
        }
    }
    /**
     * Creates an observable which takes values from source until
     * the indexed predicate returns false for the current element, then skips the remaining values.
     * @param <T> the element type
     * @author akarnokd, 2013.01.14.
     */
    public static class WhileIndexed<T> implements Observable<T> {
        /** */
        protected final Observable<? extends T> source;
        /** */
        protected final Func2<? super T, ? super Integer, Boolean> predicate;

        /**
         * Constructor.
         * @param source the source of Ts
         * @param predicate the predicate
         */
        public WhileIndexed(Observable<? extends T> source,
                Func2<? super T, ? super Integer, Boolean> predicate) {
            this.source = source;
            this.predicate = predicate;
        }

        @Override
        @Nonnull 
        public Closeable register(@Nonnull final Observer<? super T> observer) {
            DefaultObserverEx<T> obs = new DefaultObserverEx<T>(true) {
                /** The index. */
                int index;
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
                    if (predicate.invoke(value, index++)) {
                        observer.next(value);
                    } else {
                        observer.finish();
                        close();
                    }
                }
            };
            return obs.registerWith(source);
        }
    }
    /**
     * Creates an observable which takes values from source until
     * the indexed predicate returns false for the current element, then skips the remaining values.
     * @param <T> the element type
     * @author akarnokd, 2013.01.14.
     */
    public static class WhileLongIndexed<T> implements Observable<T> {
        /** */
        protected final Observable<? extends T> source;
        /** */
        protected final Func2<? super T, ? super Long, Boolean> predicate;

        /**
         * Constructor.
         * @param source the source of Ts
         * @param predicate the predicate
         */
        public WhileLongIndexed(Observable<? extends T> source,
                Func2<? super T, ? super Long, Boolean> predicate) {
            this.source = source;
            this.predicate = predicate;
        }

        @Override
        @Nonnull 
        public Closeable register(@Nonnull final Observer<? super T> observer) {
            DefaultObserverEx<T> obs = new DefaultObserverEx<T>(true) {
                /** The index. */
                long index;
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
                    if (predicate.invoke(value, index++)) {
                        observer.next(value);
                    } else {
                        observer.finish();
                        close();
                    }
                }
            };
            return obs.registerWith(source);
        }
    }
    /**
     * Creates an observable which takes values from the source until
     * the signaller produces a value. If the signaller never signals,
     * all source elements are relayed.
     * @param <T> the element type
     * @param <U> the signaller element type, irrelevant
     * @author akarnokd, 2013.01.14.
     */
    public static class Until<T, U> implements Observable<T> {
        /** */
        protected final Observable<U> signaller;
        /** */
        protected final Observable<? extends T> source;

        /**
         * Constructor.
         * @param source the source of Ts
         * @param signaller the source of Us
         */
        public Until(Observable<? extends T> source, Observable<U> signaller) {
            this.signaller = signaller;
            this.source = source;
        }

        @Override
        @Nonnull 
        public Closeable register(@Nonnull final Observer<? super T> observer) {
            final Lock lock0 = new ReentrantLock(R4JConfigManager.get().useFairLocks());
            DefaultObserverEx<T> o = new DefaultObserverEx<T>(lock0, true) {
                /** Error call from the inner. */
                protected void innerError(Throwable t) {
                    error(t);
                }
                /** Finish call from the inner. */
                protected void innerFinish() {
                    finish();
                }
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
                    observer.next(value);
                }

                @Override
                protected void onRegister() {
                    add("signaller", signaller.register(new Observer<U>() {
                        @Override
                        public void error(@Nonnull Throwable ex) {
                            innerError(ex);
                        }

                        @Override
                        public void finish() {
                            innerFinish();
                        }

                        @Override
                        public void next(U value) {
                            innerFinish();
                        }
                    }));
                }
            };
            return o.registerWith(source);
        }
    }
    /**
     * Returns an observable which returns the last <code>count</code>
     * elements from the source observable.
     * @param <T> the element type
     * @author akarnokd, 2013.01.14.
     */
    public static class Last<T> implements Observable<T> {
        /** */
        protected final Observable<? extends T> source;
        /** */
        protected final int count;

        /**
         * Constructor.
         * @param source the source of the elements
         * @param count the number elements to return
         */
        public Last(Observable<? extends T> source, int count) {
            this.source = source;
            this.count = count;
        }

        @Override
        @Nonnull 
        public Closeable register(@Nonnull final Observer<? super T> observer) {
            return source.register(new Observer<T>() {
                final CircularBuffer<T> buffer = new CircularBuffer<T>(count);

                @Override
                public void error(@Nonnull Throwable ex) {
                    observer.error(ex);
                }

                @Override
                public void finish() {
                    while (!buffer.isEmpty()) {
                        observer.next(buffer.take());
                    }
                    observer.finish();
                }

                @Override
                public void next(T value) {
                    buffer.add(value);
                }
            });
        }
    }
    /**
     * Returns an observable which returns the last <code>count</code>
     * elements from the source observable and emits them from
     * the specified scheduler pool.
     * @param <T> the element type
     * @author akarnokd, 2013.01.14.
     */
    public static class LastScheduled<T> implements Observable<T> {
        /** */
        protected final Observable<? extends T> source;
        /** */
        protected final int count;
        /** */
        protected final Scheduler pool;

        /**
         * Constructor.
         * @param source the source sequence
         * @param count the retainment count
         * @param pool the scheduled pool
         */
        public LastScheduled(Observable<? extends T> source, int count, Scheduler pool) {
            this.source = source;
            this.count = count;
            this.pool = pool;
        }

        @Override
        @Nonnull 
        public Closeable register(@Nonnull final Observer<? super T> observer) {
            return (new DefaultObserverEx<T>(true) {
                final CircularBuffer<T> buffer = new CircularBuffer<T>(count);

                @Override
                public void onError(@Nonnull Throwable ex) {
                    observer.error(ex);
                }

                @Override
                public void onFinish() {
                    add("emit", pool.schedule(new DefaultRunnable() {
                        @Override
                        protected void onRun() {
                            while (!cancelled()) {
                                if (!buffer.isEmpty()) {
                                    observer.next(buffer.take());
                                } else {
                                    observer.finish();
                                }
                            }
                        }
                    }));
                    
                }

                @Override
                public void onNext(T value) {
                    buffer.add(value);
                }
            }).registerWith(source);
        }
    }
    /**
     * Creates an observable which takes the specified number of
     * Ts from the source, unregisters and completes.
     * @param <T> the element type
     * @author akarnokd, 2013.01.14.
     */
    public static class First<T> implements Observable<T> {
        /** */
        protected final Observable<? extends T> source;
        /** */
        protected final int count;

        /**
         * Construction.
         * @param source the source of Ts
         * @param count the number of elements to relay, setting
         * it to zero will finish the output after the reception of 
         * the first event.
         */
        public First(Observable<? extends T> source, int count) {
            this.source = source;
            this.count = count;
        }

        @Override
        @Nonnull 
        public Closeable register(@Nonnull final Observer<? super T> observer) {
            DefaultObserverEx<T> o = new DefaultObserverEx<T>(true) {
                /** The countdown. */
                protected int i = count;
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
                    if (i > 0) {
                        observer.next(value);
                        i--;
                        if (i == 0) {
                            finish();
                        }
                    }
                }

            };
            return o.registerWith(source);
        }
    }
    /**
     * Returns an observable which returns the last <code>count</code>
     * elements from the source observable and
     * returns it as a single list.
     * @param <T> the element type
     * @author akarnokd, 2013.01.14.
     */
    public static class LastBuffer<T> implements Observable<List<T>> {
        /** */
        protected final Observable<? extends T> source;
        /** */
        protected final int count;

        /**
         * Constructor.
         * @param source the source of the elements
         * @param count the number elements to return
         */
        public LastBuffer(Observable<? extends T> source, int count) {
            this.source = source;
            this.count = count;
        }

        @Override
        @Nonnull 
        public Closeable register(@Nonnull final Observer<? super List<T>> observer) {
            return source.register(new Observer<T>() {
                final CircularBuffer<T> buffer = new CircularBuffer<T>(count);

                @Override
                public void error(@Nonnull Throwable ex) {
                    observer.error(ex);
                }

                @Override
                public void finish() {
                    List<T> result = new ArrayList<T>(count);
                    while (!buffer.isEmpty()) {
                        result.add(buffer.take());
                    }
                    observer.next(result);
                    observer.finish();
                }

                @Override
                public void next(T value) {
                    buffer.add(value);
                }
            });
        }
    }
    /**
     * Takes the elements from the source sequence
     * until the time runs out.
     * @author akarnokd, 2013.01.16.
     * @param <T> the element type
     */
    public static class FirstTimed<T> implements Observable<T> {
        /** */
        protected final Observable<? extends T> source;
        /** */
        protected final long time;
        /** */
        protected final TimeUnit unit;
        /** */
        protected final Scheduler pool;
        /**
         * Constructor.
         * @param source the source sequence
         * @param time the time
         * @param unit the unit
         * @param pool the pool for timed operation
         */
        public FirstTimed(
                Observable<? extends T> source,
                long time,
                TimeUnit unit,
                Scheduler pool
        ) {
            this.source = source;
            this.time = time;
            this.unit = unit;
            this.pool = pool;
            
        }
        @Override
        @Nonnull
        public Closeable register(final Observer<? super T> observer) {
            final DefaultObserverEx<T> obs = new DefaultObserverEx<T>() {

                @Override
                protected void onNext(T value) {
                    observer.next(value);
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
            obs.add("timer", pool.schedule(new DefaultRunnable(obs.getLock()) {
                @Override
                public void onRun() {
                    observer.finish();
                    obs.close();
                }
            }, time, unit));
            return obs.registerWith(source);
        }
    }
    /**
     * Returns elements from the end of the sequence during the specified
     * time interval.
     * <p>Exception semantics: exceptions are immediately forwarded.</p>
     * @author akarnokd, 2013.01.16.
     * @param <T> the element type
     */
    public static class LastTimed<T> implements Observable<T> {
        /** */
        protected final Observable<? extends T> source;
        /** */
        protected final long time;
        /** */
        protected final TimeUnit unit;
        /** */
        protected final Scheduler drainPool;
        /**
         * Constructor.
         * @param source the source sequence
         * @param time the time
         * @param unit the unit
         * @param drainPool the optional pool to drain the accumulated values
         */
        public LastTimed(
                Observable<? extends T> source,
                long time,
                TimeUnit unit,
                Scheduler drainPool
        ) {
            this.source = source;
            this.time = time;
            this.unit = unit;
            this.drainPool = drainPool;
        }
        @Override
        @Nonnull
        public Closeable register(final Observer<? super T> observer) {
            final long delta = unit.toNanos(time);
            final CompositeCloseable c = new CompositeCloseable();
            DefaultObserverEx<T> obs = new DefaultObserverEx<T>() {
                /** The queue. */
                final Queue<TimeInterval<T>> queue = new LinkedList<TimeInterval<T>>();
                @Override
                protected void onNext(T value) {
                    long now = Schedulers.now();
                    queue.add(TimeInterval.of(value, now));
                    trim(now);
                }

                @Override
                protected void onError(Throwable ex) {
                    observer.error(ex);
                }

                @Override
                protected void onFinish() {
                    long now = Schedulers.now();
                    // remove too old elements
                    trim(now);
                    if (drainPool == null) {
                        flush();
                    } else {
                        c.add(drainPool.schedule(new Runnable() {
                            @Override
                            public void run() {
                                flush();
                            }
                        }));
                    }
                }
                /** Flush the contents of the queue.*/
                protected void flush() {
                    while (!queue.isEmpty()) {
                        observer.next(queue.poll().value());
                    }
                    observer.finish();
                }
                /** Trim the queue. */
                private void trim(long now) {
                    while (!queue.isEmpty() && queue.peek().interval() < now - delta) {
                        queue.poll();
                    }
                }
                
            };
            
            c.add(obs);
            
            obs.registerWith(source);
            
            return c;
        }
    }
    /**
     * Returns elements from the end of the sequence during the specified
     * time interval in one list.
     * <p>Exception semantics: exceptions are immediately forwarded.</p>
     * @author akarnokd, 2013.01.16.
     * @param <T> the element type
     */
    public static class LastBufferTimed<T> implements Observable<List<T>> {
        /** */
        protected final Observable<? extends T> source;
        /** */
        protected final long time;
        /** */
        protected final TimeUnit unit;
        /** */
        protected final Scheduler drainPool;
        /**
         * Constructor.
         * @param source the source sequence
         * @param time the time
         * @param unit the unit
         * @param drainPool the optional pool to drain the accumulated values
         */
        public LastBufferTimed(
                Observable<? extends T> source,
                long time,
                TimeUnit unit,
                Scheduler drainPool
        ) {
            this.source = source;
            this.time = time;
            this.unit = unit;
            this.drainPool = drainPool;
        }
        @Override
        @Nonnull
        public Closeable register(final Observer<? super List<T>> observer) {
            final long delta = unit.toNanos(time);
            final CompositeCloseable c = new CompositeCloseable();
            DefaultObserverEx<T> obs = new DefaultObserverEx<T>() {
                /** The queue. */
                final Queue<TimeInterval<T>> queue = new LinkedList<TimeInterval<T>>();
                @Override
                protected void onNext(T value) {
                    long now = Schedulers.now();
                    queue.add(TimeInterval.of(value, now));
                    trim(now);
                }

                @Override
                protected void onError(Throwable ex) {
                    observer.error(ex);
                }

                @Override
                protected void onFinish() {
                    long now = Schedulers.now();
                    // remove too old elements
                    trim(now);
                    if (drainPool == null) {
                        flush();
                    } else {
                        c.add(drainPool.schedule(new Runnable() {
                            @Override
                            public void run() {
                                flush();
                            }
                        }));
                    }
                }
                /** Flush the contents of the queue.*/
                protected void flush() {
                    List<T> result = new ArrayList<T>(queue.size());
                    
                    while (!queue.isEmpty()) {
                        result.add(queue.poll().value());
                    }
                    
                    observer.next(result);
                    observer.finish();
                }
                /** Trim the queue. */
                private void trim(long now) {
                    while (!queue.isEmpty() && queue.peek().interval() < now - delta) {
                        queue.poll();
                    }
                }
                
            };
            
            c.add(obs);
            
            obs.registerWith(source);
            
            return c;
        }
    }
}
