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

import hu.akarnokd.reactive4java.base.Func0;
import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Func2;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;
import hu.akarnokd.reactive4java.util.DefaultObserverEx;
import hu.akarnokd.reactive4java.util.Observers;
import hu.akarnokd.reactive4java.util.SingleCloseable;

import java.io.Closeable;

import javax.annotation.Nonnull;

/**
 * Helper class of select and few similar operators.
 * @author akarnokd, 2013.01.15.
 * @since 0.97
 */
public final class Select {
    /** Helper class. */
    private Select() { }
    // #GWT-IGNORE-START
    /**
     * Casts the values of the source sequence into the
     * given type via the type token. ClassCastExceptions
     * are relayed through the error method and the stream
     * is stopped.
     * @param <T> the type of the expected values
     * @author akarnokd, 2013.01.15.
     */
    public static final class CastToken<T> implements Observable<T> {
        /** */
        protected final Observable<?> source;
        /** */
        protected final Class<T> token;

        /**
         * Constructor.
         * @param source the source of unknown elements
         * @param token the token to test agains the elements
         */
        public CastToken(Observable<?> source, Class<T> token) {
            this.source = source;
            this.token = token;
        }

        @Override
        @Nonnull 
        public Closeable register(@Nonnull final Observer<? super T> observer) {
            return (new DefaultObserverEx<Object>() {
                @Override
                public void onError(@Nonnull Throwable ex) {
                    observer.error(ex);
                }

                @Override
                public void onFinish() {
                    observer.finish();
                }

                @Override
                public void onNext(Object value) {
                    try {
                        observer.next(token.cast(value));
                    } catch (ClassCastException ex) {
                        error(ex);
                    }
                }

            }).registerWith(source);
        }
    }
    // #GWT-IGNORE-END
    /**
     * Casts the values of the source sequence into the target type. 
     * ClassCastExceptions
     * are relayed through the error method and the stream
     * is stopped.
     * <p>Note that generics information is erased, the
     * actual exception might come from much deeper of the
     * operator chain.</p>
     * @param <T> the type of the expected values
     * @author akarnokd, 2013.01.15.
     */
    public static final class Cast<T> implements Observable<T> {
        /** */
        protected final Observable<?> source;

        /**
         * Constructor.
         * @param source the source of unknown elements
         */
        public Cast(Observable<?> source) {
            this.source = source;
        }

        @Override
        @Nonnull 
        public Closeable register(@Nonnull final Observer<? super T> observer) {
            return (new DefaultObserverEx<Object>() {
                @Override
                public void onError(@Nonnull Throwable ex) {
                    observer.error(ex);
                }

                @Override
                public void onFinish() {
                    observer.finish();
                }

                @Override
                @SuppressWarnings("unchecked")
                public void onNext(Object value) {
                    try {
                        observer.next((T)value);
                    } catch (ClassCastException ex) {
                        error(ex);
                    }
                }

            }).registerWith(source);
        }
    }
    /**
     * Returns a single value produced by a function in case the source
     * observable is empty.
     * @author akarnokd, 2013.01.15.
     * @param <T> the element type
     */
    public static class DefaultIfEmptyFunc<T> implements Observable<T> {
        /** */
        private Observable<? extends T> source;
        /** */
        private Func0<? extends T> defaultFunc;
        /**
         * Constructor.
         * @param source the source sequence.
         * @param defaultFunc the default value producer
         */
        public DefaultIfEmptyFunc(@Nonnull Observable<? extends T> source, 
                @Nonnull Func0<? extends T> defaultFunc) {
            this.source = source;
            this.defaultFunc = defaultFunc;
        }
        @Override
        @Nonnull
        public Closeable register(@Nonnull final Observer<? super T> observer) {
            return source.register(new Observer<T>() {
                /** Remember if any elements were sent. */
                boolean isEmpty = true;
                @Override
                public void next(T value) {
                    isEmpty = false;
                    observer.next(value);
                }

                @Override
                public void error(Throwable ex) {
                    observer.error(ex);
                }

                @Override
                public void finish() {
                    if (isEmpty) {
                        observer.next(defaultFunc.invoke());
                    }
                    observer.finish();
                }
                
            });
        }
    }
    /**
     * Transforms the elements of the source observable into 
     * Us by using a selector which receives an index indicating
     * how many elements have been transformed this far.
     * @author akarnokd, 2013.01.15.
     * @param <T> the source element type
     * @param <U> the output element type
     */
    public static final class Indexed<T, U> implements Observable<U> {
        /** */
        private final Func2<? super T, ? super Integer, ? extends U> selector;
        /** */
        private final Observable<? extends T> source;

