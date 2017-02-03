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

import hu.akarnokd.reactive4java.base.Func2;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;
import hu.akarnokd.reactive4java.util.Closeables;
import hu.akarnokd.reactive4java.util.CompositeCloseable;
import hu.akarnokd.reactive4java.util.DefaultObserverEx;
import hu.akarnokd.reactive4java.util.R4JConfigManager;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;

/**
 * Helper class for the combineLatest operator implementations.
 * @author akarnokd, 2013.01.13.
 * @since 0.97
 */
public final class CombineLatest {
    /** Helper class. */
    private CombineLatest() { }
    /**
     * Returns an observable which combines the latest values of
     * both streams whenever one sends a new value, but only after both sent a value.
     * <p><b>Exception semantics:</b> if any stream throws an exception, the output stream
     * throws an exception and all registrations are terminated.</p>
     * <p><b>Completion semantics:</b> The output stream terminates
     * after both streams terminate.</p>
     * <p>Note that at the beginning, when the left or right fires first, the selector function
     * will receive (value, null) or (null, value). If you want to react only in cases when both have sent
     * a value, use the {@link Sent} class and <code>combineLatest</code> operator.</p>
     * @param <T> the left element type
     * @param <U> the right element type
     * @param <V> the result element type
     * @author akarnokd, 2013.01.13.
     */
    public static final class NullStart<T, U, V> implements Observable<V> {
        /** The result selector. */
        private final Func2<? super T, ? super U, ? extends V> selector;
        /** The left sequence. */
        private final Observable<? extends T> left;
        /** The right sequence. */
        private final Observable<? extends U> right;

        /**
         * Constructor.
         * @param left the left stream
         * @param right the right stream
         * @param selector the function which combines values from both streams and returns a new value
         */
        public NullStart(
                Observable<? extends T> left, 
                Observable<? extends U> right,
                Func2<? super T, ? super U, ? extends V> selector) {
            this.selector = selector;
            this.left = left;
            this.right = right;
        }

        @Override
        @Nonnull 
        public Closeable register(@Nonnull final Observer<? super V> observer) {
            final Lock lock = new ReentrantLock(R4JConfigManager.get().useFairLocks());
            final CompositeCloseable closeBoth = new CompositeCloseable();
            
            final AtomicReference<T> leftRef = new AtomicReference<T>();
            final AtomicReference<U> rightRef = new AtomicReference<U>();
            final AtomicInteger wip = new AtomicInteger(2);
            DefaultObserverEx<T> obs1 = new DefaultObserverEx<T>(lock, false) {

                @Override
                protected void onError(@Nonnull Throwable ex) {
                    observer.error(ex);
                    Closeables.closeSilently(closeBoth);
                }

                @Override
                protected void onFinish() {
                    if (wip.decrementAndGet() == 0) {
                        observer.finish();
                    }
                    close();
                }

                @Override
                protected void onNext(T value) {
                    leftRef.set(value);
                    observer.next(selector.invoke(value, rightRef.get()));
                }

            };
            DefaultObserverEx<U> obs2 = new DefaultObserverEx<U>(lock, false) {

                @Override
                protected void onError(@Nonnull Throwable ex) {
                    observer.error(ex);
                    Closeables.closeSilently(closeBoth);
                }

                @Override
                protected void onFinish() {
                    if (wip.decrementAndGet() == 0) {
                        observer.finish();
                    }
                    close();
                }

                @Override
                protected void onNext(U value) {
                    rightRef.set(value);
                    observer.next(selector.invoke(leftRef.get(), value));
                }

            };
            closeBoth.add(obs1, obs2);
            obs1.registerWith(left);
            obs2.registerWith(right);
            return closeBoth;
        }
    }
    /**
     * Returns an observable which combines the latest values of
     * both streams whenever one sends a new value.
     * <p><b>Exception semantics:</b> if any stream throws an exception, the output stream
     * throws an exception and all registrations are terminated.</p>
     * <p><b>Completion semantics:</b> The output stream terminates
     * after both streams terminate.</p>
     * <p>The function will start combining the values only when both sides have already sent
     * a value.</p>
     * @param <T> the left element type
     * @param <U> the right element type
     * @param <V> the result element type
     * @author akarnokd, 2013.01.13.
     */
    public static final class Sent<V, T, U> implements Observable<V> {
        /** The left source. */
        private final Observable<? extends T> left;
        /** The right source. */
        private final Observable<? extends U> right;
        /** The result selector. */
        private final Func2<? super T, ? super U, ? extends V> selector;

        /**
         * Constructor.
         * @param left the left stream
         * @param right the right stream
         * @param selector the function which combines values from both streams and returns a new value
         */
        public Sent(
                Observable<? extends T> left,
                Observable<? extends U> right,
                Func2<? super T, ? super U, ? extends V> selector
                ) {
            this.right = right;
            this.selector = selector;
            this.left = left;
        }

        @Override
        @Nonnull 
        public Closeable register(@Nonnull final Observer<? super V> observer) {
            final Lock lock = new ReentrantLock(R4JConfigManager.get().useFairLocks());
            final CompositeCloseable closeBoth = new CompositeCloseable();
            final AtomicReference<T> leftRef = new AtomicReference<T>();
            final AtomicBoolean leftFirst = new AtomicBoolean();
            final AtomicReference<U> rightRef = new AtomicReference<U>();
            final AtomicBoolean rightFirst = new AtomicBoolean();
            final AtomicInteger wip = new AtomicInteger(2);
            DefaultObserverEx<T> obs1 = new DefaultObserverEx<T>(lock, false) {

                @Override
                protected void onError(@Nonnull Throwable ex) {
                    observer.error(ex);
                    Closeables.closeSilently(closeBoth);
                }

                @Override
                protected void onFinish() {
                    if (wip.decrementAndGet() == 0) {
                        observer.finish();
                    }
                    close();
                }

                @Override
                protected void onNext(T value) {
                    leftRef.set(value);
                    leftFirst.set(true);
                    if (rightFirst.get()) {
                        observer.next(selector.invoke(value, rightRef.get()));
                    }
                }

            };
            DefaultObserverEx<U> obs2 = new DefaultObserverEx<U>(lock, false) {

                @Override
                protected void onError(@Nonnull Throwable ex) {
                    observer.error(ex);
                    Closeables.closeSilently(closeBoth);
                }

                @Override
                protected void onFinish() {
                    if (wip.decrementAndGet() == 0) {
                        observer.finish();
                    }
                    close();
                }

                @Override
                protected void onNext(U value) {
                    rightRef.set(value);
                    rightFirst.set(true);
                    if (leftFirst.get()) {
                        observer.next(selector.invoke(leftRef.get(), value));
                    }
                }

            };
            closeBoth.add(obs1, obs2);
            obs1.registerWith(left);
            obs2.registerWith(right);
            return closeBoth;
        }
    }
}
