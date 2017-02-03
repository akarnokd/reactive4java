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
import hu.akarnokd.reactive4java.util.DefaultObserver;
import hu.akarnokd.reactive4java.util.DefaultObserverEx;

import java.io.Closeable;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

/**
 * Contains implementation classes for concatenating
 * various sources. See its inner classes.
 * @author akarnokd, 2013.01.13.
 * @since 0.97
 */
public final class Concat {
    /** Helper class. */
    private Concat() { }
    /**
     * Iterable main source.
     * @author akarnokd, 2013.01.13.
     */
    public static final class FromIterable {
        /** Helper class. */
        private FromIterable() { }
        /**
         * Concatenates the observable sequences resulting from enumerating
         * the sorce iterable and calling the resultSelector function.
         * <p>Remark: RX calls this For.</p>
         * @param <T> the source element type
         * @param <U> the result type
         */
        public static class Selector<T, U> implements Observable<U> {
            /** The source sequence. */
            @Nonnull
            protected final Iterable<? extends T> source;
            /** The result selector. */
            @Nonnull
            protected final Func1<? super T, ? extends Observable<? extends U>> resultSelector;
            /**
             * Constructor.
             * @param source the source sequence
             * @param resultSelector the observable selector
             */
            public Selector(
                    @Nonnull Iterable<? extends T> source, 
                    @Nonnull Func1<? super T, ? extends Observable<? extends U>> resultSelector) {
                        this.source = source;
                        this.resultSelector = resultSelector;
            }
            @Override
            @Nonnull
            public Closeable register(@Nonnull final Observer<? super U> observer) {
                final Iterator<? extends T> it = source.iterator();
                DefaultObserverEx<U> obs = new DefaultObserverEx<U>(false) {

                    @Override
                    protected void onNext(U value) {
                        observer.next(value);
                    }

                    @Override
                    protected void onError(@Nonnull Throwable ex) {
                        observer.error(ex);
                        close();
                    }

                    @Override
                    protected void onFinish() {
                        if (it.hasNext()) {
                            registerWith(resultSelector.invoke(it.next()));
                        } else {
                            observer.finish();
                            close();
                        }
                    }
                    
                };
                if (it.hasNext()) {
                    obs.registerWith(resultSelector.invoke(it.next()));
                }
                return obs;
            }
        }
        /**
         * Concatenates the observable sequences resulting from enumerating
         * the sorce iterable and calling the indexed resultSelector function.
         * <p>Remark: RX calls this For.</p>
         * @param <T> the source element type
         * @param <U> the result type
         */
        public static class IndexedSelector<T, U> implements Observable<U> {
            /** The source sequence. */
            @Nonnull
            protected final Iterable<? extends T> source;
            /** The result selector. */
            @Nonnull
            protected final Func2<? super T, ? super Integer, ? extends Observable<? extends U>> resultSelector;
            /**
             * Constructor.
             * @param source the source sequence
             * @param resultSelector the observable selector
             */
            public IndexedSelector(
                    @Nonnull Iterable<? extends T> source, 
                    @Nonnull Func2<? super T, ? super Integer, ? extends Observable<? extends U>> resultSelector) {
                        this.source = source;
                        this.resultSelector = resultSelector;
            }
            @Override
            @Nonnull
            public Closeable register(@Nonnull final Observer<? super U> observer) {
                final Iterator<? extends T> it = source.iterator();
                DefaultObserverEx<U> obs = new DefaultObserverEx<U>(false) {
                    /** The running index for the selector. */
                    int index = 1;
                    @Override
                    protected void onNext(U value) {
                        observer.next(value);
                    }

                    @Override
                    protected void onError(@Nonnull Throwable ex) {
                        observer.error(ex);
                        close();
                    }

                    @Override
                    protected void onFinish() {
                        if (it.hasNext()) {
                            registerWith(resultSelector.invoke(it.next(), index++));
                        } else {
                            observer.finish();
                            close();
                        }
                    }
                    
                };
                if (it.hasNext()) {
                    obs.registerWith(resultSelector.invoke(it.next(), 0));
                }
                return obs;
            }
        }
    }
    /**
     * Observable main source.
     * @author akarnokd, 2013.01.13.
     */
    public static final class FromObservable {
        /** Helper class. */
        private FromObservable() { }
        /**
         * Concatenate the the multiple sources selected by the function after another.
         * <p><b>Exception semantics:</b> if the sources or any inner observer signals an
         * error, the outer observable will signal that error and the sequence is terminated.</p>
         * @author akarnokd, 2013.01.13.
         * @param <T> the source observable element type
         * @param <U> the result observable element type
         */
        public static class Selector<T, U> implements Observable<U> {
            /** The source. */
            protected final Observable<? extends Observable<? extends T>> sources;
            /** The result selector. */
            @Nonnull
            protected final Func1<? super Observable<? extends T>, ? extends Observable<? extends U>> resultSelector;

            /**
             * Constructor.
             * @param sources the sources
             * @param resultSelector the concatenation result selector
             */
            public Selector(
                    Observable<? extends Observable<? extends T>> sources, 
                    Func1<? super Observable<? extends T>, ? extends Observable<? extends U>> resultSelector) {
                this.sources = sources;
                this.resultSelector = resultSelector;
            }