        /**
         * Constructor.
         * @param source the source observable
         * @param selector the selector taking an index and the current T
         */
        public Indexed(
                Observable<? extends T> source,
                Func2<? super T, ? super Integer, ? extends U> selector) {
            this.selector = selector;
            this.source = source;
        }

        @Override
        @Nonnull 
        public Closeable register(@Nonnull final Observer<? super U> observer) {
            final SingleCloseable self = new SingleCloseable();
            self.set(Observers.registerSafe(source, new Observer<T>() {
                /** The running index. */
                int index;
                @Override
                public void error(@Nonnull Throwable ex) {
                    observer.error(ex);
                    self.closeSilently();
                }

                @Override
                public void finish() {
                    observer.finish();
                    self.closeSilently();
                }

                @Override
                public void next(T value) {
                    try {
                        observer.next(selector.invoke(value, index++));
                    } catch (Throwable t) {
                        error(t);
                    }
                }

            }));
            return self;
        }
    }
    /**
     * Use the mapper to transform the T source into an U source.
     * @param <T> the type of the original observable
     * @param <U> the type of the new observable
     * @author akarnokd, 2013.01.15.
     */
    public static final class Simple<T, U> implements Observable<U> {
        /** */
        private final Func1<? super T, ? extends U> mapper;
        /** */
        private final Observable<? extends T> source;

        /**
         * Constructor.
         * @param source the source of Ts
         * @param mapper the mapper from Ts to Us
         */
        public Simple(
                Observable<? extends T> source,
                Func1<? super T, ? extends U> mapper
                ) {
            this.mapper = mapper;
            this.source = source;
        }

        @Override
        @Nonnull 
        public Closeable register(@Nonnull final Observer<? super U> observer) {
            final SingleCloseable self = new SingleCloseable();
            self.set(Observers.registerSafe(source, new Observer<T>() {
                @Override
                public void error(@Nonnull Throwable ex) {
                    observer.error(ex);
                    self.closeSilently();
                }

                @Override
                public void finish() {
                    observer.finish();
                    self.closeSilently();
                }

                @Override
                public void next(T value) {
                    try {
                        observer.next(mapper.invoke(value));
                    } catch (Throwable t) {
                        error(t);
                    }
                }

            }));
            return self;
        }
    }
    /**
     * Transforms the elements of the source observable into 
     * Us by using a selector which receives an index indicating
     * how many elements have been transformed this far.
     * @author akarnokd, 2013.01.15.
     * @param <T> the source element type
     * @param <U> the output element type
     */
    public static final class LongIndexed<T, U> implements Observable<U> {
        /** */
        private final Func2<? super T, ? super Long, ? extends U> selector;
        /** */
        private final Observable<? extends T> source;

        /**
         * Constructor.
         * @param source the source observable
         * @param selector the selector taking an index and the current T
         */
        public LongIndexed(
                Observable<? extends T> source,
                Func2<? super T, ? super Long, ? extends U> selector) {
            this.selector = selector;
            this.source = source;
        }

        @Override
        @Nonnull 
        public Closeable register(@Nonnull final Observer<? super U> observer) {
            final SingleCloseable self = new SingleCloseable();
            self.set(Observers.registerSafe(source, new Observer<T>() {
                /** The running index. */
                long index;
                @Override
                public void error(@Nonnull Throwable ex) {
                    observer.error(ex);
                    self.closeSilently();
                }

                @Override
                public void finish() {
                    observer.finish();
                    self.closeSilently();
                }

                @Override
                public void next(T value) {
                    try {
                        observer.next(selector.invoke(value, index++));
                    } catch (Throwable t) {
                        error(t);
                    }
                }

            }));
            return self;
        }
    }
}
