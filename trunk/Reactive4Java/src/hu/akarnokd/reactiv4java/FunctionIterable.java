/*
 * Copyright 2011 David Karnok
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

package hu.akarnokd.reactiv4java;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * The functional iterable support with anamorphism, bind and catamorphism.
 * http://blogs.bartdesmet.net/blogs/bart/archive/2010/01/01/the-essence-of-linq-minlinq.aspx
 * @author akarnokd
 */
public final class FunctionIterable {
	/** Utility class. */
	private FunctionIterable() {
		// utility class
	}
	/**
	 * Returns a function of function that returns an empty option.
	 * @param <T> the type of the value (irrelevant)
	 * @return the function of function of option
	 */
	public static <T> Func0<Func0<Option<T>>> empty() {
		return new Func0<Func0<Option<T>>>() {
			@Override
			public Func0<Option<T>> invoke() {
				return new Func0<Option<T>>() {
					@Override
					public Option<T> invoke() {
						return Option.none();
					}
				};
			}
		};
	}
	/**
	 * Returns a fuction of function that returns the single value presented.
	 * @param <T> the type of the value
	 * @param value the single value to return
	 * @return the function of function of option of something
	 */
	public static <T> Func0<Func0<Option<T>>> single(final T value) {
		return new Func0<Func0<Option<T>>>() {
			boolean once;
			@Override
			public Func0<Option<T>> invoke() {
				return new Func0<Option<T>>() {
					@Override
					public Option<T> invoke() {
						if (once) {
							 return Option.none();
						}
						once = true;
						return Option.some(value);
					}
				};
			}
		};
	}
	/**
	 * The generic binding operator to convert a source of Ts into a Rs.
	 * @param <T> the source element type
	 * @param <R> the output element type
	 * @param source the source creator
	 * @param selector the selector to produce Rs out of Ts.
	 * @return a function to function to option of R.
	 */
	public static <T, R> Func0<Func0<Option<R>>> bind(final Func0<Func0<Option<T>>> source, final Func1<Func0<Func0<Option<R>>>, T> selector) {
		return new Func0<Func0<Option<R>>>() {
			@Override
			public Func0<Option<R>> invoke() {
				
				return new Func0<Option<R>>() {
					/** The source of Ts. */
					final Func0<Option<T>> e = source.invoke();
					/** The last option of T we got. */
					Option<T> lastOuter = Option.<T>none();
					/** The last R we produced. */
					Option<R> lastInner = Option.<R>none();
					/** The source of Rs. */
					Func0<Option<R>> innerE = null;
					@Override
					public Option<R> invoke() {
						do {
							while (lastInner == Option.none()) {
								lastOuter = e.invoke();
								if (lastOuter == Option.none()) {
									return Option.none();
								} else {
									innerE = selector.invoke(lastOuter.value()).invoke();
								}
								lastInner = innerE.invoke();
								if (lastInner != Option.none()) {
									return lastInner;
								}
							}
							lastInner = innerE.invoke();
						} while (lastInner == Option.none());
						
						return lastInner;
					}
				};
			}
		};
	}
	/**
	 * Anamorphism that creates Ts starting from the seed value until the condition holds.
	 * Its equivalent is a for loop: for (int i = 0; i &lt; 10; i++) 
	 * @param <T> the type of the values to generate
	 * @param seed the initial value
	 * @param condition the condition until the Ts should be produced
	 * @param next the way of compute the next T
	 * @return the function of founction of option of T
	 */
	public static <T> Func0<Func0<Option<T>>> ana(final T seed, final Func1<Boolean, T> condition, final Func1<T, T> next) {
		return new Func0<Func0<Option<T>>>() {
			@Override
			public Func0<Option<T>> invoke() {
				return new Func0<Option<T>>() {
					/** Current value. */
					Option<T> value = Option.none();
					@Override
					public Option<T> invoke() {
						value = Option.some(value == Option.none() ? seed : next.invoke(value.value()));
						return condition.invoke(value.value()) ? value : Option.<T>none();
					}
				};
			}
		};
	}
	/**
	 * The empty operator expressed by using the anamorphism.
	 * @param <T> the type of the value (irrelevant)
	 * @return the function of function of option of T
	 */
	public static <T> Func0<Func0<Option<T>>> emptyAna() {
		Func1<Boolean, T> condition = Functions.alwaysFalse(); // type inference issues
		return ana(null, condition, null);
	}
	/**
	 * The single operator expressed by using anamorphism.
	 * @param <T> the type of the value
	 * @param value the value to return once
	 * @return the function of function of option T
	 */
	public static <T> Func0<Func0<Option<T>>> singleAna(T value) {
		Func1<T, T> next = Functions.identity(); // type inference issues
		return ana(value, new Func1<Boolean, T>() {
			/** Return true only once. */
			boolean once;
			@Override
			public Boolean invoke(T param1) {
				if (once) {
					return false;
				}
				once = true;
				return true;
			};
		}, next);
	}
	/**
	 * A catamorphism which creates a single R out of the sequence of Ts by using an aggregator.
	 * The method is a greedy operation: it must wait all source values to arrive, therefore, do not use it on infinite sources.
	 * @param <T> the type of the sequence values
	 * @param <R> the output type
	 * @param source the source of the sequence values
	 * @param seed the initial value of the aggregation (e.g., start from zero)
	 * @param aggregator the aggregator function which takes a sequence value and the previous output value and produces a new output value
	 * @return the aggregation result
	 */
	public static <T, R> R cata(Func0<Func0<Option<T>>> source, R seed, Func2<R, R, T> aggregator) {
		Func0<Option<T>> e = source.invoke();
		
		R result = seed;
		Option<T> value = null;
		while ((value = e.invoke()) != Option.none()) {
			result = aggregator.invoke(result, value.value());
		}
		return result;
	}
	/**
	 * Convert a regular iterable instance into a functional-iterable format.
	 * @param <T> the type of the iterable
	 * @param iterable the iterable object
	 * @return the function of function of option of T
	 */
	public static <T> Func0<Func0<Option<T>>> asFIterable(final Iterable<T> iterable) {
		return new Func0<Func0<Option<T>>>() {
			@Override
			public Func0<Option<T>> invoke() {
				return new Func0<Option<T>>() {
					/** The iterator. */
					Iterator<T> it = iterable.iterator();
					@Override
					public Option<T> invoke() {
						return it.hasNext() ? Option.some(it.next()) : Option.<T>none();
					}
				};
			}
		};
	}
	/**
	 * Convert the submitted array into a functional-iterable format.
	 * @param <T> the array element type
	 * @param array the array of elements
	 * @return the function of function of option of T
	 */
	public static <T> Func0<Func0<Option<T>>> asFIterable(final T... array) {
		return new Func0<Func0<Option<T>>>() {
			@Override
			public Func0<Option<T>> invoke() {
				return new Func0<Option<T>>() {
					int index = 0;
					@Override
					public Option<T> invoke() {
						if (index < array.length) {
							return Option.some(array[index++]);
						}
						return Option.none();
					}
				};
			}
		};
	}
	/**
	 * Convert a function of function of option of T into a simple iterable object.
	 * @param <T> the type of the values
	 * @param source the source of the values
	 * @return the iterable of the values
	 */
	public static <T> Iterable<T> asIterable(final Func0<Func0<Option<T>>> source) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					/** The value source. */
					Func0<Option<T>> e = source.invoke();
					/** The peek value due hasNext. */
					Option<T> peek;
					/** Indicator if there was a hasNext() call before the next() call. */
					boolean peekBeforeNext;
					@Override
					public boolean hasNext() {
						if (peek != Option.none()) {
							if (!peekBeforeNext) {
								peek = e.invoke();
							}
							peekBeforeNext = true;
						}
						return peek != Option.none();
					}
					@Override
					public T next() {
						if (peekBeforeNext) {
							peekBeforeNext = false;
							if (peek != Option.none()) {
								return peek.value();
							}
							throw new NoSuchElementException();
						}
						peekBeforeNext = false;
						if (peek != Option.none()) {
							peek = e.invoke();
							if (peek != Option.none()) {
								return peek.value();
							}
						}
						throw new NoSuchElementException();
					}
					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}
}
