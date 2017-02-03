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
import hu.akarnokd.reactive4java.util.DefaultObserverEx;

import java.io.Closeable;

import javax.annotation.Nonnull;

/**
 * Helper class for containment testing operators.
 * @author akarnokd, 2013.01.13.
 * @since 0.97
 */
public final class Containment {
    /** Helper class. */
    private Containment() { }
    /**
     * Signals a single true if the source observable contains any element.
     * It might return early for a non-empty source but waits for the entire observable to return false.
     * @param <T> the type of the source data
     * @author akarnokd, 2013.01.13.
     */
    public static final class Any<T> implements Observable<Boolean> {
        /** Source sequence. */
        private final Observable<? extends T> source;
        /** The predicate. */
        private final Func1<? super T, Boolean> predicate;

        /**
         * Constructor.
         * @param source the source observable
         * @param predicate the predicate to satisfy
         */
        public Any(Observable<? extends T> source, Func1<? super T, Boolean> predicate) {
            this.source = source;
            this.predicate = predicate;
        }

        @Override
        @Nonnull
        public Closeable register(@Nonnull final Observer<? super Boolean> observer) {
            DefaultObserverEx<T> obs = new DefaultObserverEx<T>(true) {
                @Override
                public void onError(@Nonnull Throwable ex) {
                    observer.error(ex);
                    close();
                }

                @Override
                public void onFinish() {
                    observer.next(false);
                    observer.finish();
                    close();
                }

                @Override
                public void onNext(T value) {
                    if (predicate.invoke(value)) {
                        observer.next(true);
                        observer.finish();
                        close();
                    }
                }

            };
            return obs.registerWith(source);
        }
    }
    /**
     * Signals a single true or false if all elements of the observable match the predicate.
     * It may return early with a result of false if the predicate simply does not match the current element.
     * For a true result, it waits for all elements of the source observable.
     * @author akarnokd, 2013.01.13.
     * @param <T> the source element type
     */
    public static final class All<T> implements Observable<Boolean> {
        /** The predicate. */
        private final Func1<? super T, Boolean> predicate;
        /** The source sequence. */
        private final Observable<? extends T> source;

        /**
         * Constructor.
         * @param source Ths source
         * @param predicate the predicate function
         */
        public All(
                @Nonnull Observable<? extends T> source,
                @Nonnull Func1<? super T, Boolean> predicate) {
            this.predicate = predicate;
            this.source = source;
        }

        @Override
        @Nonnull
        public Closeable register(@Nonnull final Observer<? super Boolean> observer) {
            DefaultObserverEx<T> o = new DefaultObserverEx<T>(true) {
                /** Indicate if we returned early. */
                boolean done;
                @Override
                public void onError(@Nonnull Throwable ex) {
                    observer.error(ex);
                }
                @Override
                public void onFinish() {
                    if (!done) {
                        done = true;
                        observer.next(true);
                        observer.finish();
                    }
                }
                @Override
                public void onNext(T value) {
                    if (!predicate.invoke(value)) {
                        done = true;
                        observer.next(false);
                        observer.finish();
                    }
                }
            };
            return o.registerWith(source);
        }
    }

}
