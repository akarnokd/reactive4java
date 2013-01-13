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

import java.io.Closeable;

import javax.annotation.Nonnull;

/**
 * Container class for aggregate operators.
 * @author akarnokd, 2013.01.13.
 * @since 0.97
 */
public final class Aggregate {
	/** Helper class. */
	private Aggregate() { }
	/**
	 * @author akarnokd, 2013.01.13.
	 * @param <V>
	 * @param <T>
	 * @param <U>
	 */
	public static final class SeededIndexedProjected<V, T, U> implements
			Observable<V> {
		/**
		 * 
		 */
		private final Func2<? super U, ? super Integer, ? extends V> divide;
		/**
		 * 
		 */
		private final Func2<? super U, ? super T, ? extends U> accumulator;
		/**
		 * 
		 */
		private final Observable<? extends T> source;
		/**
		 * 
		 */
		private final U seed;

		/**
		 * @param divide
		 * @param accumulator
		 * @param source
		 * @param seed
		 */
		public SeededIndexedProjected(
				Observable<? extends T> source,
				U seed,
				Func2<? super U, ? super T, ? extends U> accumulator,
				Func2<? super U, ? super Integer, ? extends V> divide
				) {
			this.divide = divide;
			this.accumulator = accumulator;
			this.source = source;
			this.seed = seed;
		}

		@Override
		@Nonnull
		public Closeable register(@Nonnull final Observer<? super V> observer) {
			return source.register(new Observer<T>() {
				/** The number of values. */
				int count;
				/** The sum of the values thus far. */
				U temp = seed;
				@Override
				public void error(@Nonnull Throwable ex) {
					observer.error(ex);
				}

				@Override
				public void finish() {
					if (count > 0) {
						observer.next(divide.invoke(temp, count));
					}
					observer.finish();
				}

				@Override
				public void next(T value) {
					temp = accumulator.invoke(temp, value);
					count++;
				}

			});
		}
	}
	/**
	 * @author akarnokd, 2013.01.13.
	 * @param <U>
	 * @param <T>
	 */
	public static final class Seeded<U, T> implements Observable<U> {
		/**
		 * 
		 */
		private final Func2<? super U, ? super T, ? extends U> accumulator;
		/**
		 * 
		 */
		private final U seed;
		/**
		 * 
		 */
		private final Observable<? extends T> source;

		/**
		 * @param accumulator
		 * @param seed
		 * @param source
		 */
		public Seeded(
				Observable<? extends T> source,
				U seed, 
				Func2<? super U, ? super T, ? extends U> accumulator
				) {
			this.accumulator = accumulator;
			this.seed = seed;
			this.source = source;
		}

		@Override
		@Nonnull
		public Closeable register(@Nonnull final Observer<? super U> observer) {
			return source.register(new Observer<T>() {
				/** The current aggregation result. */
				U result = seed;
				@Override
				public void error(@Nonnull Throwable ex) {
					observer.error(ex);
				}
				@Override
				public void finish() {
					observer.next(result);
					observer.finish();
				}
				@Override
				public void next(T value) {
					result = accumulator.invoke(result, value);
				}
			});
		}
	}
	/**
	 * @author akarnokd, 2013.01.13.
	 * @param <V>
	 * @param <T>
	 * @param <U>
	 */
	public static final class Projected<V, T, U> implements Observable<V> {
		/**
		 * 
		 */
		private final Func2<? super U, ? super T, ? extends U> accumulator;
		/**
		 * 
		 */
		private final Func2<? super U, ? super Integer, ? extends V> divide;
		/**
		 * 
		 */
		private final Observable<? extends T> source;

		/**
		 * @param accumulator
		 * @param divide
		 * @param source
		 */
		public Projected(
				Observable<? extends T> source,
				Func2<? super U, ? super T, ? extends U> accumulator,
				Func2<? super U, ? super Integer, ? extends V> divide
				) {
			this.accumulator = accumulator;
			this.divide = divide;
			this.source = source;
		}

		@Override
		@Nonnull
		public Closeable register(@Nonnull final Observer<? super V> observer) {
			return source.register(new Observer<T>() {
				/** The number of values. */
				int count;
				/** The sum of the values thus far. */
				U temp;
				@Override
				public void error(@Nonnull Throwable ex) {
					observer.error(ex);
				}

				@Override
				public void finish() {
					if (count > 0) {
						observer.next(divide.invoke(temp, count));
					}
					observer.finish();
				}

				@Override
				public void next(T value) {
					temp = accumulator.invoke(temp, value);
					count++;
				}

			});
		}
	}
	/**
	 * @author akarnokd, 2013.01.13.
	 * @param <T>
	 */
	public static final class Simple<T> implements Observable<T> {
		/**
		 * 
		 */
		private final Observable<? extends T> source;
		/**
		 * 
		 */
		private final Func2<? super T, ? super T, ? extends T> accumulator;

		/**
		 * @param source
		 * @param accumulator
		 */
		public Simple(
				Observable<? extends T> source,
				Func2<? super T, ? super T, ? extends T> accumulator) {
			this.source = source;
			this.accumulator = accumulator;
		}

		@Override
		@Nonnull 
		public Closeable register(@Nonnull final Observer<? super T> observer) {
			return source.register(new Observer<T>() {
				/** The current aggregation result. */
				T result;
				/** How many items did we get */
				int phase;
				@Override
				public void error(@Nonnull Throwable ex) {
					observer.error(ex);
				}
				@Override
				public void finish() {
					if (phase >= 1) {
						observer.next(result);
					}
					observer.finish();
				}
				@Override
				public void next(T value) {
					if (phase == 0) {
						result = value;
						phase++;
					} else {
						result = accumulator.invoke(result, value);
						phase = 2;
					}
				}
			});
		}
	}
}
