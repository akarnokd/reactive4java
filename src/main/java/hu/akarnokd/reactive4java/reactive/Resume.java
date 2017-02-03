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
import hu.akarnokd.reactive4java.scheduler.NewThreadScheduler;
import hu.akarnokd.reactive4java.util.Closeables;
import hu.akarnokd.reactive4java.util.DefaultObserverEx;
import hu.akarnokd.reactive4java.util.SequentialCloseable;

import java.io.Closeable;
import java.util.Iterator;

import javax.annotation.Nonnull;

/**
 * Helper class for the Reactive.resumeXYZ operators.
 * @author akarnokd, 2013.01.14.
 * @since 0.97
 */
public final class Resume {
    /** Helper class. */
    private Resume() { }
    /**
     * Returns an observable which listens to elements from a source until it signals an error()
     * or finish() and continues with the next observable. The registration happens only when the
     * previous observables finished in any way.
     * @author akarnokd, 2013.01.14.
     * @param <T> the type of the elements
     */
    public static final class Always<T> implements Observable<T> {
        /** The source sequence. */
        private final Iterable<? extends Observable<? extends T>> sources;
        /**
         * Constructor.
         * @param sources the source sequence
         */
        public Always(Iterable<? extends Observable<? extends T>> sources) {
            this.sources = sources;
        }

        @Override
        @Nonnull 
        public Closeable register(@Nonnull final Observer<? super T> observer) {
            final Iterator<? extends Observable<? extends T>> it = sources.iterator();
            if (it.hasNext()) {
                DefaultObserverEx<T> obs = new DefaultObserverEx<T>(false) {
                    @Override
                    public void onError(@Nonnull Throwable ex) {
                        remove(this);
                        if (it.hasNext()) {
                            registerWith(it.next());
                        } else {
                            observer.finish();
                            close();
                        }
                    }

                    @Override
                    public void onFinish() {
                        remove(this);
                        if (it.hasNext()) {
                            registerWith(it.next());
                        } else {
                            observer.finish();
                            close();
                        }
                    }
                    @Override
                    public void onNext(T value) {
                        observer.next(value);
                    }
                };
                return obs.registerWith(it.next());
            }
            observer.finish();
            return Closeables.emptyCloseable();
        }
    }
    /**
     * It tries to submit the values of first observable, but when it throws an exeption,
     * the next observable within source is used further on. Basically a failover between the Observables.
     * If the current source finish() then the result observable calls finish().
     * If the last of the sources calls error() the result observable calls error()
     * @param <T> the type of the values
     * @author akarnokd, 2013.01.14.
     */
    public static final class OnError<T> implements Observable<T> {
        /** The source sequence. */
        public final Iterable<? extends Observable<? extends T>> sources;

        /**
         * Constructor.
         * @param sources the source sequences
         */
        public OnError(Iterable<? extends Observable<? extends T>> sources) {
            this.sources = sources;
        }

        @Override
        @Nonnull 
        public Closeable register(@Nonnull final Observer<? super T> observer) {
            final Iterator<? extends Observable<? extends T>> it = sources.iterator();
            if (it.hasNext()) {
                DefaultObserverEx<T> obs = new DefaultObserverEx<T>(false) {
                    @Override
                    public void onError(@Nonnull Throwable ex) {
                        if (it.hasNext()) {
                            registerWith(it.next());
                        } else {
                            observer.error(ex);
                            close();
                        }
                    }

                    @Override
                    public void onFinish() {
                        observer.finish();
                        close();
                    }
                    @Override
                    public void onNext(T value) {
                        observer.next(value);
                    }
                };
                return obs.registerWith(it.next());
            }
            observer.finish();
            return Closeables.emptyCloseable();
        }
    }
    /**
     * Continues the observable sequence in case of exception
     * whith the sequence provided by the function for that particular
     * exception.
     * <p>Exception semantics: in case of an exception in source,
     * the exception is turned into a continuation, but the second
     * observable's error now terminates the sequence.
     * @author akarnokd, 2013.01.14.
     * @param <T> the source and result element type
     */
    public static class Conditionally<T> implements Observable<T> {
        /** The source sequence. */
        protected final Observable<? extends T> source;
        /** The exception handler. */
        protected final Func1<? super Throwable, ? extends Observable<? extends T>> handler;
        /**
         * Constructor.
         * @param source The source sequence
         * @param handler The exception handler
         */
        public Conditionally(
                Observable<? extends T> source,
                Func1<? super Throwable, ? extends Observable<? extends T>> handler) {
            super();
            this.source = source;
            this.handler = handler;
        }

