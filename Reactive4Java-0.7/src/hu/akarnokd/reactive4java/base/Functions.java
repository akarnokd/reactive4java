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

package hu.akarnokd.reactive4java.base;

import java.io.Closeable;
import java.math.BigDecimal;
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
	/** Function to sum integers in aggregators. */
	static final Func2<Integer, Integer, Integer> SUM_INTEGER = new Func2<Integer, Integer, Integer>() {
		@Override
		public Integer invoke(Integer param1, Integer param2) {
			return param1 != null ? param1 + param2 : param2;
		}
	};
	/** Function to sum integers in aggregators. */
	static final Func2<Float, Float, Float> SUM_FLOAT = new Func2<Float, Float, Float>() {
		@Override
		public Float invoke(Float param1, Float param2) {
			return param1 != null ? param1 + param2 : param2;
		}
	};
	/** Function to sum integers in aggregators. */
	static final Func2<Double, Double, Double> SUM_DOUBLE = new Func2<Double, Double, Double>() {
		@Override
		public Double invoke(Double param1, Double param2) {
			return param1 != null ? param1 + param2 : param2;
		}
	};
	/** Function to sum integers in aggregators. */
	static final Func2<Long, Long, Long> SUM_LONG = new Func2<Long, Long, Long>() {
		@Override
		public Long invoke(Long param1, Long param2) {
			return param1 != null ? param1 + param2 : param2;
		}
	};
	/** Function to sum integers in aggregators. */
	static final Func2<BigInteger, BigInteger, BigInteger> SUM_BIGINTEGER = new Func2<BigInteger, BigInteger, BigInteger>() {
		@Override
		public BigInteger invoke(BigInteger param1, BigInteger param2) {
			return param1 != null ? param1.add(param2) : param2;
		}
	};
	/** Function to sum integers in aggregators. */
	static final Func2<BigDecimal, BigDecimal, BigDecimal> SUM_BIGDECIMAL = new Func2<BigDecimal, BigDecimal, BigDecimal>() {
		@Override
		public BigDecimal invoke(BigDecimal param1, BigDecimal param2) {
			return param1 != null ? param1.add(param2) : param2;
		}
	};
	/** @return Function to sum integers in aggregators. */
	public static Func2<Integer, Integer, Integer> sumInteger() {
		return SUM_INTEGER;
	}
	/** @return Function to sum integers in aggregators. */
	public static Func2<Long, Long, Long> sumLong() {
		return SUM_LONG;
	}
	/** @return Function to sum integers in aggregators. */
	public static Func2<Float, Float, Float> sumFloat() {
		return SUM_FLOAT;
	}
	/** @return Function to sum integers in aggregators. */
	public static Func2<Double, Double, Double> sumDouble() {
		return SUM_DOUBLE;
	}
	/** @return Function to sum integers in aggregators. */
	public static Func2<BigInteger, BigInteger, BigInteger> sumBigInteger() {
		return SUM_BIGINTEGER;
	}
	/** @return Function to sum integers in aggregators. */
	public static Func2<BigDecimal, BigDecimal, BigDecimal> sumBigDecimal() {
		return SUM_BIGDECIMAL;
	}
	/** A helper function which returns its first parameter. */
	private static final Func2<Object, Object, Object> IDENTITY_FIRST = new Func2<Object, Object, Object>() {
		@Override
		public Object invoke(Object param1, Object param2) {
			return param1;
		}
	};
	/** A helper function which returns its second parameter. */
	private static final Func2<Object, Object, Object> IDENTITY_SECOND = new Func2<Object, Object, Object>() {
		@Override
		public Object invoke(Object param1, Object param2) {
			return param2;
		}
	};
	/**
	 * Returns a helper function of two parameters which always returns its first parameter.
	 * @param <T> the result and the first parameter type
	 * @param <U> the second parameter type, irrelevant
	 * @return the function
	 */
	@SuppressWarnings("unchecked")
	public static <T, U> Func2<T, T, U> identityFirst() {
		return (Func2<T, T, U>)IDENTITY_FIRST;
	}
	/**
	 * Returns a helper function of two parameters which always returns its second parameter.
	 * @param <T> the result and the second parameter type
	 * @param <U> the first parameter type, irrelevant
	 * @return the function
	 */
	@SuppressWarnings("unchecked")
	public static <T, U> Func2<T, U, T> identitySecond() {
		return (Func2<T, U, T>)IDENTITY_SECOND;
	}
	/**
	 * Returns a function which returns the greater of its parameters.
	 * If only one of the parameters is null, the other parameter is returned.
	 * If both parameters are null, null is returned.
	 * @param <T> the parameter types, which must be self-comparable
	 * @return the function
	 */
	public static <T extends Comparable<? super T>> Func2<T, T, T> max() {
		return new Func2<T, T, T>() {
			@Override
			public T invoke(T param1, T param2) {
				if (param1 == null || param2 == null) {
					if (param2 == null) {
						return param1;
					}
					return param2;
				}
				return param1.compareTo(param2) < 0 ? param2 : param1;
			};
		};
	}
	/**
	 * Returns a function which returns the smaller of its parameters.
	 * If only one of the parameters is null, the other parameter is returned.
	 * If both parameters are null, null is returned.
	 * @param <T> the parameter types, which must be self-comparable
	 * @return the function
	 */
	public static <T extends Comparable<? super T>> Func2<T, T, T> min() {
		return new Func2<T, T, T>() {
			@Override
			public T invoke(T param1, T param2) {
				if (param1 == null || param2 == null) {
					if (param2 == null) {
						return param1;
					}
					return param2;
				}
				return param1.compareTo(param2) > 0 ? param2 : param1;
			};
		};
	}
	/**
	 * Returns a function which returns the greater of its parameters in respect to the supplied <code>Comparator</code>.
	 * If only one of the parameters is null, the other parameter is returned.
	 * If both parameters are null, null is returned.
	 * @param <T> the parameter types, which must be self-comparable
	 * @param comparator the value comparator
	 * @return the function
	 */
	public static <T> Func2<T, T, T> max(final Comparator<? super T> comparator) {
		return new Func2<T, T, T>() {
			@Override
			public T invoke(T param1, T param2) {
				if (param1 == null || param2 == null) {
					if (param2 == null) {
						return param1;
					}
					return param2;
				}
				return comparator.compare(param1, param2) < 0 ? param2 : param1;
			};
		};
	}
	/**
	 * Returns a function which returns the smaller of its parameters in respect to the supplied <code>Comparator</code>.
	 * If only one of the parameters is null, the other parameter is returned.
	 * If both parameters are null, null is returned.
	 * @param <T> the parameter types, which must be self-comparable
	 * @param comparator the value comparator
	 * @return the function
	 */
	public static <T> Func2<T, T, T> min(final Comparator<? super T> comparator) {
		return new Func2<T, T, T>() {
			@Override
			public T invoke(T param1, T param2) {
				if (param1 == null || param2 == null) {
					if (param2 == null) {
						return param1;
					}
					return param2;
				}
				return comparator.compare(param1, param2) > 0 ? param2 : param1;
			};
		};
	}
	/**
	 * Returns a convenience comparator which basically compares objects which implement the <code>Comparable</code>
	 * interface. The comparator is null safe in the manner, that nulls are always less than any non-nulls.
	 * To have a comparator which places nulls last, use the <code>comparator0()</code> method.
	 * @param <T> the element types to compare
	 * @return the comparator
	 */
	public static <T extends Comparable<? super T>> Comparator<T> comparator() {
		return new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				if (o1 != null && o2 == null) {
					return 1;
				} else
				if (o1 == null && o2 != null) {
					return -1;
				} else
				if (o1 == o2) {
					return 0;
				}
				return o1.compareTo(o2);
			};
		};
	}
	/**
	 * Returns a convenience comparator which basically compares objects which implement the <code>Comparable</code>
	 * interface. The comparator is null safe in the manner, that nulls are always greater than any non-nulls.
	 * To have a comparator which places nulls first, use the <code>comparator()</code> method.
	 * @param <T> the element types to compare
	 * @return the comparator
	 */
	public static <T extends Comparable<? super T>> Comparator<T> comparator0() {
		return new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				if (o1 != null && o2 == null) {
					return -1;
				} else
				if (o1 == null && o2 != null) {
					return 1;
				} else
				if (o1 == o2) {
					return 0;
				}
				return o1.compareTo(o2);
			};
		};
	}
}
