/*
 * Copyright 2011-2012 David Karnok
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

/**
 * Helper class with function types.
 * @author akarnokd
 */
public final class Functions {
	/** Constant function returning always false. */
	private static final Pred1<Void> FALSE1 = new Pred1<Void>() {
		@Override
		public Boolean invoke(Void param1) {
			return false;
		}
	};
	/** Constant function returning always true. */
	private static final Pred1<Void> TRUE1 = new Pred1<Void>() {
		@Override
		public Boolean invoke(Void param1) {
			return true;
		}
	};
	/** Constant parameterless function which returns always false. */
	public static final Pred0 FALSE = new Pred0() {
		@Override
		public Boolean invoke() {
			return false;
		}
	};
	/** Constant parameterless function which returns always true. */
	public static final Pred0 TRUE = new Pred0() {
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
	 * Returns a function which always returns false regardless of its parameters.
	 * @param <T> the type of the parameter (irrelevant)
	 * @return the function which returns always false
	 */
	@SuppressWarnings("unchecked")
	@Nonnull 
	public static <T> Pred1<T> alwaysFalse() {
		return (Pred1<T>)FALSE1;
	}
	/**
	 * Returns a function which always returns true regardless of its parameters.
	 * @param <T> the type of the parameter (irrelevant)
	 * @return the function which returns always true
	 */
	@Nonnull 
	@SuppressWarnings("unchecked")
	public static <T> Pred1<T> alwaysTrue() {
		return (Pred1<T>)TRUE1;
	}
	/**
	 * Wraps the given Func0 object into a callable instance.
	 * @param <T> the return type
	 * @param func the function to wrap
	 * @return the callable wrapper
	 */
	@Nonnull 
	public static <T> Callable<T> asCallable(
			@Nonnull final Func0<? extends T> func) {
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
	@Nonnull 
	public static <T> Comparator<T> asComparator(
			@Nonnull final Func2<? super T, ? super T, Integer> func) {
		return new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				return func.invoke(o1, o2);
			};
		};
	}
	/**
	 * Wraps the given action into a function which calls the
	 * action and then returns the <code>result</code> value.
	 * @param <T> the result type
	 * @param action the action to invoke
	 * @param result the result to present after the action invocation
	 * @return the function
	 */
	public static <T> Func0<T> asFunc1(final Action0 action, final T result) {
		return new Func0<T>() {
			@Override
			public T invoke() {
				action.invoke();
				return result;
			};
		};
	}
	/**
	 * Wraps the given action into a function which calls the
	 * action and then returns the <code>result</code> value.
	 * @param <T> the parameter type
	 * @param <U> the result type
	 * @param action the action to invoke
	 * @param result the result to present after the action invocation
	 * @return the function
	 */
	public static <T, U> Func1<T, U> asFunc1(final Action1<T> action, final U result) {
		return new Func1<T, U>() {
			@Override
			public U invoke(T param1) {
				action.invoke(param1);
				return result;
			};
		};
	}
	/**
	 * Wraps the given action into a function which calls the
	 * action and then returns the <code>result</code> value.
	 * @param <T> the first parameter type
	 * @param <U> the second parameter type
	 * @param <V> the result type
	 * @param action the action to invoke
	 * @param result the result to present after the action invocation
	 * @return the function
	 */
	public static <T, U, V> Func2<T, U, V> asFunc2(final Action2<T, U> action, final V result) {
		return new Func2<T, U, V>() {
			@Override
			public V invoke(T param1, U param2) {
				action.invoke(param1, param2);
				return result;
			};
		};
	}
	/**
	 * Wraps the given Callable function into a Func0 object.
	 * @param <T> the return type of the function
	 * @param call the original call function
	 * @return the Func0 function wrapping the call
	 */
	@Nonnull 
	public static <T> Func0<T> asFunction(
			@Nonnull final Callable<? extends T> call) {
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
	@Nonnull 
	public static <T> Func2<T, T, Integer> asFunction(
			@Nonnull final Comparator<? super T> comparator) {
		return new Func2<T, T, Integer>() {
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
	@Nonnull 
	public static Func0<Boolean> atomicSource(
			@Nonnull final AtomicBoolean source) {
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
	@Nonnull 
	public static Func0<Integer> atomicSource(
			@Nonnull final AtomicInteger source) {
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
	@Nonnull 
	public static Func0<Long> atomicSource(
			@Nonnull final AtomicLong source) {
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
	@Nonnull 
	public static <T> Func0<T> atomicSource(
			@Nonnull final AtomicReference<? extends T> source) {
		return new Func0<T>() {
			@Override
			public T invoke() {
				return source.get();
			}
		};
	}
	/**
	 * Returns a convenience comparator which basically compares objects which implement the <code>Comparable</code>
	 * interface. The comparator is null safe in the manner, that nulls are always less than any non-nulls.
	 * To have a comparator which places nulls last, use the <code>comparator0()</code> method.
	 * @param <T> the element types to compare
	 * @return the comparator
	 */
	@Nonnull 
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
	@Nonnull 
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
	/**
	 * Creates a new comparator which reverses the order of the comparison.
	 * @param <T> the element type, which must be self comparable
	 * @return the new comparator
	 */
	@Nonnull
	public static <T extends Comparable<? super T>> Comparator<T> comparatorReverse() {
		return new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				return o2.compareTo(o1);
			};
		};
	}
	/**
	 * Creates a new comparator which reverses the order produced by the given
	 * normal comparator.
	 * @param <T> the element type
	 * @param normal the normal comparator
	 * @return the new comparator
	 */
	@Nonnull
	public static <T> Comparator<T> comparatorReverse(
			@Nonnull final Comparator<? super T> normal) {
		return new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				return normal.compare(o2, o1);
			};
		};
	}
	/**
	 * Creates a function which returns always the same value.
	 * @param <Param1> the parameter type, irrelevant
	 * @param <Result> the value type to return
	 * @param value the value to return
	 * @return the function
	 */
	@Nonnull 
	public static <Param1, Result> Func1<Param1, Result> constant(final Result value) {
		return new Func1<Param1, Result>() {
			@Override
			public Result invoke(Param1 param1) {
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
	@Nonnull 
	public static <T> Func0<T> constant0(final T value) {
		return new Func0<T>() {
			@Override
			public T invoke() {
				return value;
			};
		};
	}
	/**
	 * @return a function which returns param - 1 for <code>BigInteger</code>s.
	 */
	@Nonnull 
	public static Func1<BigInteger, BigInteger> decrementBigInteger() {
		return new Func1<BigInteger, BigInteger>() {
			@Override
			public BigInteger invoke(BigInteger param1) {
				return param1.subtract(BigInteger.ONE);
			}
		};
	}
	/**
	 * @return a function which returns param - 1 for Integers.
	 */
	@Nonnull 
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
	@Nonnull 
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
	@Nonnull 
	public static <T> Func1<T, Boolean> equal(final T value) {
		return new Func1<T, Boolean>() {
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
	@Nonnull 
	public static <T extends Comparable<? super T>> Func1<T, Boolean> greaterOrEqual(
			@Nonnull final T value) {
		return new Func1<T, Boolean>() {
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
	@Nonnull 
	public static <T> Func1<T, Boolean> greaterOrEqual(
			@Nonnull final T value, 
			@Nonnull final Comparator<? super T> comparator) {
		return new Func1<T, Boolean>() {
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
	@Nonnull 
	public static <T extends Comparable<? super T>> Func1<T, Boolean> greaterThan(
			@Nonnull final T value) {
		return new Func1<T, Boolean>() {
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
	@Nonnull 
	public static <T> Func1<T, Boolean> greaterThan(
			@Nonnull final T value, 
			@Nonnull final Comparator<? super T> comparator) {
		return new Func1<T, Boolean>() {
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
	@Nonnull 
	public static <T> Func1<T, T> identity() {
		return (Func1<T, T>)IDENTITY;
	}
	/**
	 * Returns a helper function of two parameters which always returns its first parameter.
	 * @param <T> the result and the first parameter type
	 * @param <U> the second parameter type, irrelevant
	 * @return the function
	 */
	@SuppressWarnings("unchecked")
	@Nonnull 
	public static <T, U> Func2<T, U, T> identityFirst() {
		return (Func2<T, U, T>)IDENTITY_FIRST;
	}
	/**
	 * Returns a helper function of two parameters which always returns its second parameter.
	 * @param <T> the result and the second parameter type
	 * @param <U> the first parameter type, irrelevant
	 * @return the function
	 */
	@SuppressWarnings("unchecked")
	@Nonnull 
	public static <T, U> Func2<T, U, U> identitySecond() {
		return (Func2<T, U, U>)IDENTITY_SECOND;
	}
	/**
	 * @return a function which returns param + 1 for <code>BigInteger</code>s.
	 */
	@Nonnull 
	public static Func1<BigInteger, BigInteger> incrementBigInteger() {
		return new Func1<BigInteger, BigInteger>() {
			@Override
			public BigInteger invoke(BigInteger param1) {
				return param1.add(BigInteger.ONE);
			}
		};
	}
	/**
	 * Returns a function which increments its parameter by the given amount.
	 * Can be used to decrement if value is less than zero
	 * @param value the value to increment by
	 * @return the function
	 */
	@Nonnull 
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
	@Nonnull 
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
	@Nonnull 
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
	@Nonnull 
	public static Func1<Long, Long> incrementLong() {
		return new Func1<Long, Long>() {
			@Override
			public Long invoke(Long param1) {
				return param1 + 1;
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
	@Nonnull 
	public static <T extends Comparable<? super T>> Func1<T, Boolean> lessOrEqual(
			@Nonnull final T value) {
		return new Func1<T, Boolean>() {
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
	@Nonnull 
	public static <T> Func1<T, Boolean> lessOrEqual(
			@Nonnull final T value, 
			@Nonnull final Comparator<? super T> comparator) {
		return new Func1<T, Boolean>() {
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
	@Nonnull 
	public static <T extends Comparable<? super T>> Func1<T, Boolean> lessThan(
			@Nonnull final T value) {
		return new Func1<T, Boolean>() {
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
	@Nonnull 
	public static <T> Func1<T, Boolean> lessThan(
			@Nonnull final T value, 
			@Nonnull final Comparator<? super T> comparator) {
		return new Func1<T, Boolean>() {
			@Override
			public Boolean invoke(T param1) {
				return comparator.compare(param1, value) < 0;
			}
		};
	}
	/**
	 * Returns a function which returns the greater of its parameters.
	 * If only one of the parameters is null, the other parameter is returned.
	 * If both parameters are null, null is returned.
	 * @param <T> the parameter types, which must be self-comparable
	 * @return the function
	 */
	@Nonnull 
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
	 * Returns a function which returns the greater of its parameters in respect to the supplied <code>Comparator</code>.
	 * If only one of the parameters is null, the other parameter is returned.
	 * If both parameters are null, null is returned.
	 * @param <T> the parameter types, which must be self-comparable
	 * @param comparator the value comparator
	 * @return the function
	 */
	@Nonnull 
	public static <T> Func2<T, T, T> max(
			@Nonnull final Comparator<? super T> comparator) {
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
	 * Returns a function which returns the smaller of its parameters.
	 * If only one of the parameters is null, the other parameter is returned.
	 * If both parameters are null, null is returned.
	 * @param <T> the parameter types, which must be self-comparable
	 * @return the function
	 */
	@Nonnull 
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
	 * Returns a function which returns the smaller of its parameters in respect to the supplied <code>Comparator</code>.
	 * If only one of the parameters is null, the other parameter is returned.
	 * If both parameters are null, null is returned.
	 * @param <T> the parameter types, which must be self-comparable
	 * @param comparator the value comparator
	 * @return the function
	 */
	@Nonnull 
	public static <T> Func2<T, T, T> min(
			@Nonnull final Comparator<? super T> comparator) {
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
	 * Creates a function which negates the supplied function's value.
	 * @param func the original function
	 * @return the wrapped negator function
	 */
	@Nonnull 
	public static Func0<Boolean> negate(
			@Nonnull final Func0<Boolean> func) {
		return new Func0<Boolean>() {
			@Override
			public Boolean invoke() {
				return func.invoke() == Boolean.TRUE ? Boolean.FALSE : Boolean.TRUE;
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
	@Nonnull 
	public static <T> Func1<T, Boolean> notEqual(
			@Nonnull final T value) {
		return new Func1<T, Boolean>() {
			@Override
			public Boolean invoke(T param) {
				return value != param && (value == null || !value.equals(param));
			};
		};
	}
	/**
	 * Retuns a function that adds two BigDecimal numbers and
	 * returns a new one. 
	 * <p>If the first parameter is null, it returns the second parameter.</p> 
	 * @return Function to sum integers in aggregators. 
	 */
	@Nonnull 
	public static Func2<BigDecimal, BigDecimal, BigDecimal> sumBigDecimal() {
		return SUM_BIGDECIMAL;
	}
	/** 
	 * Retuns a function that adds two BigInteger numbers and
	 * returns a new one. 
	 * <p>If the first parameter is null, it returns the second parameter.</p> 
	 * @return Function to sum integers in aggregators. 
	 */
	@Nonnull 
	public static Func2<BigInteger, BigInteger, BigInteger> sumBigInteger() {
		return SUM_BIGINTEGER;
	}
	/** 
	 * Retuns a function that adds two Double number and
	 * returns a new one. 
	 * <p>If the first parameter is null, it returns the second parameter.</p> 
	 * @return Function to sum integers in aggregators. 
	 */
	@Nonnull 
	public static Func2<Double, Double, Double> sumDouble() {
		return SUM_DOUBLE;
	}
	/** 
	 * Retuns a function that adds two Float number and
	 * returns a new one. 
	 * <p>If the first parameter is null, it returns the second parameter.</p> 
	 * @return Function to sum integers in aggregators. 
	 */
	@Nonnull 
	public static Func2<Float, Float, Float> sumFloat() {
		return SUM_FLOAT;
	}
	/** 
	 * Retuns a function that adds two Integer number and
	 * returns a new one. 
	 * <p>If the first parameter is null, it returns the second parameter.</p> 
	 * @return Function to sum integers in aggregators. 
	 */
	@Nonnull 
	public static Func2<Integer, Integer, Integer> sumInteger() {
		return SUM_INTEGER;
	}
	/** 
	 * Retuns a function that adds two Long number and
	 * returns a new one. 
	 * <p>If the first parameter is null, it returns the second parameter.</p> 
	 * @return Function to sum integers in aggregators. 
	 */
	@Nonnull 
	public static Func2<Long, Long, Long> sumLong() {
		return SUM_LONG;
	}
	/** A null safe equals function. */
	private static final Func2<Object, Object, Boolean> NULL_SAFE_EQUALS = new Func2<Object, Object, Boolean>() {
		@Override
		public Boolean invoke(Object param1, Object param2) {
			return param1 == param2 || (param1 != null && param1.equals(param2));
		}
	};
	/**
	 * Returns a function which compares its two paramerers by a null-safe
	 * equals.
	 * @param <T> the parameter type
	 * @return the function
	 */
	@SuppressWarnings("unchecked")
	public static <T> Func2<T, T, Boolean> equals() {
		return (Func2<T, T, Boolean>)NULL_SAFE_EQUALS;
	}
	/**
	 * A convenience function which returns a new ArrayList on every invocation.
	 * <p>May be used with the Reactive.toMultiMap() method.</p>
	 * @param <T> the element type
	 * @return the function
	 */
	public static <T> Func0<List<T>> listSupplier() {
		return new Func0<List<T>>() {
			@Override
			public List<T> invoke() {
				return new ArrayList<T>();
			}
		};
	}
	/**
	 * A convenience function which returns a new hashSet on every invocation.
	 * <p>May be used with the Reactive.toMultiMap() method.</p>
	 * @param <T> the element type
	 * @return the function
	 */
	public static <T> Func0<Set<T>> setSupplier() {
		return new Func0<Set<T>>() {
			@Override
			public Set<T> invoke() {
				return new HashSet<T>();
			}
		};
	}
	/**
	 * Creates a single parameter function which returns values from the given map.
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param map the backing map.
	 * @return the created function
	 * @since 0.96
	 */
	public static <K, V> Func1<K, V> asFunc1(final Map<? super K, ? extends V> map) {
		return new Func1<K, V>() {
			@Override
			public V invoke(K param1) {
				return map.get(param1);
			}
		};
	}
	/**
	 * Wraps a two-layer (map of map of something) into a two parameter function.
	 * <p>If the first level map returns null, the function returns null.</p>
	 * @param <K1> the first level key type
	 * @param <K2> the second level key type
	 * @param <V> the value type type
	 * @param map the source map of map of something 
	 * @return the function
	 * @since 0.96
	 */
	public static <K1, K2, V> Func2<K1, K2, V> asFunc2(
			final Map<? super K1, ? extends Map<? super K2, ? extends V>> map) {
		return new Func2<K1, K2, V>() {
			@Override
			public V invoke(K1 param1, K2 param2) {
				Map<? super K2, ? extends V> map2 = map.get(param1);
				if (map2 != null) {
					return map2.get(param2);
				}
				return null;
			}
		};
	}
	/**
	 * Creates a single parameter predicate function which returns true if the supplied
	 * parameter is in the given collection.
	 * @param <K> the element type
	 * @param set the backing set
	 * @return the created function
	 * @since 0.96
	 */
	public static <K> Func1<K, Boolean> asFunc1(final Collection<? extends K> set) {
		return new Pred1<K>() {
			@Override
			public Boolean invoke(K param1) {
				return set.contains(param1);
			};
		};
	}
	/** Utility class. */
	private Functions() {
		// TODO Auto-generated constructor stub
	}
}
