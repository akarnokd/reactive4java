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

import hu.akarnokd.reactive4java.base.Action0;
import hu.akarnokd.reactive4java.base.Action1;
import hu.akarnokd.reactive4java.base.CircularBuffer;
import hu.akarnokd.reactive4java.base.CloseableIterable;
import hu.akarnokd.reactive4java.base.CloseableIterator;
import hu.akarnokd.reactive4java.base.Closeables;
import hu.akarnokd.reactive4java.base.Func0;
import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Func2;
import hu.akarnokd.reactive4java.base.Functions;
import hu.akarnokd.reactive4java.base.Option;
import hu.akarnokd.reactive4java.base.Pair;
import hu.akarnokd.reactive4java.base.Scheduler;
import hu.akarnokd.reactive4java.base.SingleContainer;
import hu.akarnokd.reactive4java.base.TooManyElementsException;
import hu.akarnokd.reactive4java.interactive.Interactive;
import hu.akarnokd.reactive4java.util.DefaultScheduler;
import hu.akarnokd.reactive4java.util.SingleLaneExecutor;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

/**
 * Utility class with operators and helper methods for reactive programming with <code>Observable</code>s and <code>Observer</code>s.
 * Guidances were taken from
 * <ul>
 * <li><a href='http://theburningmonk.com/tags/rx/'>http://theburningmonk.com/tags/rx/</a></li>
 * <li><a href='http://blogs.bartdesmet.net/blogs/bart/archive/2010/01/01/the-essence-of-linq-minlinq.aspx'>http://blogs.bartdesmet.net/blogs/bart/archive/2010/01/01/the-essence-of-linq-minlinq.aspx</a></li>
 * <li><a href='http://reactive4java.googlecode.com/svn/trunk/Reactive4Java/docs/javadoc/hu/akarnokd/reactive4java/reactive/Reactive.html'>http://reactive4java.googlecode.com/svn/trunk/Reactive4Java/docs/javadoc/hu/akarnokd/reactive4java/reactive/Reactive.html</a></li>
 * <li><a href='http://rxwiki.wikidot.com/101samples#toc3'>http://rxwiki.wikidot.com/101samples#toc3</a></li>
 * <li><a href='http://channel9.msdn.com/Tags/rx'>http://channel9.msdn.com/Tags/rx</a></li>
 * </ul>
 *
 * @author akarnokd, 2011.01.26
 * @see hu.akarnokd.reactive4java.interactive.Interactive
 */
public final class Reactive {