        @Override
        @Nonnull
        public Closeable register(@Nonnull final Observer<? super T> observer) {
            final NewThreadScheduler pool = new NewThreadScheduler();
            final Observable<T> ssource = Reactive.registerOn(source, 
                    pool);
            final SequentialCloseable close = new SequentialCloseable();
            DefaultObserverEx<T> obs = new DefaultObserverEx<T>(true) {
                @Override
                protected void onNext(T value) {
                    observer.next(value);
                }

                @Override
                protected void onError(Throwable ex) {
                    remove(this);
                    Observable<? extends T> alter = 
                            Reactive.registerOn(handler.invoke(ex), pool);
                    close.set(alter.register(observer));
                }

                @Override
                protected void onFinish() {
                    observer.finish();
                }
                
            };
            close.set(obs.registerWith(ssource));
            return close;
        }
    }
    /**
     * Restarts the observation until the source observable terminates normally 
     * or the <code>count</code> retry count was used up.
     * @param <T> the type of elements
     * @author akarnokd, 2013.01.14.
     */
    public static final class RetryCount<T> implements Observable<T> {
        /** */
        protected final Observable<? extends T> source;
        /** */
        protected final int count;

        /**
         * Constructor.
         * @param source the source sequence
         * @param count the number of retries
         */
        public RetryCount(Observable<? extends T> source, int count) {
            this.source = source;
            this.count = count;
        }

        @Override
        @Nonnull 
        public Closeable register(@Nonnull final Observer<? super T> observer) {
            final Observable<T> ssource = Reactive.registerOn(source, 
                    new NewThreadScheduler());
            DefaultObserverEx<T> obs = new DefaultObserverEx<T>(false) {
                /** The remaining retry count. */
                int remainingCount = count;
                @Override
                public void onError(@Nonnull Throwable ex) {
                    if (remainingCount-- > 0) {
                        registerWith(ssource);
                    } else {
                        observer.error(ex);
                        close();
                    }
                }

                @Override
                public void onFinish() {
                    observer.finish();
                    close();
                }

                @Override
                public void onNext(T value) {
                    observer.next(value);
                }

            };
            return obs.registerWith(ssource);
        }
    }
    /**
     * Restarts the observation until the source observable terminates normally.
     * @param <T> the type of elements
     * @author akarnokd, 2013.01.14.
     */
    public static final class Retry<T> implements Observable<T> {
        /** */
        protected final Observable<? extends T> source;

        /**
         * Constructor.
         * @param source the source sequence
         */
        public Retry(Observable<? extends T> source) {
            this.source = source;
        }

        @Override
        @Nonnull 
        public Closeable register(@Nonnull final Observer<? super T> observer) {
            final Observable<T> ssource = Reactive.registerOn(source, 
                    new NewThreadScheduler());
            DefaultObserverEx<T> obs = new DefaultObserverEx<T>(false) {
                @Override
                public void onError(@Nonnull Throwable ex) {
                    registerWith(ssource);
                }

                @Override
                public void onFinish() {
                    observer.finish();
                    close();
                }
                @Override
                public void onNext(T value) {
                    observer.next(value);
                }
            };
            return obs.registerWith(ssource);
        }
    }
}
