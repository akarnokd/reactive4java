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

import hu.akarnokd.reactive4java.base.Action0;
import hu.akarnokd.reactive4java.base.Action1;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;

import java.io.Closeable;

import javax.annotation.Nonnull;

/**
 * Helper class for invoking actions during an stream
 * of observable sequence.
 * <p>Note: Rx calls these as Do.</p>
 * @author akarnokd, 2013.01.14.
 * @since 0.97
 */
public final class Invoke {
    /** Helper class. */
    private Invoke() { }
    /**
     * Executes an action on the received element in
     * the next() call while relaying events.
     * @author akarnokd, 2013.01.14.
     * @param <T> the element type
     */
    public static class OnNext<T> implements Observable<T> {
        /** */
        private Observable<? extends T> source;
        /** */
        private Action1<? super T> onNext;
        /**
         * Constructor.
         * @param source the source sequence
         * @param onNext the action to invoke
         */
        public OnNext(
                @Nonnull Observable<? extends T> source, 
                @Nonnull Action1<? super T> onNext) {
            this.source = source;
            this.onNext = onNext;
        }
        @Override
        @Nonnull
        public Closeable register(@Nonnull final Observer<? super T> observer) {
            return source.register(new Observer<T>() {
                @Override
                public void next(T value) {
                    onNext.invoke(value);
                    observer.next(value);
                }

                @Override
                public void error(Throwable ex) {
                    observer.error(ex);
                }

                @Override
                public void finish() {
                    observer.finish();
                }
                
            });
        }
        
    }
    /**
     * Invokes an action on the next and error methods
     * while relaying events.
     * @author akarnokd, 2013.01.14.
     * @param <T> the element type
     */
    public static class OnNextError<T> implements Observable<T> {
        /** */
        private Observable<? extends T> source;
        /** */
        private Action1<? super T> onNext;
        /** */
        private Action1<? super Throwable> onError;
        /**
         * Constructor.
         * @param source the source sequence
         * @param onNext the action to invoke
         * @param onError the error action to invoke
         */
        public OnNextError(
                @Nonnull Observable<? extends T> source, 
                @Nonnull Action1<? super T> onNext,
                @Nonnull Action1<? super Throwable> onError) {
            this.source = source;
            this.onNext = onNext;
            this.onError = onError;
        }
        @Override
        @Nonnull
        public Closeable register(@Nonnull final Observer<? super T> observer) {
            return source.register(new Observer<T>() {
                @Override
                public void next(T value) {
                    onNext.invoke(value);
                    observer.next(value);
                }

                @Override
                public void error(Throwable ex) {
                    onError.invoke(ex);
                    observer.error(ex);
                }

                @Override
                public void finish() {
                    observer.finish();
                }
                
            });
        }
    }
    /**
     * Invoke an action on the next and finish methods
     * while relaying events.
     * @author akarnokd, 2013.01.14.
     * @param <T> the element type
     */
    public static class OnNextFinish<T> implements Observable<T> {
        /** */
        private Observable<? extends T> source;
        /** */
        private Action1<? super T> onNext;
        /** */
        private Action0 onFinish;
        
        /**
         * Constructor.
         * @param source the source sequence
         * @param onNext the action to invoke
         * @param onFinish the finish action to invoke
         */
        public OnNextFinish(
                @Nonnull Observable<? extends T> source, 
                @Nonnull Action1<? super T> onNext,
                @Nonnull Action0 onFinish) {
            this.source = source;
            this.onNext = onNext;
            this.onFinish = onFinish;
        }
        @Override
        @Nonnull
        public Closeable register(@Nonnull final Observer<? super T> observer) {
            return source.register(new Observer<T>() {
                @Override
                public void next(T value) {
                    onNext.invoke(value);
                    observer.next(value);
                }

                @Override
                public void error(Throwable ex) {
                    observer.error(ex);
                }

                @Override
                public void finish() {
                    onFinish.invoke();
                    observer.finish();
                }
                
            });
        }
    }
    /**
     * Invoke an action on the next, error and finish methods
     * while relaying events.
     * @author akarnokd, 2013.01.14.
     * @param <T>
     */
    public static class OnNextErrorFinish<T> implements Observable<T> {
        /** */
        private Observable<? extends T> source;
        /** */
        private Action1<? super T> onNext;
        /** */
        private Action0 onFinish;
        /** */
        private Action1<? super Throwable> onError;
        
        /**
         * Constructor.
         * @param source the source sequence
         * @param onNext the action to invoke
         * @param onError the error action to invoke
         * @param onFinish the finish action to invoke
         */
        public OnNextErrorFinish(
                @Nonnull Observable<? extends T> source, 
                @Nonnull Action1<? super T> onNext,
                @Nonnull Action1<? super Throwable> onError,
                @Nonnull Action0 onFinish) {
            this.source = source;
            this.onNext = onNext;
            this.onError = onError;
            this.onFinish = onFinish;
        }
        @Override
        @Nonnull
        public Closeable register(@Nonnull final Observer<? super T> observer) {
            return source.register(new Observer<T>() {
                @Override
                public void next(T value) {
                    onNext.invoke(value);
                    observer.next(value);
                }

                @Override
                public void error(Throwable ex) {
                    onError.invoke(ex);
                    observer.error(ex);
                }

                @Override
                public void finish() {
                    onFinish.invoke();
                    observer.finish();
                }
                
            });
        }
    }
    /**
     * Invoke the methods of the given observer
     * while relaying events.
     * @author akarnokd, 2013.01.14.
     * @param <T> the element type
     */
    public static class OnObserver<T> implements Observable<T> {
        /** */
        private Observable<? extends T> source;
        /** */
        private Observer<? super T> observeBefore;
        /**
         * Constructor.
         * @param source the source sequence
         * @param observeBefore the action to invoke
         */
        public OnObserver(
                @Nonnull Observable<? extends T> source, 
                @Nonnull Observer<? super T> observeBefore) {
            this.source = source;
            this.observeBefore = observeBefore;
        }
        @Override
        @Nonnull
        public Closeable register(@Nonnull final Observer<? super T> observer) {
            return source.register(new Observer<T>() {
                @Override
                public void next(T value) {
                    observeBefore.next(value);
                    observer.next(value);
                }

                @Override
                public void error(Throwable ex) {
                    observeBefore.error(ex);
                    observer.error(ex);
                }

                @Override
                public void finish() {
                    observeBefore.finish();
                    observer.finish();
                }
                
            });
        }
    }
}