	/**
	 * A variant of the registering observable which stores a group key.
	 * @author akarnokd, 2011.01.29.
	 * @param <Key> the type of the key
	 * @param <Value> the value type
	 */
	static class GroupedRegisteringObservable<Key, Value> extends DefaultObservable<Value> implements GroupedObservable<Key, Value> {
		/** The group key. */
		private final Key key;
		/**
		 * Constructor.
		 * @param key the group key
		 */
		public GroupedRegisteringObservable(Key key) {
			this.key = key;
		}
		@Override
		public Key key() {
			return key;
		}
	}
	/** The diagnostic states of the current runnable. */
	public enum ObserverState { OBSERVER_ERROR, OBSERVER_FINISHED, OBSERVER_RUNNING }
	/** The common observable pool where the Observer methods get invoked by default. */
	static final AtomicReference<Scheduler> DEFAULT_SCHEDULER = new AtomicReference<Scheduler>(new DefaultScheduler());
	/**
	 * Returns an observable which provides a TimeInterval of Ts which
	 * records the elapsed time between successive elements.
	 * The time interval is evaluated using the System.nanoTime() differences
	 * as nanoseconds.
	 * The first element contains the time elapsed since the registration occurred.
	 * @param <T> the time source
	 * @param source the source of Ts
	 * @return the new observable
	 */
	@Nonnull
	public static <T> Observable<TimeInterval<T>> addTimeInterval(
			@Nonnull final Observable<? extends T> source) {
		return new Observable<TimeInterval<T>>() {
			@Override
			public Closeable register(final Observer<? super TimeInterval<T>> observer) {
				return source.register(new Observer<T>() {
					long lastTime = System.nanoTime();
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.finish();
					}

					@Override
					public void next(T value) {
						long t2 = System.nanoTime();
						observer.next(TimeInterval.of(value, t2 - lastTime));
						lastTime = t2;
					}

				});
			}
		};
	}
	/**
	 * Wrap the values within a observable to a timestamped value having always
	 * the System.currentTimeMillis() value.
	 * @param <T> the element type
	 * @param source the source which has its elements in a timestamped way.
	 * @return the raw observables of Ts
	 */
	@Nonnull
	public static <T> Observable<Timestamped<T>> addTimestamped(
			@Nonnull Observable<? extends T> source) {
		return select(source, Reactive.<T>wrapTimestamped());
	}
	/**
	 * Apply an accumulator function over the observable source 
	 * and submit the accumulated value to the returned observable at each incoming value.
	 * <p>If the source observable terminates before sending a single value,
	 * the output observable terminates as well. The first incoming value is relayed as-is.</p>
	 * @param <T> the element type
	 * @param source the source observable
	 * @param accumulator the accumulator function where the first parameter is the current accumulated value and the second is the now received value.
	 * @return the observable for the result of the accumulation
	 */
	@Nonnull
	public static <T> Observable<T> aggregate(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func2<? super T, ? super T, ? extends T> accumulator) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** The current aggregation result. */
					T result;
					/** How many items did we get */
					int phase;
					@Override
					public void error(Throwable ex) {
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
		};
	}
	/**
	 * Computes an aggregated value of the source Ts by applying a sum function and applying the divide function when the source
	 * finishes, sending the result to the output.
	 * @param <T> the type of the values
	 * @param <U> the type of the intermediate sum value
	 * @param <V> the type of the final average value
	 * @param source the source of BigDecimals to aggregate.
	 * @param accumulator the function which accumulates the input Ts. The first received T will be accompanied by a null U.
	 * @param divide the function which perform the final division based on the number of elements
	 * @return the observable for the average value
	 */
	@Nonnull
	public static <T, U, V> Observable<V> aggregate(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func2<? super U, ? super T, ? extends U> accumulator,
			@Nonnull final Func2<? super U, ? super Integer, ? extends V> divide) {
		return new Observable<V>() {
			@Override
			public Closeable register(final Observer<? super V> observer) {
				return source.register(new Observer<T>() {
					/** The number of values. */
					int count;
					/** The sum of the values thus far. */
					U temp;
					@Override
					public void error(Throwable ex) {
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
		};
	}
	/**
	 * Computes an aggregated value of the source Ts by applying a 
	 * sum function and applying the divide function when the source
	 * finishes, sending the result to the output.
	 * @param <T> the type of the values
	 * @param <U> the type of the intermediate sum value
	 * @param <V> the type of the final average value
	 * @param source the source of BigDecimals to aggregate.
	 * @param seed the initieal value for the aggregation
	 * @param accumulator the function which accumulates the input Ts. The first received T will be accompanied by a null U.
	 * @param divide the function which perform the final division based on the number of elements
	 * @return the observable for the average value
	 * @since 0.97
	 */
	@Nonnull
	public static <T, U, V> Observable<V> aggregate(
			@Nonnull final Observable<? extends T> source,
			final U seed,
			@Nonnull final Func2<? super U, ? super T, ? extends U> accumulator,
			@Nonnull final Func2<? super U, ? super Integer, ? extends V> divide) {
		return new Observable<V>() {
			@Override
			public Closeable register(final Observer<? super V> observer) {
				return source.register(new Observer<T>() {
					/** The number of values. */
					int count;
					/** The sum of the values thus far. */
					U temp = seed;
					@Override
					public void error(Throwable ex) {
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
		};
	}
	/**
	 * Apply an accumulator function over the observable source and submit the accumulated value to the returned observable.
	 * @param <T> the input element type
	 * @param <U> the output element type
	 * @param source the source observable
	 * @param seed the initial value of the accumulator
	 * @param accumulator the accumulator function where the first parameter is the current accumulated value and the second is the now received value.
	 * @return the observable for the result of the accumulation
	 */
	@Nonnull
	public static <T, U> Observable<U> aggregate(
			@Nonnull final Observable<? extends T> source,
			final U seed,
			@Nonnull final Func2<? super U, ? super T, ? extends U> accumulator) {
		return new Observable<U>() {
			@Override
			public Closeable register(final Observer<? super U> observer) {
				return source.register(new Observer<T>() {
					/** The current aggregation result. */
					U result = seed;
					@Override
					public void error(Throwable ex) {
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
		};
	}
	/**
	 * Signals a single true or false if all elements of the observable match the predicate.
	 * It may return early with a result of false if the predicate simply does not match the current element.
	 * For a true result, it waits for all elements of the source observable.
	 * @param <T> the type of the source data
	 * @param source the source observable
	 * @param predicate the predicate to satisfy
	 * @return the observable resulting in a single result
	 */
	@Nonnull
	public static <T> Observable<Boolean> all(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super T, Boolean> predicate) {
		return new Observable<Boolean>() {
			@Override
			public Closeable register(final Observer<? super Boolean> observer) {
				DefaultObserverEx<T> o = new DefaultObserverEx<T>(true) {
					{
						add("source", source);
					}
					/** Indicate if we returned early. */
					boolean done;
					@Override
					public void onError(Throwable ex) {
						observer.error(ex);
					}
					@Override
					public void onFinish() {
						if (!done) {
							done = true;
							observer.next(true);
							observer.finish();
						}
					}
					@Override
					public void onNext(T value) {
						if (!predicate.invoke(value)) {
							done = true;
							observer.next(false);
							observer.finish();
						}
					}
				};
				return o;
			}
		};
	}
	/**
	 * Channels the values of the first observable who fires first from the given set of observables.
	 * E.g., <code>O3 = Amb(O1, O2)</code> if O1 starts to submit events first, O3 will relay these events and events of O2 will be completely ignored
	 * @param <T> the type of the observed element
	 * @param sources the iterable list of source observables.
	 * @return the observable of which reacted first
	 */
	@Nonnull
	public static <T> Observable<T> amb(
			@Nonnull final Iterable<? extends Observable<? extends T>> sources) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				final CompositeCloseable result = new CompositeCloseable();
				
				final List<DefaultObserverEx<T>> observers = new ArrayList<DefaultObserverEx<T>>();
				List<Observable<? extends T>> observables = new ArrayList<Observable<? extends T>>();

				final AtomicReference<Object> first = new AtomicReference<Object>();

				int i = 0;
				for (final Observable<? extends T> os : sources) {
					observables.add(os);
					final int thisIndex = i;
					DefaultObserverEx<T> obs = new DefaultObserverEx<T>(true) {
						/** We won the race. */
						boolean weWon;
						/** Cancel everyone else. */
						void cancelRest() {
							for (int i = 0; i < observers.size(); i++) {
								if (i != thisIndex) {
									observers.get(i).close();
								}
							}
						}
						/** @return Check if we won the race. */
						boolean didWeWon() {
							if (!weWon) {
								if (first.compareAndSet(null, this)) {
									weWon = true;
									cancelRest();
								} else {
									close();
								}
							}
							return weWon;
						}
						@Override
						public void onError(Throwable ex) {
							if (didWeWon()) {
								observer.error(ex);
							}
						}
						@Override
						public void onFinish() {
							if (didWeWon()) {
								observer.finish();
							}
						}
						@Override
						public void onNext(T value) {
							if (didWeWon()) {
								observer.next(value);
							} else {
								close();
							}
						}
					};
					observers.add(obs);
					result.add(obs);
				}
				i = 0;
				for (final Observable<? extends T> os : observables) {
					DefaultObserverEx<T> observerEx = observers.get(i);
					observerEx.registerWith(os);
					i++;
				}
				return result;
			}
		};
	}
	/**
	 * Channels the values of either left or right depending on who fired its first value.
	 * @param <T> the observed value type
	 * @param left the left observable
	 * @param right the right observable
	 * @return the observable that will stream one of the sources.
	 * @since 0.97
	 */
	public static <T> Observable<T> amb(
			@Nonnull final Observable<? extends T> left,
			@Nonnull final Observable<? extends T> right
			) {
		List<Observable<? extends T>> sources = new ArrayList<Observable<? extends T>>();
		sources.add(left);
		sources.add(right);
		return amb(sources);
	}
	/**
	 * Signals a single true if the source observable contains any element.
	 * It might return early for a non-empty source but waits for the entire observable to return false.
	 * @param <T> the element type
	 * @param source the source
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<Boolean> any(
			@Nonnull final Observable<T> source) {
		return any(source, Functions.alwaysTrue1());
	}
	/**
	 * Signals a single TRUE if the source ever signals next() and any of 
	 * the values matches the predicate before it signals a finish(), and deregisters
	 * from the source. It signals a false at the end of stream otherwise.
	 * @param <T> the source element type.
	 * @param source the source observable
	 * @param predicate the predicate to test the values
	 * @return the observable.
	 */
	@Nonnull
	public static <T> Observable<Boolean> any(
			@Nonnull final Observable<T> source,
			@Nonnull final Func1<? super T, Boolean> predicate) {
		return new Observable<Boolean>() {
			@Override
			public Closeable register(final Observer<? super Boolean> observer) {
				DefaultObserverEx<T> obs = new DefaultObserverEx<T>(true) {
					@Override
					public void onError(Throwable ex) {
						observer.error(ex);
						close();
					}

					@Override
					public void onFinish() {
						observer.next(false);
						observer.finish();
						close();
					}

					@Override
					public void onNext(T value) {
						if (predicate.invoke(value)) {
							observer.next(true);
							observer.finish();
							close();
						}
					}

				};
				return obs.registerWith(source);
			}
		};
	}
	/**
	 * Computes and signals the average value of the BigDecimal source.
	 * The source may not send nulls.
	 * @param source the source of BigDecimals to aggregate.
	 * @return the observable for the average value
	 */
	@Nonnull
	public static Observable<BigDecimal> averageBigDecimal(
			@Nonnull final Observable<BigDecimal> source) {
		return aggregate(source,
			Functions.sumBigDecimal(),
			new Func2<BigDecimal, Integer, BigDecimal>() {
				@Override
				public BigDecimal invoke(BigDecimal param1, Integer param2) {
					return param1.divide(BigDecimal.valueOf(param2.longValue()), RoundingMode.HALF_UP);
				}
			}
		);
	}
	/**
	 * Computes and signals the average value of the BigInteger source.
	 * The source may not send nulls.
	 * @param source the source of BigIntegers to aggregate.
	 * @return the observable for the average value
	 */
	@Nonnull
	public static Observable<BigDecimal> averageBigInteger(
			@Nonnull final Observable<BigInteger> source) {
		return aggregate(source,
			Functions.sumBigInteger(),
			new Func2<BigInteger, Integer, BigDecimal>() {
				@Override
				public BigDecimal invoke(BigInteger param1, Integer param2) {
					return new BigDecimal(param1).divide(BigDecimal.valueOf(param2.longValue()), RoundingMode.HALF_UP);
				}
			}
		);
	}
	/**
	 * Computes and signals the average value of the Double source.
	 * The source may not send nulls.
	 * @param source the source of Doubles to aggregate.
	 * @return the observable for the average value
	 */
	@Nonnull
	public static Observable<Double> averageDouble(
			@Nonnull final Observable<Double> source) {
		return aggregate(source,
			Functions.sumDouble(),
			new Func2<Double, Integer, Double>() {
				@Override
				public Double invoke(Double param1, Integer param2) {
					return param1 / param2;
				}
			}
		);
	}
	/**
	 * Computes and signals the average value of the Float source.
	 * The source may not send nulls.
	 * @param source the source of Floats to aggregate.
	 * @return the observable for the average value
	 */
	@Nonnull
	public static Observable<Float> averageFloat(
			@Nonnull final Observable<Float> source) {
		return aggregate(source,
			Functions.sumFloat(),
			new Func2<Float, Integer, Float>() {
				@Override
				public Float invoke(Float param1, Integer param2) {
					return param1 / param2;
				}
			}
		);
	}
	/**
	 * Computes and signals the average value of the integer source.
	 * The source may not send nulls.
	 * The intermediate aggregation used double values.
	 * @param source the source of integers to aggregate.
	 * @return the observable for the average value
	 */
	@Nonnull
	public static Observable<Double> averageInt(
			@Nonnull final Observable<Integer> source) {
		return aggregate(source,
			new Func2<Double, Integer, Double>() {
				@Override
				public Double invoke(Double param1, Integer param2) {
					if (param1 != null) {
						return param1 + param2;
					}
					return param2.doubleValue();
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
	 * Computes and signals the average value of the Long source.
	 * The source may not send nulls.
	 * The intermediate aggregation used double values.
	 * @param source the source of longs to aggregate.
	 * @return the observable for the average value
	 */
	@Nonnull
	public static Observable<Double> averageLong(
			@Nonnull final Observable<Long> source) {
		return aggregate(source,
			new Func2<Double, Long, Double>() {
				@Override
				public Double invoke(Double param1, Long param2) {
					if (param1 != null) {
						return param1 + param2;
					}
					return param2.doubleValue();
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
	 * Buffer the nodes as they become available and send them out in bufferSize chunks.
	 * The observers return a new and modifiable list of T on every next() call.
	 * @param <T> the type of the elements
	 * @param source the source observable
	 * @param bufferSize the target buffer size
	 * @return the observable of the list
	 */
	@Nonnull
	public static <T> Observable<List<T>> buffer(
			@Nonnull final Observable<? extends T> source,
			final int bufferSize) {
		return new Observable<List<T>>() {
			@Override
			public Closeable register(final Observer<? super List<T>> observer) {
				return source.register(new Observer<T>() {
					/** The current buffer. */
					List<T> buffer;

					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						if (buffer != null && buffer.size() > 0) {
							observer.next(buffer);
						}
						observer.finish();
					}

					@Override
					public void next(T value) {
						if (buffer == null) {
							buffer = new ArrayList<T>(bufferSize);
						}
						buffer.add(value);
						if (buffer.size() == bufferSize) {
							observer.next(buffer);
							buffer = new ArrayList<T>(bufferSize);
						}
					}

				});
			}
		};
	}
	/**
	 * Buffer the Ts of the source until the buffer reaches its capacity or the current time unit runs out.
	 * Might result in empty list of Ts and might complete early when the source finishes before the time runs out.
	 * It uses the default scheduler pool.
	 * @param <T> the type of the values
	 * @param source the source observable
	 * @param bufferSize the allowed buffer size
	 * @param time the time value to wait betveen buffer fills
	 * @param unit the time unit
	 * @return the observable of list of Ts
	 */
	@Nonnull
	public static <T> Observable<List<T>> buffer(
			@Nonnull final Observable<? extends T> source,
			final int bufferSize,
			final long time,
			@Nonnull final TimeUnit unit) {
		return buffer(source, bufferSize, time, unit, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Buffer the Ts of the source until the buffer reaches its capacity or the current time unit runs out.
	 * Might result in empty list of Ts and might complete early when the source finishes before the time runs out.
	 * @param <T> the type of the values
	 * @param source the source observable
	 * @param bufferSize the allowed buffer size
	 * @param time the time value to wait between buffer fills
	 * @param unit the time unit
	 * @param pool the pool where to schedule the buffer splits
	 * @return the observable of list of Ts
	 */
	@Nonnull
	public static <T> Observable<List<T>> buffer(
			@Nonnull final Observable<? extends T> source,
			final int bufferSize,
			final long time,
			@Nonnull final TimeUnit unit,
			@Nonnull final Scheduler pool) {
		return new Observable<List<T>>() {
			@Override
			public Closeable register(final Observer<? super List<T>> observer) {
				final BlockingQueue<T> buffer = new LinkedBlockingQueue<T>();
				final AtomicInteger bufferLength = new AtomicInteger();
				final Lock lock = new ReentrantLock(true);
				final DefaultRunnable r = new DefaultRunnable(lock) {
					@Override
					public void onRun() {
						List<T> curr = new ArrayList<T>();
						buffer.drainTo(curr);
						bufferLength.addAndGet(-curr.size());
						observer.next(curr);
					}
				};
				DefaultObserver<T> s = new DefaultObserver<T>(lock, true) {
					/** The timer companion. */
					Closeable timer = pool.schedule(r, time, time, unit);
					@Override
					protected void onClose() {
						Closeables.closeSilently(timer);
					}

					@Override
					public void onError(Throwable ex) {
						observer.error(ex);
					}
					@Override
					public void onFinish() {
						List<T> curr = new ArrayList<T>();
						buffer.drainTo(curr);
						bufferLength.addAndGet(-curr.size());
						observer.next(curr);

						observer.finish();
					}

					/** The buffer to fill in. */
					@Override
					public void onNext(T value) {
						buffer.add(value);
						if (bufferLength.incrementAndGet() == bufferSize) {
							List<T> curr = new ArrayList<T>();
							buffer.drainTo(curr);
							bufferLength.addAndGet(-curr.size());

							observer.next(curr);
						}
					}
				};
				return Closeables.newCloseable(s, source.register(s));
			}
		};

	}
	/**
	 * Buffers the source observable Ts into a list of Ts periodically and submits them to the returned observable.
	 * Each next() invocation contains a new and modifiable list of Ts. The signaled List of Ts might be empty if
	 * no Ts appeared from the original source within the current timespan.
	 * The last T of the original source triggers an early submission to the output.
	 * The scheduling is done on the default Scheduler.
	 * @param <T> the type of elements to observe
	 * @param source the source of Ts.
	 * @param time the time value to split the buffer contents.
	 * @param unit the time unit of the time
	 * @return the observable of list of Ts
	 */
	@Nonnull
	public static <T> Observable<List<T>> buffer(
			@Nonnull final Observable<? extends T> source,
			final long time,
			@Nonnull final TimeUnit unit) {
		return buffer(source, time, unit, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Buffers the source observable Ts into a list of Ts periodically and submits them to the returned observable.
	 * Each next() invocation contains a new and modifiable list of Ts. The signaled List of Ts might be empty if
	 * no Ts appeared from the original source within the current timespan.
	 * The last T of the original source triggers an early submission to the output.
	 * @param <T> the type of elements to observe
	 * @param source the source of Ts.
	 * @param time the time value to split the buffer contents.
	 * @param unit the time unit of the time
	 * @param pool the scheduled execution pool to use
	 * @return the observable of list of Ts
	 */
	@Nonnull
	public static <T> Observable<List<T>> buffer(
			@Nonnull final Observable<? extends T> source,
			final long time,
			@Nonnull final TimeUnit unit,
			@Nonnull final Scheduler pool) {
		return new Observable<List<T>>() {
			@Override
			public Closeable register(final Observer<? super List<T>> observer) {

				final BlockingQueue<T> buffer = new LinkedBlockingQueue<T>();
				final Lock lock = new ReentrantLock(true);

				final DefaultRunnable r = new DefaultRunnable(lock) {
					@Override
					public void onRun() {
						List<T> curr = new ArrayList<T>();
						buffer.drainTo(curr);
						observer.next(curr);
					}
				};
				DefaultObserver<T> o = new DefaultObserver<T>(lock, true) {
					Closeable timer = pool.schedule(r, time, time, unit);
					@Override
					protected void onClose() {
						Closeables.closeSilently(timer);
					}

					@Override
					public void onError(Throwable ex) {
						observer.error(ex);
					}
					@Override
					public void onFinish() {
						List<T> curr = new ArrayList<T>();
						buffer.drainTo(curr);
						observer.next(curr);
						observer.finish();
					}
					/** The buffer to fill in. */
					@Override
					public void onNext(T value) {
						buffer.add(value);
					}
				};
				return Closeables.newCloseable(o, source.register(o));
			}
		};
	}
	/**
	 * Combine a value supplied by the function with a stream of Ts whenever the src fires.
	 * The observed sequence contains the supplied value as first, the src value as second.
	 * @param <T> the element type
	 * @param supplier the function that returns a value
	 * @param src the source of Ts
	 * @return the new observable
	 * @since 0.97
	 */
	public static <T> Observable<List<T>> combine(final Func0<? extends T> supplier, Observable<? extends T> src) {
		return select(src, new Func1<T, List<T>>() {
			@Override
			public List<T> invoke(T param1) {
				List<T> result = new ArrayList<T>();
				result.add(supplier.invoke());
				result.add(param1);
				return result;
			}
		});
	}
	/**
	 * Combine the incoming Ts of the various observables into a single list of Ts like
	 * using zip() on more than two sources.
	 * @param <T> the element type
	 * @param srcs the iterable of observable sources.
	 * @return the new observable
	 */
	public static <T> Observable<List<T>> combine(final List<? extends Observable<? extends T>> srcs) {
		if (srcs.size() < 1) {
			return never();
		} else
		if (srcs.size() == 1) {
			return select(srcs.get(0), new Func1<T, List<T>>() {
				@Override
				public List<T> invoke(T param1) {
					List<T> result = new ArrayList<T>(1);
					result.add(param1);
					return result;
				}
			});
		}
		return new Observable<List<T>>() {
			@Override
			public Closeable register(Observer<? super List<T>> observer) {
				Observable<List<T>> res0 = zip(srcs.get(0), srcs.get(1), new Func2<T, T, List<T>>() {
					@Override
					public List<T> invoke(T param1, T param2) {
						List<T> result = new ArrayList<T>();
						result.add(param1);
						result.add(param2);
						return result;
					}
				});
				for (int i = 2; i < srcs.size(); i++) {
					res0 = zip(res0, srcs.get(i), new Func2<List<T>, T, List<T>>() {
						@Override
						public List<T> invoke(List<T> param1, T param2) {
							param1.add(param2);
							return param1;
						}
					});
				}
				return res0.register(observer);
			}
		};
	}
	/**
	 * Combine a stream of Ts with a constant T whenever the src fires.
	 * The observed list contains the values of src as the first value, constant as the second.
	 * @param <T> the element type
	 * @param src the source of Ts
	 * @param constant the constant T to combine with
	 * @return the new observable
	 */
	public static <T> Observable<List<T>> combine(Observable<? extends T> src, final T constant) {
		return select(src, new Func1<T, List<T>>() {
			@Override
			public List<T> invoke(T param1) {
				List<T> result = new ArrayList<T>();
				result.add(param1);
				result.add(constant);
				return result;
			}
		});
	}
	/**
	 * Combine a constant T with a stream of Ts whenever the src fires.
	 * The observed sequence contains the constant as first, the src value as second.
	 * @param <T> the element type
	 * @param constant the constant T to combine with
	 * @param src the source of Ts
	 * @return the new observable
	 */
	public static <T> Observable<List<T>> combine(final T constant, Observable<? extends T> src) {
		return select(src, new Func1<T, List<T>>() {
			@Override
			public List<T> invoke(T param1) {
				List<T> result = new ArrayList<T>();
				result.add(constant);
				result.add(param1);
				return result;
			}
		});
	}
	/**
	 * Returns an observable which combines the latest values of
	 * both streams whenever one sends a new value, but only after both sent a value.
	 * <p><b>Exception semantics:</b> if any stream throws an exception, the output stream
	 * throws an exception and all subscriptions are terminated.</p>
	 * <p><b>Completion semantics:</b> The output stream terminates
	 * after both streams terminate.</p>
	 * <p>The function will start combining the values only when both sides have already sent
	 * a value.</p>
	 * @param <T> the left element type
	 * @param <U> the right element type
	 * @param <V> the result element type
	 * @param left the left stream
	 * @param right the right stream
	 * @param selector the function which combines values from both streams and returns a new value
	 * @return the new observable.
	 */
	public static <T, U, V> Observable<V> combineLatest(
			final Observable<? extends T> left,
			final Observable<? extends U> right,
			final Func2<? super T, ? super U, ? extends V> selector
	) {
		return new Observable<V>() {
			@Override
			public Closeable register(final Observer<? super V> observer) {
				final Lock lock = new ReentrantLock(true);
				final CompositeCloseable closeBoth = new CompositeCloseable();
				final AtomicReference<T> leftRef = new AtomicReference<T>();
				final AtomicBoolean leftFirst = new AtomicBoolean();
				final AtomicReference<U> rightRef = new AtomicReference<U>();
				final AtomicBoolean rightFirst = new AtomicBoolean();
				final AtomicInteger wip = new AtomicInteger(2);
				DefaultObserverEx<T> obs1 = new DefaultObserverEx<T>(lock, false) {

					@Override
					protected void onError(Throwable ex) {
						observer.error(ex);
						Closeables.closeSilently(closeBoth);
					}

					@Override
					protected void onFinish() {
						if (wip.decrementAndGet() == 0) {
							observer.finish();
						}
						close();
					}

					@Override
					protected void onNext(T value) {
						leftRef.set(value);
						leftFirst.set(true);
						if (rightFirst.get()) {
							observer.next(selector.invoke(value, rightRef.get()));
						}
					}

				};
				DefaultObserverEx<U> obs2 = new DefaultObserverEx<U>(lock, false) {

					@Override
					protected void onError(Throwable ex) {
						observer.error(ex);
						Closeables.closeSilently(closeBoth);
					}

					@Override
					protected void onFinish() {
						if (wip.decrementAndGet() == 0) {
							observer.finish();
						}
						close();
					}

					@Override
					protected void onNext(U value) {
						rightRef.set(value);
						rightFirst.set(true);
						if (leftFirst.get()) {
							observer.next(selector.invoke(leftRef.get(), value));
						}
					}

				};
				closeBoth.add(obs1);
				closeBoth.add(obs2);
				obs1.add(new Object(), left);
				obs2.add(new Object(), right);
				return closeBoth;
			}
		};
	}
	/**
	 * Returns an observable which combines the latest values of
	 * both streams whenever one sends a new value.
	 * <p><b>Exception semantics:</b> if any stream throws an exception, the output stream
	 * throws an exception and all subscriptions are terminated.</p>
	 * <p><b>Completion semantics:</b> The output stream terminates
	 * after both streams terminate.</p>
	 * <p>Note that at the beginning, when the left or right fires first, the selector function
	 * will receive (value, null) or (null, value). If you want to react only in cases when both have sent
	 * a value, use the {@link #combineLatest(Observable, Observable, Func2)} method.</p>
	 * @param <T> the left element type
	 * @param <U> the right element type
	 * @param <V> the result element type
	 * @param left the left stream
	 * @param right the right stream
	 * @param selector the function which combines values from both streams and returns a new value
	 * @return the new observable.
	 */
	public static <T, U, V> Observable<V> combineLatest0(
			final Observable<? extends T> left,
			final Observable<? extends U> right,
			final Func2<? super T, ? super U, ? extends V> selector
	) {
		return new Observable<V>() {
			@Override
			public Closeable register(final Observer<? super V> observer) {
				final Lock lock = new ReentrantLock(true);
				final CompositeCloseable closeBoth = new CompositeCloseable();
				
				final AtomicReference<T> leftRef = new AtomicReference<T>();
				final AtomicReference<U> rightRef = new AtomicReference<U>();
				final AtomicInteger wip = new AtomicInteger(2);
				DefaultObserverEx<T> obs1 = new DefaultObserverEx<T>(lock, false) {

					@Override
					protected void onError(Throwable ex) {
						observer.error(ex);
						Closeables.closeSilently(closeBoth);
					}

					@Override
					protected void onFinish() {
						if (wip.decrementAndGet() == 0) {
							observer.finish();
						}
						close();
					}

					@Override
					protected void onNext(T value) {
						leftRef.set(value);
						observer.next(selector.invoke(value, rightRef.get()));
					}

				};
				DefaultObserverEx<U> obs2 = new DefaultObserverEx<U>(lock, false) {

					@Override
					protected void onError(Throwable ex) {
						observer.error(ex);
						Closeables.closeSilently(closeBoth);
					}

					@Override
					protected void onFinish() {
						if (wip.decrementAndGet() == 0) {
							observer.finish();
						}
						close();
					}

					@Override
					protected void onNext(U value) {
						rightRef.set(value);
						observer.next(selector.invoke(leftRef.get(), value));
					}

				};
				closeBoth.add(obs1);
				closeBoth.add(obs2);
				obs1.add(new Object(), left);
				obs2.add(new Object(), right);
				return closeBoth;
			}
		};
	}
	/**
	 * Concatenates the source observables in a way that when the first finish(), the
	 * second gets registered and continued, and so on.
	 * <p>If the sources sequence is empty, a no-op observable is returned.</p>
	 * <p>Note that the source iterable is not consumed up front but as the individual observables
	 * complete, therefore the Iterator methods might be called from any thread.</p>
	 * @param <T> the type of the values to observe
	 * @param sources the source list of subsequent observables
	 * @return the concatenated observable
	 */
	@Nonnull
	public static <T> Observable<T> concat(
			@Nonnull final Iterable<? extends Observable<? extends T>> sources) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				final Iterator<? extends Observable<? extends T>> it = sources.iterator();
				if (it.hasNext()) {
					DefaultObserverEx<T> obs = new DefaultObserverEx<T>(false) {
						@Override
						public void onError(Throwable ex) {
							observer.error(ex);
							close();
						}

						@Override
						public void onFinish() {
							if (it.hasNext()) {
								remove(this);
								registerWith(it.next());
							} else {
								observer.finish();
								close();
							}
						}
						@Override
						public void onNext(T value) {
							observer.next(value);
						}
					};
					
					return obs.registerWith(it.next());
				}
				return Closeables.emptyCloseable();
			}
		};
	}
	/**
	 * Concatenate the the multiple sources of T one after another.
	 * <p><b>Exception semantics:</b> if the sources or any inner observer signals an
	 * error, the outer observable will signal that error and the sequence is terminated.</p>
	 * @param <T> the element type
	 * @param sources the observable sequence of the observable sequence of Ts.
	 * @return the new observable
	 */
	public static <T> Observable<T> concat(
			final Observable<? extends Observable<? extends T>> sources
	) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				final LinkedBlockingQueue<Observable<? extends T>> sourceQueue = new LinkedBlockingQueue<Observable<? extends T>>();
				final AtomicInteger wip = new AtomicInteger(1);
				DefaultObserverEx<Observable<? extends T>> o = new DefaultObserverEx<Observable<? extends T>>(true) {
					/** The first value arrived? */
					@GuardedBy("lock")
					boolean first;
					/**
					 * The inner exception to forward.
					 * @param ex the exception
					 */
					void innerError(Throwable ex) {
						error(ex);
					}
					@Override
					protected void onError(Throwable ex) {
						observer.error(ex);
					}
					@Override
					protected void onFinish() {
						if (wip.decrementAndGet() == 0) {
							observer.finish();
						}
					}
					@Override
					protected void onNext(Observable<? extends T> value) {
						if (!first) {
							first = true;
							registerOn(value);
						} else {
							sourceQueue.add(value);
						}
					}

					void registerOn(Observable<? extends T> value) {
						wip.incrementAndGet();
						replace("source", "source", value.register(new DefaultObserver<T>(lock, true) {
							@Override
							public void onError(Throwable ex) {
								innerError(ex);
							}

							@Override
							public void onFinish() {
								Observable<? extends T> nextO = sourceQueue.poll();
								if (nextO != null) {
									registerOn(nextO);
								} else {
									if (wip.decrementAndGet() == 0) {
										observer.finish();
										remove("source");
									} else {
										first = true;
									}
								}
							}

							@Override
							public void onNext(T value) {
								observer.next(value);
							}

						}));
					}
					
				};
				o.registerWith(sources);
				return o;
			}
		};
	}
	/**
	 * Concatenate two observables in a way when the first finish() the second is registered
	 * and continued with.
	 * @param <T> the type of the elements
	 * @param first the first observable
	 * @param second the second observable
	 * @return the concatenated observable
	 */
	@Nonnull
	public static <T> Observable<T> concat(
			@Nonnull Observable<? extends T> first,
			@Nonnull Observable<? extends T> second) {
		List<Observable<? extends T>> list = new ArrayList<Observable<? extends T>>();
		list.add(first);
		list.add(second);
		return concat(list);
	}
	/**
	 * Signals a single TRUE if the source observable signals a value equals() with the source value.
	 * Both the source and the test value might be null. The signal goes after the first encounter of
	 * the given value.
	 * @param <T> the type of the observed values
	 * @param source the source observable
	 * @param value the value to look for
	 * @return the observer for contains
	 */
	@Nonnull
	public static <T> Observable<Boolean> contains(
			@Nonnull final Observable<? extends T> source,
			final T value) {
		return any(source, new Func1<T, Boolean>() {
			@Override
			public Boolean invoke(T param1) {
				return param1 == value || (param1 != null && param1.equals(value));
			}
		});
	}
	/**
	 * Signals a single TRUE if the source observable signals a value equals() with 
	 * the supplied value.
	 * Both the source and the test value might be null. The signal goes after the first encounter of
	 * the given value.
	 * @param <T> the type of the observed values
	 * @param source the source observable
	 * @param supplier the supplier for the value containment test
	 * @return the observer for contains
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<Boolean> contains(
			@Nonnull final Observable<? extends T> source,
			final Func0<? extends T> supplier) {
		return any(source, new Func1<T, Boolean>() {
			@Override
			public Boolean invoke(T param1) {
				T value = supplier.invoke();
				return param1 == value || (param1 != null && param1.equals(value));
			}
		});
	}
	/**
	 * Counts the number of elements in the observable source.
	 * @param <T> the element type
	 * @param source the source observable
	 * @return the count signal
	 */
	@Nonnull
	public static <T> Observable<Integer> count(
			@Nonnull final Observable<T> source) {
		return new Observable<Integer>() {
			@Override
			public Closeable register(final Observer<? super Integer> observer) {
				return source.register(new Observer<T>() {
					/** The counter. */
					int count;
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.next(count);
						observer.finish();
					}

					@Override
					public void next(T value) {
						count++;
					}

				});
			}
		};
	}
	/**
	 * Counts the number of elements where the predicate returns true.
	 * @param <T> the element type
	 * @param source the source sequence of Ts
	 * @param predicate the predicate function 
	 * @return  the observable with a single value
	 * @since 0.97
	 */
	public static <T> Observable<Integer> count(
			Observable<? extends T> source, 
			Func1<? super T, Boolean> predicate) {
		return count(where(source, predicate));
	}
	/**
	 * Counts the number of elements in the observable source as a long.
	 * @param <T> the element type
	 * @param source the source observable
	 * @return the count signal
	 */
	@Nonnull
	public static <T> Observable<Long> countLong(
			@Nonnull final Observable<T> source) {
		return new Observable<Long>() {
			@Override
			public Closeable register(final Observer<? super Long> observer) {
				return source.register(new Observer<T>() {
					/** The counter. */
					long count;
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.next(count);
						observer.finish();
					}

					@Override
					public void next(T value) {
						count++;
					}

				});
			}
		};
	}
	/**
	 * Counts the number of elements where the predicate returns true as long.
	 * @param <T> the element type
	 * @param source the source sequence of Ts
	 * @param predicate the predicate function 
	 * @return  the observable with a single value
	 * @since 0.97
	 */
	public static <T> Observable<Long> countLong(
			Observable<? extends T> source, 
			Func1<? super T, Boolean> predicate) {
		return countLong(where(source, predicate));
	}
	/**
	 * Create an observable instance by submitting a function which takes responsibility
	 * for registering observers.
	 * @param <T> the type of the value to observe
	 * @param register the function to manage new subscriptions
	 * @return the observable instance
	 */
	@Nonnull
	public static <T> Observable<T> create(
			@Nonnull final Func1<Observer<? super T>, ? extends Action0> register) {
		return new Observable<T>() {
			@Override
			public Closeable register(Observer<? super T> observer) {
				final Action0 a = register.invoke(observer);
				return new Closeable() {
					@Override
					public void close() {
						a.invoke();
					}
				};
			}
		};
	}
	/**
	 * Create an observable instance by submitting a function which takes responsibility
	 * for registering observers and returns a custom Closeable to terminate the registration.
	 * @param <T> the type of the value to observe
	 * @param subscribe the function to manage new subscriptions
	 * @return the observable instance
	 */
	@Nonnull
	public static <T> Observable<T> createWithCloseable(
			@Nonnull final Func1<Observer<? super T>, ? extends Closeable> subscribe) {
		return new Observable<T>() {
			@Override
			public Closeable register(Observer<? super T> observer) {
				return subscribe.invoke(observer);
			}
		};
	}
	/**
	 * Constructs an observer which logs errors in case next(), finish() or error() is called
	 * and the observer is not in running state anymore due an earlier finish() or error() call.
	 * @param <T> the element type.
	 * @param source the source observable
	 * @return the augmented observable
	 */
	@Nonnull
	public static <T> Observable<T> debugState(
			@Nonnull final Observable<? extends T> source) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					ObserverState state = ObserverState.OBSERVER_RUNNING;
					@Override
					public void error(Throwable ex) {
						if (state != ObserverState.OBSERVER_RUNNING) {
							new IllegalStateException(state.toString()).printStackTrace();
						}
						state = ObserverState.OBSERVER_ERROR;
						observer.error(ex);
					}

					@Override
					public void finish() {
						if (state != ObserverState.OBSERVER_RUNNING) {
							new IllegalStateException(state.toString()).printStackTrace();
						}
						state = ObserverState.OBSERVER_FINISHED;
						observer.finish();
					}

					@Override
					public void next(T value) {
						if (state != ObserverState.OBSERVER_RUNNING) {
							new IllegalStateException(state.toString()).printStackTrace();
						}
						observer.next(value);
					}

				});
			}
		};
	}
	/**
	 * The returned observable invokes the <code>observableFactory</code> whenever an observer
	 * tries to subscribe to it.
	 * @param <T> the type of elements to observer
	 * @param observableFactory the factory which is responsible to create a source observable.
	 * @return the result observable
	 */
	@Nonnull
	public static <T> Observable<T> defer(
			@Nonnull final Func0<? extends Observable<? extends T>> observableFactory) {
		return new Observable<T>() {
			@Override
			public Closeable register(Observer<? super T> observer) {
				return observableFactory.invoke().register(observer);
			}
		};
	}
	/**
	 * Delays the propagation of events of the source by the given amount. It uses the pool for the scheduled waits.
	 * The delay preserves the relative time difference between subsequent notifications.
	 * It uses the default scheduler pool when submitting the delayed values
	 * @param <T> the type of elements
	 * @param source the source of Ts
	 * @param time the time value
	 * @param unit the time unit
	 * @return the delayed observable of Ts
	 */
	@Nonnull
	public static <T> Observable<T> delay(
			@Nonnull final Observable<? extends T> source,
			final long time,
			@Nonnull final TimeUnit unit) {
		return delay(source, time, unit, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Delays the propagation of events of the source by the given amount. It uses the pool for the scheduled waits.
	 * The delay preserves the relative time difference between subsequent notifications
	 * @param <T> the type of elements
	 * @param source the source of Ts
	 * @param time the time value
	 * @param unit the time unit
	 * @param pool the pool to use for scheduling
	 * @return the delayed observable of Ts
	 */
	@Nonnull
	public static <T> Observable<T> delay(
			@Nonnull final Observable<? extends T> source,
			final long time,
			@Nonnull final TimeUnit unit,
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				DefaultObserver<T> obs = new DefaultObserver<T>(true) {
					/** The outstanding requests. */
					final BlockingQueue<Closeable> outstanding = new LinkedBlockingQueue<Closeable>();
					@Override
					public void onClose() {
						List<Closeable> list = new LinkedList<Closeable>();
						outstanding.drainTo(list);
						for (Closeable c : list) {
							Closeables.closeSilently(c);
						}
						super.close();
					}

					@Override
					public void onError(final Throwable ex) {
						Runnable r = new Runnable() {
							@Override
							public void run() {
								try {
									observer.error(ex);
									close();
								} finally {
									outstanding.poll();
								}
							}
						};
						outstanding.add(pool.schedule(r, time, unit));
					}

					@Override
					public void onFinish() {
						Runnable r = new Runnable() {
							@Override
							public void run() {
								try {
									observer.finish();
									close();
								} finally {
									outstanding.poll();
								}
							}
						};
						outstanding.add(pool.schedule(r, time, unit));
					}
					@Override
					public void onNext(final T value) {
						Runnable r = new Runnable() {
							@Override
							public void run() {
								try {
									observer.next(value);
								} finally {
									outstanding.poll();
								}
							}
						};
						outstanding.add(pool.schedule(r, time, unit));
					}
				};
				return obs;
			}
		};
	}
	/**
	 * Returns an observable which converts all option messages
	 * back to regular next(), error() and finish() messages.
	 * The returned observable adheres to the <code>next* (error|finish)?</code> pattern,
	 * which ensures that no further options are relayed after an error or finish.
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @return the new observable
	 * @see #materialize(Observable)
	 */
	@Nonnull
	public static <T> Observable<T> dematerialize(
			@Nonnull final Observable<? extends Option<T>> source) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<Option<T>>() {
					/** Keeps track of the observer's state. */
					final AtomicBoolean done = new AtomicBoolean();
					@Override
					public void error(Throwable ex) {
						if (!done.get()) {
							done.set(true);
							observer.error(ex);
						}
					}

					@Override
					public void finish() {
						if (!done.get()) {
							done.set(true);
							observer.finish();
						}
					}

					@Override
					public void next(Option<T> value) {
						if (!done.get()) {
							if (Option.isNone(value)) {
								done.set(true);
								observer.finish();
							} else
							if (Option.isSome(value)) {
								observer.next(value.value());
							} else {
								done.set(true);
								observer.error(Option.getError(value));
							}
						}
					}

				});
			}
		};
	}
	/**
	 * Dispatches the option to the various Observer methods.
	 * @param <T> the value type
	 * @param observer the observer
	 * @param value the value to dispatch
	 */
	@Nonnull
	public static <T> void dispatch(
			@Nonnull Observer<? super T> observer,
			@Nonnull Option<T> value) {
		if (value == Option.none()) {
			observer.finish();
		} else
		if (Option.isError(value)) {
			observer.error(((Option.Error<?>)value).error());
		} else {
			observer.next(value.value());
		}
	}
	/**
	 * Returns an observable which fires next() events only when the subsequent values differ
	 * in terms of Object.equals().
	 * @param <T> the type of the values
	 * @param source the source observable
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> distinct(
			@Nonnull final Observable<? extends T> source) {
		return distinct(source, Functions.<T>identity());
	}
	/**
	 * Returns Ts from the source observable if the subsequent keys extracted by <code>keyExtractor</code> are different.
	 * @param <T> the type of the values to observe
	 * @param <U> the key type check for distinction
	 * @param source the source of Ts
	 * @param keyExtractor the extractor for the keys
	 * @return the new filtered observable
	 */
	@Nonnull
	public static <T, U> Observable<T> distinct(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<T, U> keyExtractor) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** Indication as the first. */
					boolean first = true;
					/** The last value. */
					U lastKey;
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.finish();
					}

					@Override
					public void next(T value) {
						U key = keyExtractor.invoke(value);
						if (first) {
							first = false;
							observer.next(value);
						} else
						if (lastKey != value && (lastKey == null || !lastKey.equals(key))) {
							observer.next(value);
						}
						lastKey = key;
					}

				});
			}
		};
	}
	/**
	 * Maintains a queue of Ts which is then drained by the pump. Uses the default pool.
	 * FIX ME not sure what this method should do and how.
	 * @param <T> the type of the values
	 * @param source the source of Ts
	 * @param pump the pump that drains the queue
	 * @return the new observable
	 */
	@Nonnull
	public static <T> Observable<Void> drain(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends Observable<Void>> pump) {
		return drain(source, pump, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Maintains a queue of Ts which is then drained by the pump.
	 * FIX ME not sure what this method should do and how.
	 * @param <T> the type of the values
	 * @param source the source of Ts
	 * @param pump the pump that drains the queue
	 * @param pool the pool for the drain
	 * @return the new observable
	 */
	@Nonnull
	public static <T> Observable<Void> drain(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends Observable<Void>> pump,
			@Nonnull final Scheduler pool) {
		return new Observable<Void>() {
			@Override
			public Closeable register(final Observer<? super Void> observer) {
				// keep track of the forked observers so the last should invoke finish() on the observer
				DefaultObserverEx<T> obs = new DefaultObserverEx<T>(true) {
					/** The work in progress counter. */
					final AtomicInteger wip = new AtomicInteger(1);
					/** The executor which ensures the sequence. */
					final SingleLaneExecutor<T> exec = new SingleLaneExecutor<T>(pool, new Action1<T>() {
						@Override
						public void invoke(T value) {
							pump.invoke(value).register(
								new Observer<Void>() {
									@Override
									public void error(Throwable ex) {
										lock.lock();
										try {
											observer.error(ex);
											close();
										} finally {
											lock.unlock();
										}
									}

									@Override
									public void finish() {
										if (wip.decrementAndGet() == 0) {
											observer.finish();
											close();
										}
									}

									@Override
									public void next(Void value) {
										throw new AssertionError();
									}

								}
							);
						}
					});
					@Override
					public void onClose() {
//						exec.close(); FIX ME should not cancel the pool?!
					}

					@Override
					public void onError(Throwable ex) {
						observer.error(ex);
						close();
					}

					@Override
					public void onFinish() {
						if (wip.decrementAndGet() == 0) {
							observer.finish();
						}
					}
					@Override
					public void onNext(T value) {
						wip.incrementAndGet();
						exec.add(value);
					}
				};
				return obs.registerWith(source);
			}
		};
	}
	/**
	 * @param <T> the type of the values to observe (irrelevant)
	 * @return Returns an empty observable which signals only finish() on the default observer pool.
	 */
	@Nonnull
	public static <T> Observable<T> empty() {
		return empty(DEFAULT_SCHEDULER.get());
	}
	/**
	 * Returns an empty observable which signals only finish() on the given pool.
	 * @param <T> the expected type, (irrelevant)
	 * @param pool the pool to invoke the the finish()
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> empty(
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return pool.schedule(new Runnable() {
					@Override
					public void run() {
						observer.finish();
					}
				});
			}
		};
	}
	/**
	 * Invokes the given action when the source signals a finish() or error().
	 * @param <T> the type of the observed values
	 * @param source the source of Ts
	 * @param action the action to invoke on finish() or error()
	 * @return the new observable
	 */
	@Nonnull
	public static <T> Observable<T> finish(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Action0 action) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					@Override
					public void error(Throwable ex) {
						action.invoke();
						observer.error(ex);
					}

					@Override
					public void finish() {
						action.invoke();
						observer.finish();
					}

					@Override
					public void next(T value) {
						observer.next(value);
					}

				});
			}
		};
	}
	/**
	 * Blocks until the first element of the observable becomes available and returns that element.
	 * Might block forever.
	 * Might throw a NoSuchElementException when the observable doesn't produce any more elements
	 * @param <T> the type of the elements
	 * @param source the source of Ts
	 * @return the first element
	 */
	public static <T> T first(
			@Nonnull final Observable<? extends T> source) {
		CloseableIterator<T> it = toIterable(source).iterator();
		try {
			if (it.hasNext()) {
				return it.next();
			}
			throw new NoSuchElementException();
		} finally {
			Closeables.closeSilently(it);
		}
	}
	/**
	 * Creates a concatenated sequence of Observables based on the decision function of <code>selector</code> keyed by the source iterable.
	 * @param <T> the type of the source values
	 * @param <U> the type of the observable elements.
	 * @param source the source of keys
	 * @param selector the selector of keys which returns a new observable
	 * @return the concatenated observable.
	 */
	public static <T, U> Observable<U> forEach(
			@Nonnull final Iterable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends Observable<? extends U>> selector) {
		List<Observable<? extends U>> list = new ArrayList<Observable<? extends U>>();
		for (T t : source) {
			list.add(selector.invoke(t));
		}
		return concat(list);
	}
	/**
	 * Runs the observables in parallel and joins their last values whenever one fires.
	 * FIXME not sure what this method should do in case of error.
	 * @param <T> the type of the source values
	 * @param sources the list of sources
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<List<T>> forkJoin(
			@Nonnull final Iterable<? extends Observable<? extends T>> sources) {
		return new Observable<List<T>>() {
			@Override
			public Closeable register(final Observer<? super List<T>> observer) {
				final List<AtomicReference<T>> lastValues = new ArrayList<AtomicReference<T>>();
				final List<Observable<? extends T>> observableList = new ArrayList<Observable<? extends T>>();
				final List<Observer<T>> observers = new ArrayList<Observer<T>>();
				final AtomicInteger wip = new AtomicInteger(observableList.size() + 1);

				int i = 0;
				for (Observable<? extends T> o : sources) {
					final int j = i;
					observableList.add(o);
					lastValues.add(new AtomicReference<T>());
					observers.add(new Observer<T>() {
						/** The last value. */
						T last;
						@Override
						public void error(Throwable ex) {
							// TODO Auto-generated method stub

						}

						@Override
						public void finish() {
							lastValues.get(j).set(last);
							runIfComplete(observer, lastValues, wip);
						}

						@Override
						public void next(T value) {
							last = value;
						}

					});
				}
				CompositeCloseable closeables = new CompositeCloseable();
				i = 0;
				for (Observable<? extends T> o : observableList) {
					closeables.add(o.register(observers.get(i)));
					i++;
				}
				runIfComplete(observer, lastValues, wip);
				return closeables;
			}
			/**
			 * Runs the completion sequence once the WIP drops to zero.
			 * @param observer the observer who will receive the values
			 * @param lastValues the array of last values
			 * @param wip the work in progress counter
			 */
			public void runIfComplete(
					final Observer<? super List<T>> observer,
					final List<AtomicReference<T>> lastValues,
					final AtomicInteger wip) {
				if (wip.decrementAndGet() == 0) {
					List<T> values = new ArrayList<T>();
					for (AtomicReference<T> r : lastValues) {
						values.add(r.get());
					}
					observer.next(values);
					observer.finish();
				}
			}
		};
	}
	/**
	 * Generates a stream of Us by using a value T stream using the default pool for the generator loop.
	 * If T = int and U is double, this would be seen as <code>for (int i = 0; i &lt; 10; i++) { yield return i / 2.0; }</code>
	 * @param <T> the type of the generator values
	 * @param <U> the type of the observed values
	 * @param initial the initial generator value
	 * @param condition the condition that must hold to continue generating Ts
	 * @param next the function that computes the next value of T
	 * @param selector the selector which turns Ts into Us.
	 * @return the observable
	 */
	@Nonnull
	public static <T, U> Observable<U> generate(
			final T initial,
			@Nonnull final Func1<? super T, Boolean> condition,
			@Nonnull final Func1<? super T, ? extends T> next,
			@Nonnull final Func1<? super T, ? extends U> selector) {
		return generate(initial, condition, next, selector, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Generates a stream of Us by using a value T stream.
	 * If T = int and U is double, this would be seen as <code>for (int i = 0; i &lt; 10; i++) { yield return i / 2.0; }</code>
	 * @param <T> the type of the generator values
	 * @param <U> the type of the observed values
	 * @param initial the initial generator value
	 * @param condition the condition that must hold to continue generating Ts
	 * @param next the function that computes the next value of T
	 * @param selector the selector which turns Ts into Us.
	 * @param pool the thread pool where the generation loop should run.
	 * @return the observable
	 */
	@Nonnull
	public static <T, U> Observable<U> generate(
			final T initial,
			@Nonnull final Func1<? super T, Boolean> condition,
			@Nonnull final Func1<? super T, ? extends T> next,
			@Nonnull final Func1<? super T, ? extends U> selector,
			@Nonnull final Scheduler pool) {
		return new Observable<U>() {
			@Override
			public Closeable register(final Observer<? super U> observer) {
				DefaultRunnable s = new DefaultRunnable() {
					@Override
					public void onRun() {
						T t = initial;
						while (condition.invoke(t) && !cancelled()) {
							observer.next(selector.invoke(t));
							t = next.invoke(t);
						}
						if (!cancelled()) {
							observer.finish();
						}
					}
				};
				return pool.schedule(s);
			}
		};
	}
	/**
	 * Generates a stream of Us by using a value T stream.
	 * If T = int and U is double, this would be seen as <code>for (int i = 0; i &lt; 10; i++) { sleep(time); yield return i / 2.0; }</code>
	 * @param <T> the type of the generator values
	 * @param <U> the type of the observed values
	 * @param initial the initial generator value
	 * @param condition the condition that must hold to continue generating Ts
	 * @param next the function that computes the next value of T
	 * @param selector the selector which turns Ts into Us.
	 * @param delay the selector which tells how much to wait before releasing the next U
	 * @return the observable
	 */
	@Nonnull
	public static <T, U> Observable<Timestamped<U>> generateTimed(
			final T initial,
			@Nonnull final Func1<? super T, Boolean> condition,
			@Nonnull final Func1<? super T, ? extends T> next,
			@Nonnull final Func1<? super T, ? extends U> selector,
			@Nonnull final Func1<? super T, Long> delay) {
		return generateTimed(initial, condition, next, selector, delay, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Generates a dynamically timed stream of Us by using a value T stream.
	 * If T = int and U is double, this would be seen as <code>for (int i = 0; i &lt; 10; i++) { sleep(time); yield return i / 2.0; }</code>
	 * @param <T> the type of the generator values
	 * @param <U> the type of the observed values
	 * @param initial the initial generator value
	 * @param condition the condition that must hold to continue generating Ts
	 * @param next the function that computes the next value of T
	 * @param selector the selector which turns Ts into Us.
	 * @param delay the selector which tells how much to wait (in milliseconds) before releasing the next U
	 * @param pool the scheduled pool where the generation loop should run.
	 * @return the observable
	 */
	@Nonnull
	public static <T, U> Observable<Timestamped<U>> generateTimed(
			final T initial,
			@Nonnull final Func1<? super T, Boolean> condition,
			@Nonnull final Func1<? super T, ? extends T> next,
			@Nonnull final Func1<? super T, ? extends U> selector,
			@Nonnull final Func1<? super T, Long> delay,
			@Nonnull final Scheduler pool) {
		return generateTimedWithUnit(initial, condition, next, selector, new Func1<T, Pair<Long, TimeUnit>>() {
			@Override
			public Pair<Long, TimeUnit> invoke(T param1) {
				return Pair.of(delay.invoke(param1), TimeUnit.MILLISECONDS);
			}
		}, pool);
	}
	/**
	 * Generates a dynamically timed stream of Us by using a value T stream.
	 * If T = int and U is double, this would be seen as <code>for (int i = 0; i &lt; 10; i++) { sleep(time); yield return i / 2.0; }</code>
	 * @param <T> the type of the generator values
	 * @param <U> the type of the observed values
	 * @param initial the initial generator value
	 * @param condition the condition that must hold to continue generating Ts
	 * @param next the function that computes the next value of T
	 * @param selector the selector which turns Ts into Us.
	 * @param delay the selector which returns a pair of time and timeunit that 
	 * tells how much time to wait before releasing the next U
	 * @param pool the scheduled pool where the generation loop should run.
	 * @return the observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T, U> Observable<Timestamped<U>> generateTimedWithUnit(
			final T initial,
			@Nonnull final Func1<? super T, Boolean> condition,
			@Nonnull final Func1<? super T, ? extends T> next,
			@Nonnull final Func1<? super T, ? extends U> selector,
			@Nonnull final Func1<? super T, Pair<Long, TimeUnit>> delay,
			@Nonnull final Scheduler pool) {
		return new Observable<Timestamped<U>>() {
			@Override
			public Closeable register(final Observer<? super Timestamped<U>> observer) {
				// the cancellation indicator

				DefaultRunnable s = new DefaultRunnable() {
					T current = initial;
					@Override
					public void onRun() {
						U invoke = selector.invoke(current);
						Timestamped<U> of = Timestamped.of(invoke, System.currentTimeMillis());
						observer.next(of);
						final T tn = next.invoke(current);
						current = tn;
						if (condition.invoke(tn) && !cancelled()) {
							Pair<Long, TimeUnit> toWait = delay.invoke(tn);
							pool.schedule(this, toWait.first, toWait.second);
						} else {
							if (!cancelled()) {
								observer.finish();
							}
						}

					}
				};

				if (condition.invoke(initial)) {
					Pair<Long, TimeUnit> toWait = delay.invoke(initial);
					return pool.schedule(s, toWait.first, toWait.second);
				}
				return Functions.EMPTY_CLOSEABLE;
			}
		};
	}
	/**
	 * @return the current default pool used by the Observables methods
	 */
	@Nonnull
	public static Scheduler getDefaultScheduler() {
		return DEFAULT_SCHEDULER.get();
	}
	/**
	 * Group the specified source according to the keys provided by the extractor function.
	 * The resulting observable gets notified once a new group is encountered.
	 * Each previously encountered group by itself receives updates along the way.
	 * If the source finish(), all encountered group will finish().
	 * @param <T> the type of the source element
	 * @param <Key> the key type of the group
	 * @param source the source of Ts
	 * @param keyExtractor the key extractor which creates Keys from Ts
	 * @return the observable
	 */
	@Nonnull
	public static <T, Key> Observable<GroupedObservable<Key, T>> groupBy(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends Key> keyExtractor) {
		return groupBy(source, keyExtractor, Functions.<T>identity());
	}
	/**
	 * Group the specified source according to the keys provided by the extractor function.
	 * The resulting observable gets notified once a new group is encountered.
	 * Each previously encountered group by itself receives updates along the way.
	 * If the source finish(), all encountered group will finish().
	 * <p>Exception semantics: if the source sends an exception, the group observable and the individual groups'
	 * observables receive this error.</p>
	 * @param <T> the type of the source element
	 * @param <U> the type of the output element
	 * @param <Key> the key type of the group
	 * @param source the source of Ts
	 * @param keyExtractor the key extractor which creates Keys from Ts
	 * @param valueExtractor the extractor which makes Us from Ts
	 * @return the observable
	 */
	@Nonnull
	public static <T, U, Key> Observable<GroupedObservable<Key, U>> groupBy(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends Key> keyExtractor,
			@Nonnull final Func1<? super T, ? extends U> valueExtractor) {
		return new Observable<GroupedObservable<Key, U>>() {
			@Override
			public Closeable register(
					final Observer<? super GroupedObservable<Key, U>> observer) {
				final ConcurrentMap<Key, GroupedRegisteringObservable<Key, U>> knownGroups = new ConcurrentHashMap<Key, GroupedRegisteringObservable<Key, U>>();
				return source.register(new Observer<T>() {
					@Override
					public void error(Throwable ex) {
						for (Observer<U> group : knownGroups.values()) {
							group.error(ex);
						}
						observer.error(ex);
					}

					@Override
					public void finish() {
						for (Observer<U> group : knownGroups.values()) {
							group.finish();
						}
						observer.finish();
					}

					@Override
					public void next(T value) {
						final Key key = keyExtractor.invoke(value);
						GroupedRegisteringObservable<Key, U> group = knownGroups.get(key);
						if (group == null) {
							group = new GroupedRegisteringObservable<Key, U>(key);
							GroupedRegisteringObservable<Key, U> group2 = knownGroups.putIfAbsent(key, group);
							if (group2 != null) {
								group = group2;
							}
							observer.next(group);
						}
						group.next(valueExtractor.invoke(value));
					}

				});
			}
		};
	}
	/**
	 * Groups the source sequence of Ts until the specified duration for that group fires.
	 * <p>The key comparison is done by the <code>Object.equals()</code> semantics of the <code>HashMap</code>.</p>
	 * <p><b>Exception semantics:</b> if the source throws an exception, all active groups will receive
	 * the exception followed by the outer observer of the groups.</p>
	 * <p><b>Completion semantics:</b> if the source finishes, all active groups will receive a finish
	 * signal followed by the outer observer.</p>
	 * @param <T> the source element type
	 * @param <K> the key type
	 * @param <D> the duration element type, ignored
	 * @param source the source of Ts
	 * @param keySelector the key extractor
	 * @param durationSelector the observable for a particular group termination
	 * @return the new observable
	 */
	public static <T, K, D> Observable<GroupedObservable<K, T>> groupByUntil(
			final Observable<? extends T> source,
			final Func1<? super T, ? extends K> keySelector,
			final Func1<? super GroupedObservable<K, T>, ? extends Observable<D>> durationSelector
	) {
		return groupByUntil(source, keySelector, Functions.<T>identity(), durationSelector);
	}
	/**
	 * Groups the source sequence of Ts until the specified duration for that group fires.
	 * <p><b>Exception semantics:</b> if the source throws an exception, all active groups will receive
	 * the exception followed by the outer observer of the groups.</p>
	 * <p><b>Completion semantics:</b> if the source finishes, all active groups will receive a finish
	 * signal followed by the outer observer.</p>
	 * @param <T> the source element type
	 * @param <K> the key type
	 * @param <D> the duration element type, ignored
	 * @param source the source of Ts
	 * @param keySelector the key extractor
	 * @param durationSelector the observable for a particular group termination
	 * @param keyComparer the key comparer for the grouping
	 * @return the new observable
	 */
	public static <T, K, D> Observable<GroupedObservable<K, T>> groupByUntil(
			final Observable<? extends T> source,
			final Func1<? super T, ? extends K> keySelector,
			final Func1<? super GroupedObservable<K, T>, ? extends Observable<D>> durationSelector,
			final Func2<? super K, ? super K, Boolean> keyComparer
	) {
		return groupByUntil(source, keySelector, Functions.<T>identity(), durationSelector, keyComparer);
	}
	/**
	 * Groups the source sequence of Ts until the specified duration for that group fires.
	 * <p>The key comparison is done by the <code>Object.equals()</code> semantics of the <code>HashMap</code>.</p>
	 * <p><b>Exception semantics:</b> if the source throws an exception, all active groups will receive
	 * the exception followed by the outer observer of the groups.</p>
	 * <p><b>Completion semantics:</b> if the source finishes, all active groups will receive a finish
	 * signal followed by the outer observer.</p>
	 * @param <T> the source element type
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param <D> the duration element type, ignored
	 * @param source the source of Ts
	 * @param keySelector the key extractor
	 * @param valueSelector the value extractor
	 * @param durationSelector the observable for a particular group termination
	 * @return the new observable
	 */
	public static <T, K, V, D> Observable<GroupedObservable<K, V>> groupByUntil(
			final Observable<? extends T> source,
			final Func1<? super T, ? extends K> keySelector,
			final Func1<? super T, ? extends V> valueSelector,
			final Func1<? super GroupedObservable<K, V>, ? extends Observable<D>> durationSelector
	) {
		return new Observable<GroupedObservable<K, V>>() {
			@Override
			public Closeable register(
					final Observer<? super GroupedObservable<K, V>> observer) {
				DefaultObserverEx<T> o = new DefaultObserverEx<T>(true) {
					/** The active groups. */
					final Map<K, GroupedRegisteringObservable<K, V>> groups = new HashMap<K, GroupedRegisteringObservable<K, V>>();
					@Override
					protected void onError(Throwable ex) {
						for (Observer<V> o : groups.values()) {
							o.error(ex);
						}
						observer.error(ex);
					}

					@Override
					protected void onFinish() {
						for (Observer<V> o : groups.values()) {
							o.finish();
						}
						observer.finish();
					}

					@Override
					protected void onNext(T value) {
						final K k = keySelector.invoke(value);
						final V v = valueSelector.invoke(value);
						GroupedRegisteringObservable<K, V> gr = groups.get(k);
						if (gr != null) {
							gr = new GroupedRegisteringObservable<K, V>(k);
							final GroupedRegisteringObservable<K, V> fgr = gr;
							groups.put(k, gr);
							add(fgr, durationSelector.invoke(gr).register(new DefaultObserver<D>(lock, true) {

								@Override
								protected void onError(Throwable ex) {
									fgr.error(ex); // FIXME error propagation
									groups.remove(k);
									remove(fgr);
								}

								@Override
								protected void onFinish() {
									fgr.finish();
									groups.remove(k);
									remove(fgr);
								}

								@Override
								protected void onNext(D value) {
									fgr.finish();
									groups.remove(k);
									remove(fgr);
								}

							}));
							observer.next(gr);
						}
						gr.next(v);
					}

				};
				o.registerWith(source);
				return o;
			}
		};
	}
	/**
	 * Groups the source sequence of Ts until the specified duration for that group fires.
	 * <p><b>Exception semantics:</b> if the source throws an exception, all active groups will receive
	 * the exception followed by the outer observer of the groups.</p>
	 * <p><b>Completion semantics:</b> if the source finishes, all active groups will receive a finish
	 * signal followed by the outer observer.</p>
	 * @param <T> the source element type
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param <D> the duration element type, ignored
	 * @param source the source of Ts
	 * @param keySelector the key extractor
	 * @param valueSelector the value extractor
	 * @param durationSelector the observable for a particular group termination
	 * @param keyComparer the key comparer for the grouping
	 * @return the new observable
	 */
	public static <T, K, V, D> Observable<GroupedObservable<K, V>> groupByUntil(
			final Observable<? extends T> source,
			final Func1<? super T, ? extends K> keySelector,
			final Func1<? super T, ? extends V> valueSelector,
			final Func1<? super GroupedObservable<K, V>, ? extends Observable<D>> durationSelector,
			final Func2<? super K, ? super K, Boolean> keyComparer
	) {
		return new Observable<GroupedObservable<K, V>>() {
			@Override
			public Closeable register(
					final Observer<? super GroupedObservable<K, V>> observer) {
				DefaultObserverEx<T> o = new DefaultObserverEx<T>(true) {
					/** The key class with custom equality comparer. */
					class Key {
						/** The key value. */
						final K key;
						/**
						 * Constructor.
						 * @param key the key
						 */
						Key(K key) {
							this.key = key;
						}
						@Override
						public boolean equals(Object obj) {
							if (obj instanceof Key) {
								return keyComparer.invoke(key, ((Key)obj).key);
							}
							return false;
						}
						@Override
						public int hashCode() {
							return key != null ? key.hashCode() : 0;
						}
					}
					/** The active groups. */
					final Map<Key, GroupedRegisteringObservable<K, V>> groups = new HashMap<Key, GroupedRegisteringObservable<K, V>>();
					@Override
					protected void onError(Throwable ex) {
						for (Observer<V> o : groups.values()) {
							o.error(ex);
						}
						observer.error(ex);
					}

					@Override
					protected void onFinish() {
						for (Observer<V> o : groups.values()) {
							o.finish();
						}
						observer.finish();
					}

					@Override
					protected void onNext(T value) {
						final K kv = keySelector.invoke(value);
						final Key k = new Key(kv);
						final V v = valueSelector.invoke(value);
						GroupedRegisteringObservable<K, V> gr = groups.get(k);
						if (gr != null) {
							gr = new GroupedRegisteringObservable<K, V>(kv);
							final GroupedRegisteringObservable<K, V> fgr = gr;
							groups.put(k, gr);
							add(fgr, durationSelector.invoke(gr).register(new DefaultObserver<D>(lock, true) {

								@Override
								protected void onError(Throwable ex) {
									fgr.error(ex); // FIXME error propagation
									groups.remove(k);
									remove(fgr);
								}

								@Override
								protected void onFinish() {
									fgr.finish();
									groups.remove(k);
									remove(fgr);
								}

								@Override
								protected void onNext(D value) {
									fgr.finish();
									groups.remove(k);
									remove(fgr);
								}

							}));
							observer.next(gr);
						}
						gr.next(v);
					}

				};
				o.registerWith(source);
				return o;
			}
		};
	}
	/**
	 * Returns an observable which correlates two streams of values based on
	 * their time when they overlapped and groups the results.
	 * FIXME not sure how to implement it
	 * @param <Left> the element type of the left stream
	 * @param <Right> the element type of the right stream
	 * @param <LeftDuration> the overlapping duration indicator for the left stream (e.g., the event when it leaves)
	 * @param <RightDuration> the overlapping duration indicator for the right stream (e.g., the event when it leaves)
	 * @param <Result> the type of the grouping based on the coincidence.
	 * @param left the left source of elements
	 * @param right the right source of elements
	 * @param leftDurationSelector the duration selector for a left element
	 * @param rightDurationSelector the duration selector for a right element
	 * @param resultSelector the selector which will produce the output value
	 * @return the new observable
	 * @see #join(Observable, Observable, Func1, Func1, Func2)
	 */
	public static <Left, Right, LeftDuration, RightDuration, Result> Observable<Result> groupJoin(
			final Observable<? extends Left> left,
			final Observable<? extends Right> right,
			final Func1<? super Left, ? extends Observable<LeftDuration>> leftDurationSelector,
			final Func1<? super Right, ? extends Observable<RightDuration>> rightDurationSelector,
			final Func2<? super Left, ? super Observable<? extends Right>, ? extends Result> resultSelector
	) {
		return new Observable<Result>() {
			@Override
			public Closeable register(final Observer<? super Result> observer) {
				final Lock lock = new ReentrantLock(true);
				final HashSet<Left> leftActive = new HashSet<Left>();
				final HashSet<Right> rightActive = new HashSet<Right>();
				final Map<Right, DefaultObservable<Right>> rightGroups = new IdentityHashMap<Right, DefaultObservable<Right>>();

				final CompositeCloseable closeBoth = new CompositeCloseable();

				DefaultObserverEx<Left> o1 = new DefaultObserverEx<Left>(lock, true) {
					/** Relay the inner error to the outer. */
					void innerError(Throwable ex) {
						error(ex);
					}
					@Override
					protected void onClose() {
						super.onClose();
						Closeables.closeSilently(closeBoth);
					}

					@Override
					protected void onError(Throwable ex) {
						observer.error(ex);
					}

					@Override
					protected void onFinish() {
						observer.finish();
					}
					@Override
					protected void onNext(final Left value) {
						leftActive.add(value);

						Observable<LeftDuration> completion = leftDurationSelector.invoke(value);
						final Object token = new Object();
						add(token, completion.register(new DefaultObserver<LeftDuration>(lock, true) {

							@Override
							protected void onClose() {
								remove(token);
							}

							@Override
							protected void onError(Throwable ex) {
								innerError(ex);
							}

							@Override
							protected void onFinish() {
								leftActive.remove(value);
							}
							@Override
							protected void onNext(LeftDuration value) {
								// FIXME NO OP?
							}
						}));
						for (Right r : rightActive) {
							observer.next(resultSelector.invoke(value, rightGroups.get(r)));
						}
					}
				};
				DefaultObserverEx<Right> o2 = new DefaultObserverEx<Right>(lock, true) {
					/** Relay the inner error to the outer. */
					void innerError(Throwable ex) {
						error(ex);
					}
					@Override
					protected void onClose() {
						super.onClose();
						Closeables.closeSilently(closeBoth);
					}
					@Override
					protected void onError(Throwable ex) {
						observer.error(ex);
					}

					@Override
					protected void onFinish() {
						observer.finish();
					}
					@Override
					protected void onNext(final Right value) {
						rightActive.add(value);

						Observable<RightDuration> completion = rightDurationSelector.invoke(value);
						final Object token = new Object();
						add(token, completion.register(new DefaultObserver<RightDuration>(lock, true) {

							@Override
							protected void onClose() {
								remove(token);
								DefaultObservable<Right> rg = rightGroups.remove(value);
								if (rg != null) {
									rg.finish();
								}
							}

							@Override
							protected void onError(Throwable ex) {
								innerError(ex);
							}

							@Override
							protected void onFinish() {
								rightActive.remove(value);
							}
							@Override
							protected void onNext(RightDuration value) {
								// FIXME NO OP?!
							}
						}));
						DefaultObservable<Right> r = rightGroups.get(value);
						if (r == null) {
							r = new DefaultObservable<Right>();
							rightGroups.put(value, r);
						}
						for (Left left : leftActive) {
							observer.next(resultSelector.invoke(left, r));
						}
						r.next(value);
					}
				};
				closeBoth.add(o1);
				closeBoth.add(o2);
				o1.registerWith(left);
				o2.registerWith(right);
				return closeBoth;
			}
		};
	}
	/**
	 * Returns an observable where the submitted condition decides whether the <code>then</code> source is allowed to submit values.
	 * @param <T> the type of the values to observe
	 * @param condition the condition function
	 * @param then the source to use when the condition is true
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> ifThen(
			@Nonnull final Func0<Boolean> condition,
			@Nonnull final Observable<? extends T> then) {
		return ifThen(condition, then, Reactive.<T>never());
	}
	/**
	 * Returns an observable where the submitted condition decides whether the <code>then</code> or <code>orElse</code>
	 * source is allowed to submit values.
	 * FIXME not sure how it should work
	 * @param <T> the type of the values to observe
	 * @param condition the condition function
	 * @param then the source to use when the condition is true
	 * @param orElse the source to use when the condition is false
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> ifThen(
			@Nonnull final Func0<Boolean> condition,
			@Nonnull final Observable<? extends T> then,
			@Nonnull final Observable<? extends T> orElse) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				final Closeable s1 = then.register(new Observer<T>() {

					@Override
					public void error(Throwable ex) {
						if (condition.invoke()) {
							observer.error(ex);
						}
					}

					@Override
					public void finish() {
						if (condition.invoke()) {
							observer.finish();
						}
					}

					@Override
					public void next(T value) {
						if (condition.invoke()) {
							observer.next(value);
						}
					}

				});
				final Closeable s2 = orElse.register(new Observer<T>() {

					@Override
					public void error(Throwable ex) {
						if (!condition.invoke()) {
							observer.error(ex);
						}
					}

					@Override
					public void finish() {
						if (!condition.invoke()) {
							observer.finish();
						}
					}

					@Override
					public void next(T value) {
						if (!condition.invoke()) {
							observer.next(value);
						}
					}

				});

				return Closeables.newCloseable(s1, s2);
			}
		};
	}
	/**
	 * Ignores the next() messages of the source and forwards only the error() and
	 * finish() messages.
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @return the new observable
	 */
	public static <T> Observable<T> ignoreValues(final Observable<? extends T> source) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.finish();
					}

					@Override
					public void next(T value) {
						// ignored
					}

				});
			}
		};
	}
	/**
	 * Invoke a specific action before relaying the Ts to the observable. The <code>action</code> might
	 * have some effect on each individual Ts passing through this filter.
	 * @param <T> the type of the values observed
	 * @param source the source of Ts
	 * @param action the action to invoke on every T
	 * @return the new observable
	 */
	@Nonnull
	public static <T> Observable<T> invoke(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Action1<? super T> action) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.finish();
					}

					@Override
					public void next(T value) {
						action.invoke(value);
						observer.next(value);
					}

				});
			}
		};
	}
	/**
	 * Invoke a specific observer before relaying the Ts, finish() and error() to the observable. The <code>action</code> might
	 * have some effect on each individual Ts passing through this filter.
	 * @param <T> the type of the values observed
	 * @param source the source of Ts
	 * @param observer the observer to invoke before any registered observers are called
	 * @return the new observable
	 */
	@Nonnull
	public static <T> Observable<T> invoke(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Observer<? super T> observer) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> o) {
				return source.register(new Observer<T>() {
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
						o.error(ex);
					}

					@Override
					public void finish() {
						observer.finish();
						o.finish();
					}

					@Override
					public void next(T value) {
						observer.next(value);
						o.next(value);
					}

				});
			}
		};
	}
	/**
	 * Invoke the given callable on the default pool and observe its result via the returned observable.
	 * Any exception thrown by the callable is relayed via the error() message.
	 * @param <T> the return type
	 * @param call the callable
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> invokeAsync(
			@Nonnull final Callable<? extends T> call) {
		return invokeAsync(call, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Invoke the given callable on the given pool and observe its result via the returned observable.
	 * Any exception thrown by the callable is relayed via the error() message.
	 * @param <T> the return type
	 * @param call the callable
	 * @param pool the thread pool
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> invokeAsync(
			@Nonnull final Callable<? extends T> call,
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return pool.schedule(new Runnable() {
					@Override
					public void run() {
						try {
							observer.next(call.call());
							observer.finish();
						} catch (Throwable ex) {
							observer.error(ex);
						}
					}
				});
			}
		};
	}
	/**
	 * Invoke the given callable on the given pool and observe its result via the returned observable.
	 * Any exception thrown by the callable is relayed via the error() message.
	 * @param <T> the return type
	 * @param run the runnable
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> invokeAsync(
			@Nonnull final Runnable run) {
		return invokeAsync(run, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Invoke the given callable on the given pool and observe its result via the returned observable.
	 * Any exception thrown by the callable is relayed via the error() message.
	 * @param <T> the return type
	 * @param run the runnable
	 * @param pool the thread pool
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> invokeAsync(
			@Nonnull final Runnable run,
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return pool.schedule(new Runnable() {
					@Override
					public void run() {
						try {
							run.run();
							observer.finish();
						} catch (Throwable ex) {
							observer.error(ex);
						}
					}
				});
			}
		};
	}
	/**
	 * Invoke the given callable on the given pool and observe its result via the returned observable.
	 * Any exception thrown by the callable is relayed via the error() message.
	 * @param <T> the return type
	 * @param run the runnable
	 * @param defaultValue the value to return when the Runnable completes
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> invokeAsync(
			@Nonnull final Runnable run,
			final T defaultValue) {
		return invokeAsync(run, defaultValue, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Invoke the given callable on the given pool and observe its result via the returned observable.
	 * Any exception thrown by the callable is relayed via the error() message.
	 * @param <T> the return type
	 * @param run the runnable
	 * @param pool the thread pool
	 * @param defaultValue the value to return by default
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> invokeAsync(
			@Nonnull final Runnable run,
			final T defaultValue,
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return pool.schedule(new Runnable() {
					@Override
					public void run() {
						try {
							run.run();
							observer.next(defaultValue);
							observer.finish();
						} catch (Throwable ex) {
							observer.error(ex);
						}
					}
				});
			}
		};
	}
	/**
	 * Signals true if the source observable fires finish() without ever firing next().
	 * This means once the next() is fired, the resulting observer will return early.
	 * @param source the source observable of any type
	 * @return the observer
	 */
	@Nonnull
	public static Observable<Boolean> isEmpty(
			@Nonnull final Observable<?> source) {
		return new Observable<Boolean>() {
			@Override
			public Closeable register(final Observer<? super Boolean> observer) {
				return source.register(new Observer<Object>() {
					/** We already determined the answer? */
					boolean done;
					@Override
					public void error(Throwable ex) {
						if (!done) {
							observer.error(ex);
						}
					}

					@Override
					public void finish() {
						if (!done) {
							done = true;
							observer.next(false);
							observer.finish();
						}
					}

					@Override
					public void next(Object value) {
						if (!done) {
							done = true;
							observer.next(true);
							observer.finish();
						}
					}

				});
			}
		};
	}
	/**
	 * Returns an observable which correlates two streams of values based on
	 * their time when they overlapped.
	 * <p>The difference between this operator and the groupJoin operator
	 * is that in this case, the result selector takes the concrete left and
	 * right elements, whereas the groupJoin associates an observable of rights
	 * for each left.</p>
	 * FIXME not sure how to implement it
	 * @param <Left> the element type of the left stream
	 * @param <Right> the element type of the right stream
	 * @param <LeftDuration> the overlapping duration indicator for the left stream (e.g., the event when it leaves)
	 * @param <RightDuration> the overlapping duration indicator for the right stream (e.g., the event when it leaves)
	 * @param <Result> the type of the grouping based on the coincidence.
	 * @param left the left source of elements
	 * @param right the right source of elements
	 * @param leftDurationSelector the duration selector for a left element
	 * @param rightDurationSelector the duration selector for a right element
	 * @param resultSelector the selector which will produce the output value
	 * @return the new observable
	 * @see #groupJoin(Observable, Observable, Func1, Func1, Func2)
	 */
	public static <Left, Right, LeftDuration, RightDuration, Result> Observable<Result> join(
			final Observable<? extends Left> left,
			final Observable<? extends Right> right,
			final Func1<? super Left, ? extends Observable<LeftDuration>> leftDurationSelector,
			final Func1<? super Right, ? extends Observable<RightDuration>> rightDurationSelector,
			final Func2<? super Left, ? super Right, ? extends Result> resultSelector
	) {
		return new Observable<Result>() {
			@Override
			public Closeable register(final Observer<? super Result> observer) {
				final Lock lock = new ReentrantLock(true);
				final HashSet<Left> leftActive = new HashSet<Left>();
				final HashSet<Right> rightActive = new HashSet<Right>();

				final CompositeCloseable closeBoth = new CompositeCloseable();

				DefaultObserverEx<Left> o1 = new DefaultObserverEx<Left>(lock, true) {
					/** Relay the inner error to the outer. */
					void innerError(Throwable ex) {
						error(ex);
					}
					@Override
					protected void onClose() {
						super.onClose();
						Closeables.closeSilently(closeBoth);
					}

					@Override
					protected void onError(Throwable ex) {
						observer.error(ex);
					}

					@Override
					protected void onFinish() {
						observer.finish();
					}
					@Override
					protected void onNext(final Left value) {
						leftActive.add(value);

						Observable<LeftDuration> completion = leftDurationSelector.invoke(value);
						final Object token = new Object();
						add(token, completion.register(new DefaultObserver<LeftDuration>(lock, true) {

							@Override
							protected void onClose() {
								remove(token);
							}

							@Override
							protected void onError(Throwable ex) {
								innerError(ex);
							}

							@Override
							protected void onFinish() {
								leftActive.remove(value);
							}
							@Override
							protected void onNext(LeftDuration value) {
								// NO OP?
							}
						}));
						for (Right r : rightActive) {
							observer.next(resultSelector.invoke(value, r));
						}
					}
				};
				DefaultObserverEx<Right> o2 = new DefaultObserverEx<Right>(lock, true) {
					/** Relay the inner error to the outer. */
					void innerError(Throwable ex) {
						error(ex);
					}
					@Override
					protected void onClose() {
						super.onClose();
						Closeables.closeSilently(closeBoth);
					}
					@Override
					protected void onError(Throwable ex) {
						observer.error(ex);
					}

					@Override
					protected void onFinish() {
						observer.finish();
					}
					@Override
					protected void onNext(final Right value) {
						rightActive.add(value);

						Observable<RightDuration> completion = rightDurationSelector.invoke(value);
						final Object token = new Object();
						add(token, completion.register(new DefaultObserver<RightDuration>(lock, true) {

							@Override
							protected void onClose() {
								remove(token);
							}

							@Override
							protected void onError(Throwable ex) {
								innerError(ex);
							}

							@Override
							protected void onFinish() {
								rightActive.remove(value);
							}
							@Override
							protected void onNext(RightDuration value) {
								// NO OP?!
							}
						}));
						for (Left left : leftActive) {
							observer.next(resultSelector.invoke(left, value));
						}
					}
				};
				closeBoth.add(o1, o2);
				o1.registerWith(left);
				o2.registerWith(right);
				return closeBoth;
			}
		};
	}
	/**
	 * Returns the last element of the source observable or throws
	 * NoSuchElementException if the source is empty.
	 * <p>Exception semantics: the exceptions thrown by the source are ignored and treated
	 * as termination signals.</p>
	 * @param <T> the type of the elements
	 * @param source the source of Ts
	 * @return the last element
	 */
	@Nonnull
	public static <T> T last(
			@Nonnull final Observable<? extends T> source) {
		final LinkedBlockingQueue<Option<T>> queue = new LinkedBlockingQueue<Option<T>>();
		Closeable c = source.register(new Observer<T>() {
			/** The current value. */
			T current;
			/** Are we the first? */
			boolean first = true;
			@Override
			public void error(Throwable ex) {
				queue.add(Option.<T>none());
			}

			@Override
			public void finish() {
				if (first) {
					queue.add(Option.<T>none());
				} else {
					queue.add(Option.some(current));
				}
			}

			@Override
			public void next(T value) {
				first = false;
				current = value;
			}

		});
		try {
			Option<T> value = queue.take();
			c.close();
			if (value == Option.none()) {
				throw new NoSuchElementException();
			}
			return value.value();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	/**
	 * Returns an iterable which returns values on a momentary basis from the
	 * source. Useful when source produces values at different rate than the consumer takes it.
	 * The iterable.next() call might block until the first value becomes available or something else happens in the observable
	 * FIXME not sure where the observer should run
	 * @param <T> the type of the values
	 * @param source the source
	 * @return the iterable
	 */
	@Nonnull
	public static <T> Iterable<T> latest(
			@Nonnull final Observable<? extends T> source) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				final AtomicBoolean complete = new AtomicBoolean();
				final CountDownLatch first = new CountDownLatch(1);
				final AtomicBoolean hasValue = new AtomicBoolean();
				final AtomicReference<T> current = new AtomicReference<T>();
				final Closeable c = source.register(new Observer<T>() {
					/** Set the has value once. */
					boolean once = true;
					@Override
					public void error(Throwable ex) {
						complete.set(true);
						first.countDown();
					}

					@Override
					public void finish() {
						complete.set(true);
						first.countDown();
					}

					@Override
					public void next(T value) {
						if (once) {
							once = false;
							hasValue.set(true);
						}
						current.set(value);
						first.countDown();
					}

				});
				return new Iterator<T>() {
					@Override
					protected void finalize() throws Throwable {
						c.close();
					}

					@Override
					public boolean hasNext() {
						try {
							first.await();
						} catch (InterruptedException e) {
							throw new RuntimeException(e);
						}
						return !complete.get() && hasValue.get();
					}

					@Override
					public T next() {
						if (hasValue.get()) {
							return current.get();
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
	 * Returns an observable which calls the given selector with the given value
	 * when a client wants to register with it. The client then
	 * gets registered with the observable returned by the function.
	 * E.g., <code>return selector.invoke(value).register(observer)</code> in the outer register method.
	 * @param <T> the selection key type
	 * @param <U> the result type
	 * @param value the value to pass to the selector function
	 * @param selector the selector function
	 * @return a new observable
	 */
	public static <T, U> Observable<U> let(
			final T value,
			final Func1<? super T, ? extends Observable<U>> selector) {
		return new Observable<U>() {
			@Override
			public Closeable register(Observer<? super U> observer) {
				return selector.invoke(value).register(observer);
			}
		};
	}
	/**
	 * For each of the source elements, creates a view of the source starting with the given
	 * element and calls the selector function. The function's return observable is then merged
	 * into a single observable sequence.<p>
	 * For example, a source sequence of (1, 2, 3) will create three function calls with (1, 2, 3), (2, 3) and (3) as a content.
	 * @param <T> the source element type
	 * @param <U> the result element type
	 * @param source the source of Ts
	 * @param selector the selector function
	 * @return the new observable
	 */
	public static <T, U> Observable<U> manySelect(
			final Observable<? extends T> source,
			final Func1<? super Observable<T>, ? extends Observable<U>> selector
	) {
		return merge(select(source, new Func1<T, Observable<U>>() {
			/** The skip position. */
			int counter;

			@Override
			public Observable<U> invoke(T param1) {
				int i = counter++;
				return selector.invoke(skip(source, i));
			}

		}));
	}
	/**
	 * For each value of the source observable, it creates a view starting from that value into the source
	 * and calls the given selector function asynchronously on the given scheduler.
	 * The result of that computation is then transmitted to the observer.
	 * <p>It is sometimes called the comonadic bind operator and compared to the ContinueWith
	 * semantics.</p>
	 * @param <T> the source element type
	 * @param <U> the result element type
	 * @param source the source of Ts
	 * @param selector the selector that extracts an U from the series of Ts.
	 * @param scheduler the scheduler where the extracted U will be emitted from.
	 * @return the new observable.
	 */
	public static <T, U> Observable<U> manySelect(
			final Observable<? extends T> source,
			final Func1<? super Observable<T>, ? extends U> selector,
			final Scheduler scheduler) {
		return new Observable<U>() {
			@Override
			public Closeable register(final Observer<? super U> observer) {
				final AtomicInteger wip = new AtomicInteger(1);
				Closeable c = source.register(new DefaultObserverEx<T>(true) {
					/** The skip position. */
					int counter;
					@Override
					protected void onError(Throwable ex) {
						observer.error(ex);
						close();
					}

					@Override
					protected void onFinish() {
						if (wip.decrementAndGet() == 0) {
							observer.finish();
						}
					}

					@Override
					protected void onNext(T value) {
						final Observable<T> ot = skip(source, counter);
						wip.incrementAndGet();
						add(counter, scheduler.schedule(new Runnable() {
							@Override
							public void run() {
								observer.next(selector.invoke(ot));
								if (wip.decrementAndGet() == 0) {
									observer.finish();
								}
							}
						}));
						counter++;
					}

				});
				return c;
			}
		};
	}
	/**
	 * Uses the selector function on the given source observable to extract a single
	 * value and send this value to the registered observer.
	 * It is sometimes called the comonadic bind operator and compared to the ContinueWith
	 * semantics.
	 * The default scheduler is used to emit the output value
	 * FIXME not sure what it should do
	 * @param <T> the source element type
	 * @param <U> the result element type
	 * @param source the source of Ts
	 * @param selector the selector that extracts an U from the series of Ts.
	 * @return the new observable.
	 */
	public static <T, U> Observable<U> manySelect0(
			final Observable<? extends T> source,
			final Func1<? super Observable<T>, ? extends U> selector) {
		return manySelect(source, selector, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Returns an observable which converts all messages to an <code>Option</code> value.
	 * The returned observable terminates once error or finish is received.
	 * Its dual is the <code>dematerialize</code> method.
	 * <p>Note, before 0.97, this materialize sequence never terminated, but
	 * consulting with Rx, I changed its behavior to terminating. For
	 * unterminating version (usable for testing), use the materializeForever method.
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @return the new observable
	 * @see #materializeForever(Observable)
	 * @see #dematerialize(Observable)
	 */
	@Nonnull
	public static <T> Observable<Option<T>> materialize(
			@Nonnull final Observable<? extends T> source) {
		return new Observable<Option<T>>() {
			@Override
			public Closeable register(final Observer<? super Option<T>> observer) {
				return source.register(new Observer<T>() {
					@Override
					public void error(Throwable ex) {
						observer.next(Option.<T>error(ex));
						observer.finish();
					}

					@Override
					public void finish() {
						observer.next(Option.<T>none());
						observer.finish();
					}

					@Override
					public void next(T value) {
						observer.next(Option.some(value));
					}
				});
			}
		};
	}
	/**
	 * Returns an observable which converts all messages to an <code>Option</code> value.
	 * The returned observable never terminates on its own, providing an infinte stream of events.
	 * Its dual is the <code>dematerialize</code> method.
	 * <p>For terminating version, see the materialize method.</p>
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @return the new observable
	 * @see #materialize(Observable)
	 * @see #dematerialize(Observable)
	 */
	@Nonnull
	public static <T> Observable<Option<T>> materializeForever(
			@Nonnull final Observable<? extends T> source) {
		return new Observable<Option<T>>() {
			@Override
			public Closeable register(final Observer<? super Option<T>> observer) {
				return source.register(new Observer<T>() {
					@Override
					public void error(Throwable ex) {
						observer.next(Option.<T>error(ex));
					}

					@Override
					public void finish() {
						observer.next(Option.<T>none());
					}

					@Override
					public void next(T value) {
						observer.next(Option.some(value));
					}

				});
			}
		};
	}
	/**
	 * Returns the maximum value encountered in the source observable once it sends finish().
	 * @param <T> the element type which must be comparable to itself
	 * @param source the source of integers
	 * @return the the maximum value
	 */
	@Nonnull
	public static <T extends Comparable<? super T>> Observable<T> max(
			@Nonnull final Observable<? extends T> source) {
		return aggregate(source, Functions.<T>max(), Functions.<T, Integer>identityFirst());
	}
	/**
	 * Returns the maximum value encountered in the source observable once it sends finish().
	 * @param <T> the element type
	 * @param source the source of integers
	 * @param comparator the comparator to decide the relation of values
	 * @return the the maximum value
	 * @see Functions#asComparator(Func2)
	 */
	@Nonnull
	public static <T> Observable<T> max(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Comparator<? super T> comparator) {
		return aggregate(source, Functions.<T>max(comparator), Functions.<T, Integer>identityFirst());
		}
	/**
	 * Returns an observable which provides with the list of <code>T</code>s which had their keys as maximums.
	 * The returned observer may finish() if the source sends finish() without any next().
	 * The generated list is modifiable.
	 * @param <T> the type of elements
	 * @param <Key> the key type, which must be comparable to itself
	 * @param source the source of <code>T</code>s
	 * @param keyExtractor the key extractor to produce <code>Key</code>s from <code>T</code>s.
	 * @return the observable for the maximum keyed Ts
	 */
	@Nonnull
	public static <T, Key extends Comparable<? super Key>> Observable<List<T>> maxBy(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends Key> keyExtractor) {
		return minMax(source, keyExtractor, Functions.<Key>comparator(), true);
	}
	/**
	 * Returns an observable which provides with the list of <code>T</code>s which had their keys as maximums.
	 * The returned observer may finish() if the source sends finish() without any next().
	 * The generated list is modifiable.
	 * @param <T> the type of elements
	 * @param <Key> the key type
	 * @param source the source of <code>T</code>s
	 * @param keyExtractor the key extractor to produce <code>Key</code>s from <code>T</code>s.
	 * @param keyComparator the comparator for the keys
	 * @return the observable for the maximum keyed Ts
	 */
	@Nonnull
	public static <T, Key> Observable<List<T>> maxBy(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends Key> keyExtractor,
			@Nonnull final Comparator<? super Key> keyComparator) {
		return minMax(source, keyExtractor, keyComparator, true);
	}
	/**
	 * Combines the notifications of all sources. 
	 * The resulting stream of Ts might come from any of the sources.
	 * @param <T> the type of the values
	 * @param sources the list of sources
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> merge(
			@Nonnull final Iterable<? extends Observable<? extends T>> sources) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				final CompositeCloseable closeables = new CompositeCloseable();
				
				List<Observable<? extends T>> sourcesList = new ArrayList<Observable<? extends T>>();
				for (Observable<? extends T> os : sources) {
					sourcesList.add(os);
				}
				final AtomicInteger wip = new AtomicInteger(sourcesList.size() + 1);
				final List<DefaultObserverEx<T>> observers = new ArrayList<DefaultObserverEx<T>>();
				final Lock lock = new ReentrantLock();
				for (int i = 0; i < sourcesList.size(); i++) {
					final int j = i;
					DefaultObserverEx<T> obs = new DefaultObserverEx<T>(lock, true) {
						@Override
						public void onError(Throwable ex) {
							observer.error(ex);
							for (int k = 0; k < observers.size(); k++) {
								if (k != j) {
									observers.get(k).close();
								}
							}
						}

						@Override
						public void onFinish() {
							if (wip.decrementAndGet() == 0) {
								observer.finish();
							}
						}

						@Override
						public void onNext(T value) {
							observer.next(value);
						}
					};
					observers.add(obs);
					closeables.add(obs);
				}
				for (int i = 0; i < observers.size(); i++) {
					DefaultObserverEx<T> obs = observers.get(i);
					Observable<? extends T> ov = sourcesList.get(i);
					obs.registerWith(ov);
				}
				if (wip.decrementAndGet() == 0) {
					observer.finish();
				}
				return closeables;
			}
		};
	}
	/**
	 * Merge the dynamic sequence of observables of T.
	 * <p><b>Exception semantics:</b> if the sources or any inner observer signals an
	 * error, the outer observable will signal that error and all active source observers are terminated.</p>
	 * @param <T> the element type
	 * @param sources the observable sequence of observable sequence of Ts
	 * @return the new observable
	 */
	public static <T> Observable<T> merge(
			final Observable<? extends Observable<? extends T>> sources) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				final AtomicInteger wip = new AtomicInteger(1);
				DefaultObserverEx<Observable<? extends T>> obs = new DefaultObserverEx<Observable<? extends T>>(false) {
					/** Signal finish if the sources and inner observables have all finished. */
					void ifDoneFinish() {
						if (wip.decrementAndGet() == 0) {
							observer.finish();
							close();
						}
					}
					/**
					 * The inner exception to forward.
					 * @param ex the exception
					 */
					void innerError(Throwable ex) {
						error(ex);
					}
					@Override
					protected void onError(Throwable ex) {
						observer.error(ex);
					}
					@Override
					protected void onFinish() {
						ifDoneFinish();
					}

					@Override
					protected void onNext(Observable<? extends T> value) {
						final Object token = new Object();
						wip.incrementAndGet();
						add(token, value.register(new DefaultObserver<T>(lock, true) {
							@Override
							public void onError(Throwable ex) {
								innerError(ex);
							}

							@Override
							public void onFinish() {
								remove(token);
								ifDoneFinish();
							}

							@Override
							public void onNext(T value) {
								observer.next(value);
							}

						}));
					}

				};
				return obs.registerWith(sources);
			}
		};
	}
	/**
	 * Merge the events of two observable sequences.
	 * @param <T> the type of the elements
	 * @param first the first observable
	 * @param second the second observable
	 * @return the merged observable
	 */
	@Nonnull
	public static <T> Observable<T> merge(
			@Nonnull Observable<? extends T> first,
			@Nonnull Observable<? extends T> second) {
		List<Observable<? extends T>> list = new ArrayList<Observable<? extends T>>();
		list.add(first);
		list.add(second);
		return merge(list);
	}
	/**
	 * Returns the minimum value encountered in the source observable once it sends finish().
	 * @param <T> the element type which must be comparable to itself
	 * @param source the source of integers
	 * @return the the minimum value
	 */
	@Nonnull
	public static <T extends Comparable<? super T>> Observable<T> min(
			@Nonnull final Observable<? extends T> source) {
		return aggregate(source, Functions.<T>min(), Functions.<T, Integer>identityFirst());
	}
	/**
	 * Returns the minimum value encountered in the source observable once it sends finish().
	 * @param <T> the element type
	 * @param source the source of integers
	 * @param comparator the comparator to decide the relation of values
	 * @return the the minimum value
	 * @see Functions#asComparator(Func2)
	 */
	@Nonnull
	public static <T> Observable<T> min(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Comparator<? super T> comparator) {
		return aggregate(source, Functions.<T>min(comparator), Functions.<T, Integer>identityFirst());
	}
	/**
	 * Returns an observable which provides with the list of <code>T</code>s which had their keys as minimums.
	 * The returned observer may finish() if the source sends finish() without any next().
	 * The generated list is modifiable.
	 * @param <T> the type of elements
	 * @param <Key> the key type, which must be comparable to itself
	 * @param source the source of <code>T</code>s
	 * @param keyExtractor the key extractor to produce <code>Key</code>s from <code>T</code>s.
	 * @return the observable for the minimum keyed Ts
	 */
	@Nonnull
	public static <T, Key extends Comparable<? super Key>> Observable<List<T>> minBy(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends Key> keyExtractor) {
		return minMax(source, keyExtractor, Functions.<Key>comparator(), false);
	}
	/**
	 * Returns an observable which provides with the list of <code>T</code>s which had their keys as minimums.
	 * The returned observer may finish() if the source sends finish() without any next().
	 * The generated list is modifiable.
	 * @param <T> the type of elements
	 * @param <Key> the key type
	 * @param source the source of <code>T</code>s
	 * @param keyExtractor the key extractor to produce <code>Key</code>s from <code>T</code>s.
	 * @param keyComparator the comparator for the keys
	 * @return the observable for the minimum keyed Ts
	 * @see Functions#asComparator(Func2)
	 */
	@Nonnull
	public static <T, Key> Observable<List<T>> minBy(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends Key> keyExtractor,
			@Nonnull final Comparator<? super Key> keyComparator) {
		return minMax(source, keyExtractor, keyComparator, false);
	}
	/**
	 * Returns an observable which provides with the list of <code>T</code>s which had their keys as maximums.
	 * The returned observer may finish() if the source sends finish() without any next().
	 * The generated list is modifiable.
	 * @param <T> the type of elements
	 * @param <Key> the key type
	 * @param source the source of <code>T</code>s
	 * @param keyExtractor the key extractor to produce <code>Key</code>s from <code>T</code>s.
	 * @param keyComparator the comparator for the keys
	 * @param max compute the maximums?
	 * @return the observable for the maximum keyed Ts
	 * @see Functions#asComparator(Func2)
	 */
	@Nonnull
	public static <T, Key> Observable<List<T>> minMax(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends Key> keyExtractor,
			@Nonnull final Comparator<? super Key> keyComparator,
			@Nonnull final boolean max
	) {
		return new Observable<List<T>>() {
			@Override
			public Closeable register(final Observer<? super List<T>> observer) {
				return source.register(new Observer<T>() {
					/** The current collection for the minimum of Ts. */
					List<T> collect;
					/** The current minimum value. */
					Key maxKey;
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						if (collect != null) {
							observer.next(collect);
						}
						observer.finish();
					}

					@Override
					public void next(T value) {
						Key key = keyExtractor.invoke(value);
						if (collect == null) {
							maxKey = key;
							collect = new ArrayList<T>();
							collect.add(value);
						} else {
							int order = keyComparator.compare(maxKey, key);
							if (order == 0) {
								collect.add(value);
							} else
							if (max ^ (order > 0)) {
								maxKey = key;
								collect = new ArrayList<T>();
								collect.add(value);
							}
						}
					}

				});
			}
		};
	}
	/**
	 * Samples the latest T value coming from the source observable or the initial
	 * value when no messages arrived so far. If the producer and consumer run
	 * on different speeds, the consumer might receive the same value multiple times.
	 * The iterable sequence terminates if the source finishes or returns an error.
	 * <p>The returned iterator throws <code>UnsupportedOperationException</code> for its <code>remove()</code> method.</p>
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @param initialValue the initial value to return until the source actually produces something.
	 * @return the iterable
	 */
	public static <T> Iterable<T> mostRecent(final Observable<? extends T> source, final T initialValue) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				final AtomicReference<Option<T>> latest = new AtomicReference<Option<T>>(Option.some(initialValue));
				final Closeable c = source.register(new Observer<T>() {

					@Override
					public void error(Throwable ex) {
						latest.set(Option.<T>error(ex));
					}

					@Override
					public void finish() {
						latest.set(Option.<T>none());
					}

					@Override
					public void next(T value) {
						latest.set(Option.some(value));
					}

				});
				return new Iterator<T>() {
					@Override
					protected void finalize() throws Throwable {
						Closeables.closeSilently(c);
						super.finalize();
					}

					@Override
					public boolean hasNext() {
						return !Option.isNone(latest.get());
					}

					@Override
					public T next() {
						if (hasNext()) {
							Option<T> o = latest.get();
							// if the latest value is error, emit it only once, then
							// do if the source simply terminated
							if (Option.isError(o)) {
								latest.set(Option.<T>none());
								return o.value();
							}
							return o.value();
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
	 * Multicasts the source sequence through the supplied subject by allowing
	 * connection and disconnection from the source without the need to reconnect
	 * any observers to the returned observable.
	 * <p>Scenario: having a continuous source, the event sequence can be interrupted
	 * and reestablished at any time, during the time the registered observers won't
	 * receive any events.</p>
	 * @param <T> the source element type
	 * @param <U> the type of the elements the observers will receive
	 * @param source the source observable
	 * @param subject the observer that receives the Ts and at the same time, the Observable that registers
	 * observers for the Us.
	 * @return the new connectable observable
	 */
	@Nonnull
	public static <T, U> ConnectableObservable<U> multicast(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Subject<? super T, ? extends U> subject) {
		return new DefaultConnectableObservable<T, U>(source, subject);
	}
	/**
	 * Multicasts the source events through the subject instantiated via
	 * the subjectSelector. Each subscription to this sequence
	 * causes a separate multicast invocation.
	 * @param <T> the element type of the source
	 * @param <U> the element type of the intermediate subject's output
	 * @param <V> the result type 
	 * @param source the source sequence to be multicasted
	 * @param subjectSelector the factory function to create an intermediate
	 * subject which through the source values will be multicasted.
	 * @param selector the factory method to use the multicasted subject and enforce some policies on it
	 * @return the observable sequence that contains all elements of the multicasted functions
	 * @since 0.97
	 */
	@Nonnull
	public static <T, U, V> Observable<V> multicast(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func0<? extends Subject<? super T, ? extends U>> subjectSelector,
			@Nonnull final Func1<? super Observable<? extends U>, ? extends Observable<? extends V>> selector
			) {
		return new Observable<V>() {
			@Override
			@Nonnull
			public Closeable register(Observer<? super V> observer) {
				Subject<? super T, ? extends U> subject = subjectSelector.invoke();
				ConnectableObservable<U> connectable = new DefaultConnectableObservable<T, U>(source, subject);
				Observable<? extends V> observable = selector.invoke(connectable);
				
				Closeable c = DefaultObserverEx.wrap(observer).registerWith(observable);
				Closeable conn = connectable.connect();
				
				return new CompositeCloseable(c, conn);
			}
		};
	}
	/**
	 * Returns an observable which never fires.
	 * @param <T> the type of the observable, irrelevant
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> never() {
		return new Observable<T>() {
			@Override
			public Closeable register(Observer<? super T> observer) {
				return Functions.EMPTY_CLOSEABLE;
			}
		};
	}
	/**
	 * Creates a new nullsafe equality comparison function where if two Option.Some&lt;T>
	 * meet, their values are compared by using the supplied objectComparer.
	 * @param <T> the element type
	 * @param objectComparer the objectComparer.
	 * @return the new comparison function
	 */
	protected static <T> Func2<? super Option<T>, ? super Option<T>, Boolean> newOptionComparer(
			@Nonnull final Func2<? super T, ? super T, Boolean> objectComparer) {
		return new Func2<Option<T>, Option<T>, Boolean>() {
			@Override
			public Boolean invoke(Option<T> param1, Option<T> param2) {
				if (Option.isSome(param1) && Option.isSome(param2)) {
					return objectComparer.invoke(param1.value(), param2.value());
				}
				return param1 == param2 || (param1 != null && param1.equals(param2));
			}
		};
	}
	/**
	 * Returns an iterable which returns a single element from the
	 * given source then terminates. It blocks the current thread.
	 * <p>For hot observables, this
	 * will be the first element they produce, for cold observables,
	 * this will be the next value (e.g., the next mouse move event).</p>
	 * <p><b>Exception semantics:</b> The <code>Iterator.next()</code> will rethrow the exception.</p>
	 * <p><b>Completion semantics:</b> If the source completes instantly, the iterator completes as empty.</p>
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code> for its
	 * <code>remove()</code> method.
	 * @param <T> the element type
	 * @param source the source of elements
	 * @return the iterable
	 */
	public static <T> Iterable<T> next(final Observable<? extends T> source) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				final BlockingQueue<Option<T>> element = new LinkedBlockingQueue<Option<T>>();
				final Closeable c = source.register(new DefaultObserver<T>(true) {

					@Override
					protected void onError(Throwable ex) {
						element.add(Option.<T>error(ex));
					}

					@Override
					protected void onFinish() {
						element.add(Option.<T>none());
					}

					@Override
					protected void onNext(T value) {
						element.add(Option.some(value));
						close();
					}

				});
				return new Iterator<T>() {
					/** The completion marker. */
					boolean done;
					/** The single element look-ahead. */
					final SingleContainer<Option<T>> peek = new SingleContainer<Option<T>>();
					@Override
					public boolean hasNext() {
						if (!done) {
							if (peek.isEmpty()) {
								try {
									Option<T> e = element.take();
									if (!Option.isNone(e)) {
										peek.add(e);
									}
								} catch (InterruptedException ex) {
									peek.add(Option.<T>error(ex));
								}
								done = true;
								Closeables.closeSilently(c);
							}
						}
						return !peek.isEmpty() && !done;
					}
					@Override
					public T next() {
						if (hasNext()) {
							return element.peek().value();
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
	 * Wrap the given observable object in a way that any of its observers receive callbacks on
	 * the given thread pool.
	 * @param <T> the type of the objects to observe
	 * @param source the original observable
	 * @param pool the target observable
	 * @return the new observable
	 */
	@Nonnull
	public static <T> Observable<T> observeOn(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {

				DefaultObserverEx<T> obs = new DefaultObserverEx<T>(true) {
					/** The single lane executor. */
					final SingleLaneExecutor<Runnable> run = new SingleLaneExecutor<Runnable>(pool,
						new Action1<Runnable>() {
							@Override
							public void invoke(Runnable value) {
								value.run();
							}
						}
					);
					@Override
					public void onError(final Throwable ex) {
						run.add(new Runnable() {
							@Override
							public void run() {
								observer.error(ex);
							}
						});
					}

					@Override
					public void onFinish() {
						run.add(new Runnable() {
							@Override
							public void run() {
								observer.finish();
							}
						});
					}

					@Override
					public void onNext(final T value) {
						run.add(new Runnable() {
							@Override
							public void run() {
								observer.next(value);
							}
						});
					}
				};
				return obs.registerWith(source);
			}
		};
	}
	/**
	 * Returns an Observable which traverses the entire
	 * source Observable and creates an ordered list
	 * of elements. Once the source Observable completes,
	 * the elements are streamed to the output.
	 * @param <T> the source element type, must be self comparable
	 * @param source the source of Ts
	 * @return the new iterable
	 */
	@Nonnull
	public static <T extends Comparable<? super T>> Observable<T> orderBy(
			@Nonnull final Observable<? extends T> source
			) {
		return orderBy(source, Functions.<T>identity(), Functions.<T>comparator());
	}
	/**
	 * Returns an Observable which traverses the entire
	 * source Observable and creates an ordered list
	 * of elements. Once the source Observable completes,
	 * the elements are streamed to the output.
	 * @param <T> the source element type, must be self comparable
	 * @param source the source of Ts
	 * @param comparator the value comparator
	 * @return the new iterable
	 */
	@Nonnull
	public static <T> Observable<T> orderBy(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Comparator<? super T> comparator
			) {
		return orderBy(source, Functions.<T>identity(), comparator);
	}
	/**
	 * Returns an Observable which traverses the entire
	 * source Observable and creates an ordered list
	 * of elements. Once the source Observable completes,
	 * the elements are streamed to the output.
	 * @param <T> the source element type
	 * @param <U> the key type for the ordering, must be self comparable
	 * @param source the source of Ts
	 * @param keySelector the key selector for comparison
	 * @return the new iterable
	 */
	@Nonnull
	public static <T, U extends Comparable<? super U>> Observable<T> orderBy(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends U> keySelector
			) {
		return orderBy(source, keySelector, Functions.<U>comparator());
	}
	/**
	 * Returns an Observable which traverses the entire
	 * source Observable and creates an ordered list
	 * of elements. Once the source iterator completes,
	 * the elements are streamed to the output.
	 * <p>Note that it buffers the elements of <code>source</code> until it
	 * signals finish.</p>
	 * <p><b>Exception semantics:</b> the exception is relayed and no ordering is performed.</p>
	 * <p><b>Completion semantics:</b> the output terminates when the source terminates and the sorted values are all submitted.</p>
	 * @param <T> the source element type
	 * @param <U> the key type for the ordering
	 * @param source the source of Ts
	 * @param keySelector the key selector for comparison
	 * @param keyComparator the key comparator function
	 * @return the new iterable
	 */
	@Nonnull
	public static <T, U> Observable<T> orderBy(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends U> keySelector,
			@Nonnull final Comparator<? super U> keyComparator
			) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** The buffer. */
					final List<T> buffer = new ArrayList<T>();

					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						Collections.sort(buffer, new Comparator<T>() {
							@Override
							public int compare(T o1, T o2) {
								return keyComparator.compare(keySelector.invoke(o1), keySelector.invoke(o2));
							}
						});
						for (T t : buffer) {
							observer.next(t);
						}
						observer.finish();
					}

					@Override
					public void next(T value) {
						buffer.add(value);
					}
				});
			}
		};
	}
	/**
	 * Returns an observable which shares all registration to the source observable and
	 * each observer will only see the last notification.
	 * <p>Basically a replay with buffer size 1.</p>
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @return the observable
	 */
	public static <T> Observable<T> prune(
			final Observable<? extends T> source
	) {
		return replay(source, 1);
	}
	/**
	 * Returns an observable which shares all registration to the source observable and
	 * each observer will only see the last notification.
	 * <p>Basically a replay with buffer size 1.</p>
	 * @param <T> the source element type
	 * @param <U> the return element type
	 * @param source the source of Ts
	 * @param selector the output stream selector
	 * @return the observable
	 */
	public static <T, U> Observable<U> prune(
			final Observable<? extends T> source,
			final Func1<? super Observable<? extends T>, ? extends Observable<U>> selector
	) {
		return replay(source, selector, 1);
	}
	/**
	 * Returns an observable which shares all registration to the source observable and
	 * each observer will only see the last notification.
	 * <p>Basically a replay with buffer size 1.</p>
	 * @param <T> the source element type
	 * @param <U> the return element type
	 * @param source the source of Ts
	 * @param selector the output stream selector
	 * @param scheduler the scheduler for replaying the single value
	 * @return the observable
	 */
	public static <T, U> Observable<U> prune(
			final Observable<? extends T> source,
			final Func1<? super Observable<? extends T>, ? extends Observable<U>> selector,
			final Scheduler scheduler
	) {
		return replay(source, selector, 1, scheduler);
	}
	/**
	 * Returns an observable which shares all registration to the source observable and
	 * each observer will only see the last notification.
	 * <p>Basically a replay with buffer size 1.</p>
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @param scheduler the scheduler for replaying the single value
	 * @return the observable
	 */
	public static <T> Observable<T> prune(
			final Observable<? extends T> source,
			final Scheduler scheduler
	) {
		return replay(source, 1, scheduler);
	}
	/**
	 * Returns an observable which shares a single subscription to the underlying source.
	 * <p>This is a specialization of the multicast operator with a simple forwarding subject.</p>
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @return the new observable
	 */
	@Nonnull
	public static <T> ConnectableObservable<T> publish(
			@Nonnull final Observable<? extends T> source
			) {
		return multicast(source, Subjects.<T>newSubject());
	}
	/**
	 * Returns an observable which shares a single subscription to the underlying source
	 * and starts with with the initial value.
	 * <p>Registering parties will immediately receive the initial value but the subsequent
	 * values depend upon wether the observer is connected or not.</p>
	 * <p>This is a specialization of the multicast operator with a simple forwarding subject.</p>
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param initialValue the initial value the observers will receive when registering
	 * @return the new observable
	 */
	@Nonnull
	public static <T> ConnectableObservable<T> publish(
			@Nonnull final Observable<? extends T> source,
			final T initialValue
			) {
		return multicast(source, new Subject<T, T>() {
			/** The observable handling the registrations. */
			final DefaultObservable<T> obs = new DefaultObservable<T>(); 
			@Override
			public void next(T value) {
				obs.next(value);
			}

			@Override
			public void error(Throwable ex) {
				obs.error(ex);
			}

			@Override
			public void finish() {
				obs.finish();
			}

			@Override
			@Nonnull
			public Closeable register(Observer<? super T> observer) {
				observer.next(initialValue);
				return obs.register(observer);
			}
			
		});
	}
	/**
	 * Returns an observable sequence which is the result of
	 * invoking the selector on a connectable observable sequence
	 * that shares a single subscription with the underlying 
	 * <code>source</code> observable.
	 * <p>This is a specialization of the multicast operator with
	 * a regular subject on U.</p>
	 * @param <T> the source element type
	 * @param <U> the result element type
	 * @param source the source sequence
	 * @param selector the observable selector that can
	 * use the source sequence as many times as necessary, without
	 * multiple registration.
	 * @return the observable sequence
	 */
	public static <T, U> Observable<U> publish(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super Observable<? extends T>, ? extends Observable<? extends U>> selector
			) {
		return multicast(source, new Func0<Subject<T, T>>() {
			@Override
			public Subject<T, T> invoke() {
				return Subjects.newSubject();
			}
		}, selector);
	}
	/**
	 * Returns an observable sequence which is the result of
	 * invoking the selector on a connectable observable sequence
	 * that shares a single subscription with the underlying 
	 * <code>source</code> observable and registering parties
	 * receive the initial value immediately.
	 * <p>This is a specialization of the multicast operator with
	 * a regular subject on U.</p>
	 * @param <T> the source element type
	 * @param <U> the result element type
	 * @param source the source sequence
	 * @param selector the observable selector that can
	 * use the source sequence as many times as necessary, without
	 * multiple registration.
	 * @param initialValue the value received by registering parties immediately.
	 * @return the observable sequence
	 */
	public static <T, U> Observable<U> publish(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super Observable<? extends T>, ? extends Observable<? extends U>> selector,
			final T initialValue
			) {
		return multicast(source, new Func0<Subject<T, T>>() {
			@Override
			public Subject<T, T> invoke() {
				return new Subject<T, T>() {
					/** The observable handling the registrations. */
					final DefaultObservable<T> obs = new DefaultObservable<T>(); 
					@Override
					public void next(T value) {
						obs.next(value);
					}

					@Override
					public void error(Throwable ex) {
						obs.error(ex);
					}

					@Override
					public void finish() {
						obs.finish();
					}

					@Override
					@Nonnull
					public Closeable register(Observer<? super T> observer) {
						observer.next(initialValue);
						return obs.register(observer);
					}
					
				};
			}
		}, selector);
	}
	/**
	 * Creates an observable which generates numbers from start.
	 * @param start the start value.
	 * @param count the count
	 * @param step the stepping
	 * @return the observable
	 */
	@Nonnull
	public static Observable<BigDecimal> range(
			@Nonnull final BigDecimal start,
			final int count,
			@Nonnull final BigDecimal step) {
		return range(start, count, step, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Creates an observable which generates BigDecimal numbers from start.
	 * @param start the start value.
	 * @param count the count
	 * @param step the stepping
	 * @param pool the execution thread pool.
	 * @return the observable
	 */
	@Nonnull
	public static Observable<BigDecimal> range(
			@Nonnull final BigDecimal start,
			final int count,
			@Nonnull final BigDecimal step,
			@Nonnull final Scheduler pool) {
		return new Observable<BigDecimal>() {
			@Override
			public Closeable register(final Observer<? super BigDecimal> observer) {
				DefaultRunnable s = new DefaultRunnable() {
					@Override
					public void onRun() {
						BigDecimal value = start;
						for (int i = 0; i < count && !cancelled(); i++) {
							observer.next(value);
							value = value.add(step);
						}
						if (!cancelled()) {
							observer.finish();
						}
					}
				};
				return pool.schedule(s);
			}
		};
	}
	/**
	 * Creates an observable which generates numbers from start.
	 * @param start the start value.
	 * @param count the count
	 * @return the observable
	 */
	@Nonnull
	public static Observable<BigInteger> range(
			@Nonnull final BigInteger start,
			@Nonnull final BigInteger count) {
		return range(start, count, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Creates an observable which generates BigInteger numbers from start.
	 * @param start the start value.
	 * @param count the count
	 * @param pool the execution thread pool.
	 * @return the observable
	 */
	@Nonnull
	public static Observable<BigInteger> range(
			@Nonnull final BigInteger start,
			@Nonnull final BigInteger count,
			final Scheduler pool) {
		return new Observable<BigInteger>() {
			@Override
			public Closeable register(final Observer<? super BigInteger> observer) {
				DefaultRunnable s = new DefaultRunnable() {
					@Override
					public void onRun() {
						BigInteger end = start.add(count);
						for (BigInteger i = start; i.compareTo(end) < 0
						&& !cancelled(); i = i.add(BigInteger.ONE)) {
							observer.next(i);
						}
						if (!cancelled()) {
							observer.finish();
						}
					}
				};
				return pool.schedule(s);
			}
		};
	}
	/**
	 * Creates an observable which generates numbers from start.
	 * @param start the start value.
	 * @param count the count
	 * @param step the stepping
	 * @return the observable
	 */
	@Nonnull
	public static Observable<Double> range(
			final double start,
			final int count,
			final double step) {
		return range(start, count, step, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Creates an observable which produces Double values from <code>start</code> in <code>count</code>
	 * amount and each subsequent element has a difference of <code>step</code>.
	 * @param start the starting value
	 * @param count how many values to produce
	 * @param step the incrementation amount
	 * @param pool the pool where to emit the values
	 * @return the observable of float
	 */
	public static Observable<Double> range(
			final double start,
			final int count,
			final double step,
			@Nonnull final Scheduler pool) {
		return new Observable<Double>() {
			@Override
			public Closeable register(final Observer<? super Double> observer) {
				DefaultRunnable s = new DefaultRunnable() {
					@Override
					public void onRun() {
						for (int i = 0; i < count && !cancelled(); i++) {
							observer.next(start + i * step);
						}
						if (!cancelled()) {
							observer.finish();
						}
					}
				};
				return pool.schedule(s);
			}
		};

	}
	/**
	 * Creates an observable which generates numbers from start.
	 * @param start the start value.
	 * @param count the count
	 * @param step the stepping
	 * @return the observable
	 */
	@Nonnull
	public static Observable<Float> range(
			final float start,
			final int count,
			final float step) {
		return range(start, count, step, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Creates an observable which produces Float values from <code>start</code> in <code>count</code>
	 * amount and each subsequent element has a difference of <code>step</code>.
	 * @param start the starting value
	 * @param count how many values to produce
	 * @param step the incrementation amount
	 * @param pool the pool where to emit the values
	 * @return the observable of float
	 */
	@Nonnull
	public static Observable<Float> range(
			final float start,
			final int count,
			final float step,
			@Nonnull final Scheduler pool) {
		return new Observable<Float>() {
			@Override
			public Closeable register(final Observer<? super Float> observer) {
				DefaultRunnable s = new DefaultRunnable() {
					@Override
					public void onRun() {
						for (int i = 0; i < count && !cancelled(); i++) {
							observer.next(start + i * step);
						}
						if (!cancelled()) {
							observer.finish();
						}
					}
				};
				return pool.schedule(s);
			}
		};

	}
	/**
	 * Creates an observable which generates numbers from start.
	 * @param start the start value.
	 * @param count the count
	 * @return the observable
	 */
	@Nonnull
	public static Observable<Integer> range(
			final int start,
			@Nonnull final int count) {
		return range(start, count, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Creates an observable which generates numbers from start.
	 * @param start the start value.
	 * @param count the count
	 * @param pool the execution thread pool.
	 * @return the observable
	 */
	public static Observable<Integer> range(
			final int start,
			final int count,
			@Nonnull final Scheduler pool) {
		return new Observable<Integer>() {
			@Override
			public Closeable register(final Observer<? super Integer> observer) {
				DefaultRunnable s = new DefaultRunnable() {
					@Override
					public void onRun() {
						for (int i = start; i < start + count && !cancelled(); i++) {
							observer.next(i);
						}
						if (!cancelled()) {
							observer.finish();
						}
					}
				};
				return pool.schedule(s);
			}
		};
	}
	/**
	 * Wrap the given observable into an new Observable instance, which calls the original register() method
	 * on the supplied pool.
	 * @param <T> the type of the objects to observe
	 * @param observable the original observable
	 * @param pool the pool to perform the original subscribe() call
	 * @return the new observable
	 */
	@Nonnull
	public static <T> Observable<T> registerOn(
			@Nonnull final Observable<T> observable,
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				// start the registration asynchronously
				final BlockingQueue<Closeable> cq = new LinkedBlockingQueue<Closeable>();
				pool.schedule(new Runnable() {
					@Override
					public void run() {
						cq.add(observable.register(observer));
					}
				});
				// use the disposable future when the deregistration is required
				return new Closeable() {
					@Override
					public void close() {
						pool.schedule(new Runnable() {
							@Override
							public void run() {
								try {
									cq.take().close(); // wait until the dispose becomes available then call it
								} catch (InterruptedException e) {
									throw new RuntimeException();
								} catch (IOException e) {
									throw new RuntimeException();
								}
							}
						});
					}
				};
			}
		};
	}
	/**
	 * Relay values of T while the given condition does not hold.
	 * Once the condition turns true the relaying stops.
	 * @param <T> the element type
	 * @param source the source of elements
	 * @param condition the condition that must be false to relay Ts
	 * @return the new observable
	 */
	@Nonnull
	public static <T> Observable<T> relayUntil(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func0<Boolean> condition) {
		return relayWhile(source, Functions.not(condition));
	}
	/**
	 * Relay the stream of Ts until condition turns into false.
	 * @param <T> the type of the values
	 * @param source the source of Ts
	 * @param condition the condition that must hold to relay Ts
	 * @return the new observable
	 */
	@Nonnull
	public static <T> Observable<T> relayWhile(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func0<Boolean> condition) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				DefaultObserverEx<T> obs = new DefaultObserverEx<T>(true) {
					@Override
					public void onError(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void onFinish() {
						observer.finish();
						close();
					}

					@Override
					public void onNext(T value) {
						if (condition.invoke()) {
							observer.next(value);
						} else {
							finish();
						}
					}

				};
				return obs.registerWith(source);
			}
		};
	}
	/**
	 * Unwrap the values within a timeinterval observable to its normal value.
	 * @param <T> the element type
	 * @param source the source which has its elements in a timeinterval way.
	 * @return the raw observables of Ts
	 */
	@Nonnull
	public static <T> Observable<T> removeTimeInterval(
			@Nonnull Observable<TimeInterval<T>> source) {
		return select(source, Reactive.<T>unwrapTimeInterval());
	}
	/**
	 * Unwrap the values within a timestamped observable to its normal value.
	 * @param <T> the element type
	 * @param source the source which has its elements in a timestamped way.
	 * @return the raw observables of Ts
	 */
	@Nonnull
	public static <T> Observable<T> removeTimestamped(
			@Nonnull Observable<Timestamped<T>> source) {
		Func1<Timestamped<T>, T> f = unwrapTimestamped();
		return select(source, f);
	}
	/**
	 * Creates an observable which repeatedly calls the given function which generates the Ts indefinitely.
	 * The generator runs on the default pool. Note that observers must unregister to stop the infinite loop.
	 * @param <T> the type of elements to produce
	 * @param func the function which generates elements
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> repeat(
			@Nonnull final Func0<? extends T> func) {
		return repeat(func, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Creates an observable which repeatedly calls the given function <code>count</code> times to generate Ts
	 * and runs on the default pool.
	 * @param <T> the element type
	 * @param func the function to call to generate values
	 * @param count the numer of times to repeat the value
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> repeat(
			@Nonnull final Func0<? extends T> func,
			final int count) {
		return repeat(func, count, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Creates an observable which repeatedly calls the given function <code>count</code> times to generate Ts
	 * and runs on the given pool.
	 * @param <T> the element type
	 * @param func the function to call to generate values
	 * @param count the numer of times to repeat the value
	 * @param pool the pool where the loop should be executed
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> repeat(
			@Nonnull final Func0<? extends T> func,
			final int count,
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				DefaultRunnable r = new DefaultRunnable() {
					@Override
					public void onRun() {
						int i = count;
						while (!cancelled() && i-- > 0) {
							observer.next(func.invoke());
						}
						if (!cancelled()) {
							observer.finish();
						}
					}
				};
				return pool.schedule(r);
			}
		};
	}
	/**
	 * Creates an observable which repeatedly calls the given function which generates the Ts indefinitely.
	 * The generator runs on the pool. Note that observers must unregister to stop the infinite loop.
	 * @param <T> the type of elements to produce
	 * @param func the function which generates elements
	 * @param pool the pool where the generator loop runs
	 * @return the observable
	 */
	public static <T> Observable<T> repeat(
			@Nonnull final Func0<? extends T> func,
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
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
		};
	}
	/**
	 * Repeat the source observable count times. Basically it creates
	 * a list of observables, all the source instance and applies
	 * the concat() operator on it.
	 * @param <T> the element type
	 * @param source the source observable
	 * @param count the number of times to repeat
	 * @return the new observable
	 */
	@Nonnull
	public static <T> Observable<T> repeat(
			@Nonnull Observable<? extends T> source,
			int count) {
		if (count > 0) {
			List<Observable<? extends T>> srcs = new ArrayList<Observable<? extends T>>(count);
			for (int i = 0; i < count; i++) {
				srcs.add(source);
			}
			return concat(srcs);
		}
		return empty();
	}
	/**
	 * Creates an observable which repeates the given value indefinitely
	 * and runs on the default pool. Note that the observers must
	 * deregister to stop the infinite background loop
	 * @param <T> the element type
	 * @param value the value to repeat
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> repeat(final T value) {
		return repeat(value, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Creates an observable which repeates the given value <code>count</code> times
	 * and runs on the default pool.
	 * @param <T> the element type
	 * @param value the value to repeat
	 * @param count the numer of times to repeat the value
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> repeat(
			final T value,
			final int count) {
		return repeat(value, count, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Creates an observable which repeates the given value <code>count</code> times
	 * and runs on the given pool.
	 * @param <T> the element type
	 * @param value the value to repeat
	 * @param count the numer of times to repeat the value
	 * @param pool the pool where the loop should be executed
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> repeat(
			final T value,
			final int count,
			@Nonnull final Scheduler pool) {
		return repeat(Functions.constant0(value), count, pool);
	}
	/**
	 * Creates an observable which repeates the given value indefinitely
	 * and runs on the given pool. Note that the observers must
	 * deregister to stop the infinite background loop
	 * @param <T> the element type
	 * @param value the value to repeat
	 * @param pool the pool where the loop should be executed
	 * @return the observable
	 */
	public static <T> Observable<T> repeat(
			final T value,
			@Nonnull final Scheduler pool) {
		return repeat(Functions.constant0(value), pool);
	}
	/**
	 * Replace the current default scheduler with the specified  new scheduler.
	 * This method is threadsafe
	 * @param newScheduler the new scheduler
	 * @return the current scheduler
	 */
	@Nonnull
	public static Scheduler replaceDefaultScheduler(
			@Nonnull Scheduler newScheduler) {
		if (newScheduler == null) {
			throw new IllegalArgumentException("newScheduler is null");
		}
		return DEFAULT_SCHEDULER.getAndSet(newScheduler);
	}
	/**
	 * Creates an observable which shares the source observable and replays all source Ts
	 * to any of the registering observers.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @return the new observable
	 */
	public static <T> Observable<T> replay(
			final Observable<? extends T> source
	) {
		return replay(source, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Creates an observable which shares the source observable and replays the buffered source Ts
	 * to any of the registering observers.
	 * @param <T> the element type
	 * @param <U> the return element type
	 * @param source the source of Ts
	 * @param selector the output stream selector
	 * @param bufferSize the target buffer size
	 * @return the new observable
	 */
	public static <T, U> Observable<U> replay(
			final Observable<? extends T> source,
			final Func1<? super Observable<? extends T>, ? extends Observable<U>> selector,
			final int bufferSize
	) {
		return replay(selector.invoke(source), bufferSize);
	}
	/**
	 * Creates an observable which shares the source observable and replays the bufferSize source Ts
	 * to any of the registering observers. After the periodic timespan, the buffer is reset.
	 * @param <T> the source element type
	 * @param <U> the return element type
	 * @param source the source of Ts
	 * @param selector the output stream selector
	 * @param bufferSize the buffer size
	 * @param timeSpan the window length
	 * @param unit the time unit
	 * @return the new observable
	 */
	public static <T, U> Observable<U> replay(
			final Observable<? extends T> source,
			final Func1<? super Observable<? extends T>, ? extends Observable<U>> selector,
			final int bufferSize,
			final long timeSpan,
			final TimeUnit unit
	) {
		return replay(selector.invoke(source), bufferSize, timeSpan, unit);
	}
	/**
	 * Creates an observable which shares the source observable and replays the bufferSize source Ts
	 * to any of the registering observers. After the periodic timespan, the buffer is reset.
	 * @param <T> the source element type
	 * @param <U> the return element type
	 * @param source the source of Ts
	 * @param selector the output stream selector
	 * @param bufferSize the buffer size
	 * @param timeSpan the window length
	 * @param unit the time unit
	 * @param scheduler the target scheduler
	 * @return the new observable
	 */
	public static <T, U> Observable<U> replay(
			final Observable<? extends T> source,
			final Func1<? super Observable<? extends T>, ? extends Observable<U>> selector,
			final int bufferSize,
			final long timeSpan,
			final TimeUnit unit,
			final Scheduler scheduler
	) {
		return replay(selector.invoke(source), bufferSize, timeSpan, unit, scheduler);
	}
	/**
	 * Creates an observable which shares the source observable and replays all source Ts
	 * to any of the registering observers. After the periodic timespan, the buffer is reset.
	 * @param <T> the source element type
	 * @param <U> the return element type
	 * @param source the source of Ts
	 * @param selector the output stream selector
	 * @param timeSpan the window length
	 * @param unit the time unit
	 * @return the new observable
	 */
	public static <T, U> Observable<U> replay(
			final Observable<? extends T> source,
			final Func1<? super Observable<? extends T>, ? extends Observable<U>> selector,
			final long timeSpan,
			final TimeUnit unit
	) {
		return replay(selector.invoke(source), timeSpan, unit);
	}
	/**
	 * Creates an observable which shares the source observable and replays all source Ts
	 * to any of the registering observers. After the periodic timespan, the buffer is reset.
	 * @param <T> the source element type
	 * @param <U> the return element type
	 * @param source the source of Ts
	 * @param selector the output stream selector
	 * @param timeSpan the window length
	 * @param unit the time unit
	 * @param scheduler the target scheduler
	 * @return the new observable
	 */
	public static <T, U> Observable<U> replay(
			final Observable<? extends T> source,
			final Func1<? super Observable<? extends T>, ? extends Observable<U>> selector,
			final long timeSpan,
			final TimeUnit unit,
			final Scheduler scheduler
	) {
		return replay(selector.invoke(source), timeSpan, unit, scheduler);
	}
	/**
	 * Creates an observable which shares the source observable and replays the buffered source Ts
	 * to any of the registering observers.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param bufferSize the target buffer size
	 * @return the new observable
	 */
	public static <T> Observable<T> replay(
			final Observable<? extends T> source,
			final int bufferSize
	) {
		return replay(source, bufferSize, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Creates an observable which shares the source observable and replays the bufferSize source Ts
	 * to any of the registering observers. After the periodic timespan, the buffer is reset.
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @param bufferSize the buffer size
	 * @param timeSpan the window length
	 * @param unit the time unit
	 * @return the new observable
	 */
	public static <T> Observable<T> replay(
			final Observable<? extends T> source,
			final int bufferSize,
			final long timeSpan,
			final TimeUnit unit
	) {
		return replay(source, bufferSize, timeSpan, unit, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Creates an observable which shares the source observable and replays the bufferSize source Ts
	 * to any of the registering observers. After the periodic timespan, the buffer is reset.
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @param bufferSize the buffer size
	 * @param timeSpan the window length
	 * @param unit the time unit
	 * @param scheduler the target scheduler
	 * @return the new observable
	 */
	public static <T> Observable<T> replay(
			final Observable<? extends T> source,
			final int bufferSize,
			final long timeSpan,
			final TimeUnit unit,
			final Scheduler scheduler
	) {
		return new Observable<T>() {
			/** The read-write lock. */
			final ReadWriteLock rwLock = new ReentrantReadWriteLock(true);
			/** The read lock for reading elements of the buffer. */
			final Lock readLock = rwLock.readLock();
			/** The write lock to write elements of the buffer and add new listeners. */
			final Lock writeLock = rwLock.writeLock();
			/** The buffer that holds the observed values so far. */
			@GuardedBy("rwLock")
			CircularBuffer<Option<T>> buffer = new CircularBuffer<Option<T>>(bufferSize);
			/** The single registration handler. */
			@GuardedBy("writeLock")
			Closeable sourceClose;
			/** The single registration handler. */
			@GuardedBy("writeLock")
			Closeable timerClose;
			/** The set of listeners active. */
			@GuardedBy("writeLock")
			Set<SingleLaneExecutor<Pair<Integer, CircularBuffer<Option<T>>>>> listeners = new HashSet<SingleLaneExecutor<Pair<Integer, CircularBuffer<Option<T>>>>>();
			@Override
			protected void finalize() throws Throwable {
				Closeables.closeSilently(timerClose);
				Closeables.closeSilently(sourceClose);
				super.finalize();
			}
			@Override
			public Closeable register(final Observer<? super T> observer) {
				writeLock.lock();
				try {
					if (sourceClose != null) {
						sourceClose = source.register(new Observer<T>() {
							/**
							 * Buffer and submit the option to all registered listeners.
							 * @param opt the option to submit
							 */
							void doOption(Option<T> opt) {
								writeLock.lock();
								try {
									buffer.add(opt);
									Pair<Integer, CircularBuffer<Option<T>>> of = Pair.of(buffer.tail(), buffer);
									for (SingleLaneExecutor<Pair<Integer, CircularBuffer<Option<T>>>> l : listeners) {
										l.add(of);
									}
								} finally {
									writeLock.unlock();
								}
							}

							@Override
							public void error(Throwable ex) {
								doOption(Option.<T>error(ex));
							}

							@Override
							public void finish() {
								doOption(Option.<T>none());
							}
							@Override
							public void next(T value) {
								doOption(Option.some(value));
							}
						});
						timerClose = scheduler.schedule(new Runnable() {
							@Override
							public void run() {
								writeLock.lock();
								try {
									buffer = new CircularBuffer<Option<T>>(bufferSize);
								} finally {
									writeLock.unlock();
								}
							}
						}, timeSpan, timeSpan, unit);
					}
				} finally {
					writeLock.unlock();
				}
				final AtomicBoolean cancel = new AtomicBoolean();
				final SingleLaneExecutor<Pair<Integer, CircularBuffer<Option<T>>>> playback = SingleLaneExecutor.create(scheduler, new Action1<Pair<Integer, CircularBuffer<Option<T>>>>() {
					/** The local buffer reader index. */
					@GuardedBy("readLock")
					int index = 0;
					/** The last buffer. */
					@GuardedBy("readLock")
					CircularBuffer<Option<T>> last;
					@Override
					public void invoke(Pair<Integer, CircularBuffer<Option<T>>> value) {
						readLock.lock();
						try {
							if (last != value.second) {
								index = 0;
								last = value.second;
							}
							index = Math.max(index, buffer.head());
							while (index < value.first && !cancel.get()) {
								dispatch(observer, last.get(index++));
							}
						} finally {
							readLock.unlock();
						}
					}
				});
				writeLock.lock();
				try {
					playback.add(Pair.of(buffer.tail(), buffer));
					listeners.add(playback);
				} finally {
					writeLock.unlock();
				}
				final Closeable c = new Closeable() {
					@Override
					public void close() throws IOException {
						cancel.set(true);
						writeLock.lock();
						try {
							listeners.remove(playback);
						} finally {
							writeLock.unlock();
						}
						Closeables.closeSilently(playback);
					}
				};
				return c;
			}
		};
	}
	/**
	 * Creates an observable which shares the source observable and replays all source Ts
	 * to any of the registering observers.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param bufferSize the target buffer size
	 * @param scheduler the scheduler from where the historical elements are emitted
	 * @return the new observable
	 */
	public static <T> Observable<T> replay(
			final Observable<? extends T> source,
			final int bufferSize,
			final Scheduler scheduler
	) {
		return new Observable<T>() {
			/** The read-write lock. */
			final ReadWriteLock rwLock = new ReentrantReadWriteLock(true);
			/** The read lock for reading elements of the buffer. */
			final Lock readLock = rwLock.readLock();
			/** The write lock to write elements of the buffer and add new listeners. */
			final Lock writeLock = rwLock.writeLock();
			/** The buffer that holds the observed values so far. */
			@GuardedBy("rwLock")
			final CircularBuffer<Option<T>> buffer = new CircularBuffer<Option<T>>(bufferSize);
			/** The single registration handler. */
			@GuardedBy("writeLock")
			Closeable sourceClose;
			/** The set of listeners active. */
			@GuardedBy("writeLock")
			Set<SingleLaneExecutor<Integer>> listeners = new HashSet<SingleLaneExecutor<Integer>>();
			@Override
			protected void finalize() throws Throwable {
				Closeables.closeSilently(sourceClose);
				super.finalize();
			}
			@Override
			public Closeable register(final Observer<? super T> observer) {
				writeLock.lock();
				try {
					if (sourceClose == null) {
						sourceClose = source.register(new Observer<T>() {
							/**
							 * Buffer and submit the option to all registered listeners.
							 * @param opt the option to submit
							 */
							void doOption(Option<T> opt) {
								writeLock.lock();
								try {
									buffer.add(opt);
									for (SingleLaneExecutor<Integer> l : listeners) {
										l.add(buffer.tail());
									}
								} finally {
									writeLock.unlock();
								}
							}

							@Override
							public void error(Throwable ex) {
								doOption(Option.<T>error(ex));
							}

							@Override
							public void finish() {
								doOption(Option.<T>none());
							}
							@Override
							public void next(T value) {
								doOption(Option.some(value));
							}
						});
					}
				} finally {
					writeLock.unlock();
				}
				final AtomicBoolean cancel = new AtomicBoolean();
				final SingleLaneExecutor<Integer> playback = SingleLaneExecutor.create(scheduler, new Action1<Integer>() {
					/** The local buffer reader index. */
					@GuardedBy("readLock")
					int index = 0;
					@Override
					public void invoke(Integer value) {
						readLock.lock();
						try {
							index = Math.max(index, buffer.head());
							while (index < value && !cancel.get()) {
								dispatch(observer, buffer.get(index++));
							}
						} finally {
							readLock.unlock();
						}
					}
				});
				writeLock.lock();
				try {
					playback.add(buffer.size());
					listeners.add(playback);
				} finally {
					writeLock.unlock();
				}
				final Closeable c = new Closeable() {
					@Override
					public void close() throws IOException {
						cancel.set(true);
						writeLock.lock();
						try {
							listeners.remove(playback);
						} finally {
							writeLock.unlock();
						}
						Closeables.closeSilently(playback);
					}
				};
				return c;
			}
		};
	}
	/**
	 * Creates an observable which shares the source observable and replays all source Ts
	 * to any of the registering observers. After the periodic timespan, the buffer is reset.
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @param timeSpan the window length
	 * @param unit the time unit
	 * @return the new observable
	 */
	public static <T> Observable<T> replay(
			final Observable<? extends T> source,
			final long timeSpan,
			final TimeUnit unit
	) {
		return replay(source, timeSpan, unit, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Creates an observable which shares the source observable and replays all source Ts
	 * to any of the registering observers. After the periodic timespan, the buffer is reset.
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @param timeSpan the window length
	 * @param unit the time unit
	 * @param scheduler the target scheduler
	 * @return the new observable
	 */
	public static <T> Observable<T> replay(
			final Observable<? extends T> source,
			final long timeSpan,
			final TimeUnit unit,
			final Scheduler scheduler
	) {
		return new Observable<T>() {
			/** The read-write lock. */
			final ReadWriteLock rwLock = new ReentrantReadWriteLock(true);
			/** The read lock for reading elements of the buffer. */
			final Lock readLock = rwLock.readLock();
			/** The write lock to write elements of the buffer and add new listeners. */
			final Lock writeLock = rwLock.writeLock();
			/** The buffer that holds the observed values so far. */
			@GuardedBy("rwLock")
			List<Option<T>> buffer = new ArrayList<Option<T>>();
			/** The single registration handler. */
			@GuardedBy("writeLock")
			Closeable sourceClose;
			/** The single registration handler. */
			@GuardedBy("writeLock")
			Closeable timerClose;
			/** The set of listeners active. */
			@GuardedBy("writeLock")
			Set<SingleLaneExecutor<Pair<Integer, List<Option<T>>>>> listeners = new HashSet<SingleLaneExecutor<Pair<Integer, List<Option<T>>>>>();
			@Override
			protected void finalize() throws Throwable {
				Closeables.closeSilently(timerClose);
				Closeables.closeSilently(sourceClose);
				super.finalize();
			}
			@Override
			public Closeable register(final Observer<? super T> observer) {
				writeLock.lock();
				try {
					if (sourceClose == null) {
						sourceClose = source.register(new Observer<T>() {
							/**
							 * Buffer and submit the option to all registered listeners.
							 * @param opt the option to submit
							 */
							void doOption(Option<T> opt) {
								writeLock.lock();
								try {
									buffer.add(opt);
									Pair<Integer, List<Option<T>>> of = Pair.of(buffer.size(), buffer);
									for (SingleLaneExecutor<Pair<Integer, List<Option<T>>>> l : listeners) {
										l.add(of);
									}
								} finally {
									writeLock.unlock();
								}
							}

							@Override
							public void error(Throwable ex) {
								doOption(Option.<T>error(ex));
							}

							@Override
							public void finish() {
								doOption(Option.<T>none());
							}
							@Override
							public void next(T value) {
								doOption(Option.some(value));
							}
						});
						timerClose = scheduler.schedule(new Runnable() {
							@Override
							public void run() {
								writeLock.lock();
								try {
									buffer = new ArrayList<Option<T>>();
								} finally {
									writeLock.unlock();
								}
							}
						}, timeSpan, timeSpan, unit);
					}
				} finally {
					writeLock.unlock();
				}
				final AtomicBoolean cancel = new AtomicBoolean();
				final SingleLaneExecutor<Pair<Integer, List<Option<T>>>> playback = SingleLaneExecutor.create(scheduler, new Action1<Pair<Integer, List<Option<T>>>>() {
					/** The local buffer reader index. */
					@GuardedBy("readLock")
					int index = 0;
					/** The last buffer. */
					@GuardedBy("readLock")
					List<Option<T>> last;
					@Override
					public void invoke(Pair<Integer, List<Option<T>>> value) {
						readLock.lock();
						try {
							if (last != value.second) {
								index = 0;
								last = value.second;
							}
							while (index < value.first && !cancel.get()) {
								dispatch(observer, last.get(index++));
							}
						} finally {
							readLock.unlock();
						}
					}
				});
				writeLock.lock();
				try {
					playback.add(Pair.of(buffer.size(), buffer));
					listeners.add(playback);
				} finally {
					writeLock.unlock();
				}
				final Closeable c = new Closeable() {
					@Override
					public void close() throws IOException {
						cancel.set(true);
						writeLock.lock();
						try {
							listeners.remove(playback);
						} finally {
							writeLock.unlock();
						}
						Closeables.closeSilently(playback);
					}
				};
				return c;
			}
		};
	}
	/**
	 * Creates an observable which shares the source observable and replays all source Ts
	 * to any of the registering observers.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param scheduler the scheduler from where the historical elements are emitted
	 * @return the new observable
	 */
	public static <T> Observable<T> replay(
			final Observable<? extends T> source,
			final Scheduler scheduler
	) {
		return new Observable<T>() {
			/** The read-write lock. */
			final ReadWriteLock rwLock = new ReentrantReadWriteLock(true);
			/** The read lock for reading elements of the buffer. */
			final Lock readLock = rwLock.readLock();
			/** The write lock to write elements of the buffer and add new listeners. */
			final Lock writeLock = rwLock.writeLock();
			/** The buffer that holds the observed values so far. */
			@GuardedBy("rwLock")
			final List<Option<T>> buffer = new ArrayList<Option<T>>();
			/** The single registration handler. */
			@GuardedBy("writeLock")
			Closeable sourceClose;
			/** The set of listeners active. */
			@GuardedBy("writeLock")
			Set<SingleLaneExecutor<Integer>> listeners = new HashSet<SingleLaneExecutor<Integer>>();
			@Override
			protected void finalize() throws Throwable {
				Closeables.closeSilently(sourceClose);
				super.finalize();
			}
			@Override
			public Closeable register(final Observer<? super T> observer) {
				writeLock.lock();
				try {
					if (sourceClose == null) {
						sourceClose = source.register(new Observer<T>() {
							/**
							 * Buffer and submit the option to all registered listeners.
							 * @param opt the option to submit
							 */
							void doOption(Option<T> opt) {
								writeLock.lock();
								try {
									buffer.add(opt);
									for (SingleLaneExecutor<Integer> l : listeners) {
										l.add(buffer.size());
									}
								} finally {
									writeLock.unlock();
								}
							}

							@Override
							public void error(Throwable ex) {
								doOption(Option.<T>error(ex));
							}

							@Override
							public void finish() {
								doOption(Option.<T>none());
							}
							@Override
							public void next(T value) {
								doOption(Option.some(value));
							}
						});
					}
				} finally {
					writeLock.unlock();
				}
				final AtomicBoolean cancel = new AtomicBoolean();
				final SingleLaneExecutor<Integer> playback = SingleLaneExecutor.create(scheduler, new Action1<Integer>() {
					/** The local buffer reader index. */
					@GuardedBy("readLock")
					int index = 0;
					@Override
					public void invoke(Integer value) {
						readLock.lock();
						try {
							while (index < value && !cancel.get()) {
								dispatch(observer, buffer.get(index++));
							}
						} finally {
							readLock.unlock();
						}
					}
				});
				writeLock.lock();
				try {
					playback.add(buffer.size());
					listeners.add(playback);
				} finally {
					writeLock.unlock();
				}
				final Closeable c = new Closeable() {
					@Override
					public void close() throws IOException {
						cancel.set(true);
						writeLock.lock();
						try {
							listeners.remove(playback);
						} finally {
							writeLock.unlock();
						}
						Closeables.closeSilently(playback);
					}
				};
				return c;
			}
		};
	}
	/**
	 * Creates an observable which shares the source observable returned by the selector and replays all source Ts
	 * to any of the registering observers.
	 * @param <T> the element type
	 * @param <U> the return element type
	 * @param source the source of Ts
	 * @param selector the output stream selector
	 * @param bufferSize the target buffer size
	 * @param scheduler the scheduler from where the historical elements are emitted
	 * @return the new observable
	 */
	public static <T, U> Observable<U> replay(
			final Observable<T> source,
			final Func1<? super Observable<? extends T>, ? extends Observable<U>> selector,
			final int bufferSize,
			final Scheduler scheduler
	) {
		return replay(selector.invoke(source), bufferSize, scheduler);
	}
	/**
	 * Returns the observable sequence for the supplied source observable by
	 * invoking the selector function with it.
	 * @param <T> the source element type
	 * @param <U> the output element type
	 * @param source the source of Ts
	 * @param selector the selector which returns an observable of Us for the given <code>source</code>
	 * @return the new observable
	 */
	public static <T, U> Observable<U> replay(
		final Observable<T> source,
		final Func1<? super Observable<T>, ? extends Observable<U>> selector
	) {
		return selector.invoke(source);
	}
	/**
	 * Restore the default scheduler back to the <code>DefaultScheduler</code>
	 * used when this class was initialized.
	 */
	public static void restoreDefaultScheduler() {
		DEFAULT_SCHEDULER.set(new DefaultScheduler());
	}
	/**
	 * Returns an observable which listens to elements from a source until it signals an error()
	 * or finish() and continues with the next observable. The registration happens only when the
	 * previous observables finished in any way.
	 * @param <T> the type of the elements
	 * @param sources the list of observables
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> resumeAlways(
			@Nonnull final Iterable<? extends Observable<? extends T>> sources) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				final Iterator<? extends Observable<? extends T>> it = sources.iterator();
				if (it.hasNext()) {
					DefaultObserverEx<T> obs = new DefaultObserverEx<T>(false) {
						@Override
						public void onError(Throwable ex) {
							remove(this);
							if (it.hasNext()) {
								registerWith(it.next());
							} else {
								observer.finish();
								close();
							}
						}

						@Override
						public void onFinish() {
							remove(this);
							if (it.hasNext()) {
								registerWith(it.next());
							} else {
								observer.finish();
								close();
							}
						}
						@Override
						public void onNext(T value) {
							observer.next(value);
						}
					};
					return obs.registerWith(it.next());
				}
				return Closeables.emptyCloseable();
			}
		};
	}
	/**
	 * It tries to submit the values of first observable, but when it throws an exeption,
	 * the next observable within source is used further on. Basically a failover between the Observables.
	 * If the current source finish() then the result observable calls finish().
	 * If the last of the sources calls error() the result observable calls error()
	 * @param <T> the type of the values
	 * @param sources the available source observables.
	 * @return the failover observable
	 */
	@Nonnull
	public static <T> Observable<T> resumeOnError(
			@Nonnull final Iterable<? extends Observable<? extends T>> sources) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				final Iterator<? extends Observable<? extends T>> it = sources.iterator();
				if (it.hasNext()) {
					DefaultObserverEx<T> obs = new DefaultObserverEx<T>(false) {
						@Override
						public void onError(Throwable ex) {
							if (it.hasNext()) {
								registerWith(it.next());
							} else {
								observer.error(ex);
								close();
							}
						}

						@Override
						public void onFinish() {
							observer.finish();
							close();
						}
						@Override
						public void onNext(T value) {
							observer.next(value);
						}
					};
					return obs.registerWith(it.next());
				}
				return Closeables.emptyCloseable();
			}
		};
	}
	/**
	 * Restarts the observation until the source observable terminates normally.
	 * @param <T> the type of elements
	 * @param source the source observable
	 * @return the repeating observable
	 */
	@Nonnull
	public static <T> Observable<T> retry(
			@Nonnull final Observable<? extends T> source) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				DefaultObserverEx<T> obs = new DefaultObserverEx<T>(false) {
					@Override
					public void onError(Throwable ex) {
						registerWith(source);
					}

					@Override
					public void onFinish() {
						observer.finish();
						close();
					}
					@Override
					public void onNext(T value) {
						observer.next(value);
					}
				};
				return obs.registerWith(source);
			}
		};
	}
	/**
	 * Restarts the observation until the source observable terminates normally 
	 * or the <code>count</code> retry count was used up.
	 * @param <T> the type of elements
	 * @param source the source observable
	 * @param count the retry count
	 * @return the repeating observable
	 */
	@Nonnull
	public static <T> Observable<T> retry(
			@Nonnull final Observable<? extends T> source,
			final int count) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				DefaultObserverEx<T> obs = new DefaultObserverEx<T>(false) {
					/** The remaining retry count. */
					int remainingCount = count;
					@Override
					public void onError(Throwable ex) {
						if (remainingCount-- > 0) {
							registerWith(source);
						} else {
							observer.error(ex);
							close();
						}
					}

					@Override
					public void onFinish() {
						observer.finish();
						close();
					}

					@Override
					public void onNext(T value) {
						observer.next(value);
					}

				};
				return obs.registerWith(source);
			}
		};
	}
	/**
	 * Blocks until the observable calls finish() or error(). Values are submitted to the given action.
	 * @param <T> the type of the elements
	 * @param source the source observable
	 * @param action the action to invoke for each value
	 * @throws InterruptedException if the current thread is interrupted while waiting on
	 * the observable.
	 */
	public static <T> void run(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Action1<? super T> action) throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		Closeable c = source.register(new DefaultObserver<T>(true) {
			@Override
			public void onError(Throwable ex) {
				latch.countDown();
			}

			@Override
			public void onFinish() {
				latch.countDown();
			}

			@Override
			public void onNext(T value) {
				action.invoke(value);
			}

		});
		try {
			latch.await();
		} finally {
			Closeables.closeSilently(c);
		}
	}
	/**
	 * Blocks until the observable calls finish() or error(). Events are submitted to the given observer.
	 * @param <T> the type of the elements
	 * @param source the source observable
	 * @param observer the observer to invoke for each event
	 * @throws InterruptedException if the current thread is interrupted while waiting on
	 * the observable.
	 */
	public static <T> void run(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Observer<? super T> observer) throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		Closeable c = source.register(new DefaultObserver<T>(true) {
			@Override
			public void onError(Throwable ex) {
				try {
					observer.error(ex);
				} finally {
					latch.countDown();
				}
			}

			@Override
			public void onFinish() {
				try {
					observer.finish();
				} finally {
					latch.countDown();
				}
			}

			@Override
			public void onNext(T value) {
				observer.next(value);
			}

		});
		try {
			latch.await();
		} finally {
			Closeables.closeSilently(c);
		}
	}
	/**
	 * Blocks until the observable calls finish() or error(). Values are ignored.
	 * @param source the source observable
	 * @throws InterruptedException if the current thread is interrupted while waiting on
	 * the observable.
	 */
	public static void run(
			@Nonnull final Observable<?> source) throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		Closeable c = source.register(new DefaultObserver<Object>(true) {
			@Override
			public void onError(Throwable ex) {
				latch.countDown();
			}

			@Override
			public void onFinish() {
				latch.countDown();
			}

			@Override
			public void onNext(Object value) {

			}

		});
		try {
			latch.await();
		} finally {
			Closeables.closeSilently(c);
		}
	}
	/**
	 * Blocks until the observable calls finish() or error() or 
	 * the specified amount of time elapses. Values and exceptions are ignored.
	 * @param source the source observable
	 * @param time the time value
	 * @param unit the time unit
	 * @return false if the waiting time elapsed before the run completed
	 * @throws InterruptedException if the current thread is interrupted while waiting on
	 * the observable.
	 */
	static boolean run(
			@Nonnull final Observable<?> source,
			long time,
			@Nonnull TimeUnit unit) throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		Closeable c = source.register(new DefaultObserver<Object>(true) {
			@Override
			public void onError(Throwable ex) {
				latch.countDown();
			}

			@Override
			public void onFinish() {
				latch.countDown();
			}

			@Override
			public void onNext(Object value) {

			}

		});
		try {
			return latch.await(time, unit);
		} finally {
			Closeables.closeSilently(c);
		}
	}
	/**
	 * Periodically sample the given source observable, which means tracking the last value of
	 * the observable and periodically submitting it to the output observable.
	 * @param <T> the type of elements to watch
	 * @param source the source of elements
	 * @param time the time value to wait
	 * @param unit the time unit
	 * @return the sampled observable
	 */
	@Nonnull
	public static <T> Observable<T> sample(
			@Nonnull final Observable<? extends T> source,
			final long time,
			@Nonnull final TimeUnit unit) {
		return sample(source, time, unit, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Periodically sample the given source observable, which means tracking 
	 * the last value of
	 * the observable and periodically submitting it to the output observable.
	 * @param <T> the type of elements to watch
	 * @param source the source of elements
	 * @param time the time value to wait
	 * @param unit the time unit
	 * @param pool the scheduler pool where the periodic submission should happen.
	 * @return the sampled observable
	 */
	@Nonnull
	public static <T> Observable<T> sample(
			@Nonnull final Observable<? extends T> source,
			final long time,
			@Nonnull final TimeUnit unit,
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				final AtomicReference<T> current = new AtomicReference<T>();
				final AtomicBoolean first = new AtomicBoolean(true);
				
				final DefaultObserverEx<T> obs = new DefaultObserverEx<T>(true) {
					@Override
					public void onError(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void onFinish() {
						observer.finish();
					}
					@Override
					public void onNext(T value) {
						first.set(false);
						current.set(value);
					}
				};
				obs.add("timer", pool.schedule(new DefaultRunnable() {
						@Override
						protected void onRun() {
							if (!first.get()) {
								observer.next(current.get());
							}
						}
					}, time, time, unit));
				return obs.registerWith(source);
			}
		};
	}
	/**
	 * Creates an observable which accumultates the given source and submits each intermediate results to its subscribers.
	 * Example:<br>
	 * <code>range(0, 5).accumulate((x, y) -> x + y)</code> produces a sequence of [0, 1, 3, 6, 10];<br>
	 * basically the first event (0) is just relayed and then every pair of values are simply added together and relayed
	 * @param <T> the element type to accumulate
	 * @param source the source of the accumulation
	 * @param accumulator the accumulator which takest the current accumulation value and the current observed value
	 * and returns a new accumulated value
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> scan(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func2<? super T, ? super T, ? extends T> accumulator) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** The current accumulated value. */
					T current;
					/** Are we waiting for the first value? */
					boolean first = true;
					@Override
					public void error(Throwable ex) {
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
		};
	}
	/**
	 * Creates an observable which accumultates the given source and submits each intermediate results to its subscribers.
	 * Example:<br>
	 * <code>range(0, 5).accumulate(1, (x, y) => x + y)</code> produces a sequence of [1, 2, 4, 7, 11];<br>
	 * basically the accumulation starts from zero and the first value (0) that comes in is simply added
	 * @param <T> the element type to accumulate
	 * @param source the source of the accumulation
	 * @param seed the initial value of the accumulation
	 * @param accumulator the accumulator which takest the current accumulation value and the current observed value
	 * and returns a new accumulated value
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> scan(
			@Nonnull final Observable<? extends T> source,
			final T seed,
			@Nonnull final Func2<? super T, ? super T, ? extends T> accumulator) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** The current accumulated value. */
					T current = seed;
					@Override
					public void error(Throwable ex) {
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
		};
	}
	/**
	 * Creates an observable which accumultates the given source and submits each intermediate results to its subscribers.
	 * Example:<br>
	 * <code>range(1, 5).accumulate0(1, (x, y) => x + y)</code> produces a sequence of [1, 2, 4, 7, 11, 16];<br>
	 * basically, it submits the seed value (1) and computes the current aggregate with the current value(1).
	 * @param <T> the element type to accumulate
	 * @param source the source of the accumulation
	 * @param seed the initial value of the accumulation
	 * @param accumulator the accumulator which takest the current accumulation value and the current observed value
	 * and returns a new accumulated value
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> scan0(
			@Nonnull final Observable<? extends T> source,
			final T seed,
			@Nonnull final Func2<? super T, ? super T, ? extends T> accumulator) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** The current accumulated value. */
					T current;
					/** Are we waiting for the first value? */
					boolean first = true;
					@Override
					public void error(Throwable ex) {
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
							observer.next(seed);
							current = accumulator.invoke(seed, value);
						} else {
							current = accumulator.invoke(current, value);
						}
						observer.next(current);
					}
				});
			}
		};
	}
	/**
	 * Use the mapper to transform the T source into an U source.
	 * @param <T> the type of the original observable
	 * @param <U> the type of the new observable
	 * @param source the source of Ts
	 * @param mapper the mapper from Ts to Us
	 * @return the observable on Us
	 */
	@Nonnull
	public static <T, U> Observable<U> select(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends U> mapper) {
		return new Observable<U>() {
			@Override
			public Closeable register(final Observer<? super U> observer) {
				return source.register(new Observer<T>() {
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.finish();
					}

					@Override
					public void next(T value) {
						observer.next(mapper.invoke(value));
					}

				});
			}
		};
	}
	/**
	 * Transforms the elements of the source observable into Us by using a selector which receives an index indicating
	 * how many elements have been transformed this far.
	 * @param <T> the source element type
	 * @param <U> the output element type
	 * @param source the source observable
	 * @param selector the selector taking an index and the current T
	 * @return the transformed observable
	 */
	public static <T, U> Observable<U> select(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func2<? super Integer, ? super T, ? extends U> selector) {
		return new Observable<U>() {
			@Override
			public Closeable register(final Observer<? super U> observer) {
				return source.register(new Observer<T>() {
					/** The running index. */
					int index;
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.finish();
					}

					@Override
					public void next(T value) {
						observer.next(selector.invoke(index++, value));
					}

				});
			}
		};
	}
	/**
	 * Transform the given source of Ts into Us in a way that the
	 * selector might return an observable ofUs for a single T.
	 * The observable is fully channelled to the output observable.
	 * @param <T> the input element type
	 * @param <U> the output element type
	 * @param source the source of Ts
	 * @param selector the selector to return an Iterable of Us
	 * @return the
	 */
	@Nonnull
	public static <T, U> Observable<U> selectMany(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends Observable<? extends U>> selector) {
		return selectMany(source, selector, new Func2<T, U, U>() {
			@Override
			public U invoke(T param1, U param2) {
				return param2;
			}
		});
	}
	/**
	 * Creates an observable in which for each of Ts an observable of Vs are
	 * requested which in turn will be transformed by the resultSelector for each
	 * pair of T and V giving an U.
	 * @param <T> the source element type
	 * @param <U> the intermediate element type
	 * @param <V> the output element type
	 * @param source the source of Ts
	 * @param collectionSelector the selector which returns an observable of intermediate Vs
	 * @param resultSelector the selector which gives an U for a T and V
	 * @return the observable of Us
	 */
	@Nonnull
	public static <T, U, V> Observable<V> selectMany(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends Observable<? extends U>> collectionSelector,
			@Nonnull final Func2<? super T, ? super U, ? extends V> resultSelector) {
		return new Observable<V>() {
			@Override
			public Closeable register(final Observer<? super V> observer) {
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
					public void onError(Throwable ex) {
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
							protected void onError(Throwable ex) {
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
		};
	}
	/**
	 * Creates an observable of Us in a way when a source T arrives, the observable of
	 * Us is completely drained into the output. This is done again and again for
	 * each arriving Ts.
	 * @param <T> the type of the source, irrelevant
	 * @param <U> the output type
	 * @param source the source of Ts
	 * @param provider the source of Us
	 * @return the observable for Us
	 */
	@Nonnull
	public static <T, U> Observable<U> selectMany(
			@Nonnull Observable<? extends T> source,
			@Nonnull Observable<? extends U> provider) {
		return selectMany(source,
				Functions.<T, Observable<? extends U>>constant(provider));
	}
	/**
	 * Transform the given source of Ts into Us in a way that the selector might return zero to multiple elements of Us for a single T.
	 * The iterable is flattened and submitted to the output
	 * @param <T> the input element type
	 * @param <U> the output element type
	 * @param source the source of Ts
	 * @param selector the selector to return an Iterable of Us
	 * @return the
	 */
	@Nonnull
	public static <T, U> Observable<U> selectManyIterable(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends Iterable<? extends U>> selector) {
		return new Observable<U>() {
			@Override
			public Closeable register(final Observer<? super U> observer) {
				return source.register(new Observer<T>() {

					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.finish();
					}

					@Override
					public void next(T value) {
						for (U u : selector.invoke(value)) {
							observer.next(u);
						}
					}

				});
			}
		};
	}
	/**
	 * Compares two sequences and returns whether they are produce the same
	 * elements in terms of the null-safe object equality.
	 * <p>The equality only stands if the two sequence produces the same
	 * amount of values and those values are pairwise equal. If one of the sequences
	 * terminates before the other, the equality test will return false.</p>
	 * @param <T> the common element type
	 * @param first the first source of Ts
	 * @param second the second source of Ts
	 * @return the new observable
	 * @since 0.97
	 */
	public static <T> Observable<Boolean> sequenceEqual(
			final Iterable<? extends T> first,
			final Observable<? extends T> second) {
		return sequenceEqual(first, second, Functions.equals());
	}
	/**
	 * Compares two sequences and returns whether they are produce the same
	 * elements in terms of the comparer function.
	 * <p>The equality only stands if the two sequence produces the same
	 * amount of values and those values are pairwise equal. If one of the sequences
	 * terminates before the other, the equality test will return false.</p>
	 * @param <T> the common element type
	 * @param first the first source of Ts
	 * @param second the second source of Ts
	 * @param comparer the equality comparison function
	 * @return the new observable
	 */
	public static <T> Observable<Boolean> sequenceEqual(
			final Iterable<? extends T> first,
			final Observable<? extends T> second,
			final Func2<? super T, ? super T, Boolean> comparer) {
		return select(
				any(
					zip(
							Interactive.materialize(first), 
							materialize(second), 
							newOptionComparer(comparer)
					), 
					Functions.alwaysFalse1()
				), 
			Functions.negate());
	}
	/**
	 * Compares two sequences and returns whether they are produce the same
	 * elements in terms of the null-safe object equality.
	 * <p>The equality only stands if the two sequence produces the same
	 * amount of values and those values are pairwise equal. If one of the sequences
	 * terminates before the other, the equality test will return false.</p>
	 * @param <T> the common element type
	 * @param first the first source of Ts
	 * @param second the second source of Ts
	 * @return the new observable
	 */
	public static <T> Observable<Boolean> sequenceEqual(
			final Observable<? extends T> first,
			final Observable<? extends T> second) {
		return sequenceEqual(first, second, Functions.equals());
	}
	/**
	 * Compares two sequences and returns whether they are produce the same
	 * elements in terms of the comparer function.
	 * <p>The equality only stands if the two sequence produces the same
	 * amount of values and those values are pairwise equal. If one of the sequences
	 * terminates before the other, the equality test will return false.</p>
	 * @param <T> the common element type
	 * @param first the first source of Ts
	 * @param second the second source of Ts
	 * @param comparer the equality comparison function
	 * @return the new observable
	 */
	public static <T> Observable<Boolean> sequenceEqual(
			final Observable<? extends T> first,
			final Observable<? extends T> second,
			final Func2<? super T, ? super T, Boolean> comparer) {
		return select(
				any(
					zip(
							materialize(first), 
							materialize(second), 
							newOptionComparer(comparer)
					), 
					Functions.negate()
				), 
			Functions.negate());
	}
	/**
	 * Returns the single element of the given observable source.
	 * If the source is empty, a NoSuchElementException is thrown.
	 * If the source has more than one element, a TooManyElementsException is thrown.
	 * @param <T> the type of the element
	 * @param source the source of Ts
	 * @return the single element
	 */
	@Nonnull
	public static <T> T single(
			@Nonnull Observable<? extends T> source) {
		CloseableIterator<T> it = toIterable(source).iterator();
		try {
			if (it.hasNext()) {
				T one = it.next();
				if (!it.hasNext()) {
					return one;
				}
				throw new TooManyElementsException();
			}
			throw new NoSuchElementException();
		} finally {
			Closeables.closeSilently(it);
		}
	}
	/**
	 * Returns the single value produced by the supplier callback function
	 * on the default scheduler.
	 * @param <T> the value type
	 * @param supplier the value supplier
	 * @return the observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<T> singleton(
			final Func0<? extends T> supplier) {
		return singleton(supplier, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Returns the single value produced by the supplier callback function
	 * on the supplied scheduler.
	 * @param <T> the value type
	 * @param supplier the value supplier
	 * @param pool the pool where to submit the value to the observers
	 * @return the observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<T> singleton(
			final Func0<? extends T> supplier,
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return pool.schedule(new Runnable() {
					@Override
					public void run() {
						observer.next(supplier.invoke());
						observer.finish();
					}
				});
			}
		};
	}
	/**
	 * Returns the single value in the observables by using the default scheduler pool.
	 * @param <T> the value type
	 * @param value the value
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> singleton(
			final T value) {
		return singleton(value, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Returns the single value in the observables.
	 * @param <T> the value type
	 * @param value the value
	 * @param pool the pool where to submit the value to the observers
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> singleton(
			final T value,
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return pool.schedule(new Runnable() {
					@Override
					public void run() {
						observer.next(value);
						observer.finish();
					}
				});
			}
		};
	}

	/**
	 * Skips the given amount of next() messages from source and relays
	 * the rest.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param count the number of messages to skip
	 * @return the new observable
	 */
	@Nonnull
	public static <T> Observable<T> skip(
			@Nonnull final Observable<? extends T> source,
			final int count) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** The remaining count. */
					int remaining = count;
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.finish();
					}

					@Override
					public void next(T value) {
						if (remaining <= 0) {
							observer.next(value);
						} else {
							remaining--;
						}
					}
				});
			}
		};
	}
	/**
	 * Skips the last <code>count</code> elements from the source observable.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param count the number of elements to skip at the end
	 * @return the new observable
	 */
	@Nonnull
	public static <T> Observable<T> skipLast(final Observable<? extends T> source, final int count) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					final Queue<T> buffer = new ConcurrentLinkedQueue<T>();

					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						while (buffer.size() > count) {
							observer.next(buffer.poll());
						}
						observer.finish();
					}

					@Override
					public void next(T value) {
						buffer.add(value);
					}
				});
			}
		};
	}
	/**
	 * Skip the source elements until the signaller sends its first element.
	 * <p>Once the signaller sends its first value, it gets deregistered.</p>
	 * <p>Exception semantics: exceptions thrown by source or singaller is immediately forwarded to
	 * the output and the stream is terminated.</p>
	 * @param <T> the element type of the source
	 * @param <U> the element type of the signaller, irrelevant
	 * @param source the source of Ts
	 * @param signaller the source of Us
	 * @return the new observable
	 */
	@Nonnull
	public static <T, U> Observable<T> skipUntil(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Observable<U> signaller) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				final CompositeCloseable closeables = new CompositeCloseable();
				final AtomicBoolean gate = new AtomicBoolean();
				DefaultObserverEx<T> obs = new DefaultObserverEx<T>(true) {
					@Override
					protected void onClose() {
						super.onClose();
						closeables.closeSilently();
					}
					@Override
					public void onError(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void onFinish() {
						if (gate.get()) {
							observer.finish();
						}
					}
					@Override
					public void onNext(T value) {
						if (gate.get()) {
							observer.next(value);
						}
					}
				};
				DefaultObserverEx<U> so = new DefaultObserverEx<U>(true) {
					@Override
					public void onError(Throwable ex) {
						observer.error(ex);
					}
					@Override
					protected void onFinish() {
						// ignored
					}
					@Override
					public void onNext(U value) {
						gate.set(true);
						close();
					}
				};
				
				closeables.add(obs, so);
				obs.registerWith(source);
				so.registerWith(signaller);
				
				return closeables;
			}
		};
	}
	/**
	 * Skips the Ts from source while the specified condition returns true.
	 * If the condition returns false, all subsequent Ts are relayed,
	 * ignoring the condition further on. Errors and completion
	 * is relayed regardless of the condition.
	 * @param <T> the element types
	 * @param source the source of Ts
	 * @param condition the condition that must turn false in order to start relaying
	 * @return the new observable
	 */
	@Nonnull
	public static <T> Observable<T> skipWhile(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super T, Boolean> condition) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** Can we relay stuff? */
					boolean mayRelay;
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.finish();
					}

					@Override
					public void next(T value) {
						if (!mayRelay) {
							mayRelay = !condition.invoke(value);
							if (mayRelay) {
								observer.next(value);
							}
						} else {
							observer.next(value);
						}
					}

				});
			}
		};
	}
	/**
	 * Invokes the action asynchronously on the given pool and
	 * relays its finish() or error() messages.
	 * @param action the action to invoke
	 * @return the observable
	 */
	@Nonnull
	public static Observable<Void> start(
			@Nonnull final Action0 action) {
		return start(action, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Invokes the action asynchronously on the given pool and
	 * relays its finish() or error() messages.
	 * @param action the action to invoke
	 * @param pool the pool where the action should run
	 * @return the observable
	 */
	@Nonnull
	public static Observable<Void> start(
			@Nonnull final Action0 action,
			@Nonnull final Scheduler pool) {
		return new Observable<Void>() {
			@Override
			public Closeable register(final Observer<? super Void> observer) {
				return pool.schedule(new Runnable() {
					@Override
					public void run() {
						try {
							action.invoke();
							observer.finish();
						} catch (Throwable ex) {
							observer.error(ex);
						}
					}
				});
			}
		};
	}
	/**
	 * Invokes the function asynchronously on the default pool and
	 * relays its result followed by a finish. Exceptions are
	 * relayed as well.
	 * @param <T> the function return type
	 * @param func the function
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> start(
			@Nonnull final Func0<? extends T> func) {
		return start(func, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Invokes the function asynchronously on the given pool and
	 * relays its result followed by a finish. Exceptions are
	 * relayed as well.
	 * @param <T> the function return type
	 * @param func the function
	 * @param pool the pool where the action should run
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> start(
			@Nonnull final Func0<? extends T> func,
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return pool.schedule(new Runnable() {
					@Override
					public void run() {
						try {
							T value = func.invoke();
							observer.next(value);
							observer.finish();
						} catch (Throwable ex) {
							observer.error(ex);
						}
					}
				});
			}
		};
	}
	/**
	 * Start with the given iterable of values before relaying the Ts from the
	 * source. The iterable values are emitted on the default pool.
	 * @param <T> the element type
	 * @param source the source
	 * @param values the values to start with
	 * @return the new observable
	 */
	@Nonnull
	public static <T> Observable<T> startWith(
			@Nonnull Observable<? extends T> source,
			@Nonnull Iterable<? extends T> values) {
		return startWith(source, values, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Start with the given iterable of values before relaying the Ts from the
	 * source. The iterable values are emitted on the given pool.
	 * @param <T> the element type
	 * @param source the source
	 * @param values the values to start with
	 * @param pool the pool where the iterable values should be emitted
	 * @return the new observable
	 */
	@Nonnull
	public static <T> Observable<T> startWith(
			@Nonnull Observable<? extends T> source,
			@Nonnull Iterable<? extends T> values,
			@Nonnull Scheduler pool) {
		return concat(toObservable(values, pool), source);
	}
	/**
	 * Start with the given iterable of values before relaying the Ts from the
	 * source. The value is emitted on the default pool.
	 * @param <T> the element type
	 * @param source the source
	 * @param value the single value to start with
	 * @return the new observable
	 */
	@Nonnull
	public static <T> Observable<T> startWith(
			@Nonnull Observable<? extends T> source,
			T value) {
		return startWith(source, Collections.singleton(value), DEFAULT_SCHEDULER.get());
	}
	/**
	 * Start with the given iterable of values before relaying the Ts from the
	 * source. The value is emitted on the given pool.
	 * @param <T> the element type
	 * @param source the source
	 * @param value the value to start with
	 * @param pool the pool where the iterable values should be emitted
	 * @return the new observable
	 */
	@Nonnull
	public static <T> Observable<T> startWith(
			@Nonnull Observable<? extends T> source,
			T value,
			@Nonnull Scheduler pool) {
		return startWith(source, Collections.singleton(value), pool);
	}

	/**
	 * Computes and signals the sum of the values of the BigDecimal source.
	 * The source may not send nulls.
	 * @param source the source of BigDecimals to aggregate.
	 * @return the observable for the sum value
	 */
	@Nonnull
	public static Observable<BigDecimal> sumBigDecimal(
			@Nonnull final Observable<BigDecimal> source) {
		return aggregate(source, Functions.sumBigDecimal(), Functions.<BigDecimal, Integer>identityFirst());
	}
	/**
	 * Computes and signals the sum of the values of the BigInteger source.
	 * The source may not send nulls.
	 * @param source the source of BigIntegers to aggregate.
	 * @return the observable for the sum value
	 */
	@Nonnull
	public static Observable<BigInteger> sumBigInteger(
			@Nonnull final Observable<BigInteger> source) {
		return aggregate(source, Functions.sumBigInteger(), Functions.<BigInteger, Integer>identityFirst());
	}
	/**
	 * Computes and signals the sum of the values of the Double source.
	 * The source may not send nulls.
	 * @param source the source of Doubles to aggregate.
	 * @return the observable for the sum value
	 */
	@Nonnull
	public static Observable<Double> sumDouble(
			@Nonnull final Observable<Double> source) {
		return aggregate(source, Functions.sumDouble(), Functions.<Double, Integer>identityFirst());
	}
	/**
	 * Computes and signals the sum of the values of the Float source.
	 * The source may not send nulls.
	 * @param source the source of Floats to aggregate.
	 * @return the observable for the sum value
	 */
	@Nonnull
	public static Observable<Float> sumFloat(
			@Nonnull final Observable<Float> source) {
		return aggregate(source, Functions.sumFloat(), Functions.<Float, Integer>identityFirst());
	}
	/**
	 * Computes and signals the sum of the values of the Integer source.
	 * The source may not send nulls. An empty source produces an empty sum
	 * @param source the source of integers to aggregate.
	 * @return the observable for the sum value
	 */
	@Nonnull
	public static Observable<Integer> sumInt(
			@Nonnull final Observable<Integer> source) {
		return aggregate(source, Functions.sumInteger(), Functions.<Integer, Integer>identityFirst());
	}
	/**
	 * Computes and signals the sum of the values of the Integer source by using
	 * a double intermediate representation.
	 * The source may not send nulls. An empty source produces an empty sum
	 * @param source the source of integers to aggregate.
	 * @return the observable for the sum value
	 */
	@Nonnull
	public static Observable<Double> sumIntAsDouble(
			@Nonnull final Observable<Integer> source) {
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
	 * Computes and signals the sum of the values of the Long source.
	 * The source may not send nulls.
	 * @param source the source of longs to aggregate.
	 * @return the observable for the sum value
	 */
	@Nonnull
	public static Observable<Long> sumLong(
			@Nonnull final Observable<Long> source) {
		return aggregate(source, Functions.sumLong(), Functions.<Long, Integer>identityFirst());
	}
	/**
	 * Computes and signals the sum of the values of the Long sourceby using
	 * a double intermediate representation.
	 * The source may not send nulls.
	 * @param source the source of longs to aggregate.
	 * @return the observable for the sum value
	 */
	@Nonnull
	public static Observable<Double> sumLongAsDouble(
			@Nonnull final Observable<Long> source) {
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
	 * Returns an observer which relays Ts from the source observables in a way, when
	 * a new inner observable comes in, the previous one is deregistered and the new one is
	 * continued with. Basically, it is an unbounded ys.takeUntil(xs).takeUntil(zs)...
	 * @param <T> the element type
	 * @param sources the source of multiple observables of Ts.
	 * @return the new observable
	 */
	public static <T> Observable<T> switchToNext(final Observable<? extends Observable<? extends T>> sources) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				DefaultObserver<Observable<? extends T>> outer
				= new DefaultObserver<Observable<? extends T>>(false) {
					/** The inner observer. */
					@GuardedBy("lock")
					Closeable inner;

					DefaultObserver<T> innerObserver = new DefaultObserver<T>(lock, true) {
						@Override
						protected void onError(Throwable ex) {
							innerError(ex);
						}

						@Override
						protected void onFinish() {
							innerFinish();
						}

						@Override
						protected void onNext(T value) {
							observer.next(value);
						}

					};
					/** Called from the inner observer when an error condition occurs. */
					void innerError(Throwable ex) {
						error(ex);
					}
					/** Called from the inner observer when it finished. */
					void innerFinish() {
						observer.finish();
						close();
					}
					@Override
					protected void onClose() {
						Closeables.closeSilently(inner);
					}

					@Override
					protected void onError(Throwable ex) {
						observer.error(ex);
						close();
					}

					@Override
					protected void onFinish() {
						// nothing to do
					}
					@Override
					protected void onNext(Observable<? extends T> value) {
						Closeables.closeSilently(inner);
						inner = value.register(innerObserver);
					}
				};
				return sources.register(outer);
			}
		};
	}
	/**
	 * Creates an observable which takes the specified number of
	 * Ts from the source, unregisters and completes.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param count the number of elements to relay
	 * @return the new observable
	 */
	@Nonnull
	public static <T> Observable<T> take(
			@Nonnull final Observable<? extends T> source,
			final int count) {

		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				DefaultObserverEx<T> o = new DefaultObserverEx<T>(true) {
					/** The countdown. */
					protected int i = count;
					@Override
					protected void onError(Throwable ex) {
						observer.error(ex);
					}

					@Override
					protected void onFinish() {
						observer.finish();
					}

					@Override
					protected void onNext(T value) {
						observer.next(value);
						if (--i == 0) {
							finish();
						}
					}

				};
				return o.registerWith(source);
			}
		};
	}
	/**
	 * Returns an observable which returns the last <code>count</code>
	 * elements from the source observable.
	 * @param <T> the element type
	 * @param source the source of the elements
	 * @param count the number elements to return
	 * @return the new observable
	 */
	public static <T> Observable<T> takeLast(final Observable<? extends T> source, final int count) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					final CircularBuffer<T> buffer = new CircularBuffer<T>(count);

					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						while (!buffer.isEmpty()) {
							observer.next(buffer.take());
						}
						observer.finish();
					}

					@Override
					public void next(T value) {
						buffer.add(value);
					}
				});
			}
		};
	}
	/**
	 * Creates an observable which takes values from the source until
	 * the signaller produces a value. If the signaller never signals,
	 * all source elements are relayed.
	 * @param <T> the element type
	 * @param <U> the signaller element type, irrelevant
	 * @param source the source of Ts
	 * @param signaller the source of Us
	 * @return the new observable
	 */
	@Nonnull
	public static <T, U> Observable<T> takeUntil(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Observable<U> signaller) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				final Lock lock0 = new ReentrantLock(true);
				DefaultObserverEx<T> o = new DefaultObserverEx<T>(lock0, true) {
					/** Error call from the inner. */
					protected void innerError(Throwable t) {
						error(t);
					}
					/** Finish call from the inner. */
					protected void innerFinish() {
						finish();
					}
					@Override
					protected void onError(Throwable ex) {
						observer.error(ex);
					}
					@Override
					protected void onFinish() {
						observer.finish();
					}

					@Override
					protected void onNext(T value) {
						observer.next(value);
					}

					@Override
					protected void onRegister() {
						add("signaller", signaller.register(new Observer<U>() {
							@Override
							public void error(Throwable ex) {
								innerError(ex);
							}

							@Override
							public void finish() {
								innerFinish();
							}

							@Override
							public void next(U value) {
								innerFinish();
							}
						}));
					}
				};
				return o.registerWith(source);
			}
		};
	}
	/**
	 * Creates an observable which takes values from source until
	 * the predicate returns false for the current element, then skips the remaining values.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param predicate the predicate
	 * @return the new observable
	 */
	@Nonnull
	public static <T> Observable<T> takeWhile(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super T, Boolean> predicate) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				DefaultObserverEx<T> obs = new DefaultObserverEx<T>(true) {
					@Override
					public void onError(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void onFinish() {
						observer.finish();
					}
					@Override
					public void onNext(T value) {
						if (predicate.invoke(value)) {
							observer.next(value);
						} else {
							observer.finish();
							close();
						}
					}
				};
				return obs.registerWith(source);
			}
		};
	}
	/**
	 * Creates and observable which fires the last value
	 * from source when the given timespan elapsed without a new
	 * value occurring from the source. It is basically how Content Assistant
	 * popup works after the user pauses in its typing. Uses the default scheduler.
	 * @param <T> the value type
	 * @param source the source of Ts
	 * @param delay how much time should elapse since the last event to actually forward that event
	 * @param unit the delay time unit
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> throttle(
			@Nonnull final Observable<? extends T> source,
			final long delay,
			@Nonnull final TimeUnit unit) {
		return throttle(source, delay, unit, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Creates and observable which fires the last value
	 * from source when the given timespan elapsed without a new
	 * value occurring from the source. It is basically how Content Assistant
	 * popup works after the user pauses in its typing.
	 * @param <T> the value type
	 * @param source the source of Ts
	 * @param delay how much time should elapse since the last event to actually forward that event
	 * @param unit the delay time unit
	 * @param pool the pool where the delay-watcher should operate
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> throttle(
			@Nonnull final Observable<? extends T> source,
			final long delay,
			@Nonnull final TimeUnit unit,
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				final DefaultObserver<T> obs = new DefaultObserver<T>(true) {
					/** The last seen value. */
					T last;
					/** The closeable. */
					Closeable c;
					/** The timeout action. */
					final DefaultRunnable r = new DefaultRunnable(lock) {
						@Override
						public void onRun() {
							if (!cancelled()) {
								observer.next(last);
							}
						}
					};
					@Override
					protected void onClose() {
						Closeables.closeSilently(c);
					}
					@Override
					public void onError(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void onFinish() {
						observer.finish();
					}
					@Override
					public void onNext(T value) {
						last = value;
						Closeables.closeSilently(c);
						c = pool.schedule(r, delay, unit);
					}
				};
				return Closeables.newCloseable(obs, source.register(obs));
			}
		};
	}
	
	/**
	 * Creates an observable which instantly sends the exception 
	 * returned by the supplier function to
	 * its subscribers while running on the default pool.
	 * @param <T> the element type, irrelevant
	 * @param <E> the exception type
	 * @param supplier the exception supplier
	 * @return the new observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T, E extends Throwable> Observable<T> throwException(
			@Nonnull final Func0<E> supplier) {
		return throwException(supplier, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Creates an observable which instantly sends the exception 
	 * returned by the function to
	 * its subscribers while running on the given pool.
	 * @param <T> the element type, irrelevant
	 * @param <E> the exception type
	 * @param supplier the function that supplies the exception
	 * @param pool the pool from where to send the values
	 * @return the new observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T, E extends Throwable> Observable<T> throwException(
			@Nonnull final Func0<E> supplier,
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return pool.schedule(new Runnable() {
					@Override
					public void run() {
						observer.error(supplier.invoke());
					}
				});
			}
		};
	}
	/**
	 * Creates an observable which instantly sends the exception to
	 * its subscribers while running on the default pool.
	 * @param <T> the element type, irrelevant
	 * @param ex the exception to throw
	 * @return the new observable
	 */
	@Nonnull
	public static <T> Observable<T> throwException(
			@Nonnull final Throwable ex) {
		return throwException(ex, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Creates an observable which instantly sends the exception to
	 * its subscribers while running on the given pool.
	 * @param <T> the element type, irrelevant
	 * @param ex the exception to throw
	 * @param pool the pool from where to send the values
	 * @return the new observable
	 */
	@Nonnull
	public static <T> Observable<T> throwException(
			@Nonnull final Throwable ex,
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return pool.schedule(new Runnable() {
					@Override
					public void run() {
						observer.error(ex);
					}
				});
			}
		};
	}
	/**
	 * Returns an observable which produces an ordered sequence of numbers with the specified delay.
	 * It uses the default scheduler pool.
	 * @param start the starting value of the tick
	 * @param end the finishing value of the tick exclusive
	 * @param delay the delay value
	 * @param unit the time unit of the delay
	 * @return the observer
	 */
	@Nonnull
	public static Observable<Long> tick(
			final long start,
			final long end,
			final long delay,
			@Nonnull final TimeUnit unit) {
		return tick(start, end, delay, unit, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Returns an observable which produces an ordered sequence of numbers with the specified delay.
	 * @param start the starting value of the tick inclusive
	 * @param end the finishing value of the tick exclusive
	 * @param delay the delay value
	 * @param unit the time unit of the delay
	 * @param pool the scheduler pool for the wait
	 * @return the observer
	 */
	@Nonnull
	public static Observable<Long> tick(
			final long start,
			final long end,
			final long delay,
			@Nonnull final TimeUnit unit,
			@Nonnull final Scheduler pool) {
		if (start > end) {
			throw new IllegalArgumentException("ensure start <= end");
		}
		return new Observable<Long>() {
			@Override
			public Closeable register(final Observer<? super Long> observer) {
				return pool.schedule(new DefaultRunnable() {
					/** The current value. */
					long current = start;
					@Override
					protected void onRun() {
						if (current < end && !cancelled()) {
							observer.next(current++);
						} else {
							if (!cancelled()) {
								observer.finish();
							}
							cancel();
						}
					}
				}, delay, delay, unit);
			}
		};
	}
	/**
	 * Returns an observable which produces an ordered sequence of numbers with the specified delay.
	 * It uses the default scheduler pool.
	 * @param delay the delay value
	 * @param unit the time unit of the delay
	 * @return the observer
	 */
	@Nonnull
	public static Observable<Long> tick(
			@Nonnull final long delay,
			@Nonnull final TimeUnit unit) {
		return tick(0, Long.MAX_VALUE, delay, unit, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Creates an observable which relays events if they arrive
	 * from the source observable within the specified amount of time
	 * or it singlals a java.util.concurrent.TimeoutException.
	 * @param <T> the element type to observe
	 * @param source the source observable
	 * @param time the maximum allowed timespan between events
	 * @param unit the time unit
	 * @return the observer.
	 */
	@Nonnull
	public static <T> Observable<T> timeout(
			@Nonnull final Observable<? extends T> source,
			final long time,
			@Nonnull final TimeUnit unit) {
		return timeout(source, time, unit, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Creates an observable which relays events if they arrive
	 * from the source observable within the specified amount of time
	 * or it switches to the <code>other</code> observable.
	 * @param <T> the element type to observe
	 * @param source the source observable
	 * @param time the maximum allowed timespan between events
	 * @param unit the time unit
	 * @param other the other observable to continue with in case a timeout occurs
	 * @return the observer.
	 */
	@Nonnull
	public static <T> Observable<T> timeout(
			@Nonnull final Observable<? extends T> source,
			final long time,
			@Nonnull final TimeUnit unit,
			@Nonnull final Observable<? extends T> other) {
		return timeout(source, time, unit, other, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Creates an observable which relays events if they arrive
	 * from the source observable within the specified amount of time
	 * or it switches to the <code>other</code> observable.
	 * @param <T> the element type to observe
	 * @param source the source observable
	 * @param time the maximum allowed timespan between events
	 * @param unit the time unit
	 * @param other the other observable to continue with in case a timeout occurs
	 * @param pool the scheduler pool for the timeout evaluation
	 * @return the observer.
	 */
	@Nonnull
	public static <T> Observable<T> timeout(
			@Nonnull final Observable<? extends T> source,
			final long time,
			@Nonnull final TimeUnit unit,
			@Nonnull final Observable<? extends T> other,
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				DefaultObserverEx<T> obs = new DefaultObserverEx<T>(true) {
					@Override
					protected void onError(Throwable ex) {
						observer.error(ex);
					}

					@Override
					protected void onFinish() {
						observer.finish();
					}

					@Override
					protected void onNext(T value) {
						remove("timer");
						observer.next(value);
						registerTimer();
					}
					/**
					 * Register the timer that when fired, switches to the second
					 * observable sequence
					 */
					private void registerTimer() {
						replace("timer", "timer", pool.schedule(new DefaultRunnable(lock) {
							@Override
							public void onRun() {
								if (!cancelled()) {
									registerWith(other);
								}
							}
						}, time, unit));
					}
					@Override
					protected void onRegister() {
						registerTimer();
					}
				};
				return obs.registerWith(source);
			}
		};
	}
	/**
	 * Creates an observable which relays events if they arrive
	 * from the source observable within the specified amount of time
	 * or it singlals a java.util.concurrent.TimeoutException.
	 * @param <T> the element type to observe
	 * @param source the source observable
	 * @param time the maximum allowed timespan between events
	 * @param unit the time unit
	 * @param pool the scheduler pool for the timeout evaluation
	 * @return the observer.
	 */
	@Nonnull
	public static <T> Observable<T> timeout(
			@Nonnull final Observable<? extends T> source,
			final long time,
			@Nonnull final TimeUnit unit,
			@Nonnull final Scheduler pool) {
		Observable<T> other = throwException(new TimeoutException());
		return timeout(source, time, unit, other, pool);
	}
	/**
	 * Creates an array from the observable sequence elements by using the given
	 * array for the template to create a dynamicly typed array of Ts.
	 * <p><b>Exception semantics:</b> if the source throws an exception, that exception
	 * is forwarded (e.g., no partial array is created).</p>
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @param a the template array, noes not change its value
	 * @return the observable
	 */
	public static <T> Observable<T[]> toArray(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final T[] a) {
		final Class<?> ct = a.getClass().getComponentType();
		return new Observable<T[]>() {
			@Override
			public Closeable register(final Observer<? super T[]> observer) {
				return source.register(new Observer<T>() {
					/** The buffer for the Ts. */
					final List<T> list = new LinkedList<T>();
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						@SuppressWarnings("unchecked") T[] arr = (T[])Array.newInstance(ct, list.size());
						observer.next(list.toArray(arr));
						observer.finish();
					}

					@Override
					public void next(T value) {
						list.add(value);
					}

				});
			}
		};
	}
	/**
	 * Creates an array from the observable sequence elements.
	 * <p><b>Exception semantics:</b> if the source throws an exception, that exception
	 * is forwarded (e.g., no partial array is created).</p>
	 * @param source the source of anything
	 * @return the object array
	 */
	@Nonnull
	public static Observable<Object[]> toArray(@Nonnull final Observable<?> source) {
		return toArray(source, new Object[0]);
	}
	/**
	 * Convert the given observable instance into a classical iterable instance.
	 * <p>The resulting iterable does not support the {@code remove()} method.</p>
	 * @param <T> the element type to iterate
	 * @param observable the original observable
	 * @return the iterable
	 */
	@Nonnull
	public static <T> CloseableIterable<T> toIterable(
			@Nonnull final Observable<? extends T> observable) {
		return new CloseableIterable<T>() {
			@Override
			public CloseableIterator<T> iterator() {
				final LinkedBlockingQueue<Option<T>> queue = new LinkedBlockingQueue<Option<T>>();

				final Closeable c = observable.register(new Observer<T>() {
					@Override
					public void error(Throwable ex) {
						queue.add(Option.<T>error(ex));
					}

					@Override
					public void finish() {
						queue.add(Option.<T>none());
					}

					@Override
					public void next(T value) {
						queue.add(Option.some(value));
					}

				});

				return new CloseableIterator<T>() {
					/** Close the association if there is no more elements. */
					Closeable close = c;
					/** The peek value due hasNext. */
					Option<T> peek;
					/** Indicator if there was a hasNext() call before the next() call. */
					boolean peekBeforeNext;
					/** Close the helper observer. */
					@Override
					public void close() throws IOException {
						Closeable cl = close;
						close = null;
						if (cl != null) {
							cl.close();
						}
					}
					@Override
					protected void finalize() throws Throwable {
						close();
					}
					@Override
					public boolean hasNext() {
						if (peek != Option.none()) {
							if (!peekBeforeNext) {
								try {
									peek = queue.take();
								} catch (InterruptedException e) {
									throw new RuntimeException(e);
								}
							}
							peekBeforeNext = true;
						}
						boolean result = peek != Option.none();
						return result;
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
							try {
								peek = queue.take();
							} catch (InterruptedException e) {
								throw new RuntimeException(e);
							}
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
	/**
	 * Collect the elements of the source observable into a single list.
	 * @param <T> the source element type
	 * @param source the source observable
	 * @return the new observable
	 */
	public static <T> Observable<List<T>> toList(
		final Observable<? extends T> source
	) {
		return new Observable<List<T>>() {
			@Override
			public Closeable register(final Observer<? super List<T>> observer) {
				return source.register(new Observer<T>() {
					/** The list for aggregation. */
					final List<T> list = new LinkedList<T>();
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.next(new ArrayList<T>(list));
						observer.finish();
					}

					@Override
					public void next(T value) {
						list.add(value);
					}

				});
			}
		};
	}
	/**
	 * Maps the given source of Ts by using the key extractor and
	 * returns a single Map of them. The keys are compared against each other
	 * by the <code>Object.equals()</code> semantics.
	 * <p><b>Exception semantics:</b> if the source throws an exception, that exception
	 * is forwarded (e.g., no partial map is created).</p>
	 * @param <T> the element type
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param source the source of Ts
	 * @param keySelector the key selector
	 * @param valueSelector the value selector
	 * @return the new observable
	 */
	public static <T, K, V> Observable<Map<K, V>> toMap(
			final Observable<? extends T> source,
			final Func1<? super T, ? extends K> keySelector,
			final Func1<? super T, ? extends V> valueSelector
	) {
		return new Observable<Map<K, V>>() {
			@Override
			public Closeable register(final Observer<? super Map<K, V>> observer) {
				return source.register(new Observer<T>() {
					/** The map. */
					final Map<K, V> map = new HashMap<K, V>();
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}
					@Override
					public void finish() {
						observer.next(map);
						observer.finish();
					}
					@Override
					public void next(T value) {
						map.put(keySelector.invoke(value), valueSelector.invoke(value));
					}
				});
			}
		};
	}
	/**
	 * Maps the given source of Ts by using the key and value extractor and
	 * returns a single Map of them.
	 * <p><b>Exception semantics:</b> if the source throws an exception, that exception
	 * is forwarded (e.g., no partial map is created).</p>
	 * @param <T> the element type
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param source the source of Ts
	 * @param keySelector the key selector
	 * @param valueSelector the value selector
	 * @param keyComparer the comparison function for keys
	 * @return the new observable
	 */
	public static <T, K, V> Observable<Map<K, V>> toMap(
			final Observable<? extends T> source,
			final Func1<? super T, ? extends K> keySelector,
			final Func1<? super T, ? extends V> valueSelector,
			final Func2<? super K, ? super K, Boolean> keyComparer
	) {
		return new Observable<Map<K, V>>() {
			@Override
			public Closeable register(final Observer<? super Map<K, V>> observer) {
				return source.register(new Observer<T>() {
					/** The key class with custom equality comparer. */
					class Key {
						/** The key value. */
						final K key;
						/**
						 * Constructor.
						 * @param key the key
						 */
						Key(K key) {
							this.key = key;
						}
						@Override
						public boolean equals(Object obj) {
							if (obj instanceof Key) {
								return keyComparer.invoke(key, ((Key)obj).key);
							}
							return false;
						}
						@Override
						public int hashCode() {
							return key != null ? key.hashCode() : 0;
						}
					}
					/** The map. */
					final Map<Key, V> map = new HashMap<Key, V>();
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						Map<K, V> result = new HashMap<K, V>();
						for (Map.Entry<Key, V> e : map.entrySet()) {
							result.put(e.getKey().key, e.getValue());
						}
						observer.next(result);
						observer.finish();
					}

					@Override
					public void next(T value) {
						Key k = new Key(keySelector.invoke(value));
						V v = valueSelector.invoke(value);
						map.put(k, v);
					}

				});
			}
		};
	}
	/**
	 * Maps the given source of Ts by using the key extractor and
	 * returns a single Map of them.
	 * <p><b>Exception semantics:</b> if the source throws an exception, that exception
	 * is forwarded (e.g., no partial map is created).</p>
	 * @param <T> the element type
	 * @param <K> the key type
	 * @param source the source of Ts
	 * @param keySelector the key selector
	 * @param keyComparer the key comparer function
	 * @return the new observable
	 */
	public static <K, T> Observable<Map<K, T>> toMap(
			final Observable<? extends T> source,
			final Func1<? super T, ? extends K> keySelector,
			final Func2<? super K, ? super K, Boolean> keyComparer
	) {
		return toMap(source, keySelector, Functions.<T>identity(), keyComparer);
	}
	/**
	 * Maps the given source of Ts by using the key extractor and
	 * returns a single Map of them. The keys are compared against each other
	 * by the <code>Object.equals()</code> semantics.
	 * <p><b>Exception semantics:</b> if the source throws an exception, that exception
	 * is forwarded (e.g., no partial map is created).</p>
	 * @param <T> the element type
	 * @param <K> the key type
	 * @param source the source of Ts
	 * @param keySelector the key selector
	 * @return the new observable
	 */
	public static <K, T> Observable<Map<K, T>> toMap(
			final Observable<T> source,
			final Func1<? super T, ? extends K> keySelector
	) {
		return toMap(source, keySelector, Functions.<T>identity());
	}
	/**
	 * Maps the given source of Ts by using the key  extractor and
	 * returns a single multi-map of them. The keys are compared against each other
	 * by the <code>Object.equals()</code> semantics.
	 * <p><b>Exception semantics:</b> if the source throws an exception, that exception
	 * is forwarded (e.g., no partial map is created).</p>
	 * @param <T> the element type
	 * @param <K> the key type
	 * @param source the source of Ts
	 * @param keySelector the key selector
	 * @param collectionSupplier the function which retuns a collection to hold the Vs.
	 * @return the new observable
	 */
	public static <T, K> Observable<Map<K, Collection<T>>> toMultiMap(
			final Observable<? extends T> source,
			final Func1<? super T, ? extends K> keySelector,
			final Func0<? extends Collection<T>> collectionSupplier
	) {
		return toMultiMap(
				source,
				keySelector,
				collectionSupplier,
				Functions.<T>identity());
	}
	/**
	 * Maps the given source of Ts by using the key extractor and
	 * returns a single multi-map of them.
	 * <p><b>Exception semantics:</b> if the source throws an exception, that exception
	 * is forwarded (e.g., no partial map is created).</p>
	 * @param <T> the element type
	 * @param <K> the key type
	 * @param source the source of Ts
	 * @param keySelector the key selector
	 * @param collectionSupplier the function which retuns a collection to hold the Vs.
	 * @param keyComparer the comparison function for keys
	 * @return the new observable
	 */
	public static <T, K> Observable<Map<K, Collection<T>>> toMultiMap(
			final Observable<? extends T> source,
			final Func1<? super T, ? extends K> keySelector,
			final Func0<? extends Collection<T>> collectionSupplier,
			final Func2<? super K, ? super K, Boolean> keyComparer
	) {
		return toMultiMap(
				source,
				keySelector,
				collectionSupplier,
				Functions.<T>identity(),
				keyComparer);
	}
	/**
	 * Maps the given source of Ts by using the key and value extractor and
	 * returns a single multi-map of them. The keys are compared against each other
	 * by the <code>Object.equals()</code> semantics.
	 * <p><b>Exception semantics:</b> if the source throws an exception, that exception
	 * is forwarded (e.g., no partial map is created).</p>
	 * @param <T> the element type
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param source the source of Ts
	 * @param keySelector the key selector
	 * @param collectionSupplier the function which retuns a collection to hold the Vs.
	 * @param valueSelector the value selector
	 * @return the new observable
	 * @see Functions#listSupplier()
	 * @see Functions#setSupplier()
	 */
	public static <T, K, V> Observable<Map<K, Collection<V>>> toMultiMap(
			final Observable<? extends T> source,
			final Func1<? super T, ? extends K> keySelector,
			final Func0<? extends Collection<V>> collectionSupplier,
			final Func1<? super T, ? extends V> valueSelector
	) {
		return new Observable<Map<K, Collection<V>>>() {
			@Override
			public Closeable register(final Observer<? super Map<K, Collection<V>>> observer) {
				return source.register(new Observer<T>() {
					/** The map. */
					final Map<K, Collection<V>> map = new HashMap<K, Collection<V>>();
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.next(map);
						observer.finish();
					}

					@Override
					public void next(T value) {
						K k = keySelector.invoke(value);
						Collection<V> coll = map.get(k);
						if (coll == null) {
							coll = collectionSupplier.invoke();
							map.put(k, coll);
						}
						V v = valueSelector.invoke(value);
						coll.add(v);
					}

				});
			}
		};
	}
	/**
	 * Maps the given source of Ts by using the key and value extractor and
	 * returns a single multi-map of them.
	 * <p><b>Exception semantics:</b> if the source throws an exception, that exception
	 * is forwarded (e.g., no partial map is created).</p>
	 * @param <T> the element type
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param source the source of Ts
	 * @param keySelector the key selector
	 * @param collectionSupplier the function which retuns a collection to hold the Vs.
	 * @param valueSelector the value selector
	 * @param keyComparer the comparison function for keys
	 * @return the new observable
	 */
	public static <T, K, V> Observable<Map<K, Collection<V>>> toMultiMap(
			final Observable<? extends T> source,
			final Func1<? super T, ? extends K> keySelector,
			final Func0<? extends Collection<V>> collectionSupplier,
			final Func1<? super T, ? extends V> valueSelector,
			final Func2<? super K, ? super K, Boolean> keyComparer
	) {
		return new Observable<Map<K, Collection<V>>>() {
			@Override
			public Closeable register(final Observer<? super Map<K, Collection<V>>> observer) {
				return source.register(new Observer<T>() {
					/** The key class with custom equality comparer. */
					class Key {
						/** The key value. */
						final K key;
						/**
						 * Constructor.
						 * @param key the key
						 */
						Key(K key) {
							this.key = key;
						}
						@Override
						public boolean equals(Object obj) {
							if (obj instanceof Key) {
								return keyComparer.invoke(key, ((Key)obj).key);
							}
							return false;
						}
						@Override
						public int hashCode() {
							return key != null ? key.hashCode() : 0;
						}
					}
					/** The map. */
					final Map<Key, Collection<V>> map = new HashMap<Key, Collection<V>>();
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						Map<K, Collection<V>> result = new HashMap<K, Collection<V>>();
						for (Map.Entry<Key, Collection<V>> e : map.entrySet()) {
							result.put(e.getKey().key, e.getValue());
						}
						observer.next(result);
						observer.finish();
					}

					@Override
					public void next(T value) {
						Key k = new Key(keySelector.invoke(value));
						Collection<V> coll = map.get(k);
						if (coll == null) {
							coll = collectionSupplier.invoke();
							map.put(k, coll);
						}
						V v = valueSelector.invoke(value);
						coll.add(v);
					}

				});
			}
		};
	}
	/**
	 * Wrap the iterable object into an observable and use the
	 * default pool when generating the iterator sequence.
	 * @param <T> the type of the values
	 * @param iterable the iterable instance
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> toObservable(
			@Nonnull final Iterable<? extends T> iterable) {
		return toObservable(iterable, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Wrap the iterable object into an observable and use the
	 * given pool when generating the iterator sequence.
	 * @param <T> the type of the values
	 * @param iterable the iterable instance
	 * @param pool the thread pool where to generate the events from the iterable
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> toObservable(
			@Nonnull final Iterable<? extends T> iterable,
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				DefaultRunnable s = new DefaultRunnable() {
					@Override
					public void onRun() {
						for (T t : iterable) {
							if (cancelled()) {
								break;
							}
							observer.next(t);
						}

						if (!cancelled()) {
							observer.finish();
						}
					}
				};
				return pool.schedule(s);
			}
		};
	}
	/**
	 * Filters objects from source which are assignment compatible with T.
	 * Note that due java erasure complex generic types can't be filtered this way in runtime (e.g., List&lt;String>.class is just List.class).
	 * FIXME is this what cast stands for?
	 * @param <T> the type of the expected values
	 * @param source the source of unknown elements
	 * @param token the token to test agains the elements
	 * @return the observable containing Ts
	 */
	@Nonnull
	public static <T> Observable<T> typedAs(
			@Nonnull final Observable<?> source,
			@Nonnull final Class<T> token) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<Object>() {
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.finish();
					}

					@Override
					public void next(Object value) {
						if (token.isInstance(value)) {
							observer.next(token.cast(value));
						}
					}

				});
			}
		};
	}
	/**
	 * A convenience function which unwraps the T from a TimeInterval of T.
	 * @param <T> the value type
	 * @return the unwrapper function
	 */
	@Nonnull
	public static <T> Func1<TimeInterval<T>, T> unwrapTimeInterval() {
		return new Func1<TimeInterval<T>, T>() {
			@Override
			public T invoke(TimeInterval<T> param1) {
				return param1.value();
			}
		};
	}
	/**
	 * A convenience function which unwraps the T from a Timestamped of T.
	 * @param <T> the value type
	 * @return the unwrapper function
	 */
	@Nonnull
	public static <T> Func1<Timestamped<T>, T> unwrapTimestamped() {
		return new Func1<Timestamped<T>, T>() {
			@Override
			public T invoke(Timestamped<T> param1) {
				return param1.value();
			}
		};
	}
	/**
	 * Receives a resource from the resource selector and
	 * uses the resource until it terminates, then closes the resource.
	 * FIXME not sure how this method should work
	 * @param <T> the output resource type.
	 * @param <U> the closeable resource to work with
	 * @param resourceSelector the function that gives a resource
	 * @param resourceUsage a function that returns an observable of T for the given resource.
	 * @return the observable of Ts which terminates once the usage terminates
	 */
	@Nonnull
	public static <T, U extends Closeable> Observable<T> using(
			@Nonnull final Func0<? extends U> resourceSelector,
			@Nonnull final Func1<? super U, ? extends Observable<? extends T>> resourceUsage) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				final U resource = resourceSelector.invoke();
				return resourceUsage.invoke(resource).register(new Observer<T>() {
					@Override
					public void error(Throwable ex) {
						try {
							observer.error(ex);
						} finally {
							Closeables.closeSilently(resource);
						}
					}

					@Override
					public void finish() {
						try {
							observer.finish();
						} finally {
							Closeables.closeSilently(resource);
						}

					}

					@Override
					public void next(T value) {
						observer.next(value);
					}

				});
			}
		};
	}
	/**
	 * Creates a filtered observable where only Ts are relayed which satisfy the clause.
	 * The clause receives the index and the current element to test.
	 * The clauseFactory is used for each individual registering observer.
	 * This can be used to create memorizing filter functions such as distinct.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param clauseFactory the filter clause, the first parameter receives the current index, the second receives the current element
	 * @return the new observable
	 */
	@Nonnull
	public static <T> Observable<T> where(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func0<Func2<Integer, ? super T, Boolean>> clauseFactory) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** The current element index. */
					int index;
					/** The clause factory to use. */
					final Func2<Integer, ? super T, Boolean> clause = clauseFactory.invoke();
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.finish();
					}

					@Override
					public void next(T value) {
						if (clause.invoke(index, value)) {
							observer.next(value);
						}
						index++;
					}

				});
			}
		};
	}
	/**
	 * Creates a filtered observable where only Ts are relayed which satisfy the clause.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param clause the filter clause
	 * @return the new observable
	 */
	@Nonnull
	public static <T> Observable<T> where(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super T, Boolean> clause) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.finish();
					}

					@Override
					public void next(T value) {
						if (clause.invoke(value)) {
							observer.next(value);
						}
					}

				});
			}
		};
	}
	/**
	 * Creates a filtered observable where only Ts are relayed which satisfy the clause.
	 * The clause receives the index and the current element to test.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param clause the filter clause, the first parameter receives the current index, the second receives the current element
	 * @return the new observable
	 */
	@Nonnull
	public static <T> Observable<T> where(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func2<Integer, ? super T, Boolean> clause) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** The current element index. */
					int index;
					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						observer.finish();
					}

					@Override
					public void next(T value) {
						if (clause.invoke(index, value)) {
							observer.next(value);
						}
						index++;
					}

				});
			}
		};
	}
	/**
	 * Splits the source stream into separate observables once
	 * the windowClosing fires an event.
	 * @param <T> the element type to observe
	 * @param <U> the closing event type, irrelevant
	 * @param source the source of Ts
	 * @param windowClosing the source of the window splitting events
	 * @return the observable on sequences of observables of Ts
	 */
	@Nonnull
	public static <T, U> Observable<Observable<T>> window(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func0<? extends Observable<U>> windowClosing) {
		return window(source, windowClosing, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Splits the source stream into separate observables on
	 * each windowClosing event.
	 * FIXME not sure how to implement
	 * @param <T> the element type to observe
	 * @param <U> the closing event type, irrelevant
	 * @param source the source of Ts
	 * @param windowClosing the source of the window splitting events
	 * @param pool the pool where the first group is signalled from directly after
	 * the registration
	 * @return the observable on sequences of observables of Ts
	 */
	@Nonnull
	public static <T, U> Observable<Observable<T>> window(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func0<? extends Observable<U>> windowClosing,
			@Nonnull final Scheduler pool) {
		return new Observable<Observable<T>>() {
			@Override
			public Closeable register(final Observer<? super Observable<T>> observer) {
				// The current observable
				DefaultObserver<T> obs = new DefaultObserver<T>(true) {
					/** The current observable window. */
					@GuardedBy("lock")
					DefaultObservable<T> current;
					/** The window watcher. */
					final DefaultObserver<U> wo = new DefaultObserver<U>(lock, true) {
						@Override
						public void onError(Throwable ex) {
							innerError(ex);
						}

						@Override
						public void onFinish() {
							innerFinish();
						}

						@Override
						public void onNext(U value) {
							DefaultObservable<T> o = new DefaultObservable<T>();
							Observer<T> os = current;
							current = o;
							if (os != null) {
								os.finish();
							}
							observer.next(o);
						}

					};
					/** The close handler for the inner observer of closing events. */
					Closeable woc = windowClosing.invoke().register(wo);
					/**
					 * The scheduled action which will open the first window as soon as possible.
					 */
					Closeable openWindow = pool.schedule(new DefaultRunnable(lock) {
						@Override
						protected void onRun() {
							if (current == null) {
								DefaultObservable<T> o = new DefaultObservable<T>();
								current = o;
								observer.next(o);
							}
						}
					});
					/**
					 * The inner exception callback.
					 * @param ex the exception
					 */
					void innerError(Throwable ex) {
						error(ex);
					}
					/** The inner finish callback. */
					void innerFinish() {
						finish();
					}
					@Override
					public void onClose() {
						Closeables.closeSilently(woc);
						Closeables.closeSilently(openWindow);
					}

					@Override
					public void onError(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void onFinish() {
						observer.finish();
					}
					@Override
					public void onNext(T value) {
						if (current == null) {
							DefaultObservable<T> o = new DefaultObservable<T>();
							current = o;
							observer.next(o);
						}
						current.next(value);
					}
				};
				return Closeables.newCloseable(obs, source.register(obs));
			}
		};
	}
	/**
	 * Project the source elements into observable windows of size <code>count</code>
	 * and skip some initial values.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param count the count of elements
	 * @return the new observable
	 */
	public static <T> Observable<Observable<T>> window(
			final Observable<? extends T> source,
			int count
	) {
		return window(source, count, 0, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Project the source elements into observable windows of size <code>count</code>
	 * and skip some initial values.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param count the count of elements
	 * @param skip the elements to skip
	 * @return the new observable
	 */
	public static <T> Observable<Observable<T>> window(
			final Observable<? extends T> source,
			int count,
			int skip
	) {
		return window(source, count, skip, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Project the source elements into observable windows of size <code>count</code>
	 * and skip some initial values.
	 * FIXME implement
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param count the count of elements
	 * @param skip the elements to skip
	 * @param scheduler the scheduler
	 * @return the new observable
	 */
	public static <T> Observable<Observable<T>> window(
			final Observable<? extends T> source,
			final int count,
			final int skip,
			final Scheduler scheduler
	) {
		return new Observable<Observable<T>>() {
			@Override
			public Closeable register(final Observer<? super Observable<T>> observer) {
				final AtomicReference<DefaultObservable<T>> current = new AtomicReference<DefaultObservable<T>>();
				final AtomicInteger counter = new AtomicInteger(0);
				DefaultObserverEx<T> o = new DefaultObserverEx<T>(true) {
					/** The elements to skip at first. */
					int skipCount = skip;
					{
						registerTimer();
						add("source", source);
					}
					/**
					 * Create a new observable and reset the limit counter as well.
					 */
					void createNewObservable() {
						counter.set(count);
						DefaultObservable<T> d = current.get();
						if (d != null) {
							d.finish();
						}
						d = new DefaultObservable<T>();
						current.set(d);
						observer.next(d);
					}
					@Override
					protected void onError(Throwable ex) {
						remove("timer");
						DefaultObservable<T> d = current.get();
						d.error(ex);
						observer.error(ex);
					}
					@Override
					protected void onFinish() {
						remove("timer");
						DefaultObservable<T> d = current.get();
						d.finish();
						observer.finish();
					}

					@Override
					protected void onNext(T value) {
						if (skipCount > 0) {
							skipCount--;
							return;
						}

						if (counter.get() == 0 || current.get() == null) {
							createNewObservable();
						}
						counter.decrementAndGet();
						DefaultObservable<T> d = current.get();
						d.next(value);
					}

					void registerTimer() {
						replace("timer", "timer", scheduler.schedule(
							new DefaultRunnable(lock) {
								@Override
								protected void onRun() {
									// first only
									if (current.get() == null) {
										createNewObservable();
									}
								}
							}, 0, TimeUnit.MILLISECONDS
						));
					}

				};
				return o;
			}
		};
	}
	/**
	 * Projects each value of T into an observable which are closed by
	 * either the <code>count</code> limit or the elapsed timespan.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param count the maximum count of the elements in each window
	 * @param timeSpan the maximum time for each window
	 * @param unit the time unit
	 * @return the new observable
	 */
	public static <T> Observable<Observable<T>> window(
		final Observable<? extends T> source,
		final int count,
		final long timeSpan,
		final TimeUnit unit
	) {
		return window(source, count, timeSpan, unit, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Projects each value of T into an observable which are closed by
	 * either the <code>count</code> limit or the elapsed timespan.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param count the maximum count of the elements in each window
	 * @param timeSpan the maximum time for each window
	 * @param unit the time unit
	 * @param scheduler the scheduler
	 * @return the new observable
	 */
	public static <T> Observable<Observable<T>> window(
		final Observable<? extends T> source,
		final int count,
		final long timeSpan,
		final TimeUnit unit,
		final Scheduler scheduler
	) {
		return new Observable<Observable<T>>() {
			@Override
			public Closeable register(final Observer<? super Observable<T>> observer) {
				final AtomicReference<DefaultObservable<T>> current = new AtomicReference<DefaultObservable<T>>();
				final AtomicInteger counter = new AtomicInteger(0);
				DefaultObserverEx<T> o = new DefaultObserverEx<T>(true) {
					{
						registerTimer();
						add("source", source);
					}
					/**
					 * Create a new observable and reset the limit counter as well.
					 */
					void createNewObservable() {
						counter.set(count);
						DefaultObservable<T> d = current.get();
						if (d != null) {
							d.finish();
						}
						d = new DefaultObservable<T>();
						current.set(d);
						observer.next(d);
					}
					@Override
					protected void onError(Throwable ex) {
						remove("timer");
						DefaultObservable<T> d = current.get();
						d.error(ex);
						observer.error(ex);
					}
					@Override
					protected void onFinish() {
						remove("timer");
						DefaultObservable<T> d = current.get();
						d.finish();
						observer.finish();
					}

					@Override
					protected void onNext(T value) {
						if (counter.get() == 0 || current.get() == null) {
							createNewObservable();
						}
						counter.decrementAndGet();
						DefaultObservable<T> d = current.get();
						d.next(value);
					}

					void registerTimer() {
						replace("timer", "timer", scheduler.schedule(
							new DefaultRunnable(lock) {
								/** First run. */
								boolean first;
								@Override
								protected void onRun() {
									if (!first) {
										first = true;
										if (current.get() == null) {
											createNewObservable();
										}
									} else {
										createNewObservable();
									}
								}
							}, timeSpan, unit
						));
					}

				};
				return o;
			}
		};
	}
	/**
	 * Project the source elements into observable windows of size <code>count</code>
	 * and skip some initial values.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param count the count of elements
	 * @param scheduler the scheduler
	 * @return the new observable
	 */
	public static <T> Observable<Observable<T>> window(
			final Observable<? extends T> source,
			int count,
			Scheduler scheduler
	) {
		return window(source, count, 0, scheduler);
	}
	/**
	 * Project each of the source Ts into observable sequences separated by
	 * the timespan and initial timeskip values.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param timeSpan the timespan between window openings
	 * @param timeSkip the initial delay to open the first window
	 * @param unit the time unit
	 * @return the observable
	 */
	public static <T> Observable<Observable<T>> window(
		final Observable<? extends T> source,
		final long timeSpan,
		final long timeSkip,
		final TimeUnit unit
	) {
		return window(source, timeSpan, timeSkip, unit, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Project each of the source Ts into observable sequences separated by
	 * the timespan and initial timeskip values.
	 * FIXME implement
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param timeSpan the timespan between window openings
	 * @param timeSkip the initial delay to open the first window
	 * @param unit the time unit
	 * @param scheduler the scheduler
	 * @return the observable
	 */
	public static <T> Observable<Observable<T>> window(
		final Observable<? extends T> source,
		final long timeSpan,
		final long timeSkip,
		final TimeUnit unit,
		final Scheduler scheduler
	) {
		return new Observable<Observable<T>>() {
			@Override
			public Closeable register(final Observer<? super Observable<T>> observer) {
				final AtomicReference<DefaultObservable<T>> current = new AtomicReference<DefaultObservable<T>>();
				DefaultObserverEx<T> o = new DefaultObserverEx<T>(true) {
					{
						registerTimer();
						add("source", source);
					}
					/**
					 * Create a new observable and reset the limit counter as well.
					 */
					void createNewObservable() {
						DefaultObservable<T> d = current.get();
						if (d != null) {
							d.finish();
						}
						d = new DefaultObservable<T>();
						current.set(d);
						observer.next(d);
					}
					@Override
					protected void onError(Throwable ex) {
						remove("timer");
						DefaultObservable<T> d = current.get();
						d.error(ex);
						observer.error(ex);
					}
					@Override
					protected void onFinish() {
						remove("timer");
						DefaultObservable<T> d = current.get();
						d.finish();
						observer.finish();
					}

					@Override
					protected void onNext(T value) {
						DefaultObservable<T> d = current.get();
						if (d != null) {
							d.next(value);
						}
					}

					void registerTimer() {
						replace("timer", "timer", scheduler.schedule(
							new DefaultRunnable(lock) {
								@Override
								protected void onRun() {
									createNewObservable();
								}
							}, timeSkip, timeSpan, unit
						));
					}

				};
				return o;
			}
		};
	}
	/**
	 * Project each of the source Ts into observable sequences separated by
	 * the timespan and initial timeskip values.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param timeSpan the timespan between window openings
	 * @param unit the time unit
	 * @return the observable
	 */
	public static <T> Observable<Observable<T>> window(
		final Observable<? extends T> source,
		final long timeSpan,
		final TimeUnit unit
	) {
		return window(source, timeSpan, 0L, unit, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Project each of the source Ts into observable sequences separated by
	 * the timespan and initial timeskip values.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param timeSpan the timespan between window openings
	 * @param unit the time unit
	 * @param scheduler the scheduler
	 * @return the observable
	 */
	public static <T> Observable<Observable<T>> window(
		final Observable<? extends T> source,
		final long timeSpan,
		final TimeUnit unit,
		final Scheduler scheduler
	) {
		return window(source, timeSpan, 0L, unit, scheduler);
	}
	/**
	 * Splits the source stream into separate observables
	 * by starting at windowOpening events and closing at windowClosing events.
	 * FIXME not sure how to implement
	 * @param <T> the element type to observe
	 * @param <U> the opening event type, irrelevant
	 * @param <V> the closing event type, irrelevant
	 * @param source the source of Ts
	 * @param windowOpening te source of the window opening events
	 * @param windowClosing the source of the window splitting events
	 * @return the observable on sequences of observables of Ts
	 */
	@Nonnull
	public static <T, U, V> Observable<Observable<T>> window(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Observable<? extends U> windowOpening,
			@Nonnull final Func1<? super U, ? extends Observable<V>> windowClosing) {
		return new Observable<Observable<T>>() {
			@Override
			public Closeable register(final Observer<? super Observable<T>> observer) {
				final Lock lock = new ReentrantLock(true);
				final Map<U, DefaultObservable<T>> openWindows = new IdentityHashMap<U, DefaultObservable<T>>();
				final CompositeCloseable closeBoth = new CompositeCloseable();
				// relay Ts to open windows
				DefaultObserverEx<T> o1 = new DefaultObserverEx<T>(lock, true) {
					@Override
					protected void onClose() {
						super.onClose();
						Closeables.closeSilently(closeBoth);
					}

					@Override
					protected void onError(Throwable ex) {
						for (DefaultObservable<T> ot : openWindows.values()) {
							ot.error(ex);
						}
						observer.error(ex);
					}

					@Override
					protected void onFinish() {
						for (DefaultObservable<T> ot : openWindows.values()) {
							ot.finish();
						}
						observer.finish();
					}
					@Override
					protected void onNext(T value) {
						for (DefaultObservable<T> ot : openWindows.values()) {
							ot.next(value);
						}
					}
				};
				DefaultObserverEx<U> o2 = new DefaultObserverEx<U>(lock, true) {

					@Override
					protected void onClose() {
						super.onClose();
						Closeables.closeSilently(closeBoth);
					}

					@Override
					protected void onError(Throwable ex) {
						observer.error(ex);
					}

					@Override
					protected void onFinish() {
						observer.finish();
					}
					@Override
					protected void onNext(final U value) {
						final DefaultObservable<T> newWindow = new DefaultObservable<T>();
						openWindows.put(value, newWindow);
						add(value, windowClosing.invoke(value).register(new Observer<V>() {
							@Override
							public void error(Throwable ex) {
								openWindows.remove(value);
								newWindow.error(ex);
							}

							@Override
							public void finish() {
								openWindows.remove(value);
								newWindow.finish();
							}

							@Override
							public void next(V value) {
								// No op?!
							}

						}));
						observer.next(newWindow);
					}
				};

				closeBoth.add(o1, o2);
				o1.registerWith(source);
				o2.registerWith(windowOpening);
				return closeBoth;
			}
		};
	}
	/**
	 * Wrap the given type into a timestamped container of T.
	 * @param <T> the type of the contained element
	 * @return the function performing the wrapping
	 */
	@Nonnull
	public static <T> Func1<T, Timestamped<T>> wrapTimestamped() {
		return new Func1<T, Timestamped<T>>() {
			@Override
			public Timestamped<T> invoke(T param1) {
				return Timestamped.of(param1);
			}
		};

	}
	/**
	 * Creates an observable which waits for events from left
	 * and combines it with the next available value from the right iterable,
	 * applies the selector function and emits the resulting T.
	 * The error() and finish() signals are relayed to the output.
	 * The result is finished if the right iterator runs out of
	 * values before the left iterator.
	 * @param <T> the resulting element type
	 * @param <U> the value type streamed on the right iterable
	 * @param <V> the value type streamed on the left observable
	 * @param left the left iterable of Us
	 * @param right the right observable of Vs
	 * @param selector the selector taking the left Us and right Vs.
	 * @return the resulting observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T, U, V> Observable<V> zip(
			@Nonnull final Iterable<? extends T> left,
			@Nonnull final Observable<? extends U> right,
			@Nonnull final Func2<? super T, ? super U, ? extends V> selector) {
		return new Observable<V>() {
			@Override
			public Closeable register(final Observer<? super V> observer) {

				DefaultObserverEx<U> obs = new DefaultObserverEx<U>(true) {
					/** The second source. */
					final Iterator<? extends T> it = left.iterator();

					@Override
					public void onError(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void onFinish() {
						observer.finish();
					}
					@Override
					public void onNext(U u) {
						if (it.hasNext()) {
							T t = it.next();
							observer.next(selector.invoke(t, u));
						} else {
							observer.finish();
							close();
						}
					}
				};
				return obs.registerWith(right);
			}
		};
	}
	/**
	 * Creates an observable which waits for events from left
	 * and combines it with the next available value from the right iterable,
	 * applies the selector function and emits the resulting T.
	 * The error() and finish() signals are relayed to the output.
	 * The result is finished if the right iterator runs out of
	 * values before the left iterator.
	 * @param <T> the resulting element type
	 * @param <U> the value type streamed on the left observable
	 * @param <V> the value type streamed on the right iterable
	 * @param left the left observables of Us
	 * @param right the right iterable of Vs
	 * @param selector the selector taking the left Us and right Vs.
	 * @return the resulting observable
	 */
	@Nonnull
	public static <T, U, V> Observable<V> zip(
			@Nonnull final Observable<? extends T> left,
			@Nonnull final Iterable<? extends U> right,
			@Nonnull final Func2<? super T, ? super U, ? extends V> selector) {
		return new Observable<V>() {
			@Override
			public Closeable register(final Observer<? super V> observer) {

				DefaultObserverEx<T> obs = new DefaultObserverEx<T>(true) {
					/** The second source. */
					final Iterator<? extends U> it = right.iterator();

					@Override
					public void onError(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void onFinish() {
						observer.finish();
					}
					@Override
					public void onNext(T t) {
						if (it.hasNext()) {
							U u = it.next();
							observer.next(selector.invoke(t, u));
						} else {
							observer.finish();
							close();
						}
					}
				};
				return obs.registerWith(left);
			}
		};
	}
	/**
	 * Creates an observable which waits for events from left
	 * and combines it with the next available value from the right observable,
	 * applies the selector function and emits the resulting T.
	 * Basically it emits a T when both an U and V is available.
	 * The output stream throws error or terminates if any of the streams
	 * throws or terminates.
	 * FIXME not sure how to implement this, and how to close and signal
	 * @param <T> the resulting element type
	 * @param <U> the value type streamed on the left observable
	 * @param <V> the value type streamed on the right iterable
	 * @param left the left observables of Us
	 * @param right the right iterable of Vs
	 * @param selector the selector taking the left Us and right Vs.
	 * @return the resulting observable
	 */
	@Nonnull
	public static <T, U, V> Observable<T> zip(
			@Nonnull final Observable<? extends U> left,
			@Nonnull final Observable<? extends V> right,
			@Nonnull final Func2<U, V, T> selector) {
		return new Observable<T>() {
			@Override
			public Closeable register(final Observer<? super T> observer) {
				final LinkedBlockingQueue<U> queueU = new LinkedBlockingQueue<U>();
				final LinkedBlockingQueue<V> queueV = new LinkedBlockingQueue<V>();
				final CompositeCloseable closeBoth = new CompositeCloseable();
				final AtomicInteger wip = new AtomicInteger(2);
				final Lock lockBoth = new ReentrantLock(true);

				DefaultObserverEx<U> oU = new DefaultObserverEx<U>(lockBoth, false) {
					@Override
					public void onError(Throwable ex) {
						observer.error(ex);
						Closeables.closeSilently(closeBoth);
					}

					@Override
					public void onFinish() {
						if (wip.decrementAndGet() == 0) {
							observer.finish();
							Closeables.closeSilently(closeBoth);
						}
					}
					@Override
					public void onNext(U u) {
						V v = queueV.poll();
						if (v != null) {
							observer.next(selector.invoke(u, v));
						} else {
							if (wip.get() == 2) {
								queueU.add(u);
							} else {
								this.finish();
							}
						}
					}
				};
				DefaultObserverEx<V> oV = new DefaultObserverEx<V>(lockBoth, false) {
					@Override
					public void onError(Throwable ex) {
						observer.error(ex);
						Closeables.closeSilently(closeBoth);
					}

					@Override
					public void onFinish() {
						if (wip.decrementAndGet() == 0) {
							observer.finish();
							Closeables.closeSilently(closeBoth);
						}
					}
					@Override
					public void onNext(V v) {
						U u = queueU.poll();
						if (u != null) {
							observer.next(selector.invoke(u, v));
						} else {
							if (wip.get() == 2) {
								queueV.add(v);
							} else {
								this.finish();
							}
						}
					}
				};
				closeBoth.add(oU, oV);
				oU.registerWith(left);
				oV.registerWith(right);
				return closeBoth;
			}
		};
	}
	/**
	 * Aggregates the incoming sequence via the accumulator function
	 * and transforms the result value with the selector.
	 * @param <T> the incoming value type
	 * @param <U> the aggregation intermediate type
	 * @param <V> the result type
	 * @param source the source 
	 * @param seed the initial value for the aggregation
	 * @param accumulator the accumulation function
	 * @param resultSelector the result selector
	 * @return the new observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T, U, V> Observable<V> aggregate(
			@Nonnull final Observable<? extends T> source,
			final U seed,
			@Nonnull final Func2<? super U, ? super T, ? extends U> accumulator,
			@Nonnull final Func1<? super U, ? extends V> resultSelector
			) {
		return select(aggregate(source, seed, accumulator), resultSelector);
	}
	/**
	 * Returns a single element from the sequence at the index or throws	
	 * a NoSuchElementException if the sequence terminates before this index.
	 * <p>Exception semantics: errors from source are propagated as-is.</p>
	 * @param <T> the element type
	 * @param source the source sequence of Ts
	 * @param index the index to look at
	 * @return the observable which returns the element at index or an exception
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<T> elementAt(
			@Nonnull final Observable<? extends T> source,
			final int index
			) {
		return new Observable<T>() {
			@Override
			@Nonnull
			public Closeable register(@Nonnull final Observer<? super T> observer) {
				return (new DefaultObserverEx<T>() {
					int i = index;
					@Override
					protected void onNext(T value) {
						if (i == 0) {
							observer.next(value);
							observer.finish();
							close();
						}
						i--;
					}

					@Override
					protected void onError(Throwable ex) {
						observer.error(ex);
					}

					@Override
					protected void onFinish() {
						observer.error(new NoSuchElementException("index = " + index));
					}
					
				}).registerWith(source);
			}
		};
	}
	/**
	 * Returns a single element from the sequence at the index or the 
	 * default value if the sequence terminates before this index.
	 * <p>Exception semantics: errors from source are propagated as-is.</p>
	 * @param <T> the element type
	 * @param source the source sequence of Ts
	 * @param index the index to look at
	 * @param defaultValue the value to return if the sequence is sorter than index
	 * @return the observable which returns the element at index or the default value
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<T> elementAt(
			@Nonnull final Observable<? extends T> source,
			int index,
			T defaultValue
			) {
		return elementAt(source, index, Functions.constant0(defaultValue));
	}
	/**
	 * Returns a single element from the sequence at the index or the 
	 * default value supplied if the sequence terminates before this index.
	 * <p>Exception semantics: errors from source are propagated as-is.</p>
	 * @param <T> the element type
	 * @param source the source sequence of Ts
	 * @param index the index to look at
	 * @param defaultSupplier the function that will supply the default value 
	 * @return the observable which returns the element at index or the default value supplied
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<T> elementAt(
			@Nonnull final Observable<? extends T> source,
			final int index,
			@Nonnull final Func0<? extends T> defaultSupplier
			) {
		return new Observable<T>() {
			@Override
			@Nonnull
			public Closeable register(@Nonnull final Observer<? super T> observer) {
				return (new DefaultObserverEx<T>() {
					int i = index;
					@Override
					protected void onNext(T value) {
						if (i == 0) {
							observer.next(value);
							observer.finish();
							close();
						}
						i--;
					}

					@Override
					protected void onError(Throwable ex) {
						observer.error(ex);
					}

					@Override
					protected void onFinish() {
						observer.next(defaultSupplier.invoke());
						observer.finish();
					}
					
				}).registerWith(source);
			}
		};
	}
	/**
	 * Returns the first element of the source observable or
	 * the defaultValue if the source is empty, blocking if necessary.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param defaultValue the value to return if the source is empty
	 * @return the first element of the observable or the default value
	 * @since 0.97
	 */
	public static <T> T first(
			@Nonnull final Observable<? extends T> source, 
			T defaultValue) {
		return first(source, Functions.constant0(defaultValue));
	}
	/**
	 * Returns the first element of the source observable or
	 * the supplier's value if the source is empty, blocking if necessary.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param defaultSupplier the value to return if the source is empty
	 * @return the first element of the observable or the supplier's value
	 * @since 0.97
	 */
	public static <T> T first(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Func0<? extends T> defaultSupplier) {
		CloseableIterator<T> it = toIterable(source).iterator();
		try {
			if (it.hasNext()) {
				return it.next();
			}
		} finally {
			Closeables.closeSilently(it);
		}
		return defaultSupplier.invoke();
	}
	/**
	 * Returns an observable which takes the first value from the source observable
	 * as a single element or throws NoSuchElementException if the source is empty.
	 * <p>Exception semantics: errors from source are propagated as-is.</p>
	 * @param <T> the element type
	 * @param source the source sequence
	 * @return the new observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<T> firstAsync(
			@Nonnull final Observable<? extends T> source) {
		return new Observable<T>() {
			@Override
			@Nonnull
			public Closeable register(final Observer<? super T> observer) {
				return (new DefaultObserverEx<T>() {
					@Override
					protected void onNext(T value) {
						observer.next(value);
						observer.finish();
						close();
					}

					@Override
					protected void onError(Throwable ex) {
						observer.error(ex);
					}

					@Override
					protected void onFinish() {
						error(new NoSuchElementException());
					} 
					
				}).registerWith(source);
			}
		};
	}
	/**
	 * Returns an observable which takes the first value from the source observable
	 * as a single element or the default value if the source is empty.
	 * <p>Exception semantics: errors from source are propagated as-is.</p>
	 * @param <T> the element type
	 * @param source the source sequence
	 * @param defaultValue the default value to return
	 * @return the new observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<T> firstAsync(
			@Nonnull final Observable<? extends T> source,
			final T defaultValue) {
		return firstAsync(source, Functions.constant0(defaultValue));
	}
	/**
	 * Returns an observable which takes the first value from the source observable
	 * as a single element or the supplier's value if the source is empty.
	 * <p>Exception semantics: errors from source are propagated as-is.</p>
	 * @param <T> the element type
	 * @param source the source sequence
	 * @param defaultSupplier the default value supplier
	 * @return the new observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<T> firstAsync(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func0<? extends T> defaultSupplier) {
		return new Observable<T>() {
			@Override
			@Nonnull
			public Closeable register(final Observer<? super T> observer) {
				return (new DefaultObserverEx<T>() {
					@Override
					protected void onNext(T value) {
						observer.next(value);
						observer.finish();
						close();
					}

					@Override
					protected void onError(Throwable ex) {
						observer.error(ex);
					}

					@Override
					protected void onFinish() {
						observer.next(defaultSupplier.invoke());
						observer.finish();
					} 
					
				}).registerWith(source);
			}
		};
	}
	/**
	 * Returns the last element of the source observable or the
	 * default value if the source is empty.
	 * <p>Exception semantics: the exceptions thrown by the source are ignored and treated
	 * as termination signals.</p>
	 * @param <T> the type of the elements
	 * @param source the source of Ts
	 * @param defaultValue the value to provide if the source is empty
	 * @return the last element
	 * @since 0.97
	 */
	@Nonnull
	public static <T> T last(
			@Nonnull final Observable<? extends T> source,
			final T defaultValue) {
		return last(source, Functions.constant0(defaultValue));
	}
	/**
	 * Returns the last element of the source observable or the
	 * supplier's value if the source is empty.
	 * <p>Exception semantics: the exceptions thrown by the source are ignored and treated
	 * as termination signals.</p>
	 * @param <T> the type of the elements
	 * @param source the source of Ts
	 * @param defaultSupplier the function to provide the default value
	 * @return the last element
	 * @since 0.97
	 */
	@Nonnull
	public static <T> T last(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func0<? extends T> defaultSupplier) {
		final LinkedBlockingQueue<Option<T>> queue = new LinkedBlockingQueue<Option<T>>();
		Closeable c = source.register(new Observer<T>() {
			/** The current value. */
			T current;
			/** Are we the first? */
			boolean first = true;
			@Override
			public void error(Throwable ex) {
				queue.add(Option.<T>none());
			}

			@Override
			public void finish() {
				if (first) {
					queue.add(Option.<T>some(defaultSupplier.invoke()));
				} else {
					queue.add(Option.some(current));
				}
			}

			@Override
			public void next(T value) {
				first = false;
				current = value;
			}

		});
		try {
			Option<T> value = queue.take();
			c.close();
			if (value == Option.none()) {
				throw new NoSuchElementException();
			}
			return value.value();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	/**
	 * Returns an observable which relays the last element of the source observable
	 * or throws a NoSuchElementException() if the source is empty.
	 * <p>Exception semantics: errors from source are propagated as-is.</p>
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @return the new observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<T> lastAsync(@Nonnull final Observable<? extends T> source) {
		return new Observable<T>() {
			@Override
			@Nonnull
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** The first value is pending? */
					boolean first = true;
					/** The current value. */
					T current;
					@Override
					public void next(T value) {
						current = value;
						first = false;
					}

					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						if (first) {
							observer.error(new NoSuchElementException());
						} else {
							observer.next(current);
							observer.finish();
						}
					}
					
				});
			}
		};
	}
	/**
	 * Returns an observable which relays the last element of the source observable
	 * or the default value if the source is empty.
	 * <p>Exception semantics: errors from source are propagated as-is.</p>
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param defaultValue the default value to return in case the source is empty
	 * @return the new observable
	 * @since 0.97
	 */
	public static <T> Observable<T> lastAsync(
			@Nonnull final Observable<? extends T> source,
			final T defaultValue) {
		return lastAsync(source, Functions.constant0(defaultValue));
	}
	/**
	 * Returns an observable which relays the last element of the source observable
	 * or the supplier's value if the source is empty.
	 * <p>Exception semantics: errors from source are propagated as-is.</p>
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param defaultSupplier the supplier to produce a value to return in case the source is empty
	 * @return the new observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<T> lastAsync(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func0<? extends T> defaultSupplier) {
		return new Observable<T>() {
			@Override
			@Nonnull
			public Closeable register(final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** The first value is pending? */
					boolean first = true;
					/** The current value. */
					T current;
					@Override
					public void next(T value) {
						current = value;
						first = false;
					}

					@Override
					public void error(Throwable ex) {
						observer.error(ex);
					}

					@Override
					public void finish() {
						if (first) {
							observer.next(defaultSupplier.invoke());
						} else {
							observer.next(current);
						}
						observer.finish();
					}
					
				});
			}
		};
	}
	/**
	 * Returns the only element of the source or throws
	 * NoSuchElementException if the source is empty or TooManyElementsException if
	 * it contains more than one elements.
	 * @param <T> the element type
	 * @param source the source sequence of Ts
	 * @return the new observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<T> singleAsync(
			@Nonnull final Observable<? extends T> source) {
		return new Observable<T>() {
			@Override
			@Nonnull
			public Closeable register(final Observer<? super T> observer) {
				return (new DefaultObserverEx<T>() {
					/** True if the first element received. */
					boolean firstReceived;
					/** The first element encountered. */
					T first;
					@Override
					protected void onNext(T value) {
						if (!firstReceived) {
							first = value;
							firstReceived = true;
						} else {
							error(new TooManyElementsException());
						}
					}

					@Override
					protected void onError(Throwable ex) {
						observer.error(ex);
					}

					@Override
					protected void onFinish() {
						if (firstReceived) {
							observer.next(first);
							observer.finish();
						} else {
							observer.error(new NoSuchElementException());
						}
					}
					
				}).registerWith(source);
			}
		};
	}
	/**
	 * Returns the only element of the source, 
	 * returns the default value if the source is empty or TooManyElementsException if
	 * it contains more than one elements.
	 * @param <T> the element type
	 * @param source the source sequence of Ts
	 * @param defaultValue the default value to return in case the source is empty
	 * @return the new observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<T> singleAsync(
			@Nonnull final Observable<? extends T> source,
			@Nonnull T defaultValue) {
		return singleAsync(source, Functions.constant0(defaultValue));
	}
	/**
	 * Returns the only element of the source, 
	 * returns the supplier's value if the source is empty or TooManyElementsException if
	 * it contains more than one elements.
	 * @param <T> the element type
	 * @param source the source sequence of Ts
	 * @param defaultSupplier the function that produces
	 * @return the new observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<T> singleAsync(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func0<? extends T> defaultSupplier) {
		return new Observable<T>() {
			@Override
			@Nonnull
			public Closeable register(final Observer<? super T> observer) {
				return (new DefaultObserverEx<T>() {
					/** True if the first element received. */
					boolean firstReceived;
					/** The first element encountered. */
					T first;
					@Override
					protected void onNext(T value) {
						if (!firstReceived) {
							first = value;
							firstReceived = true;
						} else {
							error(new TooManyElementsException());
						}
					}

					@Override
					protected void onError(Throwable ex) {
						observer.error(ex);
					}

					@Override
					protected void onFinish() {
						if (firstReceived) {
							observer.next(first);
							observer.finish();
						} else {
							observer.next(defaultSupplier.invoke());
							observer.finish();
						}
					}
					
				}).registerWith(source);
			}
		};
	}
	/**
	 * Returns the single element of the given observable source,
	 * returns the default if the source is empty or throws a 
	 * TooManyElementsException in case the source has more than one item.
	 * @param <T> the type of the element
	 * @param source the source of Ts
	 * @param defaultValue the value to return if the source is empty
	 * @return the single element
	 * @see #first(Observable, Object)
	 * @since 0.97
	 */
	@Nonnull
	public static <T> T single(
			@Nonnull Observable<? extends T> source,
			T defaultValue) {
		return single(source, Functions.constant0(defaultValue));
	}
	/**
	 * Returns the single element of the given observable source,
	 * returns the supplier's value if the source is empty or throws a 
	 * TooManyElementsException in case the source has more than one item.
	 * @param <T> the type of the element
	 * @param source the source of Ts
	 * @param defaultSupplier the function that produces the default value
	 * @return the single element
	 * @see #first(Observable, Func0)
	 * @since 0.97
	 */
	@Nonnull
	public static <T> T single(
			@Nonnull Observable<? extends T> source,
			@Nonnull Func0<? extends T> defaultSupplier) {
		CloseableIterator<T> it = toIterable(source).iterator();
		try {
			if (it.hasNext()) {
				T one = it.next();
				if (!it.hasNext()) {
					return one;
				}
				throw new TooManyElementsException();
			}
		} finally {
			Closeables.closeSilently(it);
		}
		return defaultSupplier.invoke();
	}
	/**
	 * Observes the source observables in parallel on the default scheduler and collects their individual
	 * values value streams, blocking in the process. 
	 * @param <T> the common element type
	 * @param sources the source sequences
	 * @return the for each source, the list of their value streams.
	 * @throws InterruptedException if the wait is interrupted
	 * @since 0.97
	 */
	@Nonnull 
	public static <T> List<List<T>> invokeAll(
			@Nonnull final Iterable<? extends Observable<? extends T>> sources) throws InterruptedException {
		return invokeAll(sources, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Observes the source observables in parallel on the default scheduler and collects their individual
	 * values value streams, blocking in the process. 
	 * @param <T> the common element type
	 * @param source1 the first source
	 * @param source2 the second source
	 * @return the for each source, the list of their value streams.
	 * @throws InterruptedException if the wait is interrupted
	 * @since 0.97
	 */
	@Nonnull 
	public static <T> List<List<T>> invokeAll(
			@Nonnull final Observable<? extends T> source1,
			@Nonnull final Observable<? extends T> source2
			) throws InterruptedException {
		return invokeAll(source1, source2, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Observes the source observables in parallel on the given scheduler and collects their individual
	 * values value streams, blocking in the process. 
	 * @param <T> the common element type
	 * @param source1 the first source
	 * @param source2 the second source
	 * @param scheduler the target scheduler
	 * @return the for each source, the list of their value streams.
	 * @throws InterruptedException if the wait is interrupted
	 * @since 0.97
	 */
	@Nonnull 
	public static <T> List<List<T>> invokeAll(
			@Nonnull final Observable<? extends T> source1,
			@Nonnull final Observable<? extends T> source2,
			@Nonnull final Scheduler scheduler
			) throws InterruptedException {
		List<Observable<? extends T>> sources = new ArrayList<Observable<? extends T>>();
		sources.add(source1);
		sources.add(source2);
		return invokeAll(sources, scheduler);
	}
	/**
	 * Observes the source observables in parallel on the given scheduler and collects their individual
	 * values value streams, blocking in the process. 
	 * @param <T> the common element type
	 * @param sources the source sequences
	 * @param scheduler the scheduler
	 * @return the for each source, the list of their value streams.
	 * @throws InterruptedException if the wait is interrupted
	 * @since 0.97
	 */
	@Nonnull 
	public static <T> List<List<T>> invokeAll(
			@Nonnull final Iterable<? extends Observable<? extends T>> sources,
			@Nonnull Scheduler scheduler) throws InterruptedException {
		
		List<List<T>> result = new ArrayList<List<T>>();
		List<Observable<Pair<Integer, T>>> taggedSources = new ArrayList<Observable<Pair<Integer, T>>>();
		
		int idx = 0;
		for (Observable<? extends T> o : sources) {
			final int fidx = idx;
			Observable<Pair<Integer, T>> tagged = select(o, new Func1<T, Pair<Integer, T>>() { 
				@Override
				public Pair<Integer, T> invoke(T param1) {
					return Pair.of(fidx, param1);
				}
			});
			taggedSources.add(tagged);
			result.add(new ArrayList<T>());
			idx++;
		}
		
		Observable<Pair<Integer, T>> merged = merge(taggedSources);

		CloseableIterator<Pair<Integer, T>> it = toIterable(merged).iterator();
		try {
			while (it.hasNext()) {
				Pair<Integer, T> pair = it.next();
				result.get(pair.first).add(pair.second);
			}
		} finally {
			Closeables.closeSilently(it);
		}
		
		
		return result;
	}
	/**
	 * Creates an observable which finishes its observers after the specified
	 * amount of time if no error or finish events appeared till then.
	 * @param <T> the element type.
	 * @param source the source sequence
	 * @param time the time to wait
	 * @param unit the time unit to wait
	 * @param scheduler the scheduler used for the wait
	 * @return the new observable
	 * @since 0.97
	 */
	@Nonnull 
	public static <T> Observable<T> timeoutFinish(
			@Nonnull final Observable<? extends T> source, 
			final long time, 
			@Nonnull final TimeUnit unit,
			@Nonnull final Scheduler scheduler) {
		return timeout(source, time, unit, Reactive.<T>empty(scheduler), scheduler);
	}
	/**
	 * Creates an observable which finishes its observers after the specified
	 * amount of time if no error or finish events appeared till then.
	 * @param <T> the element type.
	 * @param source the source sequence
	 * @param time the time to wait
	 * @param unit the time unit to wait
	 * @return the new observable
	 * @since 0.97
	 */
	@Nonnull 
	public static <T> Observable<T> timeoutFinish(
			@Nonnull final Observable<? extends T> source, 
			final long time, 
			@Nonnull final TimeUnit unit) {
		return timeout(source, time, unit, Reactive.<T>empty());
	}
	/**
	 * Schedules an active tick timer on the specified scheduler to which
	 * observers can register or deregister getting the current tick values.
	 * @param start the start of the counter
	 * @param end the end of the counter
	 * @param time the initial and between delay of the ticks
	 * @param unit the time unit of the ticks
	 * @return the closeable observable that produces the values
	 * and lets the whole timer cancel
	 * @since 0.97
	 */
	@Nonnull
	public static CloseableObservable<Long> activeTick(
			final long start, 
			final long end, 
			long time, @Nonnull TimeUnit unit) {
		return activeTick(start, end, time, unit, DEFAULT_SCHEDULER.get());
	}
	/**
	 * Schedules an active tick timer on the specified scheduler to which
	 * observers can register or deregister getting the current tick values.
	 * @param start the start of the counter
	 * @param end the end of the counter
	 * @param time the initial and between delay of the ticks
	 * @param unit the time unit of the ticks
	 * @param scheduler the scheduler where the ticks are performed
	 * @return the closeable observable that produces the values
	 * and lets the whole timer cancel
	 * @since 0.97
	 */
	@Nonnull
	public static CloseableObservable<Long> activeTick(
			final long start, 
			final long end, 
			long time, @Nonnull TimeUnit unit, 
			@Nonnull Scheduler scheduler) {

		final DefaultObservable<Long> result = new DefaultObservable<Long>();

		final Closeable timer = scheduler.schedule(new DefaultRunnable() {
			/** The value. */
			long value = start;
			@Override
			protected void onRun() {
				if (value < end) {
					result.next(value++);
				}
				if (value >= end) {
					result.finish();
					cancel();
				}
			}
		}, time, time, unit);
		
		return new CloseableObservable<Long>() {
			@Override
			@Nonnull
			public Closeable register(Observer<? super Long> observer) {
				return result.register(observer);
			}

			@Override
			public void close() throws IOException {
				Closeables.close(timer, result);
			}
		};
	}
	/**
	 * Returns a connectable observable which uses a single registration
	 * to the underlying source sequence containing only the last value.
	 * @param <T> the element type
	 * @param source the source sequence
	 * @return the new observable
	 * @since 0.97
	 */
	public static <T> ConnectableObservable<T> publishLast(
			@Nonnull final Observable<T> source
			) {
		return multicast(source, new AsyncSubject<T>());
	}
	/**
	 * Retunrs an observable that is the result of the selector invocation
	 * on a connectable observable that shares a single registration to
	 * <code>source</code> and returns the last event of the source.
	 * @param <T> the element type
	 * @param <U> the result type
	 * @param source the source sequence
	 * @param selector function that can use the multicasted source as many times as necessary without causing new registrations to source
	 * @return the new observable
	 * @since 0.97
	 */
	public static <T, U> Observable<U> publishLast(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super Observable<? extends T>, ? extends Observable<? extends U>> selector
			) {
		return multicast(source, new Func0<Subject<T, T>>() { 
			@Override
			public Subject<T, T> invoke() {
				return new AsyncSubject<T>();
			}
		}, selector);
	}
	/** Utility class. */
	private Reactive() {
		// utility class
	}
}
