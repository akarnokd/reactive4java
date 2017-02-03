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
import hu.akarnokd.reactive4java.util.Closeables;
import hu.akarnokd.reactive4java.util.DefaultObserver;
import hu.akarnokd.reactive4java.util.DefaultObserverEx;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

/**
 * Helper class for Reactive.selectMany operators.
 * @author akarnokd, 2013.01.15.
 * @since 0.97
 */
public final class SelectMany {
	/** Helper class. */
	private SelectMany() { }
	/**
	 * Transform the given source of Ts into Us in a way that the selector might return zero to multiple elements of Us for a single T.
	 * The iterable is flattened and submitted to the output
	 * @param <T> the input element type
	 * @param <U> the iterable sequence's type
	 * @param <V> the output element type
	 * @author akarnokd, 2013.01.15.
	 */
	public static final class WithIterable<T, U, V> implements Observable<V> {
		/** */
		private final Func1<? super T, ? extends Iterable<? extends U>> selector;
		/** */
		private final Observable<? extends T> source;
		/** */
		private Func2<? super T, ? super U, ? extends V> resultSelector;

		/**
		 * Constructor.
		 * @param source the source sequence
		 * @param selector the selector function for each iterable
		 * @param resultSelector the selector function for a pair of source and iterable
		 */
		public WithIterable(
				Observable<? extends T> source,
				Func1<? super T, ? extends Iterable<? extends U>> selector,
				Func2<? super T, ? super U, ? extends V> resultSelector		
				) {
			this.selector = selector;
			this.source = source;
			this.resultSelector = resultSelector;
		}

		@Override
		@Nonnull 
		public Closeable register(@Nonnull final Observer<? super V> observer) {
			return source.register(new Observer<T>() {

				@Override
				public void error(@Nonnull Throwable ex) {
					observer.error(ex);
				}

				@Override
				public void finish() {
					observer.finish();
				}

				@Override
				public void next(T value) {
					for (U u : selector.invoke(value)) {
						observer.next(resultSelector.invoke(value, u));					}
				}

			});
		}
	}
	/**
	 * Creates an observable in which for each of Ts an observable of Vs are
	 * requested which in turn will be transformed by the resultSelector for each
	 * pair of T and V giving an U.
	 * @param <T> the source element type
	 * @param <U> the intermediate element type
	 * @param <V> the output element type
	 * @author akarnokd, 2013.01.15.
	 */
	public static final class Paired<T, U, V> implements Observable<V> {
		/** */
		private final Func1<? super T, ? extends Observable<? extends U>> collectionSelector;
		/** */
		private final Func2<? super T, ? super U, ? extends V> resultSelector;
		/** */
		private final Observable<? extends T> source;

		/**
		 * Constructor.
		 * @param source the source of Ts
		 * @param collectionSelector the selector which returns an observable of intermediate Vs
		 * @param resultSelector the selector which gives an U for a T and V
		 */
		public Paired(
				Observable<? extends T> source,
				Func1<? super T, ? extends Observable<? extends U>> collectionSelector,
				Func2<? super T, ? super U, ? extends V> resultSelector
				) {
			this.collectionSelector = collectionSelector;
			this.resultSelector = resultSelector;
			this.source = source;
		}

		@Override
		@Nonnull 
		public Closeable register(@Nonnull final Observer<? super V> observer) {
			DefaultObserverEx<T> obs = new DefaultObserverEx<T>(false) {
				/** The work in progress counter. */
				final AtomicInteger wip = new AtomicInteger(1);
				/** The active observers. */
				final Map<DefaultObserver<? extends U>, Closeable> active = new HashMap<DefaultObserver<? extends U>, Closeable>();
				@Override
				protected void onClose() {
					for (Closeable c : active.values()) {
						Closeables.closeSilently(c);
					}
				}

				@Override
				public void onError(@Nonnull Throwable ex) {
					observer.error(ex);
					close();
				}
				@Override
				public void onFinish() {
					onLast();
				}
				/**
				 * The error signal from the inner.
				 * @param ex the exception
				 */
				void onInnerError(Throwable ex) {
					onError(ex);
				}
				/** The last one will signal a finish. */
				public void onLast() {
					if (wip.decrementAndGet() == 0) {
						observer.finish();
						close();
					}
				}
				@Override
				public void onNext(final T t) {
					Observable<? extends U> sub = collectionSelector.invoke(t);
					DefaultObserver<U> o = new DefaultObserver<U>(lock, true) {
						@Override
						protected void onClose() {
							active.remove(this);
						}

						@Override
						protected void onError(@Nonnull Throwable ex) {
							onInnerError(ex);
							close();
						}

						@Override
						protected void onFinish() {
							onLast();
							close();
						}
						@Override
						protected void onNext(U u) {
							observer.next(resultSelector.invoke(t, u));
						}
					};
					wip.incrementAndGet();
					active.put(o, sub.register(o));
				}
			};
			return obs.registerWith(source);
		}
	}

}
