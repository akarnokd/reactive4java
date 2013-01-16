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
import hu.akarnokd.reactive4java.base.Action2;
import hu.akarnokd.reactive4java.base.CloseableIterable;
import hu.akarnokd.reactive4java.base.CloseableIterator;
import hu.akarnokd.reactive4java.base.CloseableObservable;
import hu.akarnokd.reactive4java.base.ConnectableObservable;
import hu.akarnokd.reactive4java.base.DoubleObservable;
import hu.akarnokd.reactive4java.base.Func0;
import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Func2;
import hu.akarnokd.reactive4java.base.GroupedObservable;
import hu.akarnokd.reactive4java.base.IntObservable;
import hu.akarnokd.reactive4java.base.LongObservable;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;
import hu.akarnokd.reactive4java.base.Option;
import hu.akarnokd.reactive4java.base.Pair;
import hu.akarnokd.reactive4java.base.Pred0;
import hu.akarnokd.reactive4java.base.Scheduler;
import hu.akarnokd.reactive4java.base.Subject;
import hu.akarnokd.reactive4java.base.TimeInterval;
import hu.akarnokd.reactive4java.base.Timestamped;
import hu.akarnokd.reactive4java.base.TooManyElementsException;
import hu.akarnokd.reactive4java.interactive.Interactive;
import hu.akarnokd.reactive4java.scheduler.SingleLaneExecutor;
import hu.akarnokd.reactive4java.util.AsyncSubject;
import hu.akarnokd.reactive4java.util.CircularBuffer;
import hu.akarnokd.reactive4java.util.Closeables;
import hu.akarnokd.reactive4java.util.CompositeCloseable;
import hu.akarnokd.reactive4java.util.DefaultConnectableObservable;
import hu.akarnokd.reactive4java.util.DefaultObservable;
import hu.akarnokd.reactive4java.util.DefaultObserver;
import hu.akarnokd.reactive4java.util.DefaultObserverEx;
import hu.akarnokd.reactive4java.util.DefaultRunnable;
import hu.akarnokd.reactive4java.util.Functions;
import hu.akarnokd.reactive4java.util.ScheduledCloseable;
import hu.akarnokd.reactive4java.util.Schedulers;
import hu.akarnokd.reactive4java.util.SequentialCloseable;
import hu.akarnokd.reactive4java.util.SingleCloseable;
import hu.akarnokd.reactive4java.util.Subjects;
import hu.akarnokd.reactive4java.util.Throwables;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
 * <li><a href='http://rx.codeplex.com'>Rx open-source at CodePlex</a></li>
 * </ul>
 *
 * @author akarnokd, 2011.01.26
 * @see hu.akarnokd.reactive4java.interactive.Interactive
 */
public final class Reactive {
	/** The diagnostic states of the current runnable. */
	public enum ObserverState { OBSERVER_ERROR, OBSERVER_FINISHED, OBSERVER_RUNNING }
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
		return activeTick(start, end, time, unit, scheduler());
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
			long time, 
			@Nonnull TimeUnit unit, 
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
			public void close() throws IOException {
				Closeables.close(timer, result);
			}

