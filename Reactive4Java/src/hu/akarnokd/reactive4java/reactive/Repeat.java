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
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;
import hu.akarnokd.reactive4java.base.Scheduler;
import hu.akarnokd.reactive4java.util.Closeables;
import hu.akarnokd.reactive4java.util.DefaultObserverEx;
import hu.akarnokd.reactive4java.util.DefaultRunnable;

import java.io.Closeable;

import javax.annotation.Nonnull;

/**
 * Contains repeat-like observable sequence implementations.
 * @author akarnokd, 2013.01.13.
 * @since 0.97
 */
public final class Repeat {
	/** Helper class. */
	private Repeat() { }
	/**
	 * Creates an observable which repeatedly calls the given function which generates the Ts indefinitely.
	 * The generator runs on the pool. Note that observers must unregister to stop the infinite loop.
	 * @param <T> the type of elements to produce
	 * @author akarnokd, 2013.01.13.
	 */
	public static class RepeatValue<T> implements Observable<T> {
		/** The pool where the generator loop runs. */
		protected final Scheduler pool;
		/** The function which generates elements. */
		protected final Func0<? extends T> func;
		/**
		 * Constructor.
		 * @param func the function which generates elements
		 * @param pool the pool where the generator loop runs
		 */
		public RepeatValue(Func0<? extends T> func, Scheduler pool) {
			this.func = func;
			this.pool = pool;
		}

		@Override
		@Nonnull 
		public Closeable register(@Nonnull final Observer<? super T> observer) {
			DefaultRunnable r = new DefaultRunnable() {
				@Override
				public void onRun() {
					while (!cancelled()) {
						observer.next(func.invoke());
					}
				}
			};
			return pool.schedule(r);
		}
	}
	/**
	 * Repeatedly registers with the source observable 
	 * if the condition holds on registration.
	 * The condition is checked before each registration.
	 * @param <T> the element type
	 * @author akarnokd, 2013.01.13.
	 */
	public static final class WhileDo<T> implements Observable<T> {
		/** The condition to check. */
		private final Func0<Boolean> condition;
		/** The source sequence. */
		private final Observable<? extends T> source;
		/**
		 * @param source the source sequence
		 * @param condition the condition to check
		 */
		public WhileDo(Observable<? extends T> source, Func0<Boolean> condition) {
			this.condition = condition;
			this.source = source;
		}

		@Override
		@Nonnull
		public Closeable register(@Nonnull final Observer<? super T> observer) {
			DefaultObserverEx<T> obs = new DefaultObserverEx<T>(false) {
				@Override
				protected void onNext(T value) {
					observer.next(value);
				}

				@Override
				protected void onError(@Nonnull Throwable ex) {
					observer.error(ex);

					close();
				}

				@Override
				protected void onFinish() {
					if (condition.invoke()) {
						registerWith(source);
					} else {
						observer.finish();
						
						close();
					}
				}
				
			};
			if (condition.invoke()) {
				return obs.registerWith(source);
			}
			observer.finish();
			return Closeables.emptyCloseable();
		}
	}
	/**
	 * Repeats the given source so long as the condition returns true.
	 * The condition is checked after each completion of the source sequence.
	 * <p>Exception semantics: exception received will stop the repeat process
	 * and is delivered to observers as-is.</p>
	 * @param <T> the element type
	 * @author akarnokd, 2013.01.13.
	 */
	public static final class DoWhile<T> implements Observable<T> {
		/** The source sequence. */
		private final Observable<? extends T> source;
		/** The condition to check. */
		private final Func0<Boolean> condition;

		/**
		 * Constructor.
		 * @param source the source sequence
		 * @param condition the condition to check
		 */
		public DoWhile(@Nonnull Observable<? extends T> source, @Nonnull Func0<Boolean> condition) {
			this.source = source;
			this.condition = condition;
		}

		@Override
		@Nonnull
		public Closeable register(@Nonnull final Observer<? super T> observer) {
			DefaultObserverEx<T> obs = new DefaultObserverEx<T>(false) {
				@Override
				protected void onNext(T value) {
					observer.next(value);
				}

				@Override
				protected void onError(@Nonnull Throwable ex) {
					observer.error(ex);

					close();
				}

				@Override
				protected void onFinish() {
					if (condition.invoke()) {
						registerWith(source);
					} else {
						observer.finish();
						
						close();
					}
				}
				
			};
			return obs.registerWith(source);
		}
	}
}
