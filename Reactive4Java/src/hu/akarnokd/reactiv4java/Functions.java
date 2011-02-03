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

import java.io.Closeable;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Helper class with function types.
 * @author akarnokd
 */
public final class Functions {
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
	 * Returns a function which always returns false regardless of its parameters.
	 * @param <T> the type of the parameter (irrelevant)
	 * @return the function which returns always false
	 */
	@SuppressWarnings("unchecked")
	public static <T> Func1<Boolean, T> alwaysFalse() {
		return (Func1<Boolean, T>)FALSE1;
	}
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
	 * Wraps the given Func0 object into a callable instance.
	 * @param <T> the return type
	 * @param func the function to wrap
	 * @return the callable wrapper
	 */
	public static <T> Callable<T> asCallable(final Func0<? extends T> func) {
		return new Callable<T>() {
			@Override
			public T call() throws Exception {
				return func.invoke();
			}
		};
	}
	/**
	 * Wrap the given two argument function returning an integer as a comparator.
	 * @param <T> the type of the elements to compare
	 * @param func the function to wrap
	 * @return the comparator
	 */
	public static <T> Comparator<T> asComparator(final Func2<Integer, ? super T, ? super T> func) {
		return new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				return func.invoke(o1, o2);
			};
		};
	}
	/**
	 * Wraps the given Callable function into a Func0 object.
	 * @param <T> the return type of the function
	 * @param call the original call function
	 * @return the Func0 function wrapping the call
	 */
	public static <T> Func0<T> asFunction(final Callable<? extends T> call) {
		return new Func0<T>() {
			@Override
			public T invoke() {
				try {
					return call.call();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		};
	}
	/**
	 * Wrap the given comparator function into a function object.
	 * @param <T> the type of elements to compare
	 * @param comparator the comparator
	 * @return the wrapped comparator
	 */
	public static <T> Func2<Integer, T, T> asFunction(final Comparator<? super T> comparator) {
		return new Func2<Integer, T, T>() {
			@Override
			public Integer invoke(T param1, T param2) {
				return comparator.compare(param1, param2);
			};
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
	 * Wraps the given atomic reference object and returns its value.
	 * @param <T> the type of the contained object
	 * @param source the source atomic reference
	 * @return the function
	 */
	public static <T> Func0<T> atomicSource(final AtomicReference<? extends T> source) {
		return new Func0<T>() {
			@Override
			public T invoke() {
				return source.get();
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
	 * Returns a function which returns true if its sumbitted parameter
	 * value equals to the given constant.
	 * @param <T> the value type
	 * @param value the value
	 * @return the function
	 */
	public static <T> Func1<Boolean, T> equal(final T value) {
		return new Func1<Boolean, T>() {
			@Override
			public Boolean invoke(T param) {
				return value == param || (value != null && value.equals(param));
			};
		};
	}
	/**
	 * Create a function which returns true for submitted values greater or equal than the given value.
	 * @param <T> a type which is comparable with itself
	 * @param value constant to compare against
	 * @return the function
	 */
	public static <T extends Comparable<? super T>> Func1<Boolean, T> greaterOrEqual(final T value) {
		return new Func1<Boolean, T>() {
			@Override
			public Boolean invoke(T param1) {
				return param1.compareTo(value) >= 0;
			}
		};
	}
	/**
	 * Returns a function which returns true if the function parameter
	 * is greater or equal to the constant in respect to the supplied comparator.
	 * @param <T> the value type
	 * @param value constant to compare against
	 * @param comparator the comparator for Ts.
	 * @return the function
	 */
	public static <T> Func1<Boolean, T> greaterOrEqual(final T value, final Comparator<? super T> comparator) {
		return new Func1<Boolean, T>() {
			@Override
			public Boolean invoke(T param1) {
				return comparator.compare(param1, value) >= 0;
			};
		};
	}
	/**
	 * Create a function which returns true for submitted values greater 
	 * than the given value.
	 * @param <T> a type which is comparable with itself
	 * @param value constant to compare against
	 * @return the function
	 */
	public static <T extends Comparable<? super T>> Func1<Boolean, T> greaterThan(final T value) {
		return new Func1<Boolean, T>() {
			@Override
			public Boolean invoke(T param1) {
				return param1.compareTo(value) > 0;
			}
		};
	}
	/**
	 * Returns a function which returns true if the function parameter
	 * is greater than the constant in respect to the supplied comparator.
	 * @param <T> the value type
	 * @param value constant to compare against
	 * @param comparator the comparator for Ts.
	 * @return the function
	 */
	public static <T> Func1<Boolean, T> greaterThan(final T value, final Comparator<? super T> comparator) {
		return new Func1<Boolean, T>() {
			@Override
			public Boolean invoke(T param1) {
				return comparator.compare(param1, value) > 0;
			};
		};
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
	 * @return a function which returns param + 1 for <code>BigInteger</code>s.
	 */
	public static Func1<BigInteger, BigInteger> incrementBigInteger() {
		return new Func1<BigInteger, BigInteger>() {
			@Override
			public BigInteger invoke(BigInteger param1) {
				return param1.add(BigInteger.ONE);
			}
		};
	}
	/**
	 * @return a function which returns param - 1 for <code>BigInteger</code>s.
	 */
	public static Func1<BigInteger, BigInteger> decrementBigInteger() {
		return new Func1<BigInteger, BigInteger>() {
			@Override
			public BigInteger invoke(BigInteger param1) {
				return param1.subtract(BigInteger.ONE);
			}
		};
	}
	/**
	 * Create a function which returns true for submitted values less or equal 
	 * than the given value.
	 * @param <T> a type which is comparable to itself
	 * @param value constant to compare against
	 * @return the function
	 */
	public static <T extends Comparable<? super T>> Func1<Boolean, T> lessOrEqual(final T value) {
		return new Func1<Boolean, T>() {
			@Override
			public Boolean invoke(T param1) {
				return param1.compareTo(value) <= 0;
			}
		};
	}
	/**
	 * Create a function which returns true for submitted values less or equal 
	 * than the given value in respect to the supplied comparator.
	 * @param <T> a type which is comparable to itself
	 * @param value constant to compare against
	 * @param comparator the comparator
	 * @return the function
	 */
	public static <T> Func1<Boolean, T> lessOrEqual(final T value, final Comparator<? super T> comparator) {
		return new Func1<Boolean, T>() {
			@Override
			public Boolean invoke(T param1) {
				return comparator.compare(param1, value) <= 0;
			}
		};
	}
	/**
	 * Create a function which returns true for submitted values less 
	 * than the given value.
	 * @param <T> a type which is comparable with itself
	 * @param value constant to compare against
	 * @return the function
	 */
	public static <T extends Comparable<? super T>> Func1<Boolean, T> lessThan(final T value) {
		return new Func1<Boolean, T>() {
			@Override
			public Boolean invoke(T param1) {
				return param1.compareTo(value) < 0;
			}
		};
	}
	/**
	 * Create a function which returns true for submitted values less 
	 * than the given value in respect to the supplied comparator.
	 * @param <T> a type which is comparable to itself
	 * @param value constant to compare against
	 * @param comparator the comparator
	 * @return the function
	 */
	public static <T> Func1<Boolean, T> lessThan(final T value, final Comparator<? super T> comparator) {
		return new Func1<Boolean, T>() {
			@Override
			public Boolean invoke(T param1) {
				return comparator.compare(param1, value) < 0;
			}
		};
	}
	/**
	 * Returns a function which returns true if its sumbitted parameter
	 * value does not equal to the given constant.
	 * @param <T> the value type
	 * @param value the value
	 * @return the function
	 */
	public static <T> Func1<Boolean, T> notEqual(final T value) {
		return new Func1<Boolean, T>() {
			@Override
			public Boolean invoke(T param) {
				return value != param && (value == null || !value.equals(param));
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
	 * A convenience function which unwraps the T from a TimeInterval of T.
	 * @param <T> the value type
	 * @return the unwrapper function
	 */
	public static <T> Func1<T, TimeInterval<T>> unwrapTimeInterval() {
		return new Func1<T, TimeInterval<T>>() {
			@Override
			public T invoke(TimeInterval<T> param1) {
				return param1.value();
			}
		};
	}
	/**
	 * Wrap the given type into a timestamped container of T.
	 * @param <T> the type of the contained element
	 * @return the function performing the wrapping
	 */
	public static <T> Func1<Timestamped<T>, T> wrapTimestamped() {
		return new Func1<Timestamped<T>, T>() {
			@Override
			public Timestamped<T> invoke(T param1) {
				return Timestamped.of(param1);
			};
		};
	}
	/**
	 * Creates a function which negates the supplied function's value.
	 * @param func the original function
	 * @return the wrapped negator function
	 */
	public static Func0<Boolean> negate(final Func0<Boolean> func) {
		return new Func0<Boolean>() {
			@Override
			public Boolean invoke() {
				return func.invoke() == Boolean.TRUE ? Boolean.FALSE : Boolean.TRUE;
			}
		};
	}
	/** Utility class. */
	private Functions() {
		// TODO Auto-generated constructor stub
	}
	/** An empty runnable. */
	public static final Runnable EMPTY_RUNNABLE = new Runnable() {
		@Override
		public void run() {
			
		};
	};
	/** An empty runnable. */
	public static final Closeable EMPTY_CLOSEABLE = new Closeable() {
		@Override
		public void close() {
			
		};
	};
}
