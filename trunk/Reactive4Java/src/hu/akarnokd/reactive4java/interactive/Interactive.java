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
package hu.akarnokd.reactive4java.interactive;

import hu.akarnokd.reactive4java.base.Action0;
import hu.akarnokd.reactive4java.base.Action1;
import hu.akarnokd.reactive4java.base.CloseableIterable;
import hu.akarnokd.reactive4java.base.CloseableIterator;
import hu.akarnokd.reactive4java.base.Enumerable;
import hu.akarnokd.reactive4java.base.Enumerator;
import hu.akarnokd.reactive4java.base.Func0;
import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Func2;
import hu.akarnokd.reactive4java.base.GroupedIterable;
import hu.akarnokd.reactive4java.base.Option;
import hu.akarnokd.reactive4java.base.Pair;
import hu.akarnokd.reactive4java.base.Scheduler;
import hu.akarnokd.reactive4java.interactive.Interactive.LinkedBuffer.N;
import hu.akarnokd.reactive4java.util.Actions;
import hu.akarnokd.reactive4java.util.CircularBuffer;
import hu.akarnokd.reactive4java.util.Closeables;
import hu.akarnokd.reactive4java.util.DefaultGroupedIterable;
import hu.akarnokd.reactive4java.util.Functions;
import hu.akarnokd.reactive4java.util.Schedulers;
import hu.akarnokd.reactive4java.util.SingleContainer;
import hu.akarnokd.reactive4java.util.Throwables;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The interactive (i.e., <code>Iterable</code> based) counterparts
 * of the <code>Reactive</code> operators.
 * <p>The implementations of the operators are partially derived
 * from the Reactive operators.</p>
 * @author akarnokd, 2011.02.02.
 * @see hu.akarnokd.reactive4java.reactive.Reactive
 */
public final class Interactive {
	/**
	 * A linked buffer, which can be only filled and queried.
	 * @author akarnokd, 2011.02.03.
	 * @param <T> the element type
	 */
	public static class LinkedBuffer<T> {
		/** The node. */
		public static class N<T> {
			/** The element value. */
			T value;
			/** The next node. */
			N<T> next;
		}
		/** The head pointer. */
		final N<T> head = new N<T>();
		/** The tail pointer. */
		N<T> tail = head;
		/** The size. */
		int size;
		/**
		 * Add a new value.
		 * @param value the new value
		 */
		public void add(T value) {
			N<T> n = new N<T>();
			n.value = value;
			tail.next = n;
			tail = n;
			size++;
		}
	}