            @Override
            @Nonnull 
            public Closeable register(@Nonnull final Observer<? super U> observer) {
                final LinkedBlockingQueue<Observable<? extends T>> sourceQueue = new LinkedBlockingQueue<Observable<? extends T>>();
                final AtomicInteger wip = new AtomicInteger(1);
                DefaultObserverEx<Observable<? extends T>> o = new DefaultObserverEx<Observable<? extends T>>(true) {
                    /** The first value arrived? */
                    @GuardedBy("lock")
                    boolean first;
                    /**
                     * The inner exception to forward.
                     * @param ex the exception
                     */
                    void innerError(@Nonnull Throwable ex) {
                        error(ex);
                    }
                    @Override
                    protected void onError(@Nonnull Throwable ex) {
                        observer.error(ex);
                    }
                    @Override
                    protected void onFinish() {
                        if (wip.decrementAndGet() == 0) {
                            observer.finish();
                        }
                    }
                    @Override
                    protected void onNext(Observable<? extends T> value) {
                        if (!first) {
                            first = true;
                            registerOn(value);
                        } else {
                            sourceQueue.add(value);
                        }
                    }

                    void registerOn(@Nonnull Observable<? extends T> value) {
                        wip.incrementAndGet();
                        
                        Observable<? extends U> source = resultSelector.invoke(value);
                        
                        add("source", source.register(new DefaultObserver<U>(lock, true) {
                            @Override
                            public void onError(@Nonnull Throwable ex) {
                                innerError(ex);
                            }

                            @Override
                            public void onFinish() {
                                Observable<? extends T> nextO = sourceQueue.poll();
                                if (nextO != null) {
                                    registerOn(nextO);
                                } else {
                                    if (wip.decrementAndGet() == 0) {
                                        observer.finish();
                                        remove("source");
                                    } else {
                                        first = true;
                                    }
                                }
                            }

                            @Override
                            public void onNext(U value) {
                                observer.next(value);
                            }

                        }));
                    }
                    
                };
                o.registerWith(sources);
                return o;
            }
        }
        /**
         * Concatenate the the multiple sources selected by the indexed function after another.
         * <p><b>Exception semantics:</b> if the sources or any inner observer signals an
         * error, the outer observable will signal that error and the sequence is terminated.</p>
         * @author akarnokd, 2013.01.13.
         * @param <T> the source observable element type
         * @param <U> the result observable element type
         */
        public static class IndexedSelector<T, U> implements Observable<U> {
            /** The source. */
            @Nonnull
            protected final Observable<? extends Observable<? extends T>> sources;
            /** The result selector. */
            @Nonnull
            protected final Func2<? super Observable<? extends T>, ? super Integer, ? extends Observable<? extends U>> resultSelector;

            /**
             * Constructor.
             * @param sources the sources
             * @param resultSelector the indexed concatenation result selector
             */
            public IndexedSelector(
                    Observable<? extends Observable<? extends T>> sources, 
                    Func2<? super Observable<? extends T>, ? super Integer, ? extends Observable<? extends U>> resultSelector) {
                this.sources = sources;
                this.resultSelector = resultSelector;
            }

            @Override
            @Nonnull 
            public Closeable register(@Nonnull final Observer<? super U> observer) {
                final LinkedBlockingQueue<Observable<? extends T>> sourceQueue = new LinkedBlockingQueue<Observable<? extends T>>();
                final AtomicInteger wip = new AtomicInteger(1);
                DefaultObserverEx<Observable<? extends T>> o = new DefaultObserverEx<Observable<? extends T>>(true) {
                    /** The first value arrived? */
                    @GuardedBy("lock")
                    boolean first;
                    /** The index counter for the outer observable. */
                    @GuardedBy("lock")
                    int index;
                    /**
                     * The inner exception to forward.
                     * @param ex the exception
                     */
                    void innerError(@Nonnull Throwable ex) {
                        error(ex);
                    }
                    @Override
                    protected void onError(@Nonnull Throwable ex) {
                        observer.error(ex);
                    }
                    @Override
                    protected void onFinish() {
                        if (wip.decrementAndGet() == 0) {
                            observer.finish();
                        }
                    }
                    @Override
                    protected void onNext(Observable<? extends T> value) {
                        if (!first) {
                            first = true;
                            registerOn(value);
                        } else {
                            sourceQueue.add(value);
                        }
                    }

                    void registerOn(@Nonnull Observable<? extends T> value) {
                        wip.incrementAndGet();
                        
                        Observable<? extends U> source = resultSelector.invoke(value, index++);
                        
                        add("source", source.register(new DefaultObserver<U>(lock, true) {
                            @Override
                            public void onError(@Nonnull Throwable ex) {
                                innerError(ex);
                            }

                            @Override
                            public void onFinish() {
                                Observable<? extends T> nextO = sourceQueue.poll();
                                if (nextO != null) {
                                    registerOn(nextO);
                                } else {
                                    if (wip.decrementAndGet() == 0) {
                                        observer.finish();
                                        remove("source");
                                    } else {
                                        first = true;
                                    }
                                }
                            }

                            @Override
                            public void onNext(U value) {
                                observer.next(value);
                            }

                        }));
                    }
                    
                };
                o.registerWith(sources);
                return o;
            }
        }

    }
}
