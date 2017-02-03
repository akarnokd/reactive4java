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
	 * Computes an aggregated value of the source Ts by applying a 
	 * sum function and applying the divide function when the source
	 * finishes, sending the result to the output.
	 * @param <T> the type of the values
	 * @param <U> the type of the intermediate sum value
	 * @param <V> the type of the final average value
	 * @author akarnokd, 2013.01.13.
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
		 * @param source the source of BigDecimals to aggregate.
		 * @param seed the initieal value for the aggregation
		 * @param accumulator the function which accumulates the input Ts. The first received T will be accompanied by a null U.
		 * @param divide the function which perform the final division based on the number of elements
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
	 * Apply an accumulator function over the observable source and submit the accumulated value to the returned observable.
	 * @param <T> the input element type
	 * @param <U> the output element type
	 * @author akarnokd, 2013.01.13.
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
		 * Constructor.
		 * @param source the source observable
		 * @param seed the initial value of the accumulator
		 * @param accumulator the accumulator function where the first parameter is the current accumulated value and the second is the now received value.
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
	 * Computes an aggregated value of the source Ts by applying a sum function and applying the divide function when the source
	 * finishes, sending the result to the output.
	 * @param <T> the type of the values
	 * @param <U> the type of the intermediate sum value
	 * @param <V> the type of the final average value
	 * @author akarnokd, 2013.01.13.
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
		 * Constructor.
		 * @param source the source of BigDecimals to aggregate.
		 * @param accumulator the function which accumulates the input Ts. The first received T will be accompanied by a null U.
		 * @param divide the function which perform the final division based on the number of elements
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
	 * Apply an accumulator function over the observable source 
	 * and submit the accumulated value to the returned observable at each incoming value.
	 * <p>If the source observable terminates before sending a single value,
	 * the output observable terminates as well. The first incoming value is relayed as-is.</p>
	 * @param <T> the element type
	 * @author akarnokd, 2013.01.13.
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
		 * Constructor.
		 * @param source the source observable
		 * @param accumulator the accumulator function where the first parameter is the current accumulated value and the second is the now received value.
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
	/**
	 * Creates an observable which accumultates the given source and submits each intermediate results to its subscribers.
	 * Example:<br>
	 * <code>range(0, 5).accumulate((x, y) -> x + y)</code> produces a sequence of [0, 1, 3, 6, 10];<br>
	 * basically the first event (0) is just relayed and then every pair of values are simply added together and relayed
	 * @param <T> the element type to accumulate
	 * @author akarnokd, 2013.01.14.
	 */
	public static final class Scan<T> implements Observable<T> {
		/** */
		private final Observable<? extends T> source;
		/** */
		private final Func2<? super T, ? super T, ? extends T> accumulator;

		/**
		 * Constructor.
		 * @param source the source sequence
		 * @param accumulator the accumulator
		 */
		public Scan(Observable<? extends T> source,
				Func2<? super T, ? super T, ? extends T> accumulator) {
			this.source = source;
			this.accumulator = accumulator;
		}

		@Override
		@Nonnull 
		public Closeable register(@Nonnull final Observer<? super T> observer) {
			return source.register(new Observer<T>() {
				/** The current accumulated value. */
				T current;
				/** Are we waiting for the first value? */
				boolean first = true;
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
					if (first) {
						first = false;
						current = value;

					} else {
						current = accumulator.invoke(current, value);
					}
					observer.next(current);
				}
			});
		}
	}
	/**
	 * Creates an observable which accumultates the given source and submits each intermediate results to its subscribers.
	 * Example:<br>
	 * <code>range(0, 5).accumulate(1, (x, y) => x + y)</code> produces a sequence of [1, 2, 4, 7, 11];<br>
	 * basically the accumulation starts from zero and the first value (0) that comes in is simply added
	 * @param <T> the element type to accumulate
	 * @param <U> the accumulation type
	 * @author akarnokd, 2013.01.14.
	 */
	public static final class ScanSeeded<U, T> implements Observable<U> {
		/** */
		private final Observable<? extends T> source;
		/** */
		private final U seed;
		/** */
		private final Func2<? super U, ? super T, ? extends U> accumulator;

		/**
		 * Construction.
		 * @param source the source sequence
		 * @param seed the initinal accumulator seed
		 * @param accumulator the accumulator function
		 */
		public ScanSeeded(
				Observable<? extends T> source, 
				U seed,
				Func2<? super U, ? super T, ? extends U> accumulator) {
			this.source = source;
			this.seed = seed;
			this.accumulator = accumulator;
		}

		@Override
		@Nonnull 
		public Closeable register(@Nonnull final Observer<? super U> observer) {
			return source.register(new Observer<T>() {
				/** The current accumulated value. */
				U current = seed;
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
					current = accumulator.invoke(current, value);
					observer.next(current);
				}
			});
		}
	}
}