	/** The common empty iterator. */
	@Nonnull
	private static final Iterator<Object> EMPTY_ITERATOR = new Iterator<Object>() {
		@Override
		public boolean hasNext() {
			return false;
		}
		@Override
		public Object next() {
			throw new NoSuchElementException();
		}
		@Override
		public void remove() {
			throw new IllegalStateException();
		}
	};
	/** The common empty iterable. */
	@Nonnull
	private static final Iterable<Object> EMPTY_ITERABLE = new Iterable<Object>() {
		@Override
		public Iterator<Object> iterator() {
			return EMPTY_ITERATOR;
		}
	};
	/**
	 * Creates an iterable which traverses the source iterable and maintains a running sum value based
	 * on the <code>sum</code> function parameter. Once the source is depleted, it
	 * applies the <code>divide</code> function and returns its result.
	 * This operator is a general base for averaging (where {@code sum(u, t) => u + t}, {@code divide(u, index) => u / index}),
	 * summing (where {@code sum(u, t) => u + t}, and {@code divide(u, index) => u)}),
	 * minimum, maximum, etc.
	 * If the traversal of the source fails due an exception, that exception is reflected on the
	 * {@code next()} call of the returned iterator.
	 * The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.
	 * @param <T> the source element type
	 * @param <U> the intermediate aggregation type
	 * @param <V> the resulting aggregation type
	 * @param source the source of Ts
	 * @param sum the function which takes the current intermediate value,
	 * the current source value and should produce a new intermediate value.
	 * for the first element of T, the U parameter will receive null
	 * @param divide the function which takes the last intermediate value and a total count of Ts seen and should return the final aggregation value.
	 * @return the new iterable
	 */
	@Nonnull
	public static <T, U, V> Iterable<V> aggregate(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Func2<? super U, ? super T, ? extends U> sum,
			@Nonnull final Func2<? super U, ? super Integer, ? extends V> divide) {
		return new Iterable<V>() {
			@Override
			public Iterator<V> iterator() {
				return new Iterator<V>() {
					/** The source iterator. */
					final Iterator<? extends T> it = source.iterator();
					/** The single result container. */
					final SingleContainer<Option<? extends V>> result = new SingleContainer<Option<? extends V>>();
					/** We have finished the aggregation. */
					boolean done;
					@Override
					public boolean hasNext() {
						if (!done) {
							done = true;
							if (result.isEmpty()) {
								try {
									U intermediate = null;
									int count = 0;
									try {
										while (it.hasNext()) {
											intermediate = sum.invoke(intermediate, it.next());
											count++;
										}
									} finally {
										Closeables.closeSilently(it);
									}
									if (count > 0) {
										result.add(Option.some(divide.invoke(intermediate, count)));
									}
								} catch (Throwable t) {
									result.add(Option.<V>error(t));
								}
							}
						}
						return !result.isEmpty();
					}

					@Override
					public V next() {
						if (hasNext()) {
							return result.take().value();
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
	 * Returns an iterable which contains true if all
	 * elements of the source iterable satisfy the predicate.
	 * The operator might return a false before fully iterating the source.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @param predicate the predicate
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<Boolean> all(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Func1<? super T, Boolean> predicate) {
		return new Iterable<Boolean>() {
			@Override
			public Iterator<Boolean> iterator() {
				return new Iterator<Boolean>() {
					/** The source iterator. */
					final Iterator<? extends T> it = source.iterator();
					/** The peek ahead container. */
					final SingleContainer<Option<Boolean>> peek = new SingleContainer<Option<Boolean>>();
					/** Completed. */
					boolean done;
					@Override
					public boolean hasNext() {
						if (peek.isEmpty() && !done) {
							try {
								if (it.hasNext()) {
									while (it.hasNext()) {
										T value = it.next();
										if (!predicate.invoke(value)) {
											peek.add(Option.some(false));
											return true;
										}
									}
									peek.add(Option.some(true));
								}
								done = true;
							} catch (Throwable t) {
								peek.add(Option.<Boolean>error(t));
								done = true;
							} finally {
								Closeables.closeSilently(it);
							}
						}
						return !peek.isEmpty();
					}

					@Override
					public Boolean next() {
						if (hasNext()) {
							return peek.take().value();
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
	 * Tests if there is any element of the source that satisfies the given predicate function.
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @param predicate the predicate tester function
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<Boolean> any(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Func1<? super T, Boolean> predicate) {
		return any(where(source, predicate));
	}
	/**
	 * Determines if the given source has any elements at all.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param <T> the source element type, irrelevant here
	 * @param source the source of Ts
	 * @return the new iterable with a single true or false
	 */
	@Nonnull
	public static <T> Iterable<Boolean> any(
			@Nonnull final Iterable<T> source) {
		return new Iterable<Boolean>() {
			@Override
			public Iterator<Boolean> iterator() {
				return new Iterator<Boolean>() {
					/** The source's iterator. */
					Iterator<T> it = source.iterator();
					final SingleContainer<Boolean> peek = new SingleContainer<Boolean>();
					/** Query once. */
					boolean once = true;
					@Override
					public boolean hasNext() {
						if (once) {
							once = false;
							if (peek.isEmpty()) {
								peek.add(it.hasNext());
							}
						}
						return !peek.isEmpty();
					}

					@Override
					public Boolean next() {
						if (hasNext()) {
							return peek.take();
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
	 * Returns an iterable which averages the source BigDecimal values.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param source the source of BigDecimal values
	 * @return the new iterable
	 */
	@Nonnull
	public static Iterable<BigDecimal> averageBigDecimal(
			@Nonnull Iterable<BigDecimal> source) {
		return aggregate(source,
			new Func2<BigDecimal, BigDecimal, BigDecimal>() {
				@Override
				public BigDecimal invoke(BigDecimal param1, BigDecimal param2) {
					return param1 != null ? param1.add(param2) : param2;
				}
			},
			new Func2<BigDecimal, Integer, BigDecimal>() {
				@Override
				public BigDecimal invoke(BigDecimal param1, Integer param2) {
					return param1.divide(new BigDecimal(param2), BigDecimal.ROUND_HALF_UP);
				}
			}
		);
	}
	/**
	 * Returns an iterable which averages the source BigInteger values.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param source the source of BigInteger values
	 * @return the new iterable
	 */
	@Nonnull
	public static Iterable<BigDecimal> averageBigInteger(
			@Nonnull Iterable<BigInteger> source) {
		return aggregate(source,
			new Func2<BigInteger, BigInteger, BigInteger>() {
				@Override
				public BigInteger invoke(BigInteger param1, BigInteger param2) {
					return param1 != null ? param1.add(param2) : param2;
				}
			},
			new Func2<BigInteger, Integer, BigDecimal>() {
				@Override
				public BigDecimal invoke(BigInteger param1, Integer param2) {
					return new BigDecimal(param1).divide(new BigDecimal(param2), BigDecimal.ROUND_HALF_UP);
				}
			}
		);
	}
	/**
	 * Returns an iterable which averages the source Double values.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param source the source of Double values
	 * @return the new iterable
	 */
	@Nonnull
	public static Iterable<Double> averageDouble(
			@Nonnull Iterable<Double> source) {
		return aggregate(source,
			new Func2<Double, Double, Double>() {
				@Override
				public Double invoke(Double param1, Double param2) {
					return param1 != null ? param1 + param2 : param2.doubleValue();
				}
			},
			new Func2<Double, Integer, Double>() {
				@Override
				public Double invoke(Double param1, Integer param2) {
					return param1 / param2;
				}
			}
		);
	}
	/**
	 * Returns an iterable which averages the source Float values.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param source the source of Float values
	 * @return the new iterable
	 */
	@Nonnull
	public static Iterable<Float> averageFloat(
			@Nonnull Iterable<Float> source) {
		return aggregate(source,
			new Func2<Float, Float, Float>() {
				@Override
				public Float invoke(Float param1, Float param2) {
					return param1 != null ? param1 + param2 : param2.floatValue();
				}
			},
			new Func2<Float, Integer, Float>() {
				@Override
				public Float invoke(Float param1, Integer param2) {
					return param1 / param2;
				}
			}
		);
	}
	/**
	 * Returns an iterable which averages the source Integer values.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param source the source of Integer values
	 * @return the new iterable
	 */
	@Nonnull
	public static Iterable<Double> averageInt(
			@Nonnull Iterable<Integer> source) {
		return aggregate(source,
			new Func2<Double, Integer, Double>() {
				@Override
				public Double invoke(Double param1, Integer param2) {
					return param1 != null ? param1 + param2 : param2.doubleValue();
				}
			},
			new Func2<Double, Integer, Double>() {
				@Override
				public Double invoke(Double param1, Integer param2) {
					return param1 / param2;
				}
			}
		);
	}
	/**
	 * Returns an iterable which averages the source Integer values.
	 * The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.
	 * @param source the source of Integer values
	 * @return the new iterable
	 */
	@Nonnull
	public static Iterable<Double> averageLong(
			@Nonnull Iterable<Long> source) {
		return aggregate(source,
			new Func2<Double, Long, Double>() {
				@Override
				public Double invoke(Double param1, Long param2) {
					return param1 != null ? param1 + param2 : param2.doubleValue();
				}
			},
			new Func2<Double, Integer, Double>() {
				@Override
				public Double invoke(Double param1, Integer param2) {
					return param1 / param2;
				}
			}
		);
	}
	/**
	 * Returns an iterable which buffers the source elements
	 * into <code>bufferSize</code> lists.
	 * FIXME what to do on empty source or last chunk?
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @param bufferSize the buffer size.
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<List<T>> buffer(
			@Nonnull final Iterable<? extends T> source,
			final int bufferSize) {
		if (bufferSize <= 0) {
			throw new IllegalArgumentException("bufferSize <= 0");
		}
		return new Iterable<List<T>>() {
			@Override
			public Iterator<List<T>> iterator() {
				return new Iterator<List<T>>() {
					/** The source iterator. */
					final Iterator<? extends T> it = source.iterator();
					/** The current buffer. */
					final SingleContainer<Option<List<T>>> peek = new SingleContainer<Option<List<T>>>();
					/** Did the source finish? */
					boolean done;
					@Override
					public boolean hasNext() {
						if (peek.isEmpty() && !done) {
							try {
								if (it.hasNext()) {
									try {
										List<T> buffer = new ArrayList<T>();
										while (it.hasNext() && buffer.size() < bufferSize) {
											buffer.add(it.next());
										}
										if (buffer.size() > 0) {
											peek.add(Option.some(buffer));
										}
									} catch (Throwable t) {
										done = true;
										peek.add(Option.<List<T>>error(t));
									}
								} else {
									done = true;
								}
							} finally {
								Closeables.closeSilently(it);
							}
						}
						return !peek.isEmpty();
					}

					@Override
					public List<T> next() {
						if (hasNext()) {
							return peek.take().value();
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
	 * Casts the source iterable into a different type by using a type token.
	 * If the source contains a wrong element, the <code>next()</code>
	 * will throw a <code>ClassCastException</code>.
	 * <p>The returned iterator forwards all <code>remove()</code> calls
	 * to the source.</p>
	 * @param <T> the result element type
	 * @param source the arbitrary source
	 * @param token the type token
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> cast(
			@Nonnull final Iterable<?> source,
			@Nonnull final Class<T> token) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					/** The source iterator. */
					final Iterator<?> it = source.iterator();
					@Override
					public boolean hasNext() {
						return it.hasNext();
					}

					@Override
					public T next() {
						return token.cast(it.next());
					}

					@Override
					public void remove() {
						it.remove();
					}

				};
			}
		};
	}
	/**
	 * Creates an iterable which if iterates over the source and encounters an exception, it simply stops the iteration, consuming the exception.
	 * @param <T> the element type
	 * @param source the source iterable.
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> catchException(
			@Nonnull final Iterable<? extends T> source) {
		Iterable<? extends T> e = empty();
		return catchException(source, Functions.constant(e));
	}
	/**
	 * Creates an iterable which if iterates over the source and encounters an exception,
	 * the iteration is continued on the new iterable returned by the handler function.
	 * <p>The returned iterator forwards all <code>remove()</code> calls
	 * to the source.</p>
	 * @param <T> the element type
	 * @param source the source iterable.
	 * @param handler the exception handler.
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> catchException(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Func1<? super Throwable, ? extends Iterable<? extends T>> handler) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					/** The current iterator. */
					Iterator<? extends T> it = source.iterator();
					/** The last iterator used by next(). */
					Iterator<? extends T> itForRemove;
					/** The peek ahead container. */
					final SingleContainer<T> peek = new SingleContainer<T>();
					/** Indicate that we switched to the handler. */
					boolean usingHandler;
					@Override
					public boolean hasNext() {
						if (peek.isEmpty()) {
							while (!Thread.currentThread().isInterrupted()) {
								try {
									if (it.hasNext()) {
										itForRemove = it;
										peek.add(it.next());
									}
									break;
								} catch (Throwable t) {
									if (!usingHandler) {
										Closeables.closeSilently(it);
										it = handler.invoke(t).iterator();
										usingHandler = true;
									} else {
										Throwables.throwAsUnchecked(t);
									}
								}
							}
						}
						return !peek.isEmpty();
					}

					@Override
					public T next() {
						if (hasNext()) {
							return peek.take();
						}
						throw new NoSuchElementException();
					}

					@Override
					public void remove() {
						if (itForRemove == null) {
							throw new IllegalStateException();
						}
						itForRemove.remove();
						itForRemove = null;
					}

				};
			}
		};
	}
	/**
	 * Concatenate the given iterable sources one
	 * after another in a way, that calling the second <code>iterator()</code>
	 * only happens when there is no more element in the first iterator.
	 * <p>The returned iterator forwards all <code>remove()</code> calls
	 * to the current source (e.g., you can remove the same elements from
	 * multiple collections with a single traversal on the concat result).
	 * @param <T> the element type
	 * @param sources the list of iterables to concatenate
	 * @return a new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> concat(
			@Nonnull final Iterable<? extends Iterable<? extends T>> sources) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				final Iterator<? extends Iterable<? extends T>> si = sources.iterator();
				if (si.hasNext()) {
					return new Iterator<T>() {
						/** The current iterable. */
						Iterator<? extends T> iter = si.next().iterator();
						/** Save the last iterator since hasNext might run forward into other iterators. */
						Iterator<? extends T> itForRemove;
						@Override
						public boolean hasNext() {
							while (!iter.hasNext()) {
								if (!si.hasNext()) {
									return false;
								}
								iter = si.next().iterator();
							}
							return true;
						}

						@Override
						public T next() {
							if (!hasNext()) {
								throw new NoSuchElementException();
							}
							itForRemove = iter;
							return iter.next();
						}

						@Override
						public void remove() {
							if (itForRemove == null) {
								throw new IllegalStateException();
							}
							itForRemove.remove();
							itForRemove = null;
						}
					};
				}
				return Interactive.<T>empty().iterator();
			}

		};
	}
	/**
	 * Concatenate the given iterable sources one
	 * after another in a way, that calling the second <code>iterator()</code>
	 * only happens when there is no more element in the first iterator.
	 * <p>The returned iterator forwards all <code>remove()</code> calls
	 * to the current source (first or next).
	 * @param <T> the element type
	 * @param first the first iterable
	 * @param second the second iterable
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> concat(
			@Nonnull final Iterable<? extends T> first,
			@Nonnull final Iterable<? extends T> second) {
		List<Iterable<? extends T>> list = new LinkedList<Iterable<? extends T>>();
		list.add(first);
		list.add(second);
		return concat(list);
	}
	/**
	 * Returns an iterable which checks for the existence of the supplied
	 * value by comparing the elements of the source iterable using reference
	 * and <code>equals()</code>. The iterable then returns a single true or false.
	 * @param <T> the source element type
	 * @param source the source
	 * @param value the value to check
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<Boolean> contains(
			@Nonnull final Iterable<? extends T> source, final T value) {
		return any(source, new Func1<T, Boolean>() {
			@Override
			public Boolean invoke(T param1) {
				return param1 == value || (param1 != null && param1.equals(value));
			}
		});
	}
	/**
	 * Counts the elements of the iterable source by using a 32 bit <code>int</code>.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param <T> the element type
	 * @param source the source iterable
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<Integer> count(
			@Nonnull final Iterable<T> source) {
		return new Iterable<Integer>() {
			@Override
			public Iterator<Integer> iterator() {
				final Iterator<T> it = source.iterator();
				return new Iterator<Integer>() {
					/** The peek ahead container. */
					final SingleContainer<Option<Integer>> peek = new SingleContainer<Option<Integer>>();
					/** Computation already done. */
					boolean done;
					@Override
					public boolean hasNext() {
						if (!done) {
							if (peek.isEmpty()) {
								int count = 0;
								try {
									while (it.hasNext()) {
										it.next();
										count++;
									}
									peek.add(Option.some(count));
								} catch (Throwable t) {
									peek.add(Option.<Integer>error(t));
								} finally {
									done = true;
									Closeables.closeSilently(it);
								}
							}
						}
						return !peek.isEmpty();
					}

					@Override
					public Integer next() {
						if (hasNext()) {
							return peek.take().value();
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
	 * Counts the elements of the iterable source by using a 64 bit <code>long</code>.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param <T> the element type
	 * @param source the source iterable
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<Long> countLong(
			@Nonnull final Iterable<T> source) {
		return new Iterable<Long>() {
			@Override
			public Iterator<Long> iterator() {
				final Iterator<T> it = source.iterator();
				return new Iterator<Long>() {
					/** The peek ahead container. */
					final SingleContainer<Option<Long>> peek = new SingleContainer<Option<Long>>();
					/** Computation already done. */
					boolean done;
					@Override
					public boolean hasNext() {
						if (!done) {
							if (peek.isEmpty()) {
								long count = 0;
								try {
									while (it.hasNext()) {
										it.next();
										count++;
									}
									peek.add(Option.some(count));
								} catch (Throwable t) {
									peek.add(Option.<Long>error(t));
								} finally {
									done = true;
									Closeables.closeSilently(it);
								}
							}
						}
						return !peek.isEmpty();
					}

					@Override
					public Long next() {
						if (hasNext()) {
							return peek.take().value();
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
	 * Defers the source iterable creation to registration time and
	 * calls the given <code>func</code> for the actual source.
	 * @param <T> the element type
	 * @param func the function that returns an iterable.
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> defer(
			@Nonnull final Func0<? extends Iterable<T>> func) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return func.invoke().iterator();
			}
		};
	}
	/**
	 * Convert the source materialized elements into normal iterator behavior.
	 * The returned iterator will throw an <code>UnsupportedOperationException</code> for its <code>remove()</code> method.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param <T> the source element types
	 * @param source the source of T options
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> dematerialize(
			@Nonnull final Iterable<? extends Option<? extends T>> source) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				final Iterator<? extends Option<? extends T>> it = source.iterator();
				return new Iterator<T>() {
					final SingleContainer<Option<? extends T>> peek = new SingleContainer<Option<? extends T>>();
					@Override
					public boolean hasNext() {
						if (peek.isEmpty()) {
							if (it.hasNext()) {
								Option<? extends T> o = it.next();
								if (Option.isNone(o)) {
									return false;
								}
								peek.add(o);
							}
						}
						return !peek.isEmpty();
					}

					@Override
					public T next() {
						if (hasNext()) {
							return peek.take().value();
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
	 * Creates an iterable which ensures that subsequent values of T are not equal  (reference and equals).
	 * @param <T> the element type
	 * @param source the source iterable
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> distinctNext(
			@Nonnull final Iterable<? extends T> source) {
		return where(source,
		new Func0<Func2<Integer, T, Boolean>>() {
			@Override
			public Func2<Integer, T, Boolean> invoke() {
				return new Func2<Integer, T, Boolean>() {
					/** Is this the first element? */
					boolean first = true;
					/** The last seen element. */
					T last;
					@Override
					public Boolean invoke(Integer index, T param1) {
						if (first) {
							first = false;
							last = param1;
							return true;
						}
						if (last == param1 || (last != null && last.equals(param1))) {
							last = param1;
							return false;
						}
						last = param1;
						return true;
					}
				};
			}
		});
	}
	/**
	 * Creates an iterable which ensures that subsequent values of
	 * T are not equal in respect to the extracted keys (reference and equals).
	 * @param <T> the element type
	 * @param <U> the key type
	 * @param source the source iterable
	 * @param keyExtractor the function to extract the keys which will be compared
	 * @return the new iterable
	 */
	@Nonnull
	public static <T, U> Iterable<T> distinctNext(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Func1<T, U> keyExtractor) {
		return where(source,
		new Func0<Func2<Integer, T, Boolean>>() {
			@Override
			public Func2<Integer, T, Boolean> invoke() {
				return new Func2<Integer, T, Boolean>() {
					/** Is this the first element? */
					boolean first = true;
					/** The last seen element. */
					U last;
					@Override
					public Boolean invoke(Integer index, T param1) {
						U key = keyExtractor.invoke(param1);
						if (first) {
							first = false;
							last = key;
							return true;
						}
						if (last == key || (last != null && last.equals(key))) {
							last = key;
							return false;
						}
						last = key;
						return true;
					}
				};
			}
		});
	}
	/**
	 * Returns an iterable which filters its elements based if they were ever seen before in
	 * the current iteration.
	 * Value equality is computed by reference equality and <code>equals()</code>
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> distinct(
			@Nonnull final Iterable<? extends T> source) {
		return distinct(source, Functions.<T>identity(), Functions.<T>identity());
	}
	/**
	 * Returns an iterable which filters its elements by an unique key
	 * in a way that when multiple source items produce the same key, only
	 * the first one ever seen gets relayed further on.
	 * Key equality is computed by reference equality and <code>equals()</code>
	 * @param <T> the source element type
	 * @param <U> the key element type
	 * @param <V> the output element type
	 * @param source the source of Ts
	 * @param keySelector the key selector for only-once filtering
	 * @param valueSelector the value select for the output of the first key cases
	 * @return the new iterable
	 */
	@Nonnull
	public static <T, U, V> Iterable<V> distinct(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends U> keySelector,
			@Nonnull final Func1<? super T, ? extends V> valueSelector) {
		return select(where(source,
		new Func0<Func2<Integer, T, Boolean>>() {
			@Override
			public Func2<Integer, T, Boolean> invoke() {
				return new Func2<Integer, T, Boolean>() {
					final Set<U> memory = new HashSet<U>();
					@Override
					public Boolean invoke(Integer index, T param1) {
						return memory.add(keySelector.invoke(param1));
					}
				};
			}
		})
		, new Func1<T, V>() {
			@Override
			public V invoke(T param1) {
				return valueSelector.invoke(param1);
			}
		});
	}
	/**
	 * Returns an iterable which reiterates over and over again on <code>source</code>
	 * as long as the gate is true. The gate function is checked only
	 * when a pass over the source stream was completed.
	 * Note that using this operator on an empty iterable may result
	 * in a direct infinite loop in hasNext() or next() calls depending on the gate function.
	 * <p>The returned iterator forwards all <code>remove()</code> calls
	 * to the source.</p>
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @param gate the gate function to stop the repeat
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> doWhile(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Func0<Boolean> gate) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					/** is this the first pass? */
					Iterator<? extends T> it = source.iterator();
					@Override
					public boolean hasNext() {
						while (true) {
							if (it.hasNext()) {
								return true;
							}
							if (gate.invoke()) {
								it = source.iterator();
							} else {
								break;
							}
						}
						return false;
					}
					@Override
					public T next() {
						if (hasNext()) {
							return it.next();
						}
						throw new NoSuchElementException();
					}

					@Override
					public void remove() {
						it.remove();
					}
				};
			}
		};
	}

	/**
	 * Determines whether two iterables contain equal elements in the same
	 * order. More specifically, this method returns {@code true} if
	 * {@code iterable1} and {@code iterable2} contain the same number of
	 * elements and every element of {@code iterable1} is equal to the
	 * corresponding element of {@code iterable2}.
	 * @param iterable1 the first iterable
	 * @param iterable2 the second iterable
	 * @return true if both iterables are either empty or contain the same number and equal items
	 */
	public static boolean elementsEqual(@Nonnull Iterable<?> iterable1,
			@Nonnull Iterable<?> iterable2) {
		Iterator<?> iterator1 = iterable1.iterator();
		Iterator<?> iterator2 = iterable2.iterator();
		return elementsEqual(iterator1, iterator2);
	}
	/**
	 * Compares two iterators wether they contain the same element in terms of numbers
	 * and nullsafe Object.equals().
	 * @param iterator1 the first iterator
	 * @param iterator2 the second interator
	 * @return true if they are equal
	 * @since 0.97
	 */
	public static boolean elementsEqual(
			@Nonnull Iterator<?> iterator1,
			@Nonnull Iterator<?> iterator2) {
		try {
			while (iterator1.hasNext()) {
				if (!iterator2.hasNext()) {
					return false;
				}
				Object o1 = iterator1.next();
				Object o2 = iterator2.next();
				if (!equal(o1, o2)) {
					return false;
				}
			}
			return !iterator2.hasNext();
		} finally {
			Closeables.closeSilently(iterator1);
			Closeables.closeSilently(iterator2);
		}
	}
	/**
	 * Compare two object in a null-safe manner.
	 * @param a the first object
	 * @param b the second object
	 * @return true if both are null or equal according to Object.equals
	 */
	private static boolean equal(@Nullable Object a, @Nullable Object b) {
		return (a == b) || ((a != null) && a.equals(b));
	}

	/**
	 * Returns an empty iterable which will not produce elements.
	 * Its <code>hasNext()</code> returns always false,
	 * <code>next()</code> throws a <code>NoSuchElementException</code>
	 * and <code>remove()</code> throws an <code>IllegalStateException</code>.
	 * Note that the <code>Collections.emptyIterable()</code> static method is introduced by Java 7.
	 * @param <T> the element type, irrelevant
	 * @return the iterable
	 */
	@SuppressWarnings("unchecked")
	@Nonnull
	public static <T> Iterable<T> empty() {
		return (Iterable<T>)EMPTY_ITERABLE;
	}
	/**
	 * Returns an iterable which executes the given action after
	 * the stream completes.
	 * <p>The returned iterator forwards all <code>remove()</code> calls
	 * to the source.</p>
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param action the action to invoke
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> finish(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Action0 action) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					/** The iterator. */
					final Iterator<? extends T> it = source.iterator();
					/** After the last. */
					boolean last;
					@Override
					public boolean hasNext() {
						if (!it.hasNext()) {
							if (!last) {
								last = true;
								action.invoke();
							}
							return false;
						}
						return true;
					}

					@Override
					public T next() {
						if (hasNext()) {
							return it.next();
						}
						throw new NoSuchElementException();
					}

					@Override
					public void remove() {
						it.remove();
					}

				};
			}
		};
	}
	/**
	 * Returns an iterable which runs the source iterable and
	 * returns elements from the iterable returned by the function call.
	 * The difference from SelectMany is that the {@code Iterable&lt;U>}s are
	 * created before their concatenation starts.
	 * @param <T> the source element type
	 * @param <U> the output element type
	 * @param source the source
	 * @param selector the result selector
	 * @return the new iterable
	 */
	@Nonnull
	public static <T, U> Iterable<U> forEach(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends Iterable<? extends U>> selector) {
		return concat(select(source, selector));
	}
	/**
	 * A generator function which returns Ts based on the termination condition and the way it computes the next values.
	 * This is equivalent to:
	 * <code><pre>
	 * T value = seed;
	 * while (predicate(value)) {
	 *     yield value;
	 *     value = next(value);
	 * }
	 * </pre></code>
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param <T> the element type
	 * @param seed the initial value
	 * @param predicate the predicate to terminate the process
	 * @param next the function that computes the next value.
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> generate(
			final T seed,
			@Nonnull final Func1<? super T, Boolean> predicate,
			@Nonnull final Func1<? super T, ? extends T> next) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					T value = seed;
					@Override
					public boolean hasNext() {
						return predicate.invoke(value);
					}

					@Override
					public T next() {
						if (hasNext()) {
							T current = value;
							value = next.invoke(value);
							return current;
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
	 * A generator function which returns Ts based on the termination condition and the way it computes the next values,
	 * but the first T to be returned is preceded by an <code>initialDelay</code> amount of wait and each
	 * subsequent element is then generated after <code>betweenDelay</code> sleep.
	 * The sleeping is blocking the current thread which invokes the hasNext()/next() methods.
	 * This is equivalent to:
	 * <code><pre>
	 * T value = seed;
	 * sleep(initialDelay);
	 * if (predicate(value)) {
	 *     yield value;
	 * }
	 * value = next(value);
	 * sleep(betweenDelay);
	 * while (predicate(value)) {
	 *     yield value;
	 *     value = next(value);
	 *     sleep(betweenDelay);
	 * }
	 * </pre></code>
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param <T> the element type
	 * @param seed the initial value
	 * @param predicate the predicate to terminate the process
	 * @param next the function that computes the next value.
	 * @param initialDelay the initial delay
	 * @param betweenDelay the between delay
	 * @param unit the time unit for initialDelay and betweenDelay
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> generate(
			final T seed,
			@Nonnull final Func1<? super T, Boolean> predicate,
			@Nonnull final Func1<? super T, ? extends T> next,
			final long initialDelay,
			final long betweenDelay,
			@Nonnull final TimeUnit unit) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					T value = seed;
					/** Keeps track of whether there should be an initial delay? */
					boolean shouldInitialWait = true;
					/** Keeps track of whether there should be an initial delay? */
					boolean shouldBetweenWait;
					@Override
					public boolean hasNext() {
						if (shouldInitialWait) {
							shouldInitialWait = false;
							try {
								unit.sleep(initialDelay);
							} catch (InterruptedException e) {
								return false; // FIXME not soure about this
							}
						} else {
							if (shouldBetweenWait) {
								shouldBetweenWait = false;
								try {
									unit.sleep(betweenDelay);
								} catch (InterruptedException e) {
									return false; // FIXME not soure about this
								}
							}
						}
						return predicate.invoke(value);
					}

					@Override
					public T next() {
						if (hasNext()) {
							shouldBetweenWait = true;
							T current = value;
							value = next.invoke(value);
							return current;
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
	 * @return the current default pool used by the Observables methods
	 */
	@Nonnull
	static Scheduler scheduler() {
		return Schedulers.getDefault();
	}
	/**
	 * Creates an iterable which traverses the source iterable,
	 * and based on the key selector, groups values extracted by valueSelector into GroupedIterables,
	 * which can be iterated over later on.
	 * The equivalence of the keys are determined via reference
	 * equality and <code>equals()</code> equality.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param <T> the source element type
	 * @param <U> the result group element type
	 * @param <V> the result group keys
	 * @param source the source of Ts
	 * @param keySelector the key selector
	 * @param valueSelector the value selector
	 * @return the new iterable
	 */
	@Nonnull
	public static <T, U, V> Iterable<GroupedIterable<V, U>> groupBy(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends V> keySelector,
			@Nonnull final Func1<? super T, ? extends U> valueSelector) {
		return distinct(new Iterable<GroupedIterable<V, U>>() {
			@Override
			public Iterator<GroupedIterable<V, U>> iterator() {
				final Map<V, DefaultGroupedIterable<V, U>> groups = new LinkedHashMap<V, DefaultGroupedIterable<V, U>>();
				final Iterator<? extends T> it = source.iterator();
				return new Iterator<GroupedIterable<V, U>>() {
					Iterator<DefaultGroupedIterable<V, U>> groupIt;
					@Override
					public boolean hasNext() {
						return it.hasNext() || (groupIt != null && groupIt.hasNext());
					}

					@Override
					public GroupedIterable<V, U> next() {
						if (hasNext()) {
							if (groupIt == null) {
								try {
									while (it.hasNext()) {
										T t = it.next();
										V v = keySelector.invoke(t);
										U u = valueSelector.invoke(t);
										DefaultGroupedIterable<V, U> g = groups.get(v);
										if (g == null) {
											g = new DefaultGroupedIterable<V, U>(v);
											groups.put(v, g);
										}
										g.add(u);
									}
								} finally {
									Closeables.closeSilently(it);
								}
								groupIt = groups.values().iterator();
							}
							return groupIt.next();
						}
						throw new NoSuchElementException();
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}

				};
			}
		}, new Func1<GroupedIterable<V, U>, V>() {
			@Override
			public V invoke(GroupedIterable<V, U> param1) {
				return param1.key();
			}

		}, Functions.<GroupedIterable<V, U>>identity());
	}
	/**
	 * Construct a new iterable which will invoke the specified action
	 * before the source value gets relayed through it.
	 * Can be used to inject side-effects before returning a value.
	 * <p>The returned iterator forwards all <code>remove()</code> calls
	 * to the source.</p>
	 * @param <T> the returned element type
	 * @param source the source iterable
	 * @param action the action to invoke before each next() is returned.
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> invoke(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Action1<? super T> action) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					/** The source iterator. */
					final Iterator<? extends T> it = source.iterator();
					@Override
					public boolean hasNext() {
						return it.hasNext();
					}
					@Override
					public T next() {
						T value = it.next();
						action.invoke(value);
						return value;
					}
					@Override
					public void remove() {
						it.remove();
					}
				};
			}
		};
	}
	/**
	 * Returns an iterable which invokes the given <code>next</code>
	 * action for each element and the <code>finish</code> action when
	 * the source completes.
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @param next the action to invoke on each element
	 * @param finish the action to invoke after the last element
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> invoke(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull Action1<? super T> next,
			@Nonnull Action0 finish) {
		return invoke(source, next, Actions.noAction1(), finish);
	}
	/**
	 * Returns an iterable which invokes the given <code>next</code>
	 * action for each element and  <code>error</code> when an exception is thrown.
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @param next the action to invoke on each element
	 * @param error the error action to invoke for an error
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> invoke(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Action1<? super T> next,
			@Nonnull final Action1<? super Throwable> error) {
		return invoke(source, next, error, Actions.noAction0());
	}
	/**
	 * Returns an iterable which invokes the given <code>next</code>
	 * action for each element and the <code>finish</code> action when
	 * the source completes and <code>error</code> when an exception is thrown.
	 * <p>The returned iterator forwards all <code>remove()</code> calls
	 * to the source.</p>
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @param next the action to invoke on each element
	 * @param error the error action to invoke for an error
	 * @param finish the action to invoke after the last element
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> invoke(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Action1<? super T> next,
			@Nonnull final Action1<? super Throwable> error,
			@Nonnull final Action0 finish) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					/** The iterator. */
					final Iterator<? extends T> it = source.iterator();
					/** The peek ahead container. */
					final SingleContainer<Option<? extends T>> peek = new SingleContainer<Option<? extends T>>();
					/** Finish or error once. */
					boolean once = true;
					@Override
					public boolean hasNext() {
						if (peek.isEmpty()) {
							try {
								if (it.hasNext()) {
									peek.add(Option.some(it.next()));
								} else {
									if (once) {
										once = false;
										finish.invoke();
									}
								}
							} catch (Throwable t) {
								peek.add(Option.<T>error(t));
							}
						}
						return !peek.isEmpty();
					}

					@Override
					public T next() {
						if (it.hasNext()) {
							Option<? extends T> o = peek.take();
							if (Option.isError(o) && once) {
								once = false;
								error.invoke(Option.getError(o));
							}
							return o.value();
						}
						throw new NoSuchElementException();
					}

					@Override
					public void remove() {
						it.remove();
					}
				};
			}
		};
	}
	/**
	 * Returns a single true if the target iterable is empty.
	 * @param source source iterable with any type
	 * @return the new iterable
	 */
	@Nonnull
	public static Iterable<Boolean> isEmpty(
			@Nonnull final Iterable<?> source) {
		return select(any(source), Functions.negate());
	}
	/**
	 * Concatenates the source strings one after another and uses the given separator.
	 * <p>The returned iterator forwards all <code>remove()</code> calls
	 * to the source.</p>
	 * @param source the source
	 * @param separator the separator to use
	 * @return the new iterable
	 */
	@Nonnull
	public static Iterable<String> join(
			@Nonnull final Iterable<?> source,
			final String separator) {
		return aggregate(source,
			new Func2<StringBuilder, Object, StringBuilder>() {
				@Override
				public StringBuilder invoke(StringBuilder param1, Object param2) {
					if (param1 == null) {
						param1 = new StringBuilder();
					} else {
						param1.append(separator);
					}
					param1.append(param2);
					return param1;
				}
			},
			new Func2<StringBuilder, Integer, String>() {
				@Override
				public String invoke(StringBuilder param1, Integer param2) {
					return param1.toString();
				}
			}
		);
	}
	/**
	 * Returns the last element of the iterable or throws a <code>NoSuchElementException</code> if the iterable is empty.
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @return the last value
	 */
	public static <T> T last(
			@Nonnull final Iterable<? extends T> source) {
		Iterator<? extends T> it = source.iterator();
		try {
			if (it.hasNext()) {
				T t = null;
				while (it.hasNext()) {
					t = it.next();
				}
				return t;
			}
		} finally {
			Closeables.closeSilently(it);
		}
		throw new NoSuchElementException();
	}
	/**
	 * Transforms the sequence of the source iterable into an option sequence of
	 * Option.some(), Option.none() and Option.error() values, depending on
	 * what the source's hasNext() and next() produces.
	 * The returned iterator will throw an <code>UnsupportedOperationException</code> for its <code>remove()</code> method.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param <T> the source element type
	 * @param source the source of at least Ts.
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<Option<T>> materialize(
			@Nonnull final Iterable<? extends T> source) {
		return new Iterable<Option<T>>() {
			@Override
			public Iterator<Option<T>> iterator() {
				final Iterator<? extends T> it = source.iterator();
				return new Iterator<Option<T>>() {
					/** The peeked value or exception. */
					final SingleContainer<Option<T>> peek = new SingleContainer<Option<T>>();
					/** The source iterator threw an exception. */
					boolean broken;
					@Override
					public boolean hasNext() {
						if (!broken) {
							try {
								if (peek.isEmpty()) {
									if (it.hasNext()) {
										T t = it.next();
										peek.add(Option.some(t));
									} else {
										peek.add(Option.<T>none());
										broken = true;
									}
								}
							} catch (Throwable t) {
								broken = true;
								peek.add(Option.<T>error(t));
							}
						}
						return !peek.isEmpty();
					}

					@Override
					public Option<T> next() {
						if (hasNext()) {
							return peek.take();
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
	 * Returns the maximum value of the given iterable source.
	 * @param <T> the element type, which must be self comparable
	 * @param source the source elements
	 * @return the new iterable
	 */
	@Nonnull
	public static <T extends Comparable<? super T>> Iterable<T> max(
			@Nonnull final Iterable<? extends T> source) {
		return aggregate(source, Functions.<T>max(),
				Functions.<T, Integer>identityFirst());
	}
	/**
	 * Returns the maximum value of the given iterable source in respect to the supplied comparator.
	 * @param <T> the element type, which must be self comparable
	 * @param source the source elements
	 * @param comparator the comparator to use
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> max(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Comparator<? super T> comparator) {
		return aggregate(source, Functions.<T>max(comparator), Functions.<T, Integer>identityFirst());
	}
	/**
	 * Returns an iterator which will produce a single List of the maximum values encountered
	 * in the source stream based on the supplied key selector.
	 * @param <T> the source element type, which must be self comparable
	 * @param source the source of Ts
	 * @return the new iterable
	 */
	@Nonnull
	public static <T extends Comparable<? super T>> Iterable<List<T>> maxBy(
			@Nonnull final Iterable<? extends T> source) {
		return minMax(source, Functions.<T>identity(), Functions.<T>comparator(), true);
	}
	/**
	 * Returns an iterator which will produce a single List of the maximum values encountered
	 * in the source stream based on the supplied comparator.
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @param comparator the key comparator
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<List<T>> maxBy(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Comparator<? super T> comparator) {
		return minMax(source, Functions.<T>identity(), comparator, true);
	}
	/**
	 * Returns an iterator which will produce a single List of the maximum values encountered
	 * in the source stream based on the supplied key selector.
	 * @param <T> the source element type
	 * @param <U> the key type, which must be self-comparable
	 * @param source the source of Ts
	 * @param keySelector the selector for keys
	 * @return the new iterable
	 */
	@Nonnull
	public static <T, U extends Comparable<? super U>> Iterable<List<T>> maxBy(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends U> keySelector) {
		return minMax(source, keySelector, Functions.<U>comparator(), true);
	}
	/**
	 * Returns an iterator which will produce a single List of the minimum values encountered
	 * in the source stream based on the supplied key selector and comparator.
	 * @param <T> the source element type
	 * @param <U> the key type
	 * @param source the source of Ts
	 * @param keySelector the selector for keys
	 * @param keyComparator the key comparator
	 * @return the new iterable
	 */
	@Nonnull
	public static <T, U> Iterable<List<T>> maxBy(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends U> keySelector,
			@Nonnull final Comparator<? super U> keyComparator) {
		return minMax(source, keySelector, keyComparator, true);
	}
	/**
	 * Enumerates the source iterable once and caches its results.
	 * Any iterator party will basically drain this cache, e.g.,
	 * reiterating over this iterable will produce no results.
	 * Note: the name is not a misspelling, see <a href='http://en.wikipedia.org/wiki/Memoization'>Memoization</a>.
	 * FIXME not sure about the buffer sizes.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @param bufferSize the size of the buffering
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> memoize(
			@Nonnull final Iterable<? extends T> source,
			final int bufferSize) {
		if (bufferSize < 0) {
			throw new IllegalArgumentException("bufferSize < 0");
		}
		return new Iterable<T>() {
			/** The source iterator. */
			Iterator<? extends T> it = source.iterator();
			/** The ring buffer of the memory. */
			final CircularBuffer<T> buffer = new CircularBuffer<T>(bufferSize);
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					int myHead;

					@Override
					public boolean hasNext() {
						return buffer.tail() > Math.max(myHead, buffer.head()) || it.hasNext();
					}

					@Override
					public T next() {
						if (hasNext()) {
							if (buffer.tail() == myHead) {
								T value = it.next();
								if (bufferSize > 0) {
									buffer.add(value);
								}
								myHead++;
								return value;
							} else {
								myHead = Math.max(myHead, buffer.head());
								T value = buffer.get(myHead);
								myHead++;
								return value;
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
	/**
	 * The returned iterable ensures that the source iterable is only traversed once, regardless of
	 * how many iterator attaches to it and each iterator see only the values.
	 * Note: the name is not a misspelling, see <a href='http://en.wikipedia.org/wiki/Memoization'>Memoization</a>.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> memoizeAll(
			@Nonnull final Iterable<? extends T> source) {
		final Iterator<? extends T> it = source.iterator();
		final LinkedBuffer<T> buffer = new LinkedBuffer<T>();
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					/** The element count. */
					int count = 0;
					/** The current node pointer. */
					N<T> pointer = buffer.head;
					@Override
					public boolean hasNext() {
						return count < buffer.size || it.hasNext();
					}

					@Override
					public T next() {
						if (hasNext()) {
							if (count < buffer.size) {
								T value = pointer.next.value;
								pointer = pointer.next;
								count++;
								return value;
							} else {
								T value = it.next();
								buffer.add(value);
								count++;
								pointer = pointer.next;
								return value;
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
	/**
	 * Merges a bunch of iterable streams where each of the iterable will run by
	 * a scheduler and their events are merged together in a single stream.
	 * The returned iterator throws an <code>UnsupportedOperationException</code> in its <code>remove()</code> method.
	 * @param <T> the element type
	 * @param sources the iterable of source iterables.
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> merge(
			@Nonnull final Iterable<? extends Iterable<? extends T>> sources) {
		return merge(sources, scheduler());
	}
	/**
	 * Merges a bunch of iterable streams where each of the iterable will run by
	 * a scheduler and their events are merged together in a single stream.
	 * The returned iterator throws an <code>UnsupportedOperationException</code> in its <code>remove()</code> method.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param <T> the element type
	 * @param sources the iterable of source iterables.
	 * @param scheduler the scheduler for running each inner iterable in parallel
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> merge(
			@Nonnull final Iterable<? extends Iterable<? extends T>> sources,
			@Nonnull final Scheduler scheduler) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				final BlockingQueue<Option<T>> queue = new LinkedBlockingQueue<Option<T>>();
				final AtomicInteger wip = new AtomicInteger(1);
				final List<Closeable> handlers = new LinkedList<Closeable>();
				for (final Iterable<? extends T> iter : sources) {
					Runnable r = new Runnable() {
						@Override
						public void run() {
							try {
								final Iterator<? extends T> fiter = iter.iterator();
								try {
									while (fiter.hasNext()) {
										T t = fiter.next();
										if (!Thread.currentThread().isInterrupted()) {
											queue.add(Option.some(t));
										}
									}
								} finally {
									Closeables.closeSilently(fiter);
								}
								if (wip.decrementAndGet() == 0) {
									if (!Thread.currentThread().isInterrupted()) {
										queue.add(Option.<T>none());
									}
								}
							} catch (Throwable t) {
								queue.add(Option.<T>error(t));
							}
						}
					};
					wip.incrementAndGet();
					handlers.add(scheduler.schedule(r));
				}
				if (wip.decrementAndGet() == 0) {
					queue.add(Option.<T>none());
				}
				return new Iterator<T>() {
					final SingleContainer<Option<T>> peek = new SingleContainer<Option<T>>();
					/** Are we broken? */
					boolean broken;
					@Override
					public boolean hasNext() {
						if (!broken) {
							if (peek.isEmpty()) {
								try {
									Option<T> t = queue.take();
									if (Option.isSome(t)) {
										peek.add(t);
									} else
									if (Option.isError(t)) {
										peek.add(t);
										broken = true;
									}
								} catch (InterruptedException ex) {
									return false; // FIXME not sure about this
								}
							}
						}
						return !peek.isEmpty();
					}

					@Override
					public T next() {
						if (hasNext()) {
							try {
								return peek.take().value();
							} catch (RuntimeException ex) {
								for (Closeable h : handlers) {
									Closeables.closeSilently(h);
								}
								throw ex;
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
	/**
	 * Merges two iterable streams.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param <T> the element type
	 * @param first the first iterable
	 * @param second the second iterable
	 * @return the resulting stream of Ts
	 */
	@Nonnull
	public static <T> Iterable<T> merge(
			@Nonnull final Iterable<? extends T> first,
			@Nonnull final Iterable<? extends T> second) {
		List<Iterable<? extends T>> list = new ArrayList<Iterable<? extends T>>(2);
		list.add(first);
		list.add(second);
		return merge(list);
	}
	/**
	 * Returns the minimum value of the given iterable source.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param <T> the element type, which must be self comparable
	 * @param source the source elements
	 * @return the new iterable
	 */
	@Nonnull
	public static <T extends Comparable<? super T>> Iterable<T> min(
			@Nonnull final Iterable<? extends T> source) {
		return aggregate(source, Functions.<T>min(), Functions.<T, Integer>identityFirst());
	}
	/**
	 * Returns the minimum value of the given iterable source in respect to the supplied comparator.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param <T> the element type, which must be self comparable
	 * @param source the source elements
	 * @param comparator the comparator to use
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> min(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Comparator<? super T> comparator) {
		return aggregate(source, Functions.<T>min(comparator), Functions.<T, Integer>identityFirst());
	}
	/**
	 * Returns an iterator which will produce a single List of the minimum values encountered
	 * in the source stream based on the supplied key selector.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param <T> the source element type, which must be self comparable
	 * @param source the source of Ts
	 * @return the new iterable
	 */
	@Nonnull
	public static <T extends Comparable<? super T>> Iterable<List<T>> minBy(
			@Nonnull final Iterable<? extends T> source) {
		return minMax(source, Functions.<T>identity(), Functions.<T>comparator(), false);
	}
	/**
	 * Returns an iterator which will produce a single List of the minimum values encountered
	 * in the source stream based on the supplied comparator.
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @param comparator the key comparator
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<List<T>> minBy(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Comparator<? super T> comparator) {
		return minMax(source, Functions.<T>identity(), comparator, false);
	}
	/**
	 * Returns an iterator which will produce a single List of the minimum values encountered
	 * in the source stream based on the supplied key selector.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param <T> the source element type
	 * @param <U> the key type, which must be self-comparable
	 * @param source the source of Ts
	 * @param keySelector the selector for keys
	 * @return the new iterable
	 */
	@Nonnull
	public static <T, U extends Comparable<? super U>> Iterable<List<T>> minBy(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends U> keySelector) {
		return minMax(source, keySelector, Functions.<U>comparator(), false);
	}
	/**
	 * Returns an iterator which will produce a single List of the minimum values encountered
	 * in the source stream based on the supplied key selector and comparator.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param <T> the source element type
	 * @param <U> the key type
	 * @param source the source of Ts
	 * @param keySelector the selector for keys
	 * @param keyComparator the key comparator
	 * @return the new iterable
	 */
	@Nonnull
	public static <T, U> Iterable<List<T>> minBy(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends U> keySelector,
			@Nonnull final Comparator<? super U> keyComparator) {
		return minMax(source, keySelector, keyComparator, false);
	}
	/**
	 * Returns an iterator which will produce a single List of the minimum values encountered
	 * in the source stream based on the supplied key selector and comparator.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param <T> the source element type
	 * @param <U> the key type
	 * @param source the source of Ts
	 * @param keySelector the selector for keys
	 * @param keyComparator the key comparator
	 * @param max should the computation return the minimums or the maximums
	 * @return the new iterable
	 */
	@Nonnull
	static <T, U> Iterable<List<T>> minMax(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends U> keySelector,
			@Nonnull final Comparator<? super U> keyComparator,
			final boolean max) {
		return new Iterable<List<T>>() {
			@Override
			public Iterator<List<T>> iterator() {
				return new Iterator<List<T>>() {
					/** The source iterator. */
					final Iterator<? extends T> it = source.iterator();
					/** The single result container. */
					final SingleContainer<Option<? extends List<T>>> result = new SingleContainer<Option<? extends List<T>>>();
					/** We have finished the aggregation. */
					boolean done;
					@Override
					public boolean hasNext() {
						if (!done) {
							done = true;
							if (result.isEmpty()) {
								try {
									List<T> intermediate = null;
									U lastKey = null;
									try {
										while (it.hasNext()) {
											T value = it.next();
											U key = keySelector.invoke(value);
											if (lastKey == null) {
												intermediate = new ArrayList<T>();
												lastKey = key;
												intermediate.add(value);
											} else {
												int c = keyComparator.compare(lastKey, key);
												if ((c < 0 && max) || (c > 0 && !max)) {
													intermediate = new ArrayList<T>();
													lastKey = key;
													c = 0;
												}
												if (c == 0) {
													intermediate.add(value);
												}
											}
										}
									} finally {
										Closeables.closeSilently(it);
									}
									if (intermediate != null) {
										result.add(Option.some(intermediate));
									}
								} catch (Throwable t) {
									result.add(Option.<List<T>>error(t));
								}
							}
						}
						return !result.isEmpty();
					}

					@Override
					public List<T> next() {
						if (hasNext()) {
							return result.take().value();
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
	 * Returns an iterable which traverses the entire
	 * source iterable and creates an ordered list
	 * of elements. Once the source iterator completes,
	 * the elements are streamed to the output.
	 * @param <T> the source element type, must be self comparable
	 * @param source the source of Ts
	 * @return the new iterable
	 */
	@Nonnull
	public static <T extends Comparable<? super T>> Iterable<T> orderBy(
			@Nonnull final Iterable<? extends T> source
			) {
		return orderBy(source, Functions.<T>identity(), Functions.<T>comparator());
	}
	/**
	 * Returns an iterable which traverses the entire
	 * source iterable and creates an ordered list
	 * of elements. Once the source iterator completes,
	 * the elements are streamed to the output.
	 * @param <T> the source element type, must be self comparable
	 * @param source the source of Ts
	 * @param comparator the value comparator
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> orderBy(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Comparator<? super T> comparator
			) {
		return orderBy(source, Functions.<T>identity(), comparator);
	}
	/**
	 * Returns an iterable which traverses the entire
	 * source iterable and creates an ordered list
	 * of elements. Once the source iterator completes,
	 * the elements are streamed to the output.
	 * @param <T> the source element type
	 * @param <U> the key type for the ordering, must be self comparable
	 * @param source the source of Ts
	 * @param keySelector the key selector for comparison
	 * @return the new iterable
	 */
	@Nonnull
	public static <T, U extends Comparable<? super U>> Iterable<T> orderBy(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends U> keySelector
			) {
		return orderBy(source, keySelector, Functions.<U>comparator());
	}
	/**
	 * Returns an iterable which traverses the entire
	 * source iterable and creates an ordered list
	 * of elements. Once the source iterator completes,
	 * the elements are streamed to the output.
	 * @param <T> the source element type
	 * @param <U> the key type for the ordering
	 * @param source the source of Ts
	 * @param keySelector the key selector for comparison
	 * @param keyComparator the key comparator function
	 * @return the new iterable
	 */
	@Nonnull
	public static <T, U> Iterable<T> orderBy(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends U> keySelector,
			@Nonnull final Comparator<? super U> keyComparator
			) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					/** The buffer. */
					List<T> buffer;
					/** The source iterator. */
					final Iterator<? extends T> it = source.iterator();
					/** The buffer iterator. */
					Iterator<T> bufIterator;
					@Override
					public boolean hasNext() {
						if (buffer == null) {
							buffer = new ArrayList<T>();
							try {
								while (it.hasNext()) {
									buffer.add(it.next());
								}
							} finally {
								Closeables.closeSilently(it);
							}
							Collections.sort(buffer, new Comparator<T>() {
								@Override
								public int compare(T o1, T o2) {
									U key1 = keySelector.invoke(o1);
									U key2 = keySelector.invoke(o2);
									return keyComparator.compare(key1, key2);
								}
							});
							bufIterator = buffer.iterator();
						}
						return bufIterator.hasNext();
					}

					@Override
					public T next() {
						if (hasNext()) {
							return bufIterator.next();
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
	 * Creates an observer with debugging purposes.
	 * It prints the submitted values to STDOUT separated by commas and line-broken by 80 characters, the exceptions to STDERR
	 * and prints an empty newline when it receives a finish().
	 * @param <T> the value type
	 * @return the observer
	 */
	@Nonnull
	public static <T> Action1<T> print() {
		return print(", ", 80);
	}
	/**
	 * Creates an observer with debugging purposes.
	 * It prints the submitted values to STDOUT, the exceptions to STDERR
	 * and prints an empty newline when it receives a finish().
	 * @param <T> the value type
	 * @param separator the separator to use between subsequent values
	 * @param maxLineLength how many characters to print into each line
	 * @return the observer
	 */
	@Nonnull
	public static <T> Action1<T> print(final String separator, final int maxLineLength) {
		return new Action1<T>() {
			/** Indicator for the first element. */
			boolean first = true;
			/** The current line length. */
			int len;
			@Override
			public void invoke(T value) {
				String s = String.valueOf(value);
				if (first) {
					first = false;
					System.out.print(s);
					len = s.length();
				} else {
					if (len + separator.length() + s.length() > maxLineLength) {
						if (len == 0) {
							System.out.print(separator);
							System.out.print(s);
							len = s.length() + separator.length();
						} else {
							System.out.println(separator);
							System.out.print(s);
							len = s.length();
						}
					} else {
						System.out.print(separator);
						System.out.print(s);
						len += s.length() + separator.length();
					}
				}
			}
		};
	}
	/**
	 * Creates an action for debugging purposes.
	 * It prints the submitted values to STDOUT with a line break.
	 * @param <T> the value type
	 * @return the observer
	 */
	@Nonnull
	public static <T> Action1<T> println() {
		return new Action1<T>() {
			@Override
			public void invoke(T value) {
				System.out.println(value);
			}
		};
	}
	/**
	 * Creates an action for debugging purposes.
	 * It prints the submitted values to STDOUT with a line break.
	 * @param <T> the value type
	 * @param prefix the prefix to use when printing
	 * @return the action
	 */
	@Nonnull
	public static <T> Action1<T> println(final String prefix) {
		return new Action1<T>() {
			@Override
			public void invoke(T value) {
				System.out.print(prefix);
				System.out.println(value);
			}
		};
	}
	/**
	 * Applies the <code>func</code> function for a shared instance of the source,
	 * e.g., <code>func.invoke(share(source))</code>.
	 * @param <T> the source element type
	 * @param <U> the return types
	 * @param source the source of Ts
	 * @param func invoke the function on the buffering iterable and return an iterator over it.
	 * @return the new iterable
	 */
	@Nonnull
	public static <T, U> Iterable<U> prune(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Func1<? super Iterable<? extends T>, ? extends Iterable<U>> func) {
		return func.invoke(share(source));
	}
	/**
	 * The returned iterable ensures that the source iterable is only traversed once, regardless of
	 * how many iterator attaches to it and each iterator see only the same cached values.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for <code>remove()</code> method of its first element, then it might throw for any
	 * subsequent element, depending on the source iterable.</p>
	 * @param <T> the source element type
	 * @param <U> the return types
	 * @param source the source of Ts
	 * @param func invoke the function on the buffering iterable and return an iterator over it.
	 * @param initial the initial value to append to the output stream
	 * @return the new iterable
	 */
	@SuppressWarnings("unchecked")
	@Nonnull
	public static <T, U> Iterable<U> publish(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Func1<? super Iterable<? super T>, ? extends Iterable<? extends U>> func,
			final U initial) {
		return startWith(func.invoke(memoizeAll(source)), initial);
	}
	/**
	 * The returned iterable ensures that the source iterable is only traversed once, regardless of
	 * how many iterator attaches to it and each iterator see only the values.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param <T> the source element type
	 * @param <U> the return types
	 * @param source the source of Ts
	 * @param func invoke the function on the buffering iterable and return an iterator over it.
	 * @return the new iterable
	 * TODO check
	 */
	@Nonnull
	public static <T, U> Iterable<U> publish(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Func1<? super Iterable<T>, ? extends Iterable<U>> func) {
		return func.invoke(memoizeAll(source));
	}
	/**
	 * Creates an integer iterator which returns numbers from the start position in the count size.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param start the starting value.
	 * @param count the number of elements to return, negative count means counting down from the start.
	 * @return the iterator.
	 */
	@Nonnull
	public static Iterable<Integer> range(final int start, final int count) {
		return new Iterable<Integer>() {
			@Override
			public Iterator<Integer> iterator() {
				return new Iterator<Integer>() {
					int current = start;
					@Override
					public boolean hasNext() {
						return current < start + count;
					}
					@Override
					public Integer next() {
						if (hasNext()) {
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
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param start the starting value.
	 * @param count the number of elements to return, negative count means counting down from the start.
	 * @return the iterator.
	 */
	@Nonnull
	public static Iterable<Long> range(final long start, final long count) {
		return new Iterable<Long>() {
			@Override
			public Iterator<Long> iterator() {
				return new Iterator<Long>() {
					long current = start;
					@Override
					public boolean hasNext() {
						return current < start + count;
					}
					@Override
					public Long next() {
						if (hasNext()) {
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
	 * Relays the source iterable's values until the gate returns false.
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @param gate the gate to stop the relaying
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> relayWhile(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Func0<Boolean> gate) {
		return where(source, new Func0<Func2<Integer, T, Boolean>>() {
			@Override
			public Func2<Integer, T, Boolean> invoke() {
				return new Func2<Integer, T, Boolean>() {
					/** The activity checker which turns to false once the gate returns false. */
					boolean active = true;
					@Override
					public Boolean invoke(Integer param1, T param2) {
						active &= gate.invoke();
						return active;
					}
				};
			}
		});
	}
	/**
	 * The returned iterable ensures that the source iterable is only traversed once, regardless of
	 * how many iterator attaches to it and each iterator may only see one source element.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param <T> the source element type
	 * @param <U> the return types
	 * @param source the source of Ts
	 * @param func invoke the function on the buffering iterable and return an iterator over it.
	 * @return the new iterable
	 */
	@Nonnull
	public static <T, U> Iterable<U> replay(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Func1<? super Iterable<T>, ? extends Iterable<U>> func) {
		return func.invoke(memoize(source, 0));
	}
	/**
	 * The returned iterable ensures that the source iterable is only traversed once, regardless of
	 * how many iterator attaches to it and each iterator see only the some cached values.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param <T> the source element type
	 * @param <U> the return types
	 * @param source the source of Ts
	 * @param func invoke the function on the buffering iterable and return an iterator over it.
	 * @param bufferSize the buffer size
	 * @return the new iterable
	 */
	@Nonnull
	public static <T, U> Iterable<U> replay(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Func1<? super Iterable<T>, ? extends Iterable<U>> func,
			final int bufferSize) {
		return func.invoke(memoize(source, bufferSize));
	}
	/**
	 * Creates an iterable which resumes with the next iterable from the sources when one throws an exception or completes normally.
	 * <p>The returned iterator forwards all <code>remove()</code> calls
	 * to the source.</p>
	 * @param <T> the source element type
	 * @param sources the list of sources to try one after another
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> resumeAlways(
			@Nonnull final Iterable<? extends Iterable<? extends T>> sources) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				final Iterator<? extends Iterable<? extends T>> iter0 = sources.iterator();
				if (iter0.hasNext()) {
					return new Iterator<T>() {
						/** The current iterator. */
						Iterator<? extends T> it = iter0.next().iterator();
						/** The memorized iterator for the remove call. */
						Iterator<? extends T> itForRemove = null;
						/** The peek ahead container. */
						final SingleContainer<Option<? extends T>> peek = new SingleContainer<Option<? extends T>>();
						@Override
						public boolean hasNext() {
							if (peek.isEmpty()) {
								while (!Thread.currentThread().isInterrupted()) {
									try {
										if (it.hasNext()) {
											peek.add(Option.some(it.next()));
											break;
										} else {
											if (iter0.hasNext()) {
												it = iter0.next().iterator();
											} else {
												break;
											}
										}
									} catch (Throwable t) {
										if (iter0.hasNext()) {
											it = iter0.next().iterator();
										} else {
											peek.add(Option.<T>error(t));
											break;
										}
									}
								}
							}
							return !peek.isEmpty();
						}

						@Override
						public T next() {
							if (hasNext()) {
								itForRemove = it;
								return peek.take().value();
							}
							throw new NoSuchElementException();
						}

						@Override
						public void remove() {
							if (itForRemove == null) {
								throw new IllegalStateException();
							}
							itForRemove.remove();
							itForRemove = null;
						}
					};
				}
				return Interactive.<T>empty().iterator();
			}
		};
	}
	/**
	 * Creates an iterable which resumes with the next iterable from the sources when one throws an exception.
	 * @param <T> the source element type
	 * @param first the first source
	 * @param second the second source
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> resumeAlways(
			@Nonnull final Iterable<? extends T> first,
			@Nonnull final Iterable<? extends T> second) {
		List<Iterable<? extends T>> list = new ArrayList<Iterable<? extends T>>(2);
		list.add(first);
		list.add(second);
		return resumeAlways(list);
	}
	/**
	 * Creates an iterable which resumes with the next iterable from the sources when one throws an exception.
	 * <p>The returned iterator forwards all <code>remove()</code> calls
	 * to the source.</p>
	 * @param <T> the source element type
	 * @param sources the list of sources to try one after another
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> resumeOnError(
			@Nonnull final Iterable<? extends Iterable<? extends T>> sources) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				final Iterator<? extends Iterable<? extends T>> iter0 = sources.iterator();
				if (iter0.hasNext()) {
					return new Iterator<T>() {
						/** The current iterator. */
						Iterator<? extends T> it = iter0.next().iterator();
						/** The memorized iterator for the remove call. */
						Iterator<? extends T> itForRemove = null;
						/** The peek ahead container. */
						final SingleContainer<Option<? extends T>> peek = new SingleContainer<Option<? extends T>>();
						@Override
						public boolean hasNext() {
							if (peek.isEmpty()) {
								while (!Thread.currentThread().isInterrupted()) {
									try {
										if (it.hasNext()) {
											peek.add(Option.some(it.next()));
										}
										break;
									} catch (Throwable t) {
										if (iter0.hasNext()) {
											it = iter0.next().iterator();
										} else {
											peek.add(Option.<T>error(t));
											break;
										}
									}
								}
							}
							return !peek.isEmpty();
						}

						@Override
						public T next() {
							if (hasNext()) {
								itForRemove = it;
								return peek.take().value();
							}
							throw new NoSuchElementException();
						}

						@Override
						public void remove() {
							if (itForRemove == null) {
								throw new IllegalStateException();
							}
							itForRemove.remove();
							itForRemove = null;
						}
					};
				}
				return Interactive.<T>empty().iterator();
			}
		};
	}
	/**
	 * Creates an iterable which resumes with the next iterable from the sources when one throws an exception.
	 * @param <T> the source element type
	 * @param first the first source
	 * @param second the second source
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> resumeOnError(
			@Nonnull final Iterable<? extends T> first,
			@Nonnull final Iterable<? extends T> second) {
		List<Iterable<? extends T>> list = new ArrayList<Iterable<? extends T>>(2);
		list.add(first);
		list.add(second);
		return resumeOnError(list);
	}
	/**
	 * Creates an iterator which attempts to re-iterate the source if it threw an exception.
	 * <code><pre>
	 * while (count-- > 0) {
	 * 	  try {
	 *        for (T t : source) {
	 *            yield t;
	 *        }
	 *        break;
	 *    } catch (Throwable t) {
	 *        if (count <= 0) {
	 *            throw t;
	 *        }
	 *    }
	 * }
	 * </pre></code>
	 * <p>The returned iterator forwards all <code>remove()</code> calls
	 * to the source.</p>
	 * @param <T> the source type
	 * @param source the source of Ts
	 * @param count the number of retry attempts
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> retry(
			@Nonnull final Iterable<? extends T> source,
			final int count) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					/** The retry count. */
					int retries = count;
					/** The peek store. */
					final SingleContainer<Option<? extends T>> peek = new SingleContainer<Option<? extends T>>();
					/** The current iterator. */
					Iterator<? extends T> it = source.iterator();
					@Override
					public boolean hasNext() {
						if (peek.isEmpty()) {
							while (it.hasNext()) {
								try {
									peek.add(Option.some(it.next()));
									break;
								} catch (Throwable t) {
									if (retries-- > 0) {
										it = source.iterator();
									} else {
										peek.add(Option.<T>error(t));
										break;
									}
								}
							}
						}
						return !peek.isEmpty();
					}

					@Override
					public T next() {
						if (hasNext()) {
							return peek.take().value();
						}
						throw new NoSuchElementException();
					}

					@Override
					public void remove() {
						it.remove();
					}

				};
			}
		};
	}
	/**
	 * Iterate over the source and submit each value to the
	 * given action. Basically, a for-each loop with pluggable
	 * action.
	 * This method is useful when the concrete values from the iterator
	 * are not needed but the iteration itself implies some side effects.
	 * @param <T> the element type of the iterable
	 * @param source the iterable
	 * @param action the action to invoke on with element
	 */
	public static <T> void run(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull Action1<? super T> action) {
		Iterator<? extends T> iter = source.iterator();
		try {
			while (iter.hasNext()) {
				T t = iter.next();
				action.invoke(t);
			}
		} finally {
			Closeables.closeSilently(iter);
		}
	}
	/**
	 * Iterates over the given source without using its returned value.
	 * This method is useful when the concrete values from the iterator
	 * are not needed but the iteration itself implies some side effects.
	 * @param source the source iterable to run through
	 */
	public static void run(
			@Nonnull final Iterable<?> source) {
		run(source, Actions.noAction1());
	}
	/**
	 * Generates an iterable which acts like a running sum when iterating over the source iterable, e.g.,
	 * For each element in T, it computes a value by using the current aggregation value and returns it.
	 * The first call to the aggregator function will receive a zero for its first argument.
	 * @param <T> the source element type
	 * @param <U> the destination element type
	 * @param source the source of Ts
	 * @param aggregator the function which takes the current running aggregation value, the current element and produces a new aggregation value.
	 * @return the new iterable
	 * TODO rework
	 */
	@Nonnull
	public static <T, U> Iterable<U> scan(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Func2<? super U, ? super T, ? extends U> aggregator) {
		return scan(source, null, aggregator);
	}
	/**
	 * Generates an iterable which acts like a running sum when iterating over the source iterable, e.g.,
	 * For each element in T, it computes a value by using the current aggregation value and returns it.
	 * The first call to the aggregator function will receive a zero for its first argument.
	 * <p>The returned iterator forwards all <code>remove()</code> calls
	 * to the source.</p>
	 * @param <T> the source element type
	 * @param <U> the destination element type
	 * @param source the source of Ts
	 * @param seed the initial value of the running aggregation
	 * @param aggregator the function which takes the current running aggregation value, the current element and produces a new aggregation value.
	 * @return the new iterable
	 */
	@Nonnull
	public static <T, U> Iterable<U> scan(
			@Nonnull final Iterable<? extends T> source,
			final U seed,
			@Nonnull final Func2<? super U, ? super T, ? extends U> aggregator) {
		return new Iterable<U>() {
			@Override
			public Iterator<U> iterator() {
				final Iterator<? extends T> it = source.iterator();
				return new Iterator<U>() {
					/** The current value. */
					U current = seed;

					@Override
					public boolean hasNext() {
						return it.hasNext();
					}

					@Override
					public U next() {
						current = aggregator.invoke(current, it.next());
						return current;
					}

					@Override
					public void remove() {
						it.remove();
					}
				};
			}
		};
	}
	/**
	 * Creates an iterable which is a transforms the source
	 * elements by using the selector function.
	 * The function receives the current index and the current element.
	 * @param <T> the source element type
	 * @param <U> the output element type
	 * @param source the source iterable
	 * @param selector the selector function
	 * @return the new iterable
	 */
	@Nonnull
	public static <T, U> Iterable<U> select(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends U> selector) {
		return select(source, new Func2<Integer, T, U>() {
			@Override
			public U invoke(Integer param1, T param2) {
				return selector.invoke(param2);
			}
		});
	}
	/**
	 * Creates an iterable which is a transforms the source
	 * elements by using the selector function.
	 * The function receives the current index and the current element.
	 * <p>The returned iterator forwards all <code>remove()</code> calls
	 * to the source.</p>
	 * @param <T> the source element type
	 * @param <U> the output element type
	 * @param source the source iterable
	 * @param selector the selector function
	 * @return the new iterable
	 */
	@Nonnull
	public static <T, U> Iterable<U> select(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Func2<? super Integer, ? super T, ? extends U> selector) {
		return new Iterable<U>() {
			@Override
			public Iterator<U> iterator() {
				final Iterator<? extends T> it = source.iterator();
				return new Iterator<U>() {
					/** The current counter. */
					int count;
					@Override
					public boolean hasNext() {
						return it.hasNext();
					}

					@Override
					public U next() {
						return selector.invoke(count++, it.next());
					}

					@Override
					public void remove() {
						it.remove();
					}

				};
			}
		};
	}
	/**
	 * Creates an iterable which returns a stream of Us for each source Ts.
	 * The iterable stream of Us is returned by the supplied selector function.
	 * <p>The returned iterator forwards all <code>remove()</code> calls
	 * to the current source (which might not accept it).
	 * @param <T> the source element type
	 * @param <U> the output element type
	 * @param source the source
	 * @param selector the selector for multiple Us for each T
	 * @return the new iterable
	 */
	@Nonnull
	public static <T, U> Iterable<U> selectMany(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends Iterable<? extends U>> selector) {
		return new Iterable<U>() {
			@Override
			public Iterator<U> iterator() {
				final Iterator<? extends T> it = source.iterator();
				return new Iterator<U>() {
					/** The current selected iterator. */
					Iterator<? extends U> sel;
					@Override
					public boolean hasNext() {
						if (sel == null || !sel.hasNext()) {
							while (!Thread.currentThread().isInterrupted()) {
								if (it.hasNext()) {
									sel = selector.invoke(it.next()).iterator();
									if (sel.hasNext()) {
										return true;
									}
								} else {
									break;
								}
							}
							return false;
						}
						return true;
					}

					@Override
					public U next() {
						if (hasNext()) {
							return sel.next();
						}
						throw new NoSuchElementException();
					}

					@Override
					public void remove() {
						if (sel == null) {
							throw new IllegalStateException();
						}
						sel.remove();
					}

				};
			}
		};
	}
	/**
	 * Returns an iterable which ensures the source iterable is
	 * only traversed once and clients may take values from each other,
	 * e.g., they share the same iterator.
	 * @param <T> the source element type
	 * @param source the source iterable
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> share(
			@Nonnull final Iterable<T> source) {
		return new Iterable<T>() {
			Iterator<T> it;
			@Override
			public Iterator<T> iterator() {
				if (it == null) {
					it = source.iterator();
				}
				return it;
			}
		};
	}
	/**
	 * Shares the source sequence within the specified
	 * selector function where each iterator can fetch
	 * the next element from the source.
	 * @param <T> the source element type
	 * @param <U> the result element type
	 * @param source the source sequence
	 * @param selector the selector function
	 * @return the new iterable
	 * @since 0.97
	 * TODO Builder
	 */
	@Nonnull
	public static <T, U> Iterable<U> share(
			@Nonnull final Iterable<T> source,
			@Nonnull final Func1<? super Iterable<T>, ? extends Iterable<U>> selector
			) {
		return new Iterable<U>() {
			@Override
			public Iterator<U> iterator() {
				return selector.invoke(share(source)).iterator();
			}
		};
	}
	/**
	 * Creates an iterable which returns only a single element.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param <T> the element type
	 * @param value the value to return
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> singleton(final T value) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					/** Return the only element? */
					boolean first = true;
					@Override
					public boolean hasNext() {
						return first;
					}

					@Override
					public T next() {
						if (first) {
							first = false;
							return value;
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
	 * Immediately returns the number of elements in {@code iterable}.
	 * @param iterable the input sequence
	 * @return the number of elements in the sequence
	 */
	public static int size(@Nonnull Iterable<?> iterable) {
		return first(count(iterable));
	}
	/**
	 * Returns an iterable which skips the last <code>num</code> elements from the
	 * source iterable.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param <T> the element type
	 * @param source the source iterable
	 * @param num the number of elements to skip at the end
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> skipLast(
			@Nonnull final Iterable<? extends T> source,
			final int num) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					/** The source iterator. */
					final Iterator<? extends T> it = source.iterator();
					/** The temporary buffer. */
					final CircularBuffer<Option<? extends T>> buffer = new CircularBuffer<Option<? extends T>>(num);
					@Override
					public boolean hasNext() {
						try {
							while (buffer.size() < num && it.hasNext()) {
								buffer.add(Option.some(it.next()));
							}
						} catch (Throwable t) {
							buffer.add(Option.<T>error(t));
						} finally {
							Closeables.closeSilently(it);
						}
						return buffer.size() == num && it.hasNext();
					}

					@Override
					public T next() {
						if (hasNext()) {
							return buffer.take().value();
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
	 * Returns an iterable which prefixes the source iterable values
	 * by a constant.
	 * It is equivalent to <code>concat(singleton(value), source)</code>.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method for the first element, and might
	 * throw for subsequent elements, depending on the source iterable.</p>
	 * @param <T> the element type
	 * @param source the source iterable
	 * @param value the value to prefix
	 * @return the new iterable.
	 */
	@Nonnull
	public static <T> Iterable<T> startWith(
			@Nonnull Iterable<? extends T> source,
			@Nonnull final T... value) {
		return concat(Arrays.asList(value), source);
	}
	/**
	 * Sum the source of Integer values and return it as a single element.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param source the source
	 * @return the new iterable
	 */
	@Nonnull
	public static Iterable<BigDecimal> sumBigDecimal(
			@Nonnull Iterable<BigDecimal> source) {
		return aggregate(source,
			Functions.sumBigDecimal(), Functions.<BigDecimal, Integer>identityFirst()
		);
	}
	/**
	 * Sum the source of Integer values and return it as a single element.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param source the source
	 * @return the new iterable
	 */
	@Nonnull
	public static Iterable<BigInteger> sumBigInteger(
			@Nonnull Iterable<BigInteger> source) {
		return aggregate(source,
			Functions.sumBigInteger(), Functions.<BigInteger, Integer>identityFirst()
		);
	}
	/**
	 * Sum the source of Double values and returns it as a single element.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param source the source
	 * @return the new iterable
	 */
	@Nonnull
	public static Iterable<Double> sumDouble(
			@Nonnull Iterable<Double> source) {
		return aggregate(source,
			Functions.sumDouble(), Functions.<Double, Integer>identityFirst()
		);
	}
	/**
	 * Sum the source of Float values and returns it as a single element.
	 * @param source the source
	 * @return the new iterable
	 */
	@Nonnull
	public static Iterable<Float> sumFloat(
			@Nonnull Iterable<Float> source) {
		return aggregate(source,
			Functions.sumFloat(), Functions.<Float, Integer>identityFirst()
		);
	}
	/**
	 * Sum the source of Integer values and returns it as a single element.
	 * @param source the source
	 * @return the new iterable
	 */
	@Nonnull
	public static Iterable<Integer> sumInt(
			@Nonnull Iterable<Integer> source) {
		return aggregate(source,
			Functions.sumInteger(), Functions.<Integer, Integer>identityFirst()
		);
	}
	/**
	 * Sum the source of Long values and returns it as a single element.
	 * @param source the source
	 * @return the new iterable
	 */
	@Nonnull
	public static Iterable<Long> sumLong(
			@Nonnull Iterable<Long> source) {
		return aggregate(source,
				Functions.sumLong(), Functions.<Long, Integer>identityFirst()
			);
	}
	/**
	 * Returns an iterable, which will query the selector for a key, then
	 * queries the map for an Iterable. The returned iterator will
	 * then traverse that Iterable. If the map does not contain an
	 * element, az empty iterable is used.
	 * @param <T> the key type
	 * @param <U> the output type
	 * @param selector the key selector
	 * @param options the available options in
	 * @return the new iterable
	 */
	@Nonnull
	public static <T, U> Iterable<U> switchCase(
			@Nonnull final Func0<T> selector,
			@Nonnull final Map<T, Iterable<U>> options) {
		return new Iterable<U>() {
			@Override
			public Iterator<U> iterator() {
				Iterable<U> it = options.get(selector.invoke());
				return it != null ? it.iterator() : Interactive.<U>empty().iterator();
			}
		};

	}
	/**
	 * Returns the iterable which returns the first <code>num</code> element.
	 * from the source iterable.
	 * <p>The returned iterator forwards all <code>remove()</code> calls
	 * to the source.</p>
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @param num the number of items to take
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> take(
			@Nonnull final Iterable<? extends T> source,
			final int num) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					/** The counter. */
					int count;
					/** The source iterator. */
					final Iterator<? extends T> it = source.iterator();
					@Override
					public boolean hasNext() {
						return count < num && it.hasNext();
					}

					@Override
					public T next() {
						if (hasNext()) {
							count++;
							return it.next();
						}
						throw new NoSuchElementException();
					}

					@Override
					public void remove() {
						it.remove();
					}
				};
			}
		};
	}
	/**
	 * Returns an iterable which takes only the last <code>num</code> elements from the
	 * source iterable.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param <T> the element type
	 * @param source the source iterable
	 * @param num the number of elements to skip at the end
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> takeLast(
			@Nonnull final Iterable<? extends T> source,
			final int num) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					/** The source iterator. */
					final Iterator<? extends T> it = source.iterator();
					/** The temporary buffer. */
					final CircularBuffer<Option<? extends T>> buffer = new CircularBuffer<Option<? extends T>>(num);
					@Override
					public boolean hasNext() {
						try {
							while (it.hasNext()) {
								buffer.add(Option.some(it.next()));
							}
						} catch (Throwable t) {
							buffer.add(Option.<T>error(t));
						} finally {
							Closeables.closeSilently(it);
						}
						return !buffer.isEmpty();
					}

					@Override
					public T next() {
						if (hasNext()) {
							return buffer.take().value();
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
	 * Returns an iterator which will throw the given
	 * <code>Throwable</code> exception when the client invokes
	 * <code>next()</code> the first time. Any subsequent
	 * <code>next()</code> call will simply throw a <code>NoSuchElementException</code>.
	 * Calling <code>remove()</code> will always throw a <code>IllegalStateException</code>.
	 * If the given Throwable instance extends a <code>RuntimeException</code>, it is throws
	 * as is, but when the throwable is a checked exception, it is wrapped
	 * into a <code>RuntimeException</code>.
	 * FIXME not sure about next() semantics
	 * @param <T> the element type, irrelevant
	 * @param t the exception to throw
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> throwException(
			@Nonnull final Throwable t) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					/** First call? */
					boolean first = true;
					@Override
					public boolean hasNext() {
						return first;
					}

					@Override
					public T next() {
						if (first) {
							first = false;
							if (t instanceof RuntimeException) {
								throw (RuntimeException)t;
							}
							throw new RuntimeException(t);
						}
						throw new NoSuchElementException();
					}

					@Override
					public void remove() {
						throw new IllegalStateException();
					}

				};
			}
		};
	}
	/**
	 * Convert the source Iterable into the Enumerable semantics.
	 * @param <T> the source element type
	 * @param e the iterable
	 * @return the new enumerable
	 */
	@Nonnull
	public static <T> Enumerable<T> toEnumerable(
			@Nonnull final Iterable<? extends T> e) {
		return new Enumerable<T>() {
			@Override
			@Nonnull
			public Enumerator<T> enumerator() {
				return toEnumerator(e.iterator());
			}
		};
	}
	/**
	 * Convert the given iterator to the Enumerator semantics.
	 * @param <T> the element type
	 * @param it the source iterator
	 * @return the new enumerator
	 */
	@Nonnull
	public static <T> Enumerator<T> toEnumerator(
			@Nonnull final Iterator<? extends T> it) {
		return new Enumerator<T>() {
			/** The current value. */
			T value;
			/** The current value is set. */
			boolean hasValue;
			@Override
			public T current() {
				if (hasValue) {
					return value;
				}
				throw new NoSuchElementException();
			}
			@Override
			public boolean next() {
				if (it.hasNext()) {
					value = it.next();
					hasValue = true;
					return false;
				}
				hasValue = false;
				return false;
			}

		};
	}
	/**
	 * Convert the source enumerable into the Iterable semantics.
	 * @param <T> the source element type
	 * @param e the enumerable
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> toIterable(
			@Nonnull final Enumerable<? extends T> e) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return toIterator(e.enumerator());
			}
		};
	}
	/**
	 * Convert the given enumerator to the Iterator semantics.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param <T> the element type
	 * @param en the source enumerator
	 * @return the new iterator
	 */
	@Nonnull
	public static <T> Iterator<T> toIterator(
			@Nonnull final Enumerator<? extends T> en) {
		return new Iterator<T>() {
			/** The peek-ahead buffer. */
			final SingleContainer<Option<? extends T>> peek = new SingleContainer<Option<? extends T>>();
			/** Completion indicator. */
			boolean done;
			@Override
			public boolean hasNext() {
				if (!done && peek.isEmpty()) {
					try {
						if (en.next()) {
							peek.add(Option.some(en.current()));
						} else {
							done = true;
						}
					} catch (Throwable t) {
						done = true;
						peek.add(Option.<T>error(t));
					}
				}
				return peek.isEmpty();
			}
			@Override
			public T next() {
				if (hasNext()) {
					return peek.take().value();
				}
				throw new NoSuchElementException();
			}
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
	/**
	 * Returns an iterable which is associated with a closeable handler.
	 * Once the source iterable is completed, it invokes the <code>Closeable.close()</code> on the handler.
	 * <p>The returned iterator forwards all <code>remove()</code> calls
	 * to the source.</p>
	 * @param <T> the source element type
	 * @param <U> the closeable type
	 * @param resource the function which returns a resource token
	 * @param usage the function which gives an iterable for a resource token.
	 * @return the new iterable
	 */
	@Nonnull
	public static <T, U extends Closeable> Iterable<T> using(
			@Nonnull final Func0<U> resource,
			@Nonnull final Func1<? super U, Iterable<? extends T>> usage) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				final U c = resource.invoke();
				return new CloseableIterator<T>() {
					/** The iterator. */
					final Iterator<? extends T> it = usage.invoke(c).iterator();
					/** Run once the it has no more elements. */
					boolean once = true;
					@Override
					public boolean hasNext() {
						if (it.hasNext()) {
							return true;
						}
						if (once) {
							once = false;
							Closeables.closeSilently(c);
						}
						return false;
					}

					@Override
					public T next() {
						if (hasNext()) {
							return it.next();
						}
						throw new NoSuchElementException();
					}

					@Override
					public void remove() {
						it.remove();
					}
					@Override
					public void close() throws IOException {
						if (once) {
							once = false;
							Closeables.closeSilently(c);
						}
					}

				};
			}
		};
	}
	/**
	 * Creates an iterable which filters the source iterable with the
	 * given predicate factory function. The predicate returned by the factory receives an index
	 * telling how many elements were processed thus far.
	 * Use this construct if you want to use some memorizing predicate function (e.g., filter by subsequent distinct, filter by first occurrences only)
	 * which need to be invoked per iterator() basis.
	 * <p>The returned iterator forwards all <code>remove()</code> calls
	 * to the source.</p>
	 * @param <T> the element type
	 * @param source the source iterable
	 * @param predicateFactory the predicate factory which should return a new predicate function for each iterator.
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> where(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Func0<? extends Func2<? super Integer, ? super T, Boolean>> predicateFactory) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				final Func2<? super Integer, ? super T, Boolean> predicate = predicateFactory.invoke();
				final Iterator<? extends T> it = source.iterator();
				return new Iterator<T>() {
					/** The current element count. */
					int count;
					/** The temporary store for peeked elements. */
					final SingleContainer<T> peek = new SingleContainer<T>();
					@Override
					public boolean hasNext() {
						if (peek.isEmpty()) {
							while (it.hasNext()) {
								T value = it.next();
								if (predicate.invoke(count, value)) {
									peek.add(value);
									count++;
									return true;
								}
								count++;
							}
							return false;
						}
						return true;
					}

					@Override
					public T next() {
						if (hasNext()) {
							return peek.take();
						}
						throw new NoSuchElementException();
					}

					@Override
					public void remove() {
						it.remove();
					}

				};
			}
		};
	}
	/**
	 * Creates an iterable which filters the source iterable with the
	 * given predicate function. The predicate receives the value and
	 * must return a boolean whether to accept that entry.
	 * @param <T> the element type
	 * @param source the source iterable
	 * @param predicate the predicate function
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> where(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Func1<? super T, Boolean> predicate) {
		return where(source, Functions.constant0(new Func2<Integer, T, Boolean>() {
			@Override
			public Boolean invoke(Integer param1, T param2) {
				return predicate.invoke(param2);
			}
		}));
	}
	/**
	 * Creates an iterable which filters the source iterable with the
	 * given predicate factory function. The predicate returned by the factory receives an index
	 * telling how many elements were processed thus far.
	 * @param <T> the element type
	 * @param source the source iterable
	 * @param predicate the predicate
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Iterable<T> where(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Func2<? super Integer, ? super T, Boolean> predicate) {
		return where(source, Functions.constant0(predicate));
	}
	/**
	 * Pairs each element from both iterable sources and
	 * combines them into a new value by using the <code>combiner</code>
	 * function.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param <T> the left source type
	 * @param <U> the right source type
	 * @param <V> the result type
	 * @param left the left source
	 * @param right the right source
	 * @param combiner the combiner function
	 * @return the new iterable
	 */
	@Nonnull
	public static <T, U, V> Iterable<V> zip(
			@Nonnull final Iterable<? extends T> left,
			@Nonnull final Iterable<? extends U> right,
			@Nonnull final Func2<? super T, ? super U, ? extends V> combiner) {
		return new Iterable<V>() {
			@Override
			public Iterator<V> iterator() {
				return new Iterator<V>() {
					/** The left iterator. */
					final Iterator<? extends T> ts = left.iterator();
					/** The right iterator. */
					final Iterator<? extends U> us = right.iterator();
					/** The peek-ahead container. */
					final SingleContainer<Option<? extends V>> peek = new SingleContainer<Option<? extends V>>();
					@Override
					public boolean hasNext() {
						if (peek.isEmpty()) {
							try {
								if (ts.hasNext() && us.hasNext()) {
									peek.add(Option.some(combiner.invoke(ts.next(), us.next())));
								}
							} catch (Throwable t) {
								peek.add(Option.<V>error(t));
							}
						}
						return !peek.isEmpty();
					}
					@Override
					public V next() {
						if (hasNext()) {
							return peek.take().value();
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
	 * Returns the first element from the iterable sequence or
	 * throws a NoSuchElementException.
	 * @param <T> the value type
	 * @param src the source sequence
	 * @return the first element
	 * @since 0.96
	 */
	public static <T> T first(@Nonnull Iterable<? extends T> src) {
		return src.iterator().next();
	}
	/**
	 * Takes the input elements and returns an iterable which
	 * traverses the array. The supplied array is
	 * shared by the iterator. Any changes to the array will be
	 * reflected by the iterator
	 * <p>The resulting {@code Iterable} does not support {@code remove()}.</p>
	 * @param <T> the element type
	 * @param ts the input array
	 * @return the iterable for the array
	 * @since 0.96
	 */
	@Nonnull 
	public static <T> Iterable<T> toIterable(@Nonnull final T... ts) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					/** The current location. */
					int index;
					/** The lenght. */
					final int size = ts.length;
					@Override
					public boolean hasNext() {
						return index < size;
					}
					@Override
					public T next() {
						if (hasNext()) {
							return ts[index++];
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
	 * Takes the input elements and returns an iterable which
	 * traverses the array between the two indexes. The supplied array is
	 * shared by the iterator. Any changes to the array will be
	 * reflected by the iterator.
	 * <p>The resulting {@code Iterable} does not support {@code remove()}.</p>
	 * @param <T> the element type
	 * @param from the starting index inclusive
	 * @param to the end index exclusive
	 * @param ts the input array
	 * @return the iterable for the array
	 * @since 0.96
	 */
	@Nonnull 
	public static <T> Iterable<T> toIterablePart(
			final int from, 
			final int to, 
			@Nonnull final T... ts) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					/** The current location. */
					int index = from;
					/** The lenght. */
					final int size = ts.length;
					@Override
					public boolean hasNext() {
						return index < size && index < to;
					}
					@Override
					public T next() {
						if (hasNext()) {
							return ts[index++];
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
	 * Returns a pair of the maximum argument and value from the given sequence.
	 * @param <T> the element type
	 * @param <V> the value type
	 * @param source the source sequence of Ts
	 * @param valueSelector the selector to extract the value from T
	 * @param valueComparator the comparator to compare two values
	 * @return the first pair of max argument and value or null if the source sequence was empty
	 * @since 0.96
	 */
	public static <T, V> Pair<T, V> argAndMax(
			@Nonnull Iterable<? extends T> source,
			@Nonnull Func1<? super T, ? extends V> valueSelector,
			@Nonnull Comparator<? super V> valueComparator) {
		T arg = null;
		V max = null;
		boolean hasElement = false;
		Iterator<? extends T> it = source.iterator();
		try {
			while (it.hasNext()) {
				T item = it.next();
				V itemValue = valueSelector.invoke(item);
				if (!hasElement || valueComparator.compare(max, itemValue) < 0) {
					arg = item;
					max = itemValue;
				}
				hasElement = true;
			}
			if (hasElement) {
				return Pair.of(arg, max);
			}
		} finally {
			Closeables.closeSilently(it);
		}
		return null;
	}
	/**
	 * Returns a pair of the maximum argument and value from the given sequence.
	 * @param <T> the element type of the sequence
	 * @param <V> the value type for the comparison, must be self comparable
	 * @param source the source sequence
	 * @param valueSelector the value selector function
	 * @return the pair of the first maximum element and value, null if the sequence was empty
	 * @since 0.96
	 */
	public static <T, V extends Comparable<? super V>> Pair<T, V> argAndMax(
			@Nonnull Iterable<? extends T> source,
			@Nonnull Func1<? super T, ? extends V> valueSelector) {
		return argAndMax(source, valueSelector, Functions.<V>comparator());
	}
	/**
	 * Returns a pair of the maximum argument and value from the given sequence.
	 * @param <T> the element type of the sequence
	 * @param <V> the value type for the comparison, must be self comparable
	 * @param source the source sequence
	 * @param valueSelector the value selector function
	 * @return the pair of the first maximum element and value, null if the sequence was empty
	 * @since 0.96
	 */
	public static <T, V extends Comparable<? super V>> Pair<T, V> argAndMin(
			@Nonnull Iterable<? extends T> source,
			@Nonnull Func1<? super T, ? extends V> valueSelector) {
		return argAndMin(source, valueSelector, Functions.<V>comparator());
	}
	/**
	 * Returns a pair of the minimum argument and value from the given sequence.
	 * @param <T> the element type
	 * @param <V> the value type
	 * @param source the source sequence of Ts
	 * @param valueSelector the selector to extract the value from T
	 * @param valueComparator the comparator to compare two values
	 * @return the first pair of min argument and value or null if the source sequence was empty
	 * @since 0.96
	 */
	public static <T, V> Pair<T, V> argAndMin(
			@Nonnull Iterable<? extends T> source,
			@Nonnull Func1<? super T, ? extends V> valueSelector,
			@Nonnull final Comparator<? super V> valueComparator) {
		return argAndMax(source, valueSelector, new Comparator<V>() {
			@Override
			public int compare(V o1, V o2) {
				return valueComparator.compare(o2, o1);
			}
		});
	}
	/**
	 * Creates an iterable sequence which returns the given value indefinitely.
	 * <p>(E.g., having the hasNext() always return true and the next() always return the value.</p>
	 * <p>The returned iterable does not support the {@code remove()} method.</p>
	 * @param <T> the value type
	 * @param value the value to repeat
	 * @return the iterable
	 * @since 0.96
	 */
	public static <T> Iterable<T> repeat(final T value) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					@Override
					public boolean hasNext() {
						return true;
					}
					@Override
					public T next() {
						return value;
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
	 * Returns an iterable which repeats the given single value the specified number of times.
	 * <p>The returned iterable does not support the {@code remove()} method.</p>
	 * @param <T> the value type
	 * @param value the value to repeat
	 * @param count the repeat amount
	 * @return the iterable
	 * @since 0.96
	 */
	public static <T> Iterable<T> repeat(final T value, final int count) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					int index;
					@Override
					public boolean hasNext() {
						return index < count;
					}
					@Override
					public T next() {
						if (hasNext()) {
							index++;
							return value;
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
	 * Creates an iterable sequence which returns all elements from source
	 * followed by the supplied value as last.
	 * <p>The returned iterable forwards all {@code remove()}
	 * methods to the source iterable, except the last element where it
	 * throws UnsupportedOperationException.</p>
	 * @param <T> the element type
	 * @param source the source sequence
	 * @param value the value to append
	 * @return the new iterable
	 * @since 0.96
	 */
	@Nonnull
	public static <T> Iterable<T> endWith(
			@Nonnull final Iterable<? extends T> source, T value) {
		return concat(source, singleton(value));
	}
	/**
	 * Computes and signals the sum of the values of the Integer source by using
	 * a double intermediate representation.
	 * The source may not send nulls. An empty source produces an empty sum
	 * @param source the source of integers to aggregate.
	 * @return the observable for the sum value
	 * @since 0.96
	 */
	@Nonnull
	public static Iterable<Double> sumIntAsDouble(
			@Nonnull final Iterable<Integer> source) {
		return aggregate(source,
			new Func2<Double, Integer, Double>() {
				@Override
				public Double invoke(Double param1, Integer param2) {
					return param1 + param2;
				}
			},
			Functions.<Double, Integer>identityFirst()
		);
	}
	/**
	 * Computes and signals the sum of the values of the Long sourceby using
	 * a double intermediate representation.
	 * The source may not send nulls.
	 * @param source the source of longs to aggregate.
	 * @return the observable for the sum value
	 * @since 0.96
	 */
	@Nonnull
	public static Iterable<Double> sumLongAsDouble(
			@Nonnull final Iterable<Long> source) {
		return aggregate(source,
				new Func2<Double, Long, Double>() {
					@Override
					public Double invoke(Double param1, Long param2) {
						return param1 + param2;
					}
				},
				Functions.<Double, Integer>identityFirst()
			);
	}
	/**
	 * Creates an iterable which traverses the source iterable,
	 * and based on the key selector, groups values of T into GroupedIterables,
	 * which can be iterated over later on.
	 * The equivalence of the keys are determined via reference
	 * equality and <code>equals()</code> equality.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param <T> the source element type
	 * @param <V> the result group keys
	 * @param source the source of Ts
	 * @param keySelector the key selector
	 * @return the new iterable
	 * @since 0.96.1
	 */
	@Nonnull
	public static <T, V> Iterable<GroupedIterable<V, T>> groupBy(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends V> keySelector
			) {
		return groupBy(source, keySelector, Functions.<T>identity());
	}
	/**
	 * Creates an iterable which returns two subsequent items from the source
	 * iterable as pairs of values. If the {@code source} contains zero or one elements, this
	 * iterable will be empty.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param <T> the element type
	 * @param source the source iterable
	 * @return the new iterable
	 * @since 0.96.1
	 */
	public static <T> Iterable<Pair<T, T>> subsequent(@Nonnull final Iterable<? extends T> source) {
		return new Iterable<Pair<T, T>>() {
			@Override
			public Iterator<Pair<T, T>> iterator() {
				final Iterator<? extends T> it = source.iterator();
				if (!it.hasNext()) {
					return Interactive.<Pair<T, T>>empty().iterator();
				}
				final T flast = it.next();
				return new Iterator<Pair<T, T>>() {
					/** The last source value. */
					T last = flast;
					@Override
					public boolean hasNext() {
						return it.hasNext();
					}
					@Override
					public Pair<T, T> next() {
						if (hasNext()) {
							T curr = it.next();
							Pair<T, T> ret = Pair.of(last, curr);
							last = curr;
							return ret;
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
	 * Creates an iterable which returns {@code count} subsequent items from the source
	 * iterable as sequence of values.
	 * If the {@code source} contains less than {@code count} elements, this
	 * iterable will be empty.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param <T> the element type
	 * @param source the source iterable
	 * @param count the element count
	 * @return the new iterable
	 * @since 0.96.1
	 */
	public static <T> Iterable<Iterable<T>> subsequent(
			@Nonnull final Iterable<? extends T> source,
			final int count) {
		if (count <= 0) {
			throw new IllegalArgumentException("Count must be > 0");
		}
		if (count == 1) {
			return select(source, new Func1<T, Iterable<T>>() {
				@Override
				public Iterable<T> invoke(T param1) {
					return singleton(param1);
				}
			});
		}
		return new Iterable<Iterable<T>>() {
			@Override
			public Iterator<Iterable<T>> iterator() {
				// get the first count-1 elements
				final LinkedList<T> ll = new LinkedList<T>();
				final Iterator<? extends T> it = source.iterator();
				int cnt = 0;
				try {
					while (it.hasNext() && cnt < count - 1) {
						ll.add(it.next());
						cnt++;
					}
				} finally {
					Closeables.closeSilently(it);
				}
				if (cnt < count - 1) {
					return Interactive.<Iterable<T>>empty().iterator();
				}
				return new Iterator<Iterable<T>>() {
					@Override
					public boolean hasNext() {
						return it.hasNext();
					}
					@Override
					public Iterable<T> next() {
						if (hasNext()) {
							ll.add(it.next());
							ll.removeFirst();
							return new ArrayList<T>(ll);
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
	 * Creates a new iterable sequence by wrapping the given function to
	 * provide the iterator.
	 * @param <T> the element type
	 * @param <U> the iterator type
	 * @param body the body function returning an iterator
	 * @return the iterable sequence
	 * @since 0.97
	 */
	public static <T, U extends Iterator<T>> Iterable<T> newIterable(
			@Nonnull final Func0<U> body) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return body.invoke();
			}
		};
	}
	/**
	 * A functional way of creating a new iterator from the supplied 
	 * hasNext and next callbacks.
	 * <p>The returned iterator throws a <code>UnsupportedOperationException</code>
	 * in its remove method.</p>
	 * @param <T> the element type
	 * @param hasNext function that returns true if more elements are available.
	 * @param next function that returns the next element
	 * @return the created iterator
	 * @since 0.97
	 */
	public static <T> Iterator<T> newIterator(
			@Nonnull final Func0<Boolean> hasNext, 
			@Nonnull final Func0<? extends T> next) {
		return new Iterator<T>() {
			@Override
			public boolean hasNext() {
				return hasNext.invoke();
			}
			@Override
			public T next() {
				return next.invoke();
			}
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
	/**
	 * Constructs an iterator instance by wrapping the supplied functions
	 * into the same-named iterator methods.
	 * @param <T> the element type
	 * @param hasNext function that returns true if more elements are available.
	 * @param next function that returns the next element
	 * @param remove function to remove the current element
	 * @return the created iterator
	 * @since 0.97
	 */
	public static <T> Iterator<T> newIterator(
			@Nonnull final Func0<Boolean> hasNext, 
			@Nonnull final Func0<? extends T> next,
			@Nonnull final Action0 remove
			) {
		return new Iterator<T>() {
			@Override
			public boolean hasNext() {
				return hasNext();
			}
			@Override
			public T next() {
				return next.invoke();
			}
			@Override
			public void remove() {
				remove.invoke();
			}
		};
	}
	/**
	 * Wraps the given source sequence into a CloseableIterable instance
	 * where the inner CloseableIterator.close() method calls the supplied action.
	 * @param <T> the element type
	 * @param src the source sequence
	 * @param close the close action.
	 * @return the new closeable iterable
	 * @since 0.97
	 */
	@Nonnull
	public static <T> CloseableIterable<T> newCloseableIterable(
			@Nonnull final Iterable<? extends T> src,
			@Nonnull final Action1<? super Iterator<? extends T>> close
	) {
		return new CloseableIterable<T>() {
			@Override
			public CloseableIterator<T> iterator() {
				return newCloseableIterator(src.iterator(), close);
			}
		};
	}
	/**
	 * Wraps the given source sequence into a CloseableIterable instance
	 * where the inner CloseableIterator.close() method calls the supplied action.
	 * @param <T> the element type
	 * @param src the source sequence
	 * @param close the close action.
	 * @return the new closeable iterable
	 * @since 0.97
	 */
	@Nonnull
	public static <T> CloseableIterable<T> newCloseableIterable(
			@Nonnull final Iterable<? extends T> src,
			@Nonnull final Action0 close
	) {
		return new CloseableIterable<T>() {
			@Override
			public CloseableIterator<T> iterator() {
				return newCloseableIterator(src.iterator(), close);
			}
		};
	}
	/**
	 * Wraps the given source sequence into a CloseableIterable instance
	 * where the inner CloseableIterator.close() method calls the supplied closeable object.
	 * @param <T> the element type
	 * @param src the source sequence
	 * @param close the closeable object.
	 * @return the new closeable iterable
	 * @since 0.97
	 */
	@Nonnull
	public static <T> CloseableIterable<T> newCloseableIterable(
			@Nonnull final Iterable<? extends T> src,
			@Nonnull final Closeable close
	) {
		return new CloseableIterable<T>() {
			@Override
			public CloseableIterator<T> iterator() {
				return newCloseableIterator(src.iterator(), close);
			}
		};
	}
	/**
	 * Wraps the supplied iterator into a CloseableIterator which calls the supplied
	 * close action.
	 * @param <T> the element type
	 * @param src the source iterator
	 * @param close the close action
	 * @return the new closeable iterator
	 * @since 0.97
	 */
	@Nonnull
	public static <T> CloseableIterator<T> newCloseableIterator(
			@Nonnull final Iterator<? extends T> src,
			@Nonnull final Action0 close
	) {
		return new CloseableIterator<T>() {
			@Override
			public boolean hasNext() {
				return src.hasNext();
			}

			@Override
			public T next() {
				return src.next();
			}

			@Override
			public void remove() {
				src.remove();
			}

			@Override
			public void close() throws IOException {
				close.invoke();
			}
			
		};
	}
	/**
	 * Wraps the supplied iterator into a CloseableIterator which calls the supplied
	 * closeable instance.
	 * @param <T> the element type
	 * @param src the source iterator
	 * @param close the closeable instance
	 * @return the new closeable iterator
	 * @since 0.97
	 */
	@Nonnull
	public static <T> CloseableIterator<T> newCloseableIterator(
			@Nonnull final Iterator<? extends T> src,
			@Nonnull final Closeable close
	) {
		return new CloseableIterator<T>() {
			@Override
			public boolean hasNext() {
				return src.hasNext();
			}

			@Override
			public T next() {
				return src.next();
			}

			@Override
			public void remove() {
				src.remove();
			}

			@Override
			public void close() throws IOException {
				close.close();
			}
			
		};
	}
	/**
	 * Wraps the supplied iterator into a CloseableIterator which calls the supplied
	 * close action with the given source iterator object.
	 * @param <T> the element type
	 * @param src the source iterator
	 * @param close the close action
	 * @return the new closeable iterator
	 */
	@Nonnull
	public static <T> CloseableIterator<T> newCloseableIterator(
			@Nonnull final Iterator<? extends T> src,
			@Nonnull final Action1<? super Iterator<? extends T>> close
	) {
		return new CloseableIterator<T>() {
			@Override
			public boolean hasNext() {
				return src.hasNext();
			}

			@Override
			public T next() {
				return src.next();
			}

			@Override
			public void remove() {
				src.remove();
			}

			@Override
			public void close() throws IOException {
				close.invoke(src);
			}
			
		};
	}
	
	// TODO IBuffer publish(Iterable)

	// TODO memoize(Iterable)
	
	// TODO memoize(Iterable, Func<Iterable, Iterable>)

	// TODO memoize(Iterable, int, Func<Iterable, Iterable>)
	
	// TODO throwException(Func<Throwable>)
	
	// TODO catchException(Iterable<Iterable>>)
	
	// TODO catchException(Iterable, Iterable)

	// TODO retry(Iterable)
	
	// TODO resumeOnError(Iterable...)

	// TODO ifThen(Func<bool>, Iterable)

	// TODO ifThen(Func<bool>, Iterable, Iterable)

	// TODO whileDo(Func<bool>, Iterable)

	// TODO switchCase(Func<T>, Map<T, Iterable<U>>, Iterable<U>)

	// TODO selectMany(Iterable<T>, Iterable<U>)
	
	// TODO forEach(Iterable<T>, Action<T>)

	// TODO forEach(Iterable<T>, Action<T, Integer>)

	// TODO invoke(Iterable<T>, Observer<T>)
	
	// TODO buffer(Iterable, int, int)
	
	// TODO ignoreValues(Iterable)

	// TODO distinct(Iterable, Func2<T, T, Boolean>)
	
	// TODO distinct(Iterable, Func<T, U>, Func<U, U, boolean)
	
	// TODO distinctNext(Iterable, Func2<T, T, Boolean>)
	
	// TODO distinctNext(Iterable, Func<T, U>, Func<U, U, boolean)
	
	// TODO expand(Iterable<T>, Func<T, Iterable<T>>)
	
	// TODO repeat(Iterable)
	
	// TODO repeat(Iterable, count)
	
	/** Utility class. */
	private Interactive() {
		// utility class
	}
}
