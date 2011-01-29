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

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Helper class with function types.
 * @author akarnokd
 */
public final class Functions {
	/** Utility class. */
	private Functions() {
		// TODO Auto-generated constructor stub
	}
	/** Constant function returning always false. */
	private static final Func1<Boolean, Void> FALSE1 = new Func1<Boolean, Void>() {
		@Override
		public Boolean invoke(Void param1) {
			return false;
		}
	};
	/** Constant function returning always true. */
	private static final Func1<Boolean, Void> TRUE1 = new Func1<Boolean, Void>() {
		@Override
		public Boolean invoke(Void param1) {
			return true;
		}
	};
	/** Constant parameterless function which returns always false. */
	public static final Func0<Boolean> FALSE = new Func0<Boolean>() {
		@Override
		public Boolean invoke() {
			return false;
		}
	};
	/** Constant parameterless function which returns always true. */
	public static final Func0<Boolean> TRUE = new Func0<Boolean>() {
		@Override
		public Boolean invoke() {
			return true;
		}
	};
	/** The identity function which returns its parameter. */
	private static final Func1<Object, Object> IDENTITY = new Func1<Object, Object>() {
		@Override
		public Object invoke(Object param1) {
			return param1;
		}
	};
	/**
	 * Returns a function which always returns true regardless of its parameters.
	 * @param <T> the type of the parameter (irrelevant)
	 * @return the function which returns always true
	 */
	@SuppressWarnings("unchecked")
	public static <T> Func1<Boolean, T> alwaysTrue() {
		return (Func1<Boolean, T>)TRUE1;
	}
	/**
	 * Returns a function which always returns false regardless of its parameters.
	 * @param <T> the type of the parameter (irrelevant)
	 * @return the function which returns always false
	 */
	@SuppressWarnings("unchecked")
	public static <T> Func1<Boolean, T> alwaysFalse() {
		return (Func1<Boolean, T>)FALSE1;
	}
	/**
	 * Returns a function which returns its parameter value.
	 * @param <T> the type of the object
	 * @return the function which returns its parameter as is
	 */
	@SuppressWarnings("unchecked")
	public static <T> Func1<T, T> identity() {
		return (Func1<T, T>)IDENTITY;
	}
	/**
	 * Creates an integer iterator which returns numbers from the start position in the count size.
	 * @param start the starting value.
	 * @param count the number of elements to return, negative count means counting down from the start.
	 * @return the iterator.
	 */
	public static Iterable<Integer> range(final int start, final int count) {
		return new Iterable<Integer>() {
			@Override
			public Iterator<Integer> iterator() {
				return new Iterator<Integer>() {
					int current = start;
					final boolean down = count < 0;
					@Override
					public boolean hasNext() {
						return down ? current > start + count : current < start + count;
					}
					@Override
					public Integer next() {
						if (hasNext()) {
							if (down) {
								return current--;
							}
							return current++;
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
	/**
	 * Creates an long iterator which returns numbers from the start position in the count size.
	 * @param start the starting value.
	 * @param count the number of elements to return, negative count means counting down from the start.
	 * @return the iterator.
	 */
	public static Iterable<Long> range(final long start, final long count) {
		return new Iterable<Long>() {
			@Override
			public Iterator<Long> iterator() {
				return new Iterator<Long>() {
					long current = start;
					final boolean down = count < 0;
					@Override
					public boolean hasNext() {
						return down ? current > start + count : current < start + count;
					}
					@Override
					public Long next() {
						if (hasNext()) {
							if (down) {
								return current--;
							}
							return current++;
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
	/**
	 * Create a function which returns true for submitted values less than the given value.
	 * @param value the maxium value to include
	 * @return the function
	 */
	public static Func1<Boolean, Integer> lessThan(final int value) {
		return new Func1<Boolean, Integer>() {
			@Override
			public Boolean invoke(Integer param1) {
				return param1.intValue() < value;
			}
		};
	}
	/**
	 * Create a function which returns true for submitted values greater or equal than the given value.
	 * @param value the maxium value to include
	 * @return the function
	 */
	public static Func1<Boolean, Integer> greaterOrEqual(final int value) {
		return new Func1<Boolean, Integer>() {
			@Override
			public Boolean invoke(Integer param1) {
				return param1.intValue() >= value;
			}
		};
	}
	/**
	 * Create a function which returns true for submitted values less than the given value.
	 * @param value the maxium value to include
	 * @return the function
	 */
	public static Func1<Boolean, Long> lessThan(final long value) {
		return new Func1<Boolean, Long>() {
			@Override
			public Boolean invoke(Long param1) {
				return param1.longValue() < value;
			}
		};
	}
	/**
	 * Create a function which returns true for submitted values greater or equal than the given value.
	 * @param value the maxium value to include
	 * @return the function
	 */
	public static Func1<Boolean, Long> greaterOrEqual(final long value) {
		return new Func1<Boolean, Long>() {
			@Override
			public Boolean invoke(Long param1) {
				return param1.longValue() >= value;
			}
		};
	}
	/**
	 * @return a function which returns param + 1 for Integers.
	 */
	public static Func1<Integer, Integer> incrementInt() {
		return new Func1<Integer, Integer>() {
			@Override
			public Integer invoke(Integer param1) {
				return param1 + 1;
			}
		};
	}
	/**
	 * @return a function which returns param + 1 for Longs.
	 */
	public static Func1<Long, Long> incrementLong() {
		return new Func1<Long, Long>() {
			@Override
			public Long invoke(Long param1) {
				return param1 + 1;
			}
		};
	}
	/**
	 * @return a function which returns param - 1 for Integers.
	 */
	public static Func1<Integer, Integer> decrementInt() {
		return new Func1<Integer, Integer>() {
			@Override
			public Integer invoke(Integer param1) {
				return param1 - 1;
			}
		};
	}
	/**
	 * @return a function which returns param + 1 for Longs.
	 */
	public static Func1<Long, Long> decrementLong() {
		return new Func1<Long, Long>() {
			@Override
			public Long invoke(Long param1) {
				return param1 - 1;
			}
		};
	}
	/**
	 * Returns a function which increments its parameter by the given amount.
	 * Can be used to decrement if value is less than zero
	 * @param value the value to increment by
	 * @return the function
	 */
	public static Func1<Integer, Integer> incrementBy(final int value) {
		return new Func1<Integer, Integer>() {
			@Override
			public Integer invoke(Integer param1) {
				return param1 + value;
			}
		};
	}
	/**
	 * Returns a function which increments its parameter by the given amount.
	 * Can be used to decrement if value is less than zero
	 * @param value the value to increment by
	 * @return the function
	 */
	public static Func1<Long, Long> incrementBy(final long value) {
		return new Func1<Long, Long>() {
			@Override
			public Long invoke(Long param1) {
				return param1 + value;
			}
		};
	}
	/**
	 * Creates a function which returns always the same value.
	 * @param <T> the value type to return
	 * @param <U> the parameter type, irrelevant
	 * @param value the value to return
	 * @return the function
	 */
	public static <T, U> Func1<T, U> constant(final T value) {
		return new Func1<T, U>() {
			@Override
			public T invoke(U param1) {
				return value;
			};
		};
	}
	/**
	 * Creates a function which returns always the same value.
	 * @param <T> the value type to return
	 * @param value the value to return
	 * @return the function
	 */
	public static <T> Func0<T> constant0(final T value) {
		return new Func0<T>() {
			@Override
			public T invoke() {
				return value;
			};
		};
	}
	/**
	 * A convenience function which unwraps the T from a Timestamped of T.
	 * @param <T> the value type
	 * @return the unwrapper function
	 */
	public static <T> Func1<T, Timestamped<T>> unwrapTimestamped() {
		return new Func1<T, Timestamped<T>>() {
			@Override
			public T invoke(Timestamped<T> param1) {
				return param1.value();
			}
		};
	}
	/**
	 * Wraps the given atomic reference object and returns its value.
	 * @param <T> the type of the contained object
	 * @param source the source atomic reference
	 * @return the function
	 */
	public static <T> Func0<T> atomicSource(final AtomicReference<T> source) {
		return new Func0<T>() {
			@Override
			public T invoke() {
				return source.get();
			}
		};
	}
	/**
	 * Wraps the given atomic boolean and returns its value.
	 * @param source the source atomic reference
	 * @return the function
	 */
	public static Func0<Boolean> atomicSource(final AtomicBoolean source) {
		return new Func0<Boolean>() {
			@Override
			public Boolean invoke() {
				return source.get();
			}
		};
	}
	/**
	 * Wraps the given atomic integer object and returns its value.
	 * @param source the source atomic reference
	 * @return the function
	 */
	public static Func0<Integer> atomicSource(final AtomicInteger source) {
		return new Func0<Integer>() {
			@Override
			public Integer invoke() {
				return source.get();
			}
		};
	}
	/**
	 * Wraps the given atomic integer object and returns its value.
	 * @param source the source atomic reference
	 * @return the function
	 */
	public static Func0<Long> atomicSource(final AtomicLong source) {
		return new Func0<Long>() {
			@Override
			public Long invoke() {
				return source.get();
			}
		};
	}
	/**
	 * Wrap the given comparator function into a function object.
	 * @param <T> the type of elements to compare
	 * @param comparator the comparator
	 * @return the wrapped comparator
	 */
	public static <T> Func2<Integer, T, T> asFunction(final Comparator<T> comparator) {
		return new Func2<Integer, T, T>() {
			@Override
			public Integer invoke(T param1, T param2) {
				return comparator.compare(param1, param2);
			};
		};
	}
	/**
	 * Wrap the given two argument function returning an integer as a comparator.
	 * @param <T> the type of the elements to compare
	 * @param func the function to wrap
	 * @return the comparator
	 */
	public static <T> Comparator<T> asComparator(final Func2<Integer, T, T> func) {
		return new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				return func.invoke(o1, o2);
			};
		};
	}
}