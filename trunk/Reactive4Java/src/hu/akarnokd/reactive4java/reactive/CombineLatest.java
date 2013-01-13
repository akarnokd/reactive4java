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
	 * @author akarnokd, 2013.01.13.
	 * @param <V>
	 * @param <T>
	 * @param <U>
	 */
	public static final class NullStart<V, T, U> implements Observable<V> {
		/**
		 * 
		 */
		private final Func2<? super T, ? super U, ? extends V> selector;
		/**
		 * 
		 */
		private final Observable<? extends T> left;
		/**
		 * 
		 */
		private final Observable<? extends U> right;

		/**
		 * @param selector
		 * @param left
		 * @param right
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
			final Lock lock = new ReentrantLock(true);
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
	 * @author akarnokd, 2013.01.13.
	 * @param <V>
	 * @param <T>
	 * @param <U>
	 */
	public static final class Sent<V, T, U> implements Observable<V> {
		/**
		 * 
		 */
		private final Observable<? extends U> right;
		/**
		 * 
		 */
		private final Func2<? super T, ? super U, ? extends V> selector;
		/**
		 * 
		 */
		private final Observable<? extends T> left;

		/**
		 * @param right
		 * @param selector
		 * @param left
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
			final Lock lock = new ReentrantLock(true);
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