			@Override
			@Nonnull
			public Closeable register(@Nonnull Observer<? super Long> observer) {
				return result.register(observer);
			}
		};
	}
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
		return new AddTimeInterval<T>(source);
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
		return new Aggregate.Simple<T>(source, accumulator);
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
		return new Aggregate.Projected<V, T, U>(source, accumulator, divide);
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
		return new Aggregate.Seeded<U, T>(source, seed, accumulator);
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
		return new Aggregate.SeededIndexedProjected<V, T, U>(source, seed, accumulator, divide);
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
		return new Containment.All<T>(source, predicate);
	}
	/**
	 * Channels the values of the first observable who 
	 * fires first from the given set of observables.
	 * E.g., <code>O3 = Amb(O1, O2)</code> if O1 starts 
	 * to submit events first, O3 will relay these events 
	 * and events of O2 will be completely ignored
	 * @param <T> the type of the observed element
	 * @param sources the iterable list of source observables.
	 * @return the observable of which reacted first
	 */
	@Nonnull
	public static <T> Observable<T> amb(
			@Nonnull final Iterable<? extends Observable<? extends T>> sources) {
		return new Ambiguous<T>(sources);
	}
	/**
	 * Channels the values of the first observable who 
	 * fires first from the given set of observables.
	 * E.g., <code>O3 = Amb(O1, O2)</code> if O1 starts 
	 * to submit events first, O3 will relay these events 
	 * and events of O2 will be completely ignored
	 * @param <T> the type of the observed element
	 * @param sources the array of source observables.
	 * @return the observable of which reacted first
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<T> amb(
			@Nonnull final Observable<? extends T>... sources) {
		return new Ambiguous<T>(Arrays.asList(sources));
	}
	/**
	 * Channels the values of either left or right depending on who fired its first value.
	 * @param <T> the observed value type
	 * @param left the left observable
	 * @param right the right observable
	 * @return the observable that will stream one of the sources.
	 * @since 0.97
	 */
	@Nonnull
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
		return new Containment.Any<T>(source, predicate);
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
	 * Waits indefinitely for the observable to complete and returns the last
	 * value. If the source terminated with an error, the exception
	 * is rethrown, wrapped into RuntimeException if necessary.
	 * If the source didn't produce any elements or
	 * is interrupted, a NoSuchElementException is
	 * thrown.
	 * @param <T> the element type 
	 * @param source the source sequence.
	 * @return the last value of the sequence
	 * @since 0.97
	 * <p>The difference from the <code>last</code> operator is that
	 * unlike last, this operator does not treat the error event
	 * as just a termination signal.</p>
	 * @see Reactive#last(Observable)
	 */
	public static <T> T await(@Nonnull Observable<? extends T> source) {
		AsyncSubject<T> subject = new AsyncSubject<T>();
		Closeable c = source.register(subject);
		try {
			return subject.get();
		} catch (InterruptedException ex) {
			Throwables.throwAsUncheckedWithCause(new NoSuchElementException(), ex);
			return null; // we won't get here
		} finally {
			Closeables.closeSilently(c);
		}
	}
	/**
	 * Waits a limited amount of time for the observable to complete and returns the last
	 * value. If the source terminated with an error, the exception
	 * is rethrown, wrapped into RuntimeException if necessary.
	 * If the source didn't produce any elements, times out or
	 * is interrupted, a NoSuchElementException is
	 * thrown.
	 * <p>The difference from the <code>last</code> operator is that
	 * unlike last, this operator does not treat the error event
	 * as just a termination signal.</p>
	 * @param <T> the element type 
	 * @param source the source sequence.
	 * @param time the wait time
	 * @param unit the wait time unit
	 * @return the last value of the sequence
	 * @since 0.97
	 * @see Reactive#last(Observable)
	 */
	public static <T> T await(@Nonnull Observable<? extends T> source,
			long time, @Nonnull TimeUnit unit) {
		AsyncSubject<T> subject = new AsyncSubject<T>();
		Closeable c = source.register(subject);
		try {
			return subject.get(time, unit);
		} catch (InterruptedException ex) {
			Throwables.throwAsUncheckedWithCause(new NoSuchElementException(), ex);
		} catch (TimeoutException ex) {
			Throwables.throwAsUncheckedWithCause(new NoSuchElementException(), ex);
		} finally {
			Closeables.closeSilently(c);
		}
		return null; // we won't get here
	}
	/**
	 * Buffer parts of the source until the window observable finishes.
	 * A new buffer is started when the window observable finishes.
	 * @param <T> the source and result element type
	 * @param <U> the window's own type (ignored)
	 * @param source the source sequence
	 * @param bufferCloseSelector the function that returns a buffer close observable
	 * per registering party.
	 * @return the observable for the buffered items
	 * @since 0.97
	 */
	public static <T, U> Observable<List<T>> buffer(
			@Nonnull Observable<? extends T> source,
			@Nonnull Func0<? extends Observable<U>> bufferCloseSelector) {
		return new Buffer.WithClosing<T, U>(source, bufferCloseSelector);
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
		return new Buffer.WithSizeSkip<T>(source, bufferSize, bufferSize);
	}
	/**
	 * Project the source sequence to
	 * potentially overlapping buffers whose
	 * start is determined by skip and lengths
	 * by size.
	 * @param <T> the type of the elements
	 * @param source the source observable
	 * @param bufferSize the target buffer size
	 * @param skip the number of elements to skip between buffers.
	 * @return the observable of the list
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<List<T>> buffer(
			@Nonnull final Observable<? extends T> source,
			final int bufferSize,
			int skip) {
		return new Buffer.WithSizeSkip<T>(source, bufferSize, 0);
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
		return buffer(source, bufferSize, time, unit, scheduler());
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
		return new Buffer.WithSizeOrTime<T>(source, bufferSize, time, unit, pool);

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
		return buffer(source, time, unit, scheduler());
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
		return new Buffer.WithTime<T>(source, time, unit, pool);
	}
	/**
	 * Projects the incoming values into multiple buffers based on
	 * when a window-open fires an event and a window-close finishes.
	 * An incoming value might end up in multiple buffers if their window
	 * overlaps.
	 * <p>Exception semantics: if any Observable throws an error, the whole
	 * process terminates with error.</p>
	 * @param <T> the source and result element type
	 * @param <U> the buffer opening selector type
	 * @param <V> the buffer closing element type (irrelevant)
	 * @param source the source sequence
	 * @param windowOpening the window-open observable
	 * @param windowClosing the function that returns a window-close observable
	 * for a value from the window-open
	 * @return the observable for the buffered items
	 * @since 0.97
	 */
	public static <T, U, V> Observable<List<T>> buffer(
			@Nonnull Observable<? extends T> source,
			@Nonnull Observable<? extends U> windowOpening,
			@Nonnull Func1<? super U, ? extends Observable<V>> windowClosing
		) {
		return new Buffer.WithOpenClose<T, U, V>(source, windowOpening, windowClosing);
	}
	/**
	 * Buffers the source elements into non-overlapping lists separated
	 * by notification values from the boundary observable and its finish event.
	 * <p>Exception semantics: if any Observable throws an error, the whole
	 * process terminates with error.</p>
	 * @param <T> the source and result element type
	 * @param <U> the window's own type (ignored)
	 * @param source the source sequence
	 * @param boundary the notification source of the boundary
	 * @return the observable for the buffered items
	 * @since 0.97
	 */
	public static <T, U> Observable<List<T>> buffer(
			@Nonnull Observable<? extends T> source,
			@Nonnull Observable<U> boundary
			) {
		return new Buffer.WithBoundary<T, U>(source, boundary);
	}
	/**
	 * Casts the values of the source sequence into the target type. 
	 * ClassCastExceptions
	 * are relayed through the error method and the stream
	 * is stopped.
	 * <p>Note that generics information is erased, the
	 * actual exception might come from much deeper of the
	 * operator chain.</p>
	 * @param <T> the type of the expected values
	 * @param source the source of unknown elements
	 * @return the observable containing Ts
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<T> cast(
			@Nonnull final Observable<?> source) {
		return new Select.Cast<T>(source);
	}
	/**
	 * Casts the values of the source sequence into the
	 * given type via the type token. ClassCastExceptions
	 * are relayed through the error method and the stream
	 * is stopped.
	 * @param <T> the type of the expected values
	 * @param source the source of unknown elements
	 * @param token the token to test agains the elements
	 * @return the observable containing Ts
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<T> cast(
			@Nonnull final Observable<?> source,
			@Nonnull final Class<T> token) {
		return new Select.CastToken<T>(source, token);
	}
	/**
	 * Produces an iterable sequence of consequtive (possibly empty)
	 * chunks of the source sequence.
	 * @param <T> element type
	 * @param source the source sequence
	 * @return the chunks
	 * @since 0.97
	 */
	@Nonnull 
	public static <T> CloseableIterable<List<T>> chunkify(
			@Nonnull Observable<? extends T> source) {
		return collect(
				source,
				new Func0<List<T>>() {
					@Override
					public List<T> invoke() {
						return new ArrayList<T>();
					}
				},
				new Func2<List<T>, T, List<T>>() {
					@Override
					public List<T> invoke(List<T> param1, T param2) {
						param1.add(param2);
						return param1;
					}
				},
				new Func1<List<T>, List<T>>() {
					@Override
					public List<T> invoke(List<T> param) {
						return new ArrayList<T>();
					}
				}
		);
	}
	/**
	 * Produces an enumerable sequence that returns elements
	 * collected/aggregated/whatever from the source
	 * between consequtive iterations.
	 * @param <T> the source type
	 * @param <U> the result type
	 * @param source the source sequence
	 * @param newCollector the factory method for the current collector
	 * @param merge the merger that combines elements
	 * @return the new iterable
	 * @since 0.97
	 */
	@Nonnull
	public static <T, U> CloseableIterable<U> collect(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func0<? extends U> newCollector,
			@Nonnull final Func2<? super U, ? super T, ? extends U> merge
			) {
		return collect(source, newCollector, merge, Functions.asFunc1(newCollector));
	}
	/**
	 * Produces an iterable sequence that returns elements
	 * collected/aggregated/whatever from the source
	 * sequence between consequtive iteration.
	 * FIXME not sure how this should work as the return values depend on
	 * when the next() is invoked.
	 * @param <T> the source element type
	 * @param <U> the result element type
	 * @param source the source sequence
	 * @param initialCollector the initial collector factory
	 * @param merge the merger operator
	 * @param newCollector the factory to replace the current collector
	 * @return the sequence
	 * @since 0.97
	 */
	@Nonnull
	public static <T, U> CloseableIterable<U> collect(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func0<? extends U> initialCollector,
			@Nonnull final Func2<? super U, ? super T, ? extends U> merge,
			@Nonnull final Func1<? super U, ? extends U> newCollector
			) {
		return new Collect<U, T>(source, initialCollector, merge, newCollector);
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
	@Nonnull 
	public static <T> Observable<List<T>> combine(
			@Nonnull final Func0<? extends T> supplier, 
			@Nonnull Observable<? extends T> src) {
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
	 * Returns an observable which combines the latest values of
	 * both streams whenever one sends a new value, but only after both sent a value.
	 * <p><b>Exception semantics:</b> if any stream throws an exception, the output stream
	 * throws an exception and all registrations are terminated.</p>
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
	@Nonnull 
	public static <T, U, V> Observable<V> combineLatest(
			@Nonnull final Observable<? extends T> left,
			@Nonnull final Observable<? extends U> right,
			@Nonnull final Func2<? super T, ? super U, ? extends V> selector
	) {
		return new CombineLatest.Sent<V, T, U>(left, right, selector);
	}
	/**
	 * Returns an observable which combines the latest values of
	 * both streams whenever one sends a new value.
	 * <p><b>Exception semantics:</b> if any stream throws an exception, the output stream
	 * throws an exception and all registrations are terminated.</p>
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
	@Nonnull 
	public static <T, U, V> Observable<V> combineLatest0(
			@Nonnull final Observable<? extends T> left,
			@Nonnull final Observable<? extends U> right,
			@Nonnull final Func2<? super T, ? super U, ? extends V> selector
	) {
		return new CombineLatest.NullStart<T, U, V>(left, right, selector);
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
		return new Concat.FromIterable.Selector<Observable<? extends T>, T>(
				sources, Functions.<Observable<? extends T>>identity());
	}
	/**
	 * Concatenates the source observables in a way that 
	 * when the first finish(), the
	 * second gets registered and continued, and so on.
	 * <p>If the sources sequence is empty, a no-op observable is returned.</p>
	 * <p>Note that the source iterable is not consumed up 
	 * front but as the individual observables
	 * complete, therefore the Iterator methods 
	 * might be called from any thread.</p>
	 * @param <T> the type of the values to observe
	 * @param sources the source list of subsequent observables
	 * @return the concatenated observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<T> concat(
			@Nonnull final Observable<? extends T>... sources) {
		return new Concat.FromIterable.Selector<Observable<? extends T>, T>(
				Arrays.asList(sources), Functions.<Observable<? extends T>>identity());
	}
	/**
	 * Concatenates the observable sequences resulting from enumerating
	 * the sorce iterable and calling the resultSelector function.
	 * <p>Remark: RX calls this For.</p>
	 * @param <T> the source element type
	 * @param <U> the result type
	 * @param source the source sequence
	 * @param resultSelector the observable selector
	 * @return the new observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T, U> Observable<U> concat(
			@Nonnull final Iterable<? extends T> source, 
			@Nonnull final Func1<? super T, ? extends Observable<? extends U>> resultSelector) {
		return new Concat.FromIterable.Selector<T, U>(source, resultSelector);
	}
	/**
	 * Concatenates the observable sequences resulting from enumerating
	 * the sorce iterable and calling the resultSelector function.
	 * <p>Remark: RX calls this For.</p>
	 * @param <T> the source element type
	 * @param <U> the result type
	 * @param resultSelector the observable selector
	 * @param source the source sequence
	 * @return the new observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T, U> Observable<U> concat(
			@Nonnull final Func1<? super T, ? extends Observable<? extends U>> resultSelector,
				@Nonnull final T... source
					) {
		return new Concat.FromIterable.Selector<T, U>(Arrays.asList(source), resultSelector);
	}
	/**
	 * Concatenates the observable sequences resulting from enumerating
	 * the sorce iterable and calling the indexed resultSelector function.
	 * <p>Remark: RX calls this For.</p>
	 * @param <T> the source element type
	 * @param <U> the result type
	 * @param source the source sequence
	 * @param resultSelector the observable selector
	 * @return the new observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T, U> Observable<U> concat(
			@Nonnull final Iterable<? extends T> source, 
			@Nonnull final Func2<? super T, ? super Integer, ? extends Observable<? extends U>> resultSelector) {
		return new Concat.FromIterable.IndexedSelector<T, U>(source, resultSelector);
	}
	/**
	 * Concatenates the observable sequences resulting from enumerating
	 * the sorce iterable and calling the indexed resultSelector function.
	 * <p>Remark: RX calls this For.</p>
	 * @param <T> the source element type
	 * @param <U> the result type
	 * @param resultSelector the observable selector
	 * @param source the source sequence
	 * @return the new observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T, U> Observable<U> concat(
			@Nonnull final Func2<? super T, ? super Integer, ? extends Observable<? extends U>> resultSelector,
			@Nonnull T... source) {
		return new Concat.FromIterable.IndexedSelector<T, U>(Arrays.asList(source), resultSelector);
	}
	/**
	 * Concatenate the the multiple sources of T one after another.
	 * <p><b>Exception semantics:</b> if the sources or any inner observer signals an
	 * error, the outer observable will signal that error and the sequence is terminated.</p>
	 * @param <T> the element type
	 * @param sources the observable sequence of the observable sequence of Ts.
	 * @return the new observable
	 */
	@Nonnull 
	public static <T> Observable<T> concat(
			@Nonnull final Observable<? extends Observable<? extends T>> sources
	) {
		return new Concat.FromObservable.Selector<T, T>(sources, Functions.<Observable<? extends T>>identity());
	}
	/**
	 * Concatenate the the multiple sources of T one after another.
	 * <p><b>Exception semantics:</b> if the sources or any inner observer signals an
	 * error, the outer observable will signal that error and the sequence is terminated.</p>
	 * @param <T> the source element type
	 * @param <U> the result element type
	 * @param sources the observable sequence of the observable sequence of Ts.
	 * @param resultSelector the concatenation result selector
	 * @return the new observable
	 * @since 0.97
	 */
	@Nonnull 
	public static <T, U> Observable<U> concat(
			@Nonnull final Observable<? extends Observable<? extends T>> sources,
			@Nonnull Func1<? super Observable<? extends T>, ? extends Observable<? extends U>> resultSelector
	) {
		return new Concat.FromObservable.Selector<T, U>(sources, resultSelector);
	}
	/**
	 * Concatenate the the multiple sources of T one after another.
	 * <p><b>Exception semantics:</b> if the sources or any inner observer signals an
	 * error, the outer observable will signal that error and the sequence is terminated.</p>
	 * @param <T> the source element type
	 * @param <U> the result element type
	 * @param sources the observable sequence of the observable sequence of Ts.
	 * @param resultSelector the indexed concatenation result selector
	 * @return the new observable
	 * @since 0.97
	 */
	@Nonnull 
	public static <T, U> Observable<U> concat(
			@Nonnull final Observable<? extends Observable<? extends T>> sources,
			@Nonnull Func2<? super Observable<? extends T>, ? super Integer, ? extends Observable<? extends U>> resultSelector
	) {
		return new Concat.FromObservable.IndexedSelector<T, U>(sources, resultSelector);
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
			@Nonnull final Func0<? extends T> supplier) {
		return any(source, new Func1<T, Boolean>() {
			@Override
			public Boolean invoke(T param1) {
				T value = supplier.invoke();
				return param1 == value || (param1 != null && param1.equals(value));
			}
		});
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
	 * Counts the number of elements where the predicate returns true.
	 * @param <T> the element type
	 * @param source the source sequence of Ts
	 * @param predicate the predicate function 
	 * @return  the observable with a single value
	 * @since 0.97
	 */
	@Nonnull 
	public static <T> Observable<Integer> count(
			@Nonnull Observable<? extends T> source, 
			@Nonnull Func1<? super T, Boolean> predicate) {
		return count(where(source, predicate));
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
		return new Count.AsInt<T>(source);
	}
	/**
	 * Counts the number of elements where the predicate returns true as long.
	 * @param <T> the element type
	 * @param source the source sequence of Ts
	 * @param predicate the predicate function 
	 * @return  the observable with a single value
	 * @since 0.97
	 */
	@Nonnull 
	public static <T> Observable<Long> countLong(
			@Nonnull Observable<? extends T> source, 
			@Nonnull Func1<? super T, Boolean> predicate) {
		return countLong(where(source, predicate));
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
		return new Count.AsLong<T>(source);
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					ObserverState state = ObserverState.OBSERVER_RUNNING;
					@Override
					public void error(@Nonnull Throwable ex) {
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
	 * Returns the default value provided by
	 * the function if the source sequence is empty.
	 * @param <T> the element type
	 * @param source the source sequence
	 * @param defaultValueFunc the default value factory
	 * @return the new observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<T> defaultIfEmpty(
			@Nonnull final Observable<? extends T> source, 
			Func0<? extends T> defaultValueFunc) {
		return new Select.DefaultIfEmptyFunc<T>(source, defaultValueFunc);
	}
	/**
	 * Returns the default value if the source
	 * sequence is empty.
	 * @param <T> the element type
	 * @param source the source sequence
	 * @param defaultValue the default value
	 * @return the new observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<T> defaultIfEmpty(
			@Nonnull final Observable<? extends T> source, 
			T defaultValue) {
		return defaultIfEmpty(source, Functions.constant0(defaultValue));
	}
	/**
	 * The returned observable invokes the <code>observableFactory</code> whenever an observer
	 * tries to register with it.
	 * @param <T> the type of elements to observer
	 * @param observableFactory the factory which is responsible to create a source observable.
	 * @return the result observable
	 */
	@Nonnull
	public static <T> Observable<T> defer(
			@Nonnull final Func0<? extends Observable<? extends T>> observableFactory) {
		return new Observable<T>() {
			@Override
			@Nonnull 
			public Closeable register(@Nonnull Observer<? super T> observer) {
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
		return delay(source, time, unit, scheduler());
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
		return new Delay<T>(source, time, unit, pool);
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T> observer) {
				return source.register(new Observer<Option<T>>() {
					/** Keeps track of the observer's state. */
					final AtomicBoolean done = new AtomicBoolean();
					@Override
					public void error(@Nonnull Throwable ex) {
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
	public static <T> void dispatch(
			@Nonnull Observer<? super T> observer,
			@Nonnull Option<T> value) {
		if (value == Option.none()) {
			observer.finish();
		} else
		if (Option.isError(value)) {
			observer.error(Option.getError(value));
		} else {
			observer.next(value.value());
		}
	}
	/**
	 * Returns the distinct elements from the source.
	 * @param <T> the element type
	 * @param source the source sequence
	 * @return the new observable
	 */
	@Nonnull
	public static <T> Observable<T> distinct(
			@Nonnull final Observable<? extends T> source) {
		return distinct(source, Functions.<T>identity());
	}
	/**
	 * Returns a sequence of elements distinct in
	 * terms of the key extracted from them.
	 * @param <T> the source and result element type
	 * @param <U> the key type
	 * @param source the source sequence
	 * @param keyExtractor the key extractor
	 * @return the new observer
	 */
	@Nonnull
	public static <T, U> Observable<T> distinct(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends U> keyExtractor) {
		return distinct(source, keyExtractor, Functions.<U>equals());
	}
	/**
	 * Returns a sequence of elements who are distinct
	 * in terms of the given key extracted by a function
	 * and compared against each other via the comparer function.
	 * @param <T> the source and result element type
	 * @param <U> the key type
	 * @param source the source sequence
	 * @param keyExtractor the key extractor function
	 * @param keyComparer the key comparer function.
	 * @return the new observer
	 * @since 0.97
	 */
	@Nonnull
	public static <T, U> Observable<T> distinct(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends U> keyExtractor,
			@Nonnull final Func2<? super U, ? super U, Boolean> keyComparer) {
		return new Distinct<T, U>(source, keyExtractor, keyComparer);
	}
	/**
	 * Returns the distinct elements from the source according
	 * to the given comparator function.
	 * @param <T> the element type
	 * @param source the source sequence
	 * @param comparer the element comparer
	 * @return the new observable
	 * @since 0.97 
	 */
	@Nonnull
	public static <T> Observable<T> distinct(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func2<? super T, ? super T, Boolean> comparer) {
		return distinct(source, Functions.<T>identity(), comparer);
	}
	/**
	 * Repeats the given source so long as the condition returns true.
	 * The condition is checked after each completion of the source sequence.
	 * <p>Exception semantics: exception received will stop the repeat process
	 * and is delivered to observers as-is.</p>
	 * @param <T> the element type
	 * @param source the source sequence
	 * @param condition the condition to check
	 * @return the new observable
	 * @since 0.97
	 */
	@Nonnull 
	public static <T> Observable<T> doWhile(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Func0<Boolean> condition) {
		return new Repeat.DoWhile<T>(source, condition);
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
		return drain(source, pump, scheduler());
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super Void> observer) {
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
									public void error(@Nonnull Throwable ex) {
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
					public void onError(@Nonnull Throwable ex) {
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
					protected void onError(@Nonnull Throwable ex) {
						observer.error(ex);
					}

					@Override
					protected void onFinish() {
						observer.error(new NoSuchElementException("index = " + index));
					}

					@Override
					protected void onNext(T value) {
						if (i == 0) {
							observer.next(value);
							observer.finish();
							close();
						}
						i--;
					}
					
				}).registerWith(source);
			}
		};
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
					protected void onError(@Nonnull Throwable ex) {
						observer.error(ex);
					}

					@Override
					protected void onFinish() {
						observer.next(defaultSupplier.invoke());
						observer.finish();
					}

					@Override
					protected void onNext(T value) {
						if (i == 0) {
							observer.next(value);
							observer.finish();
							close();
						}
						i--;
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
	 * @param <T> the type of the values to observe (irrelevant)
	 * @return Returns an empty observable which signals only finish() on the default observer pool.
	 */
	@Nonnull
	public static <T> Observable<T> empty() {
		return empty(Schedulers.constantTimeOperations());
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T> observer) {
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					@Override
					public void error(@Nonnull Throwable ex) {
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
			public Closeable register(@Nonnull final Observer<? super T> observer) {
				return (new DefaultObserverEx<T>() {
					@Override
					protected void onError(@Nonnull Throwable ex) {
						observer.error(ex);
					}

					@Override
					protected void onFinish() {
						error(new NoSuchElementException());
					}

					@Override
					protected void onNext(T value) {
						observer.next(value);
						observer.finish();
						close();
					} 
					
				}).registerWith(source);
			}
		};
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
			public Closeable register(@Nonnull final Observer<? super T> observer) {
				return (new DefaultObserverEx<T>() {
					@Override
					protected void onError(@Nonnull Throwable ex) {
						observer.error(ex);
					}

					@Override
					protected void onFinish() {
						observer.next(defaultSupplier.invoke());
						observer.finish();
					}

					@Override
					protected void onNext(T value) {
						observer.next(value);
						observer.finish();
						close();
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
	 * Invokes the action on each element in the source,
	 * and blocks until the source terminates either way.
	 * <p>The observation of the source is not serialized,
	 * therefore, <code>action</code> might be invoked concurrently
	 * by subsequent source elements.</p>
	 * @param <T> the element type
	 * @param source the source sequence
	 * @param action the action to invoke on each element.
	 * @throws InterruptedException if the wait is interrupted
	 * @since 0.97
	 */
	public static <T> void forEach(
			@Nonnull final Observable<T> source, 
			@Nonnull final Action1<? super T> action)
					throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		Observer<T> o = new Observer<T>() {
			/** Indicate the termination. */
			final AtomicBoolean done = new AtomicBoolean();
			@Override
			public void error(@Nonnull Throwable ex) {
				terminate();
			}

			@Override
			public void finish() {
				terminate();
			}

			@Override
			public void next(T value) {
				if (!done.get()) {
					action.invoke(value);
				}
			}
			/** Terminate the acceptance of values. */
			void terminate() {
				if (done.compareAndSet(false, true)) {
					latch.countDown();
				}
			}
			
		};
		Closeable c = source.register(o);
		try {
			latch.await();
		} finally {
			Closeables.closeSilently(c);
		}
	}
	/**
	 * Invokes the action on each element in the source,
	 * and blocks until the source terminates or the time runs out.
	 * <p>The observation of the source is not serialized,
	 * therefore, <code>action</code> might be invoked concurrently
	 * by subsequent source elements.</p>
	 * @param <T> the element type
	 * @param source the source sequence
	 * @param action the action to invoke on each element.
	 * @param time the waiting time
	 * @param unit the waiting time unit
	 * @return false if a timeout occurred instead of normal termination
	 * @throws InterruptedException if the wait is interrupted
	 * @since 0.97
	 */
	public static <T> boolean forEach(
			@Nonnull final Observable<T> source, 
			@Nonnull final Action1<? super T> action,
			long time, 
			@Nonnull TimeUnit unit)
					throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		Observer<T> o = new Observer<T>() {
			/** Indicate the termination. */
			final AtomicBoolean done = new AtomicBoolean();
			@Override
			public void error(@Nonnull Throwable ex) {
				terminate();
			}

			@Override
			public void finish() {
				terminate();
			}

			@Override
			public void next(T value) {
				if (!done.get()) {
					action.invoke(value);
				}
			}
			/** Terminate the acceptance of values. */
			void terminate() {
				if (done.compareAndSet(false, true)) {
					latch.countDown();
				}
			}
			
		};
		Closeable c = source.register(o);
		try {
			return latch.await(time, unit);
		} finally {
			Closeables.closeSilently(c);
		}
	}
	/**
	 * Invokes the indexed action on each element in the source,
	 * and blocks until the source terminates either way.
	 * <p>The observation of the source is not serialized,
	 * therefore, <code>action</code> might be invoked concurrently
	 * by subsequent source elements.</p>
	 * @param <T> the element type
	 * @param source the source sequence
	 * @param action the action to invoke on each element.
	 * @throws InterruptedException if the wait is interrupted
	 * @since 0.97
	 */
	public static <T> void forEach(
			@Nonnull final Observable<T> source, 
			@Nonnull final Action2<? super T, ? super Integer> action) 
					throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		Observer<T> o = new Observer<T>() {
			/** Indicate the termination. */
			final AtomicBoolean done = new AtomicBoolean();
			/** The next() counter. */
			final AtomicInteger index = new AtomicInteger();
			@Override
			public void error(@Nonnull Throwable ex) {
				terminate();
			}

			@Override
			public void finish() {
				terminate();
			}

			@Override
			public void next(T value) {
				if (!done.get()) {
					action.invoke(value, index.getAndIncrement());
				}
			}
			/** Terminate the acceptance of values. */
			void terminate() {
				if (done.compareAndSet(false, true)) {
					latch.countDown();
				}
			}
			
		};
		Closeable c = source.register(o);
		try {
			latch.await();
		} finally {
			Closeables.closeSilently(c);
		}
	}
	/**
	 * Invokes the indexed action on each element in the source,
	 * and blocks until the source terminates either way.
	 * <p>The observation of the source is not serialized,
	 * therefore, <code>action</code> might be invoked concurrently
	 * by subsequent source elements.</p>
	 * @param <T> the element type
	 * @param source the source sequence
	 * @param action the action to invoke on each element.
	 * @param time the waiting time
	 * @param unit the waiting time unit
	 * @return false if a timeout occurred instead of normal termination
	 * @throws InterruptedException if the wait is interrupted
	 * @since 0.97
	 */
	public static <T> boolean forEach(
			@Nonnull final Observable<T> source, 
			@Nonnull final Action2<? super T, ? super Integer> action,
			long time, 
			@Nonnull TimeUnit unit
			) 
					throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		Observer<T> o = new Observer<T>() {
			/** Indicate the termination. */
			final AtomicBoolean done = new AtomicBoolean();
			/** The next() counter. */
			final AtomicInteger index = new AtomicInteger();
			@Override
			public void error(@Nonnull Throwable ex) {
				terminate();
			}

			@Override
			public void finish() {
				terminate();
			}

			@Override
			public void next(T value) {
				if (!done.get()) {
					action.invoke(value, index.getAndIncrement());
				}
			}
			/** Terminate the acceptance of values. */
			void terminate() {
				if (done.compareAndSet(false, true)) {
					latch.countDown();
				}
			}
			
		};
		Closeable c = source.register(o);
		try {
			return latch.await(time, unit);
		} finally {
			Closeables.closeSilently(c);
		}
	}
	/**
	 * Runs the observables in parallel and joins their 
	 * last values whenever one fires.
	 * @param <T> the type of the source values
	 * @param sources the list of sources
	 * @return the observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<List<T>> forkJoin(
			@Nonnull final Observable<? extends T>... sources) {
		return forkJoin(Arrays.asList(sources));
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super List<T>> observer) {
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
						public void error(@Nonnull Throwable ex) {
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
		return generate(initial, condition, next, selector, scheduler());
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super U> observer) {
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
		return generateTimed(initial, condition, next, selector, delay, scheduler());
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super Timestamped<U>> observer) {
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
				return Closeables.emptyCloseable();
			}
		};
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
		return groupBy(source, keyExtractor, Functions.equals(), valueExtractor);
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
	 * @param keyComparer the key equality comparer
	 * @return the observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T, Key> Observable<GroupedObservable<Key, T>> groupBy(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends Key> keyExtractor,
			@Nonnull final Func2<? super Key, ? super Key, Boolean> keyComparer
			) {
		return groupBy(source, keyExtractor, keyComparer, Functions.<T>identity());
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
	 * @param keyComparer the key equality comparer
	 * @param valueExtractor the extractor which makes Us from Ts
	 * @return the observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T, U, Key> Observable<GroupedObservable<Key, U>> groupBy(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends Key> keyExtractor,
			@Nonnull final Func2<? super Key, ? super Key, Boolean> keyComparer,
			@Nonnull final Func1<? super T, ? extends U> valueExtractor) {
		return new GroupBy<Key, U, T>(source, keyExtractor, keyComparer, valueExtractor);
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
	@Nonnull 
	public static <T, K, D> Observable<GroupedObservable<K, T>> groupByUntil(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends K> keySelector,
			@Nonnull final Func1<? super GroupedObservable<K, T>, ? extends Observable<D>> durationSelector
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
	@Nonnull 
	public static <T, K, D> Observable<GroupedObservable<K, T>> groupByUntil(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends K> keySelector,
			@Nonnull final Func1<? super GroupedObservable<K, T>, ? extends Observable<D>> durationSelector,
			@Nonnull final Func2<? super K, ? super K, Boolean> keyComparer
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
	@Nonnull 
	public static <T, K, V, D> Observable<GroupedObservable<K, V>> groupByUntil(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends K> keySelector,
			@Nonnull final Func1<? super T, ? extends V> valueSelector,
			@Nonnull final Func1<? super GroupedObservable<K, V>, ? extends Observable<D>> durationSelector
	) {
		return new GroupByUntil.Default<K, V, T, D>(source, keySelector, valueSelector,
				durationSelector);
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
	@Nonnull 
	public static <T, K, V, D> Observable<GroupedObservable<K, V>> groupByUntil(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends K> keySelector,
			@Nonnull final Func1<? super T, ? extends V> valueSelector,
			@Nonnull final Func1<? super GroupedObservable<K, V>, ? extends Observable<D>> durationSelector,
			@Nonnull final Func2<? super K, ? super K, Boolean> keyComparer
	) {
		return new GroupByUntil.WithComparer<K, V, T, D>(source, keySelector, valueSelector,
				durationSelector, keyComparer);
	}
	/**
	 * Returns an observable which correlates two streams of values based on
	 * their time when they overlapped and groups the results.
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
	@Nonnull 
	public static <Left, Right, LeftDuration, RightDuration, Result> Observable<Result> groupJoin(
			@Nonnull final Observable<? extends Left> left,
			@Nonnull final Observable<? extends Right> right,
			@Nonnull final Func1<? super Left, ? extends Observable<LeftDuration>> leftDurationSelector,
			@Nonnull final Func1<? super Right, ? extends Observable<RightDuration>> rightDurationSelector,
			@Nonnull final Func2<? super Left, ? super Observable<? extends Right>, ? extends Result> resultSelector
	) {
		return new GroupJoin<Result, Left, Right, LeftDuration, RightDuration>(
				left, right, leftDurationSelector, rightDurationSelector,  
				resultSelector);
	}
	/**
	 * Returns an observable where the submitted condition decides whether 
	 * the <code>then</code> source is allowed to submit values or else
	 * an empty sequence is used.
	 * @param <T> the type of the values to observe
	 * @param condition the condition function
	 * @param then the source to use when the condition is true
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> ifThen(
			@Nonnull final Func0<Boolean> condition,
			@Nonnull final Observable<? extends T> then) {
		return ifThen(condition, then, Reactive.<T>empty());
	}
	/**
	 * Returns an observable where the submitted condition 
	 * decides whether the <code>then</code> or <code>orElse</code>
	 * source is allowed to submit values.
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T> observer) {
				Observable<? extends T> source = null;
				if (condition.invoke()) {
					source = then;
				} else {
					source = orElse;
				}
				return source.register(observer);
			}
		};
	}
	/**
	 * Returns an observable where the submitted condition decides whether 
	 * the <code>then</code> source is allowed to submit values
	 * or else an empty sequence is returned.
	 * @param <T> the type of the values to observe
	 * @param condition the condition function
	 * @param then the source to use when the condition is true
	 * @param scheduler the scheduler for the empty case.
	 * @return the observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<T> ifThen(
			@Nonnull final Func0<Boolean> condition,
			@Nonnull final Observable<? extends T> then,
			@Nonnull Scheduler scheduler) {
		return ifThen(condition, then, Reactive.<T>empty(scheduler));
	}
	/**
	 * Ignores the next() messages of the source and forwards only the error() and
	 * finish() messages.
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @return the new observable
	 */
	@Nonnull 
	public static <T> Observable<T> ignoreValues(
			@Nonnull final Observable<? extends T> source) {
		return new Observable<T>() {
			@Override
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
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
		return new Invoke.OnNext<T>(source, action);
	}
	/**
	 * Invokes the given actions while relaying events.
	 * @param <T> the element type
	 * @param source the source sequence
	 * @param onNext the action for next
	 * @param onFinish the action for finish
	 * @return the augmented observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<T> invoke(
			@Nonnull Observable<? extends T> source, 
			@Nonnull Action1<? super T> onNext, 
			@Nonnull Action0 onFinish) {
		return new Invoke.OnNextFinish<T>(source, onNext, onFinish);
	}
	/**
	 * Invokes the given actions while relaying events.
	 * @param <T> the element type
	 * @param source the source sequence
	 * @param onNext the action for next
	 * @param onError the action for error
	 * @return the augmented observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<T> invoke(
			@Nonnull Observable<? extends T> source, 
			@Nonnull Action1<? super T> onNext, 
			@Nonnull Action1<? super Throwable> onError) {
		return new Invoke.OnNextError<T>(source, onNext, onError);
	}
	/**
	 * Invokes the given actions while relaying events.
	 * @param <T> the element type
	 * @param source the source sequence
	 * @param onNext the action for next
	 * @param onError the action for error
	 * @param onFinish the action for finish
	 * @return the augmented observable
	 * @since 0.97
	 */
	@Nonnull 
	public static <T> Observable<T> invoke(
			@Nonnull Observable<? extends T> source, 
			@Nonnull Action1<? super T> onNext, 
			@Nonnull Action1<? super Throwable> onError, 
			@Nonnull Action0 onFinish) {
		return new Invoke.OnNextErrorFinish<T>(source, onNext, onError, onFinish);
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
		return new Invoke.OnObserver<T>(source, observer);
	}
	/**
	 * Observes the source observables in parallel on the default 
	 * scheduler and collects their individual
	 * value streams, blocking in the process. 
	 * @param <T> the common element type
	 * @param sources the source sequences
	 * @return the for each source, the list of their value streams.
	 * @throws InterruptedException if the wait is interrupted
	 * @since 0.97
	 */
	@Nonnull 
	public static <T> List<List<T>> invokeAll(
			@Nonnull final Iterable<? extends Observable<? extends T>> sources) throws InterruptedException {
		return invokeAll(sources, scheduler());
	}
	/**
	 * Observes the source observables in parallel on the default 
	 * scheduler and collects their individual
	 * value streams, blocking in the process. 
	 * @param <T> the common element type
	 * @param sources the source sequences
	 * @return the for each source, the list of their value streams.
	 * @throws InterruptedException if the wait is interrupted
	 * @since 0.97
	 */
	@Nonnull 
	public static <T> List<List<T>> invokeAll(
			@Nonnull final Observable<? extends T>... sources) throws InterruptedException {
		return invokeAll(Arrays.asList(sources), scheduler());
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
		return invokeAll(source1, source2, scheduler());
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
	 * Invoke the given callable on the default pool and observe its result via the returned observable.
	 * Any exception thrown by the callable is relayed via the error() message.
	 * @param <T> the return type
	 * @param call the callable
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> invokeAsync(
			@Nonnull final Callable<? extends T> call) {
		return invokeAsync(call, scheduler());
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T> observer) {
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
		return invokeAsync(run, scheduler());
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T> observer) {
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
		return invokeAsync(run, defaultValue, scheduler());
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T> observer) {
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super Boolean> observer) {
				return source.register(new Observer<Object>() {
					/** We already determined the answer? */
					boolean done;
					@Override
					public void error(@Nonnull Throwable ex) {
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
		return new Join<Left, Right, LeftDuration, RightDuration, Result>(left, right, leftDurationSelector, rightDurationSelector, resultSelector);
	}
	/**
	 * Returns the last element of the source observable or throws
	 * NoSuchElementException if the source is empty or the wait is interrupted.
	 * <p>Exception semantics: the exceptions thrown by the source are ignored and treated
	 * as termination signals.</p>
	 * <p>The difference between this and the <code>wait</code> operator is that
	 * it returns the last valid value from before an error or finish, ignoring any
	 * exceptions.</p>
	 * @param <T> the type of the elements
	 * @param source the source of Ts
	 * @return the last element
	 * @see Reactive#await(Observable)
	 */
	public static <T> T last(
			@Nonnull final Observable<? extends T> source) {
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<Option<T>> value = new AtomicReference<Option<T>>();
		Closeable c = source.register(new Observer<T>() {
			/** The current value. */
			T current;
			/** Are we the first? */
			boolean first = true;
			@Override
			public void error(@Nonnull Throwable ex) {
				finish();
			}

			@Override
			public void finish() {
				if (first) {
					value.set(Option.<T>none());
				} else {
					value.set(Option.some(current));
				}
				latch.countDown();
			}

			@Override
			public void next(T value) {
				first = false;
				current = value;
			}

		});
		try {
			latch.await();
			Option<T> v = value.get();
			if (Option.isNone(v)) {
				throw new NoSuchElementException();
			}
			return v.value();
		} catch (InterruptedException e) {
			Throwables.throwAsUncheckedWithCause(new NoSuchElementException(), e);
		} finally {
			Closeables.closeSilently(c);
		}
		return null;
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
			public void error(@Nonnull Throwable ex) {
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
			if (value == Option.none()) {
				throw new NoSuchElementException();
			}
			return value.value();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			Closeables.closeSilently(c);
		}
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
	public static <T> T last(
			@Nonnull final Observable<? extends T> source,
			final T defaultValue) {
		return last(source, Functions.constant0(defaultValue));
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
	public static <T> Observable<T> lastAsync(
			@Nonnull final Observable<? extends T> source) {
		return new Observable<T>() {
			@Override
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** The first value is pending? */
					boolean first = true;
					/** The current value. */
					T current;
					@Override
					public void error(@Nonnull Throwable ex) {
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

					@Override
					public void next(T value) {
						current = value;
						first = false;
					}
					
				});
			}
		};
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
			public Closeable register(@Nonnull final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** The first value is pending? */
					boolean first = true;
					/** The current value. */
					T current;
					@Override
					public void error(@Nonnull Throwable ex) {
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

					@Override
					public void next(T value) {
						current = value;
						first = false;
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
	@Nonnull 
	public static <T> Observable<T> lastAsync(
			@Nonnull final Observable<? extends T> source,
			final T defaultValue) {
		return lastAsync(source, Functions.constant0(defaultValue));
	}
	/**
	 * Returns an iterable sequence which returns the latest element
	 * from the observable sequence, consuming it only once.
	 * <p>Note that it is possible one doesn't receive the
	 * last value of a fixed-length observable sequence in case 
	 * the last next() call is is quickly followed by a finish() event.</p>
	 * @param <T> the type of the values
	 * @param source the source
	 * @return the iterable
	 */
	@Nonnull
	public static <T> CloseableIterable<T> latest(
			@Nonnull final Observable<? extends T> source) {
		return new Latest<T>(source);
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
	@Nonnull 
	public static <T, U> Observable<U> let(
			final T value,
			@Nonnull final Func1<? super T, ? extends Observable<U>> selector) {
		return new Observable<U>() {
			@Override
			@Nonnull 
			public Closeable register(@Nonnull Observer<? super U> observer) {
				return selector.invoke(value).register(observer);
			}
		};
	}
	/**
	 * For each of the source elements, creates a view of the source starting with the given
	 * element and calls the selector function. The function's return observable is then merged
	 * into a single observable sequence.<p>
	 * For example, a source sequence of (1, 2, 3) will create three function calls with (1, 2, 3), (2, 3) and (3) as a content.
	 * FIXME rework
	 * @param <T> the source element type
	 * @param <U> the result element type
	 * @param source the source of Ts
	 * @param selector the selector function
	 * @return the new observable
	 */
	@Nonnull 
	public static <T, U> Observable<U> manySelect(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super Observable<T>, ? extends Observable<U>> selector
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
	 * FIXME rework
	 * @param <T> the source element type
	 * @param <U> the result element type
	 * @param source the source of Ts
	 * @param selector the selector that extracts an U from the series of Ts.
	 * @param scheduler the scheduler where the extracted U will be emitted from.
	 * @return the new observable.
	 */
	@Nonnull 
	public static <T, U> Observable<U> manySelect(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super Observable<T>, ? extends U> selector,
			@Nonnull final Scheduler scheduler) {
		return new Observable<U>() {
			@Override
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super U> observer) {
				final AtomicInteger wip = new AtomicInteger(1);
				Closeable c = source.register(new DefaultObserverEx<T>(true) {
					/** The skip position. */
					int counter;
					@Override
					protected void onError(@Nonnull Throwable ex) {
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
	@Nonnull 
	public static <T, U> Observable<U> manySelect0(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super Observable<T>, ? extends U> selector) {
		return manySelect(source, selector, scheduler());
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super Option<T>> observer) {
				return source.register(new Observer<T>() {
					@Override
					public void error(@Nonnull Throwable ex) {
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super Option<T>> observer) {
				return source.register(new Observer<T>() {
					@Override
					public void error(@Nonnull Throwable ex) {
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
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<T> merge(
			@Nonnull final Observable<? extends T>... sources) {
		return merge(Arrays.asList(sources));
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T> observer) {
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
						public void onError(@Nonnull Throwable ex) {
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T> observer) {
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
					protected void onError(@Nonnull Throwable ex) {
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
							public void onError(@Nonnull Throwable ex) {
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
			final boolean max
	) {
		return new Observable<List<T>>() {
			@Override
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super List<T>> observer) {
				return source.register(new Observer<T>() {
					/** The current collection for the minimum of Ts. */
					List<T> collect;
					/** The current minimum value. */
					Key maxKey;
					@Override
					public void error(@Nonnull Throwable ex) {
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
	@Nonnull 
	public static <T> CloseableIterable<T> mostRecent(
			@Nonnull final Observable<? extends T> source, 
			final T initialValue) {
		return new MostRecent<T>(source, initialValue);
	}
	/**
	 * Multicasts the source events through the subject instantiated via
	 * the subjectSelector. Each registration to this sequence
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
			public Closeable register(@Nonnull Observer<? super V> observer) {
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
	 * Returns an observable which never fires.
	 * @param <T> the type of the observable, irrelevant
	 * @return the observable
	 */
	@Nonnull
	public static <T> Observable<T> never() {
		return new Observable<T>() {
			@Override
			@Nonnull 
			public Closeable register(@Nonnull Observer<? super T> observer) {
				return Closeables.emptyCloseable();
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
	@Nonnull 
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
	 * Returns an iterable sequence which blocks until an element
	 * becomes available from the source.
	 * The iterable's (has)next() call is paired up with the observer's next() call,
	 * therefore, values might be skipped if the iterable is not on its (has)next() call
	 * at the time of reception.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code> for its
	 * <code>remove()</code> method.</p>
	 * <p>Exception semantics: in case of exception received, the source is
	 * disconnected and the exception is rethrown from the iterator's next method
	 * as a wrapped RuntimeException if necessary.</p>
	 * @param <T> the element type
	 * @param source the source of elements
	 * @return the iterable
	 */
	@Nonnull 
	public static <T> CloseableIterable<T> next(
			@Nonnull final Observable<? extends T> source) {
		return new Next<T>(source);
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T> observer) {

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
					public void onError(@Nonnull final Throwable ex) {
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
	 * Filters the elements of the source sequence which
	 * is assignable to the provided type.
	 * @param <T> the target element type
	 * @param source the source sequence
	 * @param clazz the class token
	 * @return the filtering obserable
	 * since 0.97
	 */
	public static <T> Observable<T> ofType(@Nonnull Observable<?> source, @Nonnull Class<T> clazz) {
		return new Where.OfType<T>(source, clazz);
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T> observer) {
				return source.register(new Observer<T>() {
					/** The buffer. */
					final List<T> buffer = new ArrayList<T>();

					@Override
					public void error(@Nonnull Throwable ex) {
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
	@Nonnull 
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
	@Nonnull 
	public static <T, U> Observable<U> prune(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super Observable<? extends T>, ? extends Observable<U>> selector
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
	@Nonnull 
	public static <T, U> Observable<U> prune(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super Observable<? extends T>, ? extends Observable<U>> selector,
					@Nonnull final Scheduler scheduler
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
	@Nonnull 
	public static <T> Observable<T> prune(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Scheduler scheduler
	) {
		return replay(source, 1, scheduler);
	}
	/**
	 * Returns an observable which shares a single registration to the underlying source.
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
	 * Returns an observable sequence which is the result of
	 * invoking the selector on a connectable observable sequence
	 * that shares a single registration with the underlying 
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
	@Nonnull 
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
	 * that shares a single registration with the underlying 
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
	@Nonnull 
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
					public void error(@Nonnull Throwable ex) {
						obs.error(ex);
					}

					@Override
					public void finish() {
						obs.finish();
					}

					@Override
					public void next(T value) {
						obs.next(value);
					}

					@Override
					@Nonnull
					public Closeable register(@Nonnull Observer<? super T> observer) {
						observer.next(initialValue);
						return obs.register(observer);
					}
					
				};
			}
		}, selector);
	}
	/**
	 * Returns an observable which shares a single registration to the underlying source
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
			public void error(@Nonnull Throwable ex) {
				obs.error(ex);
			}

			@Override
			public void finish() {
				obs.finish();
			}

			@Override
			public void next(T value) {
				obs.next(value);
			}

			@Override
			@Nonnull
			public Closeable register(@Nonnull Observer<? super T> observer) {
				observer.next(initialValue);
				return obs.register(observer);
			}
			
		});
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
	@Nonnull 
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
	/**
	 * Returns a connectable observable which uses a single registration
	 * to the underlying source sequence containing only the last value.
	 * @param <T> the element type
	 * @param source the source sequence
	 * @return the new observable
	 * @since 0.97
	 */
	@Nonnull 
	public static <T> ConnectableObservable<T> publishLast(
			@Nonnull final Observable<T> source
			) {
		return multicast(source, new AsyncSubject<T>());
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
		return range(start, count, step, scheduler());
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super BigDecimal> observer) {
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
		return range(start, count, scheduler());
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
			@Nonnull final Scheduler pool) {
		return new Observable<BigInteger>() {
			@Override
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super BigInteger> observer) {
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
		return range(start, count, step, scheduler());
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
	@Nonnull 
	public static Observable<Double> range(
			final double start,
			final int count,
			final double step,
			@Nonnull final Scheduler pool) {
		return new Observable<Double>() {
			@Override
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super Double> observer) {
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
		return range(start, count, step, scheduler());
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super Float> observer) {
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
			final int count) {
		return range(start, count, scheduler());
	}
	/**
	 * Creates an observable which generates numbers from start.
	 * @param start the start value.
	 * @param count the count
	 * @param pool the execution thread pool.
	 * @return the observable
	 */
	@Nonnull 
	public static Observable<Integer> range(
			final int start,
			final int count,
			@Nonnull final Scheduler pool) {
		return new Observable<Integer>() {
			@Override
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super Integer> observer) {
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
	 * Returns an observable sequence which 
	 * connects to the source for the first registered 
	 * party and stays connected to the source
	 * as long as there is at least one registered party to it.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @return the observable sequence.
	 * @since 0.97
	 */
	@Nonnull 
	public static <T> Observable<T> refCount(
			@Nonnull final ConnectableObservable<? extends T> source) {
		return new RefCount<T>(source);
	}
	/**
	 * Wrap the given observable into an new Observable instance, which calls the original register() method
	 * on the supplied pool.
	 * @param <T> the type of the objects to observe
	 * @param observable the original observable
	 * @param pool the pool to perform the original register() call
	 * @return the new observable
	 */
	@Nonnull
	public static <T> Observable<T> registerOn(
			@Nonnull final Observable<T> observable,
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T> observer) {
				SingleCloseable cancelRegister = new SingleCloseable();
				
				final SequentialCloseable cancelUnregister = new SequentialCloseable();
				
				cancelUnregister.set(cancelRegister);
				
				cancelRegister.set(pool.schedule(new Runnable() {
					@Override
					public void run() {
						final Closeable c = observable.register(observer);
						cancelUnregister.set(new ScheduledCloseable(pool, c));
					}
				}));
				
				return cancelUnregister;
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T> observer) {
				DefaultObserverEx<T> obs = new DefaultObserverEx<T>(true) {
					@Override
					public void onError(@Nonnull Throwable ex) {
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
			@Nonnull Observable<Timestamped<? extends T>> source) {
		Func1<Timestamped<? extends T>, T> f = unwrapTimestamped();
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
		return repeat(func, scheduler());
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
		return repeat(func, count, scheduler());
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T> observer) {
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
	@Nonnull 
	public static <T> Observable<T> repeat(
			@Nonnull final Func0<? extends T> func,
			@Nonnull final Scheduler pool) {
		return new Repeat.RepeatValue<T>(func, pool);
	}
	/**
	 * Repeat the source observable indefinitely.
	 * @param <T> the element type
	 * @param source the source observable
	 * @return the new observable
	 * @see Reactive#doWhile(Observable, Func0)
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<T> repeat(
			@Nonnull Observable<? extends T> source) {
		return doWhile(source, Functions.TRUE);
	}
	/**
	 * Repeat the source observable count times.
	 * @param <T> the element type
	 * @param source the source observable
	 * @param count the number of times to repeat
	 * @return the new observable
	 * @see Reactive#doWhile(Observable, Func0)
	 */
	@Nonnull
	public static <T> Observable<T> repeat(
			@Nonnull Observable<? extends T> source,
			final int count) {
		if (count > 0) {
			Pred0 condition = new Pred0() {
				/** Repeat counter. */
				int i = count - 1;
				@Override
				public Boolean invoke() {
					return i-- > 0;
				}
			};
			return doWhile(source, condition);
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
		return repeat(value, scheduler());
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
		return repeat(value, count, scheduler());
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
	 * Creates an observable which shares the source observable and replays all source Ts
	 * to any of the registering observers.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @return the new observable
	 */
	@Nonnull 
	public static <T> Observable<T> replay(
			@Nonnull final Observable<? extends T> source
	) {
		return replay(source, scheduler());
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
	@Nonnull 
	public static <T, U> Observable<U> replay(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super Observable<? extends T>, ? extends Observable<U>> selector,
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
	@Nonnull 
	public static <T, U> Observable<U> replay(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super Observable<? extends T>, ? extends Observable<U>> selector,
			final int bufferSize,
			final long timeSpan,
			@Nonnull final TimeUnit unit
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
	@Nonnull 
	public static <T, U> Observable<U> replay(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super Observable<? extends T>, ? extends Observable<U>> selector,
			final int bufferSize,
			final long timeSpan,
			@Nonnull final TimeUnit unit,
			@Nonnull final Scheduler scheduler
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
	@Nonnull 
	public static <T, U> Observable<U> replay(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super Observable<? extends T>, ? extends Observable<U>> selector,
			final long timeSpan,
			@Nonnull final TimeUnit unit
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
	@Nonnull 
	public static <T, U> Observable<U> replay(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super Observable<? extends T>, ? extends Observable<U>> selector,
			final long timeSpan,
			@Nonnull final TimeUnit unit,
			@Nonnull final Scheduler scheduler
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
	@Nonnull 
	public static <T> Observable<T> replay(
			@Nonnull final Observable<? extends T> source,
			final int bufferSize
	) {
		return replay(source, bufferSize, scheduler());
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
	@Nonnull 
	public static <T> Observable<T> replay(
			@Nonnull final Observable<? extends T> source,
			final int bufferSize,
			final long timeSpan,
			@Nonnull final TimeUnit unit
	) {
		return replay(source, bufferSize, timeSpan, unit, scheduler());
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
	@Nonnull 
	public static <T> Observable<T> replay(
			@Nonnull final Observable<? extends T> source,
			final int bufferSize,
			final long timeSpan,
			@Nonnull final TimeUnit unit,
			@Nonnull final Scheduler scheduler
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T> observer) {
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
							public void error(@Nonnull Throwable ex) {
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
	@Nonnull 
	public static <T> Observable<T> replay(
			@Nonnull final Observable<? extends T> source,
			final int bufferSize,
			@Nonnull final Scheduler scheduler
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T> observer) {
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
							public void error(@Nonnull Throwable ex) {
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
	@Nonnull 
	public static <T> Observable<T> replay(
			@Nonnull final Observable<? extends T> source,
			final long timeSpan,
			@Nonnull final TimeUnit unit
	) {
		return replay(source, timeSpan, unit, scheduler());
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
	@Nonnull 
	public static <T> Observable<T> replay(
			@Nonnull final Observable<? extends T> source,
			final long timeSpan,
			@Nonnull final TimeUnit unit,
			@Nonnull final Scheduler scheduler
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T> observer) {
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
							public void error(@Nonnull Throwable ex) {
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
	@Nonnull 
	public static <T> Observable<T> replay(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Scheduler scheduler
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T> observer) {
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
							public void error(@Nonnull Throwable ex) {
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
	@Nonnull 
	public static <T, U> Observable<U> replay(
			@Nonnull final Observable<T> source,
			@Nonnull final Func1<? super Observable<? extends T>, ? extends Observable<U>> selector,
			final int bufferSize,
			@Nonnull final Scheduler scheduler
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
	@Nonnull 
	public static <T, U> Observable<U> replay(
			@Nonnull final Observable<T> source,
			@Nonnull final Func1<? super Observable<T>, ? extends Observable<U>> selector
	) {
		return selector.invoke(source);
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
		return new Resume.Always<T>(sources);
	}

	/**
	 * Continues the observable sequence in case of exception
	 * whith the sequence provided by the function for that particular
	 * exception.
	 * <p>Exception semantics: in case of an exception in source,
	 * the exception is turned into a continuation, but the second
	 * observable's error now terminates the sequence.
	 * <p>Note: Rx calls this Catch.</p>
	 * @author akarnokd, 2013.01.14.
	 * @param <T> the source and result element type
	 * @param source The source sequence
	 * @param handler The exception handler
	 * @return the new observable
	 * @since 0.97
	 */
	public static <T> Observable<T> resumeConditionally(
			@Nonnull Observable<? extends T> source,
			@Nonnull Func1<? super Throwable, ? extends Observable<? extends T>> handler) {
		return new Resume.Conditionally<T>(source, handler);
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
		return new Resume.OnError<T>(sources);
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
		return new Resume.Retry<T>(source);
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
		return new Resume.RetryCount<T>(source, count);
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
			public void onError(@Nonnull Throwable ex) {
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
			public void onError(@Nonnull Throwable ex) {
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
			public void onError(@Nonnull Throwable ex) {
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
			public void onError(@Nonnull Throwable ex) {
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
		return sample(source, time, unit, scheduler());
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T> observer) {
				final AtomicReference<T> current = new AtomicReference<T>();
				final AtomicBoolean first = new AtomicBoolean(true);
				
				final DefaultObserverEx<T> obs = new DefaultObserverEx<T>(true) {
					@Override
					public void onError(@Nonnull Throwable ex) {
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
		return new Aggregate.Scan<T>(source, accumulator);
	}
	/**
	 * Creates an observable which accumultates the given 
	 * source and submits each intermediate results to its subscribers.
	 * Example:<br>
	 * <code>range(0, 5).accumulate(1, (x, y) => x + y)</code> produces a sequence of [1, 2, 4, 7, 11];<br>
	 * basically the accumulation starts from zero and the 
	 * first value (0) that comes in is simply added.
	 * @param <T> the element type to accumulate
	 * @param <U> the accumulation type
	 * @param source the source of the accumulation
	 * @param seed the initial value of the accumulation
	 * @param accumulator the accumulator which takest the current accumulation value and the current observed value
	 * and returns a new accumulated value
	 * @return the observable
	 */
	@Nonnull
	public static <T, U> Observable<U> scan(
			@Nonnull final Observable<? extends T> source,
			final U seed,
			@Nonnull final Func2<? super U, ? super T, ? extends U> accumulator) {
		return new Aggregate.ScanSeeded<U, T>(source, seed, accumulator);
	}
	/**
	 * @return the current default pool used by the Observables methods
	 */
	@Nonnull
	static Scheduler scheduler() {
		return Schedulers.getDefault();
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
		return new Select.Simple<T, U>(source, mapper);
	}
	/**
	 * Transforms the elements of the source observable into 
	 * Us by using a selector which receives an index indicating
	 * how many elements have been transformed this far.
	 * @param <T> the source element type
	 * @param <U> the output element type
	 * @param source the source observable
	 * @param selector the selector taking an index and the current T
	 * @return the transformed observable
	 */
	public static <T, U> Observable<U> select(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func2<? super T, ? super Integer, ? extends U> selector) {
		return new Select.Indexed<T, U>(source, selector);
	}
	/**
	 * Transforms the elements of the source observable into 
	 * Us by using a selector which receives an long index indicating
	 * how many elements have been transformed this far.
	 * @param <T> the source element type
	 * @param <U> the output element type
	 * @param source the source observable
	 * @param selector the selector taking an index and the current T
	 * @return the transformed observable
	 * @since 0.97
	 */
	public static <T, U> Observable<U> selectLong(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func2<? super T, ? super Long, ? extends U> selector) {
		return new Select.LongIndexed<T, U>(source, selector);
	}
	
	/**
	 * Transform the given source of Ts into Us in a way that the
	 * selector might return an observable ofUs for a single T.
	 * The observable is fully channelled to the output observable.
	 * @param <T> the input element type
	 * @param <U> the output element type
	 * @param source the source of Ts
	 * @param selector the selector to return an Iterable of Us
	 * @return the serialized sequence of Us
	 */
	@Nonnull
	public static <T, U> Observable<U> selectMany(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends Observable<? extends U>> selector) {
		return selectMany(source, selector, Functions.<T, U>identitySecond());
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
		return new SelectMany.Paired<T, U, V>(source, collectionSelector, resultSelector);
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
	 * @return the observable of Us
	 */
	@Nonnull
	public static <T, U> Observable<U> selectManyIterable(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends Iterable<? extends U>> selector) {
		return selectManyIterable(source, selector, Functions.<T, U>identitySecond());
	}
	/**
	 * Transform the given source of Ts into Us in a way that the selector might return zero to multiple elements of Us for a single T.
	 * The iterable is flattened and submitted to the output
	 * @param <T> the input element type
	 * @param <U> the intermediate type of
	 * @param <V> the output type
	 * @param source the source of Ts
	 * @param selector the selector to return an Iterable of Us
	 * @param resultSelector the selector for a pair of T and U
	 * @return the observable of Vs
	 * @since 0.97
	 */
	@Nonnull
	public static <T, U, V> Observable<V> selectManyIterable(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func1<? super T, ? extends Iterable<? extends U>> selector,
			@Nonnull final Func2<? super T, ? super U, ? extends V> resultSelector) {
		return new SelectMany.WithIterable<T, U, V>(source, selector, resultSelector);
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
	@Nonnull 
	public static <T> Observable<Boolean> sequenceEqual(
			@Nonnull final Iterable<? extends T> first,
			@Nonnull final Observable<? extends T> second,
			@Nonnull final Func2<? super T, ? super T, Boolean> comparer) {
		return select(
				any(
					zip(
							materialize(second), 
							Interactive.materialize(first), 
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
	@Nonnull 
	public static <T> Observable<Boolean> sequenceEqual(
			@Nonnull final Observable<? extends T> first,
			@Nonnull final Observable<? extends T> second) {
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
	@Nonnull 
	public static <T> Observable<Boolean> sequenceEqual(
			@Nonnull final Observable<? extends T> first,
			@Nonnull final Observable<? extends T> second,
			@Nonnull final Func2<? super T, ? super T, Boolean> comparer) {
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
	public static <T> T single(
			@Nonnull Observable<? extends T> source,
			T defaultValue) {
		return single(source, Functions.constant0(defaultValue));
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
			public Closeable register(@Nonnull final Observer<? super T> observer) {
				return (new DefaultObserverEx<T>() {
					/** True if the first element received. */
					boolean firstReceived;
					/** The first element encountered. */
					T first;
					@Override
					protected void onError(@Nonnull Throwable ex) {
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

					@Override
					protected void onNext(T value) {
						if (!firstReceived) {
							first = value;
							firstReceived = true;
						} else {
							error(new TooManyElementsException());
						}
					}
					
				}).registerWith(source);
			}
		};
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
			public Closeable register(@Nonnull final Observer<? super T> observer) {
				return (new DefaultObserverEx<T>() {
					/** True if the first element received. */
					boolean firstReceived;
					/** The first element encountered. */
					T first;
					@Override
					protected void onError(@Nonnull Throwable ex) {
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

					@Override
					protected void onNext(T value) {
						if (!firstReceived) {
							first = value;
							firstReceived = true;
						} else {
							error(new TooManyElementsException());
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
		return singleton(supplier, scheduler());
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T> observer) {
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
		return singleton(value, scheduler());
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T> observer) {
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
		return new Skip.First<T>(source, count);
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
		return new Skip.Last<T>(source, count);
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
		return new Skip.Until<T, U>(source, signaller);
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
		return new Skip.While<T>(source, condition);
	}
	/**
	 * Skips the Ts from source while the specified indexed condition returns true.
	 * If the condition returns false, all subsequent Ts are relayed,
	 * ignoring the condition further on. Errors and completion
	 * is relayed regardless of the condition.
	 * @param <T> the element types
	 * @param source the source of Ts
	 * @param condition the condition that must turn false in order to start relaying
	 * @return the new observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<T> skipWhile(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func2<? super T, ? super Integer, Boolean> condition) {
		return new Skip.WhileIndexed<T>(source, condition);
	}
	/**
	 * Skips the Ts from source while the specified long indexed condition returns true.
	 * If the condition returns false, all subsequent Ts are relayed,
	 * ignoring the condition further on. Errors and completion
	 * is relayed regardless of the condition.
	 * @param <T> the element types
	 * @param source the source of Ts
	 * @param condition the condition that must turn false in order to start relaying
	 * @return the new observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<T> skipWhileLong(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func2<? super T, ? super Long, Boolean> condition) {
		return new Skip.WhileLongIndexed<T>(source, condition);
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
		return start(action, scheduler());
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super Void> observer) {
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
		return start(func, scheduler());
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T> observer) {
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
		return startWith(source, values, scheduler());
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
	 * source. The value is emitted on the given pool.
	 * @param <T> the element type
	 * @param source the source
	 * @param pool the pool where the iterable values should be emitted
	 * @param values the values to start with
	 * @return the new observable
	 */
	@Nonnull
	public static <T> Observable<T> startWith(
			@Nonnull Observable<? extends T> source,
			@Nonnull Scheduler pool,
			T... values
			) {
		return startWith(source, Arrays.asList(values), pool);
	}
	/**
	 * Start with the given iterable of values before relaying the Ts from the
	 * source. The value is emitted on the default pool.
	 * @param <T> the element type
	 * @param source the source
	 * @param values the array of values
	 * @return the new observable
	 */
	@Nonnull
	public static <T> Observable<T> startWith(
			@Nonnull Observable<? extends T> source,
			T... values) {
		return startWith(source, Arrays.asList(values), scheduler());
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
	 * Returns an observable which uses a selector function
	 * to return the observable sequence to work with or
	 * the empty sequence run on the default scheduler.
	 * @param <T> the selector type
	 * @param <U> the observable element type
	 * @param selector the selector
	 * @param sources the map of sources
	 * @return the new observable sequence
	 * @since 0.97
	 */
	@Nonnull 
	public static <T, U> Observable<U> switchCase(
			@Nonnull final Func0<? extends T> selector, 
			@Nonnull final Map<? super T, ? extends Observable<U>> sources) {
		return switchCase(selector, sources, Reactive.<U>empty());
	}
	/**
	 * Returns an observable which uses a selector function
	 * to return the observable sequence to work with or
	 * the default source.
	 * @param <T> the selector type
	 * @param <U> the observable element type
	 * @param selector the selector
	 * @param sources the map of sources
	 * @param defaultSource the default source
	 * @return the new observable sequence
	 * @since 0.97
	 */
	@Nonnull 
	public static <T, U> Observable<U> switchCase(
			@Nonnull final Func0<? extends T> selector, 
			@Nonnull final Map<? super T, ? extends Observable<U>> sources, 
			@Nonnull final Observable<U> defaultSource) {
		return new Observable<U>() {
			@Override
			@Nonnull
			public Closeable register(@Nonnull Observer<? super U> observer) {
				T key = selector.invoke();
				Observable<U> obs = sources.get(key);
				if (obs == null) {
					obs = defaultSource;
				}
				return obs.register(observer);
			}
		};
	}
	/**
	 * Returns an observable which uses a selector function
	 * to return the observable sequence to work with or
	 * the empty sequence run on the specified scheduler.
	 * @param <T> the selector type
	 * @param <U> the observable element type
	 * @param selector the selector
	 * @param sources the map of sources
	 * @param pool the scheduler to use for the empty case
	 * @return the new observable sequence
	 * @since 0.97
	 */
	@Nonnull 
	public static <T, U> Observable<U> switchCase(
			@Nonnull final Func0<? extends T> selector, 
			@Nonnull final Map<? super T, ? extends Observable<U>> sources,
			@Nonnull final Scheduler pool) {
		return switchCase(selector, sources, Reactive.<U>empty(pool));
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T> observer) {
				DefaultObserver<Observable<? extends T>> outer
				= new DefaultObserver<Observable<? extends T>>(false) {
					/** The inner observer. */
					@GuardedBy("lock")
					Closeable inner;

					DefaultObserver<T> innerObserver = new DefaultObserver<T>(lock, true) {
						@Override
						protected void onError(@Nonnull Throwable ex) {
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
					protected void onError(@Nonnull Throwable ex) {
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
	 * Returns an observable sequence which ensures that
	 * the registering observers follow the general contract
	 * on observables by serializing access to the event
	 * methods. This can be used to make
	 * non-conformant observables to work with observers conforming the
	 * contract.
	 * @param <T> the element type
	 * @param source the source sequence.
	 * @return the new observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<T> synchronize(@Nonnull final Observable<? extends T> source) {
		return new Observable<T>() {
			@Override
			@Nonnull
			public Closeable register(@Nonnull Observer<? super T> observer) {
				return DefaultObserverEx.wrap(observer).registerWith(source);
			}
		};
	}
	/**
	 * Creates an observable which takes the specified number of
	 * Ts from the source, unregisters and completes.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param count the number of elements to relay, setting
	 * it to zero will finish the output after the reception of 
	 * the first event.
	 * @return the new observable
	 */
	@Nonnull
	public static <T> Observable<T> take(
			@Nonnull final Observable<? extends T> source,
			final int count) {
		return take(source, count, Schedulers.constantTimeOperations());
	}
	/**
	 * Creates an observable which takes the specified number of
	 * Ts from the source, unregisters and completes.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param count the number of elements to relay, setting
	 * it to zero will finish the output after the reception of 
	 * the first event.
	 * @param scheduler the scheduler to emit the finish event
	 * if the count is zero
	 * @return the new observable
	 */
	@Nonnull
	public static <T> Observable<T> take(
			@Nonnull final Observable<? extends T> source,
			final int count, 
			@Nonnull Scheduler scheduler) {
		if (count == 0) {
			return empty(scheduler);
		}
		return new Take.First<T>(source, count);
	}
	/**
	 * Returns an observable which returns the last <code>count</code>
	 * elements from the source observable.
	 * @param <T> the element type
	 * @param source the source of the elements
	 * @param count the number elements to return
	 * @return the new observable
	 */
	@Nonnull 
	public static <T> Observable<T> takeLast(
			@Nonnull final Observable<? extends T> source, final int count) {
		return new Take.Last<T>(source, count);
	}
	/**
	 * Returns an observable which returns the last <code>count</code>
	 * elements from the source observable and emits them from
	 * the specified scheduler pool.
	 * @param <T> the element type
	 * @param source the source of the elements
	 * @param count the number elements to return
	 * @param pool the scheduler where from emit the last values
	 * @return the new observable
	 * @since 0.97
	 */
	@Nonnull 
	public static <T> Observable<T> takeLast(
			@Nonnull final Observable<? extends T> source, 
			final int count,
			@Nonnull final Scheduler pool) {
		return new Take.LastScheduled<T>(source, count, pool);
	}
	/**
	 * Returns an observable which returns the last <code>count</code>
	 * elements from the source observable and
	 * returns it as a single list.
	 * @param <T> the element type
	 * @param source the source of the elements
	 * @param count the number elements to return
	 * @return the new observable
	 * @since 0.97
	 */
	@Nonnull 
	public static <T> Observable<List<T>> takeLastBuffer(
			@Nonnull final Observable<? extends T> source, final int count) {
		return new Take.LastBuffer<T>(source, count);
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
		return new Take.Until<T, U>(source, signaller);
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
		return new Take.While<T>(source, predicate);
	}
	/**
	 * Creates an observable which takes values from source until
	 * the indexed predicate returns false for the current element, then skips the remaining values.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param predicate the predicate
	 * @return the new observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<T> takeWhile(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func2<? super T, ? super Integer, Boolean> predicate) {
		return new Take.WhileIndexed<T>(source, predicate);
	}
	/**
	 * Creates an observable which takes values from source until
	 * the long indexed predicate returns false for the current element, then skips the remaining values.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param predicate the predicate
	 * @return the new observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<T> takeWhileLong(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func2<? super T, ? super Long, Boolean> predicate) {
		return new Take.WhileLongIndexed<T>(source, predicate);
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
		return throttle(source, delay, unit, scheduler());
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T> observer) {
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
					public void onError(@Nonnull Throwable ex) {
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
		return throwException(supplier, Schedulers.constantTimeOperations());
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T> observer) {
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
		return throwException(ex, scheduler());
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T> observer) {
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
	 * Returns an observable which produces an ordered sequence of 
	 * numbers with the specified delay.
	 * <p>It uses the default scheduler pool.</p>
	 * <p>Finishes right after reaching the final value of <code>end - 1</code></p>
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
		return tick(start, end, delay, unit, scheduler());
	}
	/**
	 * Returns an observable which produces an ordered sequence 
	 * of numbers with the specified delay.
	 * <p>Finishes right after reaching the final value of <code>end - 1</code></p>
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super Long> observer) {
				return pool.schedule(new DefaultRunnable() {
					/** The current value. */
					long current = start;
					@Override
					protected void onRun() {
						if (current < end && !cancelled()) {
							observer.next(current++);
						}
						if (current == end) {
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
			final long delay,
			@Nonnull final TimeUnit unit) {
		return tick(0, Long.MAX_VALUE, delay, unit, scheduler());
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
		return timeout(source, time, unit, scheduler());
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
		return timeout(source, time, unit, other, scheduler());
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T> observer) {
				DefaultObserverEx<T> obs = new DefaultObserverEx<T>(true) {
					@Override
					protected void onError(@Nonnull Throwable ex) {
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
					@Override
					protected void onRegister() {
						registerTimer();
					}
					/**
					 * Register the timer that when fired, switches to the second
					 * observable sequence
					 */
					private void registerTimer() {
						add("timer", pool.schedule(new DefaultRunnable(lock) {
							@Override
							public void onRun() {
								if (!cancelled()) {
									registerWith(other);
								}
							}
						}, time, unit));
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T[]> observer) {
				return source.register(new Observer<T>() {
					/** The buffer for the Ts. */
					final List<T> list = new LinkedList<T>();
					@Override
					public void error(@Nonnull Throwable ex) {
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
		return new ToIterable<T>(observable);
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super List<T>> observer) {
				return source.register(new Observer<T>() {
					/** The list for aggregation. */
					final List<T> list = new LinkedList<T>();
					@Override
					public void error(@Nonnull Throwable ex) {
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super Map<K, V>> observer) {
				return source.register(new Observer<T>() {
					/** The map. */
					final Map<K, V> map = new HashMap<K, V>();
					@Override
					public void error(@Nonnull Throwable ex) {
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super Map<K, V>> observer) {
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
					public void error(@Nonnull Throwable ex) {
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super Map<K, Collection<V>>> observer) {
				return source.register(new Observer<T>() {
					/** The map. */
					final Map<K, Collection<V>> map = new HashMap<K, Collection<V>>();
					@Override
					public void error(@Nonnull Throwable ex) {
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super Map<K, Collection<V>>> observer) {
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
					public void error(@Nonnull Throwable ex) {
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
	 * Returns an observable which delivers the
	 * result of the future object on the default scheduler.
	 * @param <T> the return type
	 * @param future the future to wrap
	 * @return the observable
	 */
	public static <T> Observable<T> toObservable(
			@Nonnull final Future<? extends T> future) {
		return toObservable(future, Schedulers.getDefault());
	}
	/**
	 * Returns an observable which delivers the
	 * result of the future object on the given scheduler.
	 * @param <T> the return type
	 * @param future the future to wrap
	 * @param pool the scheduler pool to wait on
	 * @return the observable
	 */
	public static <T> Observable<T> toObservable(
			@Nonnull final Future<? extends T> future,
			@Nonnull final Scheduler pool) {
		return new Observable<T>() {
			@Override
			@Nonnull
			public Closeable register(@Nonnull final Observer<? super T> observer) {
				return pool.schedule(new Runnable() {
					@Override
					public void run() {
						try {
							T v = future.get();
							observer.next(v);
							observer.finish();
						} catch (InterruptedException e) {
							observer.error(e);
						} catch (ExecutionException e) {
							observer.error(e.getCause() != null ? e.getCause() : e);
						}
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
		return toObservable(iterable, scheduler());
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T> observer) {
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
	public static <T> Func1<Timestamped<? extends T>, T> unwrapTimestamped() {
		return new Func1<Timestamped<? extends T>, T>() {
			@Override
			public T invoke(Timestamped<? extends T> param1) {
				return param1.value();
			}
		};
	}
	/**
	 * Receives a resource from the resource selector and
	 * uses the resource until it terminates, then closes the resource.
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
			@Nonnull 
			public Closeable register(@Nonnull final Observer<? super T> observer) {
				final U resource = resourceSelector.invoke();
				return resourceUsage.invoke(resource).register(new Observer<T>() {
					@Override
					public void error(@Nonnull Throwable ex) {
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
			@Nonnull final Func0<? extends Func2<? super T, ? super Integer, Boolean>> clauseFactory) {
		return new Where.IndexedFactory<T>(source, clauseFactory);
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
		return new Where.Simple<T>(source, clause);
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
			@Nonnull final Func2<? super T, ? super Integer, Boolean> clause) {
		return new Where.Indexed<T>(source, clause);
	}
	/**
	 * Creates a filtered observable where only Ts are relayed which satisfy the clause.
	 * The clause receives the index and the current element to test.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param clause the filter clause, the first parameter receives the current index, the second receives the current element
	 * @return the new observable
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<T> whereLong(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func2<? super T, ? super Long, Boolean> clause) {
		return new Where.LongIndexed<T>(source, clause);
	}
	/**
	 * Repeatedly registers with the source observable 
	 * if the condition holds on registration.
	 * The condition is checked before each registration.
	 * @param <T> the element type
	 * @param source the source sequence
	 * @param condition the condition to check
	 * @return the new observable
	 * @since 0.97
	 */
	public static <T> Observable<T> whileDo(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Func0<Boolean> condition) {
		return new Repeat.WhileDo<T>(source, condition);
	}
	/**
	 * Splits the source stream into separate observables once
	 * the windowClosing fires an event.
	 * @param <T> the element type to observe
	 * @param <U> the closing event type, irrelevant
	 * @param source the source of Ts
	 * @param windowClosingSelector the source of the window splitting events
	 * @return the observable on sequences of observables of Ts
	 */
	@Nonnull
	public static <T, U> Observable<Observable<T>> window(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Func0<? extends Observable<U>> windowClosingSelector) {
		return new Windowing.WithClosing<T, U>(source, windowClosingSelector);
	}
	/**
	 * Project the source sequence to
	 * non-overlapping windows with the given
	 * size.
	 * @author akarnokd, 2013.01.14.
	 * @param <T> the element type
	 * @param source the source sequence
	 * @param size the window size
	 * @return the observable sequence
	 */
	@Nonnull
	public static <T> Observable<Observable<T>> window(
			@Nonnull Observable<? extends T> source, int size) {
		return window(source, size, size);
	}
	/**
	 * Project the source sequence to
	 * potentially overlapping windows whose
	 * start is determined by skip and lengths
	 * by size.
	 * @author akarnokd, 2013.01.14.
	 * @param <T> the element type
	 * @param source the source sequence
	 * @param size the window size
	 * @param skip the elements to skip between windows.
	 * @return the observable sequence
	 * @since 0.97
	 */
	@Nonnull
	public static <T> Observable<Observable<T>> window(
			@Nonnull Observable<? extends T> source, int size, int skip) {
		return new Windowing.WithSizeSkip<T>(source, size, skip);
	}
	/**
	 * Splits the source stream into separate observables
	 * by starting at windowOpening events and closing at windowClosing events.
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
		return new Windowing.WithOpenClose<T, U, V>(source, windowOpening, windowClosing);
	}
	/**
	 * Projects the source elements into a non-overlapping consecutive windows.
	 * <p>The first window opens immediately, The current window is closed when 
	 * the boundary observable sequence has sent a value. The finish
	 * of the boundary will finish both inner and outer observables.
	 * <p>Exception semantics: exception thrown by the source or the
	 * windowClosingSelector's observable is propagated to both the outer
	 * and inner observable returned.</p>
	 * @param <T> the source and result element type
	 * @param <U> the window boundary element type (irrelevant
	 * @param source the source sequence
	 * @param boundary the window boundary indicator.
	 * @return the new observable
	 */
	public static <T, U> Observable<Observable<T>> window(
			@Nonnull final Observable<? extends T> source,
			@Nonnull final Observable<U> boundary
			) {
		return new Windowing.WithBoundary<T, U>(source, boundary);
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
	 * Combine the incoming Ts of the various observables into a single list of Ts like
	 * using zip() on more than two sources.
	 * <p>The resulting sequence terminates if no more pairs can be
	 * established, i.e., streams of length 1 and 2 zipped will produce
	 * only 1 item.</p>
	 * <p>Exception semantics: errors from the source observable are
	 * propagated as-is.</p>
	 * @param <T> the element type
	 * @param srcs the iterable of observable sources.
	 * @return the new observable
	 */
	@Nonnull 
	public static <T> Observable<List<T>> zip(
			@Nonnull final Iterable<? extends Observable<? extends T>> srcs) {
		return zip(srcs, Functions.<List<T>>identity());
	}
	/**
	 * Combine the incoming Ts of the various observables into a
	 * single value stream by the given selector.
	 * <p>The resulting sequence terminates if no more pairs can be
	 * established, i.e., streams of length 1 and 2 zipped will produce
	 * only 1 item.</p>
	 * <p>Exception semantics: errors from the source observable are
	 * propagated as-is.</p>
	 * @param <T> the element type
	 * @param <U> the result element type
	 * @param srcs the iterable of observable sources.
	 * @param selector the result selector function
	 * @return the new observable
	 * @since 0.97
	 */
	@Nonnull 
	public static <T, U> Observable<U> zip(
			@Nonnull final Iterable<? extends Observable<? extends T>> srcs,
			@Nonnull final Func1<? super List<T>, ? extends U> selector) {
		return new Zip.ManyObservables<T, U>(srcs, selector);
	}
	/**
	 * Creates an observable which waits for events from left
	 * and combines it with the next available value from the iterable,
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
			@Nonnull final Observable<? extends T> left,
			@Nonnull final Iterable<? extends U> right,
			@Nonnull final Func2<? super T, ? super U, ? extends V> selector) {
		return new Zip.ObservableAndIterable<T, U, V>(left, right, selector);
	}
	/**
	 * Creates an observable which waits for events from left
	 * and combines it with the next available value from the right observable,
	 * applies the selector function and emits the resulting T.
	 * Basically it emits a T when both an U and V is available.
	 * The output stream throws error or terminates if any of the streams
	 * throws or terminates.
	 * @param <T> the value type streamed on the left observable
	 * @param <U> the value type streamed on the right iterable
	 * @param <V> the resulting element type
	 * @param left the left observables of Us
	 * @param right the right iterable of Vs
	 * @param selector the selector taking the left Us and right Vs.
	 * @return the resulting observable
	 */
	@Nonnull
	public static <T, U, V> Observable<V> zip(
			@Nonnull final Observable<? extends T> left,
			@Nonnull final Observable<? extends U> right,
			@Nonnull final Func2<? super T, ? super U, ? extends V> selector) {
		return new Zip.TwoObservable<T, U, V>(left, right, selector);
	}
	/**
	 * Combine a stream of Ts with a constant T whenever the src fires.
	 * The observed list contains the values of src as the first value, constant as the second.
	 * @param <T> the element type
	 * @param src the source of Ts
	 * @param constant the constant T to combine with
	 * @return the new observable
	 */
	@Nonnull 
	public static <T> Observable<List<T>> zip(
			@Nonnull Observable<? extends T> src, final T constant) {
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
	@Nonnull 
	public static <T> Observable<List<T>> zip(final T constant, 
			@Nonnull Observable<? extends T> src) {
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
	 * Convert an object-int observable into a primitive-int observable.
	 * @param source the source sequence
	 * @return the primitive sequence
	 * @since 0.97
	 */
	public static IntObservable toIntObservable(@Nonnull final Observable<Integer> source) {
		return new ToPrimitive.ToInt(source);
	}
	/**
	 * Convert an object-long observable into a primitive-long observable.
	 * @param source the source sequence
	 * @return the primitive sequence
	 * @since 0.97
	 */
	public static LongObservable toLongObservable(@Nonnull final Observable<Long> source) {
		return new ToPrimitive.ToLong(source);
	}
	/**
	 * Convert an object-double observable into a primitive-double observable.
	 * @param source the source sequence
	 * @return the primitive sequence
	 * @since 0.97
	 */
	public static DoubleObservable toDoubleObservable(@Nonnull final Observable<Double> source) {
		return new ToPrimitive.ToDouble(source);
	}
	/*
	 * TODO merge() with concurrency limit.
	 */
	/** Utility class. */
	private Reactive() {
		// utility class
	}
}
