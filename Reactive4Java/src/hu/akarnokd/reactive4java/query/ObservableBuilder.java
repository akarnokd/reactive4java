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
package hu.akarnokd.reactive4java.query;

import hu.akarnokd.reactive4java.base.Action0;
import hu.akarnokd.reactive4java.base.Action1;
import hu.akarnokd.reactive4java.base.Func0;
import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Func2;
import hu.akarnokd.reactive4java.base.Functions;
import hu.akarnokd.reactive4java.base.Option;
import hu.akarnokd.reactive4java.base.Scheduler;
import hu.akarnokd.reactive4java.interactive.Interactive;
import hu.akarnokd.reactive4java.reactive.GroupedObservable;
import hu.akarnokd.reactive4java.reactive.Observable;
import hu.akarnokd.reactive4java.reactive.Observer;
import hu.akarnokd.reactive4java.reactive.Reactive;
import hu.akarnokd.reactive4java.reactive.TimeInterval;
import hu.akarnokd.reactive4java.reactive.Timestamped;

import java.io.Closeable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * Wrapper object arount an {@code Observable} which
 * lets the user chain some Reactive operators.
 * <p>This builder is the dual of the 
 * {@link hu.akarnokd.reactive4java.query.IterableBuilder} class.</p>
 * @author akarnokd, Jan 25, 2012
 * @param <T> the element type
 */
public final class ObservableBuilder<T> implements Observable<T> {
	/** The backed observable. */
	protected final Observable<T> o;
	/**
	 * Constructor.
	 * @param source the source sequence
	 */
	protected ObservableBuilder(Observable<T> source) {
		this.o = source;
	}
	@Override
	public Closeable register(Observer<? super T> observer) {
		return o.register(observer);
	}
	/**
	 * Wraps the supplied observable sequence into an observable builder.
	 * @param <T> the element type
	 * @param source the source observable
	 * @return the created observable builder
	 */
	public static <T> ObservableBuilder<T> from(@Nonnull Observable<T> source) {
		return new ObservableBuilder<T>(source);
	}
	/**
	 * Creates an observable builder from the source sequence by using
	 * the default scheduler to emit the values.
	 * @param <T> the element type
	 * @param source the source iterable
	 * @return the created observable builder
	 */
	public static <T> ObservableBuilder<T> from(@Nonnull Iterable<? extends T> source) {
		return from(Reactive.toObservable(source));
	}
	/**
	 * Creates an observable builder from the source sequence and uses
	 * the given schduler to emit the values.
	 * @param <T> the element type
	 * @param source the source iterable
	 * @param scheduler the scheduler to emit the values
	 * @return the created observable builder
	 */
	public static <T> ObservableBuilder<T> from(@Nonnull Iterable<? extends T> source, @Nonnull Scheduler scheduler) {
		return from(Reactive.toObservable(source, scheduler));
	}
	/**
	 * Creates an observable sequence from the array of values and uses
	 * the default scheduler to emit these values.
	 * @param <T> the element type
	 * @param ts the element array
	 * @return the created observable builder
	 */
	public static <T> ObservableBuilder<T> from(@Nonnull T... ts) {
		return from(Interactive.toIterable(ts));
	}
	/**
	 * Creates an observable sequence from the subarray of values and uses
	 * the default scheduler to emit these values.
	 * @param <T> the element type
	 * @param start the start index inclusive
	 * @param end the end index exclusive
	 * @param ts the element array
	 * @return the created observable builder
	 */
	public static <T> ObservableBuilder<T> from(int start, int end, @Nonnull T... ts) {
		return from(Interactive.toIterable(start, end, ts));
	}
	/**
	 * Creates an observable sequence from the array of values and uses
	 * the given scheduler to emit these values.
	 * @param <T> the element type
	 * @param scheduler the scheduler to emit the values
	 * @param ts the element array
	 * @return the created observable builder
	 */
	public static <T> ObservableBuilder<T> from(@Nonnull Scheduler scheduler, @Nonnull T... ts) {
		return from(Interactive.toIterable(ts), scheduler);
	}
	/**
	 * Creates an observable sequence from the subarray of values and uses
	 * the given scheduler to emit these values.
	 * @param <T> the element type
	 * @param start the start index inclusive
	 * @param end the end index exclusive
	 * @param scheduler the scheduler to emit the values
	 * @param ts the element array
	 * @return the created observable builder
	 */
	public static <T> ObservableBuilder<T> from(int start, int end, @Nonnull Scheduler scheduler, @Nonnull T... ts) {
		return from(Interactive.toIterable(start, end, ts), scheduler);
	}
	/**
	 * Converts this observable into an iterable builder.
	 * @return the iterable builder
	 */
	public IterableBuilder<T> toIterable() {
		return IterableBuilder.from(o);
	}
	/**
	 * Returns an observable which provides a TimeInterval of Ts which
	 * records the elapsed time between successive elements.
	 * The time interval is evaluated using the System.nanoTime() differences
	 * as nanoseconds
	 * The first element contains the time elapsed since the registration occurred.
	 * @return the new observable
	 */
	@Nonnull 
	public ObservableBuilder<TimeInterval<T>> addTimeInterval() {
		return from(Reactive.addTimeInterval(o));
	}
	/**
	 * Wrap the values within a observable to a timestamped value having always
	 * the System.currentTimeMillis() value.
	 * @return the raw observables of Ts
	 */
	@Nonnull 
	public ObservableBuilder<Timestamped<T>> addTimestamped() {
		return from(Reactive.addTimestamped(o));
	}
	/**
	 * Apply an accumulator function over the observable source and submit the accumulated value to the returned observable.
	 * @param accumulator the accumulator function where the first parameter is the current accumulated value and the second is the now received value.
	 * @return the observable for the result of the accumulation
	 */
	@Nonnull 
	public ObservableBuilder<T> aggregate(
			@Nonnull final Func2<? super T, ? super T, ? extends T> accumulator) {
		return from(Reactive.aggregate(o, accumulator));
	}
	/**
	 * Computes an aggregated value of the source Ts by applying a sum function and applying the divide function when the source
	 * finishes, sending the result to the output.
	 * @param <U> the type of the intermediate sum value
	 * @param <V> the type of the final average value
	 * @param sum the function which sums the input Ts. The first received T will be acompanied by a null U.
	 * @param divide the function which perform the final division based on the number of elements
	 * @return the observable for the average value
	 */
	@Nonnull 
	public <U, V> ObservableBuilder<V> aggregate(
			@Nonnull final Func2<? super U, ? super T, ? extends U> sum, 
			@Nonnull final Func2<? super U, ? super Integer, ? extends V> divide) {
		return from(Reactive.aggregate(o, sum, divide));
	}
	/**
	 * Apply an accumulator function over the observable source and submit the accumulated value to the returned observable.
	 * @param <U> the ouput element type
	 * @param seed the initial value of the accumulator
	 * @param accumulator the accumulator function where the first parameter is the current accumulated value and the second is the now received value.
	 * @return the observable for the result of the accumulation
	 */
	@Nonnull 
	public <U> ObservableBuilder<U> aggregate(
			final U seed, 
			@Nonnull final Func2<? super U, ? super T, ? extends U> accumulator) {
		return from(Reactive.aggregate(o, seed, accumulator));
	}
	/**
	 * Signals a single true or false if all elements of the observable matches the predicate.
	 * It may return early with a result of false if the predicate simply does not match the current element.
	 * For a true result, it waits for all elements of the source observable.
	 * @param predicate the predicate to setisfy
	 * @return the observable resulting in a single result
	 */
	@Nonnull 
	public ObservableBuilder<Boolean> all(
			@Nonnull final Func1<? super T, Boolean> predicate) {
		return from(Reactive.all(o, predicate));
	}
	/**
	 * Channels the values of the first observable who fires first from the given set of observables.
	 * E.g., <code>O3 = Amb(O1, O2)</code> if O1 starts to submit events first, O3 will relay these events and events of O2 will be completely ignored
	 * @param others the iterable sequence of the other observables
	 * @return the observable channeling the first reacting values
	 */
	@Nonnull 
	public ObservableBuilder<T> amb(
			@Nonnull final Iterable<? extends Observable<? extends T>> others) {
		return from(Reactive.amb(Interactive.startWith(others, o)));
	}
	/**
	 * Channels the values from the first observable (this or other) who fires firts. 
	 * @param other the other observable
	 * @return the observable channeling the first reacting observable
	 */
	public ObservableBuilder<T> amb(Observable<? extends T> other) {
		return from(Reactive.amb(Interactive.toIterable(o, other)));
	}
	/**
	 * Signals a single true if the source observable contains any element.
	 * It might return early for a non-empty source but waits for the entire observable to return false. 
	 * @return the observable
	 */
	@Nonnull 
	public ObservableBuilder<Boolean> any() {
		return from(Reactive.any(o));
	}
	/**
	 * Signals a single TRUE if the source signals any next() and the value matches the predicate before it signals a finish().
	 * It signals a false otherwise. 
	 * @param predicate the predicate to test the values
	 * @return the observable.
	 */
	@Nonnull 
	public ObservableBuilder<Boolean> any(
			@Nonnull final Func1<? super T, Boolean> predicate) {
		return from(Reactive.any(o, predicate));
	}
	/**
	 * Computes and signals the average value of the BigDecimal source.
	 * The source may not send nulls.
	 * <p>Note that it uses forced cast of this sequence. If T != BigDecimal this
	 * method is guaranteed to throw ClassCastException.</p>
	 * @return the observable for the average value
	 */
	@Nonnull
	@SuppressWarnings("unchecked")
	public ObservableBuilder<BigDecimal> averageBigDecimal() {
		return from(Reactive.averageBigDecimal((Observable<BigDecimal>)o));
	}
	/**
	 * Computes and signals the average value of the BigInteger source.
	 * The source may not send nulls.
	 * <p>Note that it uses forced cast of this sequence. If T != BigInteger this
	 * method is guaranteed to throw ClassCastException.</p>
	 * @return the observable for the average value
	 */
	@Nonnull 
	public ObservableBuilder<BigDecimal> averageBigInteger() {
		return from(Reactive.averageBigInteger((Observable<BigInteger>)o));
	}
	/**
	 * Computes and signals the average value of the Double source.
	 * The source may not send nulls.
	 * @return the observable for the average value
	 */
	@Nonnull 
	public ObservableBuilder<Double> averageDouble() {
		return from(Reactive.averageDouble((Observable<Double>)o));
	}
	/**
	 * Computes and signals the average value of the Float source.
	 * The source may not send nulls.
	 * @return the observable for the average value
	 */
	@Nonnull 
	public ObservableBuilder<Float> averageFloat() {
		return from(Reactive.averageFloat((Observable<Float>)o));
	}
	/**
	 * Computes and signals the average value of the integer source.
	 * The source may not send nulls.
	 * The intermediate aggregation used double values.
	 * @return the observable for the average value
	 */
	@Nonnull 
	public ObservableBuilder<Double> averageInt() {
		return from(Reactive.averageInt((Observable<Integer>)o));

	}
	/**
	 * Computes and signals the average value of the Long source.
	 * The source may not send nulls.
	 * The intermediate aggregation used double values.
	 * @return the observable for the average value
	 */
	@Nonnull 
	public ObservableBuilder<Double> averageLong() {
		return from(Reactive.averageLong((Observable<Long>)o));
	}	
	/**
	 * Buffer the nodes as they become available and send them out in bufferSize chunks.
	 * The observers return a new and modifiable list of T on every next() call.
	 * @param bufferSize the target buffer size
	 * @return the observable of the list
	 */
	@Nonnull 
	public ObservableBuilder<List<T>> buffer(
			final int bufferSize) {
		return from(Reactive.buffer(o, bufferSize));
	}
	/**
	 * Buffer the Ts of the source until the buffer reaches its capacity or the current time unit runs out.
	 * Might result in empty list of Ts and might complete early when the source finishes before the time runs out.
	 * It uses the default scheduler pool.
	 * @param bufferSize the allowed buffer size
	 * @param time the time value to wait betveen buffer fills
	 * @param unit the time unit
	 * @return the observable of list of Ts
	 */
	@Nonnull 
	public ObservableBuilder<List<T>> buffer(
			final int bufferSize, 
			final long time, 
			@Nonnull final TimeUnit unit) {
		return from(Reactive.buffer(o, bufferSize, time, unit));
	}
	/**
	 * Buffer the Ts of the source until the buffer reaches its capacity or the current time unit runs out.
	 * Might result in empty list of Ts and might complete early when the source finishes before the time runs out.
	 * @param bufferSize the allowed buffer size
	 * @param time the time value to wait betveen buffer fills
	 * @param unit the time unit
	 * @param pool the pool where to schedule the buffer splits
	 * @return the observable of list of Ts
	 */
	@Nonnull 
	public ObservableBuilder<List<T>> buffer(
			final int bufferSize, 
			final long time, 
			@Nonnull final TimeUnit unit, 
			@Nonnull final Scheduler pool) {
		return from(Reactive.buffer(o, bufferSize, time, unit, pool));
	}
	/**
	 * Buffers the source observable Ts into a list of Ts periodically and submits them to the returned observable.
	 * Each next() invocation contains a new and modifiable list of Ts. The signaled List of Ts might be empty if
	 * no Ts appeared from the original source within the current timespan.
	 * The last T of the original source triggers an early submission to the output.
	 * The scheduling is done on the default Scheduler.
	 * @param time the time value to split the buffer contents.
	 * @param unit the time unit of the time
	 * @return the observable of list of Ts
	 */
	@Nonnull 
	public ObservableBuilder<List<T>> buffer(
			final long time, 
			@Nonnull final TimeUnit unit) {
		return from(Reactive.buffer(o, time, unit));
	}
	/**
	 * Buffers the source observable Ts into a list of Ts periodically and submits them to the returned observable.
	 * Each next() invocation contains a new and modifiable list of Ts. The signaled List of Ts might be empty if
	 * no Ts appeared from the original source within the current timespan.
	 * The last T of the original source triggers an early submission to the output.
	 * @param time the time value to split the buffer contents.
	 * @param unit the time unit of the time
	 * @param pool the scheduled execution pool to use
	 * @return the observable of list of Ts
	 */
	@Nonnull 
	public ObservableBuilder<List<T>> buffer(
			final long time, 
			@Nonnull final TimeUnit unit, 
			@Nonnull final Scheduler pool) {
		return from(Reactive.buffer(o, time, unit, pool));
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
	 * a value, use the {@link #combineLatestSent(Observable, Observable, Func2)} method.</p>
	 * @param <U> the right element type
	 * @param <V> the result element type
	 * @param right the right stream
	 * @param selector the function which combines values from both streams and returns a new value
	 * @return the new observable.
	 */
	public <U, V> ObservableBuilder<V> combineLatest(
			final Observable<? extends U> right,
			final Func2<? super T, ? super U, ? extends V> selector
	) {
		return from(Reactive.combineLatest(o, right, selector));
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
	 * @param <U> the right element type
	 * @param <V> the result element type
	 * @param right the right stream
	 * @param selector the function which combines values from both streams and returns a new value
	 * @return the new observable.
	 */
	public <U, V> ObservableBuilder<V> combineLatestSent(
			final Observable<? extends U> right,
			final Func2<? super T, ? super U, ? extends V> selector
	) {
		return from(Reactive.combineLatestSent(o, right, selector));
	}
	/**
	 * Concatenates the source observables in a way that when the first finish(), the
	 * second gets registered and continued, and so on.
	 * FIXME not sure how it should handle closability
	 * @param sources the source list of subsequent observables
	 * @return the concatenated observable
	 */
	@Nonnull 
	public ObservableBuilder<T> concat(
			@Nonnull final Iterable<? extends Observable<? extends T>> sources) {
		return from(Reactive.concat(Interactive.startWith(sources, o)));
	}
	/**
	 * Concatenate the the multiple sources of T one after another.
	 * <p><b>Exception semantics:</b> if the sources or any inner observer signals an
	 * error, the outer observable will signal that error and the sequence is terminated.</p>
	 * @param sources the observable sequence of the observable sequence of Ts.
	 * @return the new observable
	 */
	public ObservableBuilder<T> concatAll(
			final Observable<? extends Observable<T>> sources
	) {
		return from(Reactive.concat(Reactive.concat(Reactive.singleton(o), sources)));
	}
	/**
	 * Concatenate two observables in a way when the first finish() the second is registered
	 * and continued with.
	 * @param second the second observable
	 * @return the concatenated observable
	 */
	@Nonnull 
	public ObservableBuilder<T> concat(
			@Nonnull Observable<? extends T> second) {
		return from(Reactive.concat(o, second));
	}
	/**
	 * Signals a single TRUE if the source observable signals a value equals() with the source value.
	 * Both the source and the test value might be null. The signal goes after the first encounter of
	 * the given value.
	 * @param value the value to look for
	 * @return the observer for contains
	 */
	@Nonnull 
	public ObservableBuilder<Boolean> contains(
			final T value) {
		return from(Reactive.contains(o, value));
	}
	/**
	 * Counts the number of elements in the observable source.
	 * @return the count signal
	 */
	@Nonnull 
	public ObservableBuilder<Integer> count() {
		return from(Reactive.count(o));
	}
	/**
	 * Counts the number of elements in the observable source as a long.
	 * @return the count signal
	 */
	@Nonnull 
	public ObservableBuilder<Long> countLong() {
		return from(Reactive.countLong(o));
	}
	/**
	 * Constructs an observer which logs errors in case next(), finish() or error() is called 
	 * and the observer is not in running state anymore due an earlier finish() or error() call.
	 * @return the augmented observable
	 */
	@Nonnull 
	public ObservableBuilder<T> debugState() {
		return from(Reactive.debugState(o));
	}
	/**
	 * The returned observable invokes the <code>observableFactory</code> whenever an observer
	 * tries to subscribe to it.
	 * @param <T> the element type
	 * @param observableFactory the factory which is responsivle to create a source observable.
	 * @return the result observable
	 */
	@Nonnull 
	public static <T> ObservableBuilder<T> defer(
			@Nonnull final Func0<? extends Observable<? extends T>> observableFactory) {
		return from(Reactive.defer(observableFactory));
	}
	/**
	 * Delays the propagation of events of the source by the given amount. It uses the pool for the scheduled waits.
	 * The delay preserves the relative time difference between subsequent notifiactions.
	 * It uses the default scheduler pool when submitting the delayed values
	 * @param time the time value
	 * @param unit the time unit
	 * @return the delayed observable of Ts
	 */
	@Nonnull 
	public ObservableBuilder<T> delay(
			final long time, 
			@Nonnull final TimeUnit unit) {
		return from(Reactive.delay(o, time, unit));
	}
	/**
	 * Delays the propagation of events of the source by the given amount. It uses the pool for the scheduled waits.
	 * The delay preserves the relative time difference between subsequent notifiactions
	 * @param time the time value
	 * @param unit the time unit
	 * @param pool the pool to use for scheduling
	 * @return the delayed observable of Ts
	 */
	@Nonnull 
	public ObservableBuilder<T> delay(
			final long time, 
			@Nonnull final TimeUnit unit, 
			@Nonnull final Scheduler pool) {
		return from(Reactive.delay(o, time, unit, pool));
	}
	/**
	 * Returns an observable which converts all option messages
	 * back to regular next(), error() and finish() messages.
	 * The returned observable adheres to the <code>next* (error|finish)?</code> pattern,
	 * which ensures that no further options are relayed after an error or finish.
	 * @return the new observable
	 * @see #materialize(Observable)
	 */
	@Nonnull 
	public ObservableBuilder<T> dematerialize() {
		return from(Reactive.dematerialize((Observable<Option<T>>)o));
	}
	/**
	 * Returns an observable which fires next() events only when the subsequent values differ
	 * in terms of Object.equals().
	 * @return the observable
	 */
	@Nonnull 
	public ObservableBuilder<T> distinct() {
		return from(Reactive.distinct(o));
	}
	/**
	 * Returns Ts from the source observable if the subsequent keys extracted by <code>keyExtractor</code> are different.
	 * @param <U> the key type check for distinction
	 * @param keyExtractor the etractor for the keys
	 * @return the new filtered observable
	 */
	@Nonnull 
	public <U> ObservableBuilder<T> distinct(
			@Nonnull final Func1<T, U> keyExtractor) {
		return from(Reactive.distinct(o, keyExtractor));
	}
	/**
	 * Maintains a queue of Ts which is then drained by the pump. Uses the default pool.
	 * FIXME not sure what this method should do and how.
	 * @param pump the pump that drains the queue
	 * @return the new observable
	 */
	@Nonnull 
	public ObservableBuilder<Void> drain(
			@Nonnull final Func1<? super T, ? extends Observable<Void>> pump) {
		return from(Reactive.drain(o, pump));
	}
	/**
	 * Maintains a queue of Ts which is then drained by the pump.
	 * FIXME not sure what this method should do and how.
	 * @param pump the pump that drains the queue
	 * @param pool the pool for the drain
	 * @return the new observable
	 */
	@Nonnull 
	public ObservableBuilder<Void> drain(
			@Nonnull final Func1<? super T, ? extends Observable<Void>> pump, 
			@Nonnull final Scheduler pool) {
		return from(Reactive.drain(o, pump, pool));
	}
	/**
	 * Returns an empty observable, which fires only finish().
	 * @return Returns an empty observable which signals only finish() on the default observer pool.
	 */
	@Nonnull 
	public ObservableBuilder<T> empty() {
		return from(Reactive.<T>empty());
	}
	/**
	 * Returns an empty observable which signals only finish() on the given pool.
	 * @param pool the pool to invoke the the finish()
	 * @return the observable
	 */
	@Nonnull 
	public ObservableBuilder<T> empty(
			@Nonnull final Scheduler pool) {
		return from(Reactive.<T>empty(pool));
	}
	/**
	 * Invokes the given action when the source signals a finish() or error().
	 * @param action the action to invoke on finish() or error()
	 * @return the new observable
	 */
	@Nonnull 
	public ObservableBuilder<T> finish(
			@Nonnull final Action0 action) {
		return from(Reactive.finish(o, action));
	}
	/**
	 * Blocks until the first element of the observable becomes availabel and returns that element.
	 * Might block forever.
	 * Might throw a NoSuchElementException when the observable doesn't produce any more elements
	 * @return the first element
	 */
	public T first() {
		return Reactive.first(o);
	}
	/**
	 * Runs the observables in parallel and joins their last values whenever one fires.
	 * FIXME not sure what this method should do in case of error.
	 * @param sources the list of sources
	 * @return the observable 
	 */
	@Nonnull 
	public ObservableBuilder<List<T>> forkJoin(
			@Nonnull final Iterable<? extends Observable<? extends T>> sources) {
		return from(Reactive.forkJoin(Interactive.startWith(sources, o)));
	}
	/**
	 * Generates a stream of Us by using a value T stream using the default pool fo the generator loop.
	 * If T = int and U is double, this would be seen as for (int i = 0; i &lt; 10; i++) { yield return i / 2.0; }
	 * @param <T> the type of the generator values
	 * @param <U> the type of the observed values
	 * @param initial the initial generator value
	 * @param condition the condition that must hold to continue generating Ts
	 * @param next the function that computes the next value of T
	 * @param selector the selector which turns Ts into Us.
	 * @return the observable
	 */
	@Nonnull 
	public static <T, U> ObservableBuilder<U> generate(
			final T initial, 
			@Nonnull final Func1<? super T, Boolean> condition, 
			@Nonnull final Func1<? super T, ? extends T> next, 
			@Nonnull final Func1<? super T, ? extends U> selector) {
		return from(Reactive.generate(initial, condition, next, selector));
	}
	/**
	 * Generates a stream of Us by using a value T stream.
	 * If T = int and U is double, this would be seen as for (int i = 0; i &lt; 10; i++) { yield return i / 2.0; }
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
	public static <T, U> ObservableBuilder<U> generate(
			final T initial, 
			@Nonnull final Func1<? super T, Boolean> condition, 
			@Nonnull final Func1<? super T, ? extends T> next, 
			@Nonnull final Func1<? super T, ? extends U> selector, 
			@Nonnull final Scheduler pool) {
		return from(Reactive.generate(initial, condition, next, selector, pool));
	}
	/**
	 * Generates a stream of Us by using a value T stream.
	 * If T = int and U is double, this would be seen as for (int i = 0; i &lt; 10; i++) { sleep(time); yield return i / 2.0; }
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
	public static <T, U> ObservableBuilder<Timestamped<U>> generateTimed(
			final T initial, 
			@Nonnull final Func1<? super T, Boolean> condition, 
			@Nonnull final Func1<? super T, ? extends T> next, 
			@Nonnull final Func1<? super T, ? extends U> selector, 
			@Nonnull final Func1<? super T, Long> delay) {
		return from(Reactive.generateTimed(initial, condition, next, selector, delay));
	}
	/**
	 * Generates a stream of Us by using a value T stream.
	 * If T = int and U is double, this would be seen as for (int i = 0; i &lt; 10; i++) { sleep(time); yield return i / 2.0; }
	 * FIXME timeunit for the delay function!
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
	public <T, U> ObservableBuilder<Timestamped<U>> generateTimed(
			final T initial, 
			@Nonnull final Func1<? super T, Boolean> condition, 
			@Nonnull final Func1<? super T, ? extends T> next, 
			@Nonnull final Func1<? super T, ? extends U> selector, 
			@Nonnull final Func1<? super T, Long> delay, 
			@Nonnull final Scheduler pool) {
		return from(Reactive.generateTimed(initial, condition, next, selector, delay));
	}
	/**
	 * Group the specified source accoring to the keys provided by the extractor function.
	 * The resulting observable gets notified once a new group is encountered.
	 * Each previously encountered group by itself receives updates along the way.
	 * If the source finish(), all encountered group will finish().
	 * FIXME not sure how this should work.
	 * @param <Key> the key type of the group
	 * @param keyExtractor the key extractor which creates Keys from Ts
	 * @return the observable
	 */
	@Nonnull 
	public <Key> ObservableBuilder<GroupedObservable<Key, T>> groupBy(
			@Nonnull final Func1<? super T, ? extends Key> keyExtractor) {
		return from(Reactive.groupBy(o, keyExtractor));
	}
	/**
	 * Group the specified source accoring to the keys provided by the extractor function.
	 * The resulting observable gets notified once a new group is encountered.
	 * Each previously encountered group by itself receives updates along the way.
	 * If the source finish(), all encountered group will finish().
	 * FIXME not sure how this should work
	 * @param <U> the type of the output element
	 * @param <Key> the key type of the group
	 * @param keyExtractor the key extractor which creates Keys from Ts
	 * @param valueExtractor the extractor which makes Us from Ts
	 * @return the observable
	 */
	@Nonnull 
	public <U, Key> ObservableBuilder<GroupedObservable<Key, U>> groupBy(
			@Nonnull final Func1<? super T, ? extends Key> keyExtractor, 
			@Nonnull final Func1<? super T, ? extends U> valueExtractor) {
		return from(Reactive.groupBy(o, keyExtractor, valueExtractor));
	}
	/**
	 * Groups the source sequence of Ts until the specified duration for that group fires.
	 * <p>The key comparison is done by the <code>Object.equals()</code> semantics of the <code>HashMap</code>.</p>
	 * <p><b>Exception semantics:</b> if the source throws an exception, all active groups will receive
	 * the exception followed by the outer observer of the groups.</p>
	 * <p><b>Completion semantics:</b> if the source finishes, all active groups will receive a finish
	 * signal followed by the outer observer.</p>
	 * @param <K> the key type
	 * @param <D> the duration element type, ignored
	 * @param keySelector the key extractor
	 * @param durationSelector the observable for a particular group termination
	 * @return the new observable
	 */
	public <K, D> ObservableBuilder<GroupedObservable<K, T>> groupByUntil(
			final Func1<? super T, ? extends K> keySelector,
			final Func1<? super GroupedObservable<K, T>, ? extends Observable<D>> durationSelector
	) {
		return from(Reactive.groupByUntil(o, keySelector, durationSelector));
	}
	/**
	 * Groups the source sequence of Ts until the specified duration for that group fires.
	 * <p><b>Exception semantics:</b> if the source throws an exception, all active groups will receive
	 * the exception followed by the outer observer of the groups.</p>
	 * <p><b>Completion semantics:</b> if the source finishes, all active groups will receive a finish
	 * signal followed by the outer observer.</p>
	 * @param <K> the key type
	 * @param <D> the duration element type, ignored
	 * @param keySelector the key extractor
	 * @param durationSelector the observable for a particular group termination
	 * @param keyComparer the key comparer for the grouping
	 * @return the new observable
	 */
	public <K, D> ObservableBuilder<GroupedObservable<K, T>> groupByUntil(
			final Func1<? super T, ? extends K> keySelector,
			final Func1<? super GroupedObservable<K, T>, ? extends Observable<D>> durationSelector,
			final Func2<? super K, ? super K, Boolean> keyComparer
	) {
		return from(Reactive.groupByUntil(o, keySelector, durationSelector, keyComparer));
	}
	/**
	 * Groups the source sequence of Ts until the specified duration for that group fires.
	 * <p>The key comparison is done by the <code>Object.equals()</code> semantics of the <code>HashMap</code>.</p>
	 * <p><b>Exception semantics:</b> if the source throws an exception, all active groups will receive
	 * the exception followed by the outer observer of the groups.</p>
	 * <p><b>Completion semantics:</b> if the source finishes, all active groups will receive a finish
	 * signal followed by the outer observer.</p>
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param <D> the duration element type, ignored
	 * @param keySelector the key extractor
	 * @param valueSelector the value extractor
	 * @param durationSelector the observable for a particular group termination
	 * @return the new observable
	 */
	public <K, V, D> ObservableBuilder<GroupedObservable<K, V>> groupByUntil(
			final Func1<? super T, ? extends K> keySelector,
			final Func1<? super T, ? extends V> valueSelector,
			final Func1<? super GroupedObservable<K, V>, ? extends Observable<D>> durationSelector
	) {
		return from(Reactive.groupByUntil(o, keySelector, valueSelector, durationSelector));
	}
	/**
	 * Groups the source sequence of Ts until the specified duration for that group fires.
	 * <p><b>Exception semantics:</b> if the source throws an exception, all active groups will receive
	 * the exception followed by the outer observer of the groups.</p>
	 * <p><b>Completion semantics:</b> if the source finishes, all active groups will receive a finish
	 * signal followed by the outer observer.</p>
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param <D> the duration element type, ignored
	 * @param keySelector the key extractor
	 * @param valueSelector the value extractor
	 * @param durationSelector the observable for a particular group termination
	 * @param keyComparer the key comparer for the grouping
	 * @return the new observable
	 */
	public <K, V, D> ObservableBuilder<GroupedObservable<K, V>> groupByUntil(
			final Func1<? super T, ? extends K> keySelector,
			final Func1<? super T, ? extends V> valueSelector,
			final Func1<? super GroupedObservable<K, V>, ? extends Observable<D>> durationSelector,
			final Func2<? super K, ? super K, Boolean> keyComparer
	) {
		return from(Reactive.groupByUntil(o, keySelector, valueSelector, durationSelector, keyComparer));
	}
	/**
	 * Returns an observable which correlates two streams of values based on
	 * their time when they overlapped and groups the results.
	 * FIXME not sure how to implement it
	 * @param <Right> the element type of the right stream
	 * @param <LeftDuration> the overlapping duration indicator for the left stream (e.g., the event when it leaves)
	 * @param <RightDuration> the overlapping duration indicator for the right stream (e.g., the event when it leaves)
	 * @param <Result> the type of the grouping based on the coincidence.
	 * @param right the right source of elements
	 * @param leftDurationSelector the duration selector for a left element
	 * @param rightDurationSelector the duration selector for a right element
	 * @param resultSelector the selector which will produce the output value
	 * @return the new observable
	 * @see #join(Observable, Observable, Func1, Func1, Func2)
	 */
	public <Right, LeftDuration, RightDuration, Result> ObservableBuilder<Result> groupJoin(
			final Observable<? extends Right> right,
			final Func1<? super T, ? extends Observable<LeftDuration>> leftDurationSelector,
			final Func1<? super Right, ? extends Observable<RightDuration>> rightDurationSelector,
			final Func2<? super T, ? super Observable<? extends Right>, ? extends Result> resultSelector
	) {
		return from(Reactive.groupJoin(o, right, leftDurationSelector, rightDurationSelector, resultSelector));
	}
	/**
	 * Ignores the next() messages of the source and forwards only the error() and
	 * finish() messages.
	 * @return the new observable
	 */
	public ObservableBuilder<T> ignoreValues() {
		return from(Reactive.ignoreValues(o));
	}
	/**
	 * Invoke a specific action before relaying the Ts to the observable. The <code>action</code> might
	 * have some effect on each individual Ts passing through this filter.
	 * @param action the action to invoke on every T
	 * @return the new observable
	 */
	@Nonnull 
	public ObservableBuilder<T> invoke(
			@Nonnull final Action1<? super T> action) {
		return from(Reactive.invoke(o, action));
	}
	/**
	 * Invoke a specific observer before relaying the Ts, finish() and error() to the observable. The <code>action</code> might
	 * have some effect on each individual Ts passing through this filter.
	 * @param observer the observer to invoke before any registered observers are called
	 * @return the new observable
	 */
	@Nonnull 
	public ObservableBuilder<T> invoke(
			@Nonnull final Observer<? super T> observer) {
		return from(Reactive.invoke(o, observer));
	}
	/**
	 * Invoke the given callable on the default pool and observe its result via the returned observable.
	 * Any exception thrown by the callable is relayed via the error() message.
	 * @param <T> the return type
	 * @param call the callable
	 * @return the observable
	 */
	@Nonnull 
	public static <T> ObservableBuilder<T> from(
			@Nonnull final Callable<? extends T> call) {
		return from(Reactive.invokeAsync(call));
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
	public static <T> ObservableBuilder<T> from(
			@Nonnull final Callable<? extends T> call, 
			@Nonnull final Scheduler pool) {
		return from(Reactive.invokeAsync(call, pool));
	}
	/**
	 * Invoke the given callable on the given pool and observe its result via the returned observable.
	 * Any exception thrown by the callable is relayed via the error() message.
	 * @param <T> the return type
	 * @param run the runnable
	 * @return the observable
	 */
	@Nonnull 
	public static <T> ObservableBuilder<T> from(
			@Nonnull final Runnable run) {
		return from(Reactive.<T>invokeAsync(run));
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
	public static <T> ObservableBuilder<T> from(
			@Nonnull final Runnable run, 
			@Nonnull final Scheduler pool) {
		return from(Reactive.<T>invokeAsync(run, pool));
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
	public static <T> ObservableBuilder<T> from(
			@Nonnull final Runnable run,
			final T defaultValue) {
		return from(Reactive.invokeAsync(run, defaultValue));
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
	public static <T> ObservableBuilder<T> from(
			@Nonnull final Runnable run, 
			final T defaultValue,
			@Nonnull final Scheduler pool) {
		return from(Reactive.invokeAsync(run, defaultValue, pool));
	}
	/**
	 * Signals true if the source observable fires finish() without ever firing next().
	 * This means once the next() is fired, the resulting observer will return early.
	 * @return the observer
	 */
	@Nonnull
	public ObservableBuilder<Boolean> isEmpty() {
		return from(Reactive.isEmpty(o));
	}
	/**
	 * Returns an observable which correlates two streams of values based on
	 * their time when they overlapped.
	 * <p>The difference between this operator and the groupJoin operator
	 * is that in this case, the result selector takes the concrete left and
	 * right elements, whereas the groupJoin associates an observable of rights
	 * for each left.</p>
	 * FIXME not sure how to implement it
	 * @param <Right> the element type of the right stream
	 * @param <LeftDuration> the overlapping duration indicator for the left stream (e.g., the event when it leaves)
	 * @param <RightDuration> the overlapping duration indicator for the right stream (e.g., the event when it leaves)
	 * @param <Result> the type of the grouping based on the coincidence.
	 * @param right the right source of elements
	 * @param leftDurationSelector the duration selector for a left element
	 * @param rightDurationSelector the duration selector for a right element
	 * @param resultSelector the selector which will produce the output value
	 * @return the new observable
	 * @see #groupJoin(Observable, Observable, Func1, Func1, Func2)
	 */
	public <Right, LeftDuration, RightDuration, Result> ObservableBuilder<Result> join(
			final Observable<? extends Right> right,
			final Func1<? super T, ? extends Observable<LeftDuration>> leftDurationSelector,
			final Func1<? super Right, ? extends Observable<RightDuration>> rightDurationSelector,
			final Func2<? super T, ? super Right, ? extends Result> resultSelector
	) {
		return from(Reactive.join(o, right, leftDurationSelector, rightDurationSelector, resultSelector));
	}
	/**
	 * Returns the last element of the source observable or throws
	 * NoSuchElementException if the source is empty.
	 * @return the last element
	 */
	@Nonnull
	public T last() {
		return Reactive.last(o);
	}
	/**
	 * Returns an iterable which returns values on a momentary basis from the
	 * source. Useful when source produces values at different rate than the consumer takes it.
	 * The iterable.next() call might block until the first value becomes available or something else happens in the observable
	 * FIXME not sure where the observer should run
	 * @return the iterable
	 */
	@Nonnull
	public IterableBuilder<T> latest() {
		return IterableBuilder.from(Reactive.latest(o));
	}
	/**
	 * Uses the selector function on the given source observable to extract a single
	 * value and send this value to the registered observer.
	 * It is sometimes called the comonadic bind operator and compared to the ContinueWith
	 * semantics.
	 * The default scheduler is used to emit the output value
	 * FIXME not sure what it should do
	 * @param <U> the output type
	 * @param selector the selector that extracts an U from the series of Ts.
	 * @return the new observable.
	 */
	public <U> ObservableBuilder<U> manySelect0(
			final Func1<? super Observable<T>, ? extends U> selector) {
		return from(Reactive.manySelect0(o, selector));
	}
	/**
	 * For each value of the source observable, it creates a view starting from that value into the source
	 * and calls the given selector function asynchronously on the given scheduler.
	 * The result of that computation is then transmitted to the observer.
	 * <p>It is sometimes called the comonadic bind operator and compared to the ContinueWith
	 * semantics.</p>
	 * @param <U> the result element type
	 * @param selector the selector that extracts an U from the series of Ts.
	 * @param scheduler the scheduler where the extracted U will be emmitted from.
	 * @return the new observable.
	 */
	public <U> ObservableBuilder<U> manySelect(
			final Func1<? super Observable<T>, ? extends U> selector,
			final Scheduler scheduler) {
		return from(Reactive.manySelect(o, selector, scheduler));
	}
	/**
	 * For each of the source elements, creates a view of the source starting with the given
	 * element and calls the selector function. The function's return observable is then merged
	 * into a single observable sequence.<p>
	 * For example, a source sequence of (1, 2, 3) will create three function calls with (1, 2, 3), (2, 3) and (3) as a content.
	 * @param <U> the result element type
	 * @param selector the selector function
	 * @return the new observable
	 */
	public <U> ObservableBuilder<U> manySelect(
			final Func1<? super Observable<T>, ? extends Observable<U>> selector
	) {
		return from(Reactive.manySelect(o, selector));
	}
	/**
	 * Returns an observable which converts all messages to an <code>Option</code> value.
	 * The returned observable does not itself signal error or finish.
	 * Its dual is the <code>dematerialize</code> method.
	 * @return the new observable
	 * @see #dematerialize(Observable)
	 */
	@Nonnull 
	public ObservableBuilder<Option<T>> materialize() {
		return from(Reactive.materialize(o));
	};
	/**
	 * Returns the maximum value encountered in the source observable onse it finish().
	 * @param <U> the element type which must be comparable to itself
	 * @return the the maximum value
	 */
	@Nonnull 
	public <U extends Comparable<? super U>> ObservableBuilder<U> max() {
		return from(Reactive.max((Observable<U>)o));
	}
	/**
	 * Returns the maximum value encountered in the source observable onse it finish().
	 * @param comparator the comparator to decide the relation of values
	 * @return the the maximum value
	 * @see Functions#asComparator(Func2)
	 */
	@Nonnull 
	public ObservableBuilder<T> max(
			@Nonnull final Comparator<T> comparator) {
		return from(Reactive.max(o, comparator));
	}
	/**
	 * Returns an observable which provides with the list of <code>T</code>s which had their keys as maximums.
	 * The returned observer may finish() if the source sends finish() without any next().
	 * The generated list is modifiable.
	 * @param <Key> the key type, which must be comparable to itself
	 * @param keyExtractor the key extractor to produce <code>Key</code>s from <code>T</code>s.
	 * @return the observable for the maximum keyed Ts
	 */
	@Nonnull 
	public <Key extends Comparable<? super Key>> ObservableBuilder<List<T>> maxBy(
			@Nonnull final Func1<? super T, ? extends Key> keyExtractor) {
		return from(Reactive.maxBy(o, keyExtractor));
	}
	/**
	 * Returns an observable which provides with the list of <code>T</code>s which had their keys as maximums.
	 * The returned observer may finish() if the source sends finish() without any next().
	 * The generated list is modifiable.
	 * @param <Key> the key type
	 * @param keyExtractor the key extractor to produce <code>Key</code>s from <code>T</code>s.
	 * @param keyComparator the comparator for the keys
	 * @return the observable for the maximum keyed Ts
	 */
	@Nonnull 
	public <Key> ObservableBuilder<List<T>> maxBy(
			@Nonnull final Func1<? super T, ? extends Key> keyExtractor, 
			@Nonnull final Comparator<? super Key> keyComparator) {
		return from(Reactive.maxBy(o, keyExtractor, keyComparator));
	}
	/**
	 * Combines the notifications of all sources. The resulting stream of Ts might come from any of the sources.
	 * @param sources the list of sources
	 * @return the observable
	 */
	@Nonnull 
	public ObservableBuilder<T> merge(
			@Nonnull final Iterable<? extends Observable<? extends T>> sources) {
		return from(Reactive.merge(Interactive.startWith(sources, o)));
	};
	/**
	 * Merge the dynamic sequence of observables of T.
	 * <p><b>Exception semantics:</b> if the sources or any inner observer signals an
	 * error, the outer observable will signal that error and all active source observers are terminated.</p>
	 * @param sources the observable sequence of observable sequence of Ts
	 * @return the new observable
	 */
	public ObservableBuilder<T> mergeAll(
			final Observable<? extends Observable<T>> sources) {
		return from(Reactive.merge(Reactive.merge(Reactive.singleton(o), sources)));
	}
	/**
	 * Merge the events of two observable sequences.
	 * @param second the second observable
	 * @return the merged observable
	 */
	@Nonnull 
	public ObservableBuilder<T> merge(
			@Nonnull Observable<? extends T> second) {
		return from(Reactive.merge(o, second));
	}
	/**
	 * Returns the minimum value encountered in the source observable onse it finish().
	 * @param <U> the self comparable element type
	 * @return the the minimum value
	 */
	@Nonnull 
	public <U extends Comparable<? super U>> ObservableBuilder<U> min() {
		return from(Reactive.min((Observable<U>)o));
	}
	/**
	 * Returns the minimum value encountered in the source observable onse it finish().
	 * @param comparator the comparator to decide the relation of values
	 * @return the the minimum value
	 * @see Functions#asComparator(Func2)
	 */
	@Nonnull 
	public ObservableBuilder<T> min(
			@Nonnull final Comparator<? super T> comparator) {
		return from(Reactive.min(o, comparator));
	}
	/**
	 * Returns an observable which provides with the list of <code>T</code>s which had their keys as minimums.
	 * The returned observer may finish() if the source sends finish() without any next().
	 * The generated list is modifiable.
	 * @param <Key> the key type, which must be comparable to itself
	 * @param keyExtractor the key extractor to produce <code>Key</code>s from <code>T</code>s.
	 * @return the observable for the minimum keyed Ts
	 */
	@Nonnull 
	public <Key extends Comparable<? super Key>> ObservableBuilder<List<T>> minBy(
			@Nonnull final Func1<? super T, ? extends Key> keyExtractor) {
		return from(Reactive.minBy(o, keyExtractor));
	}
	/**
	 * Returns an observable which provides with the list of <code>T</code>s which had their keys as minimums.
	 * The returned observer may finish() if the source sends finish() without any next().
	 * The generated list is modifiable.
	 * @param <Key> the key type
	 * @param keyExtractor the key extractor to produce <code>Key</code>s from <code>T</code>s.
	 * @param keyComparator the comparator for the keys
	 * @return the observable for the minimum keyed Ts
	 * @see Functions#asComparator(Func2)
	 */
	@Nonnull 
	public <Key> ObservableBuilder<List<T>> minBy(
			@Nonnull final Func1<T, Key> keyExtractor, 
			@Nonnull final Comparator<Key> keyComparator) {
		return from(Reactive.minBy(o, keyExtractor, keyComparator));
	}
	/**
	 * Samples the latest T value coming from the source observable or the initial
	 * value when no messages arrived so far. If the producer and consumer run
	 * on different speeds, the consumer might receive the same value multiple times.
	 * The iterable sequence terminates if the source finishes or returns an error.
	 * <p>The returned iterator throws <code>UnsupportedOperationException</code> for its <code>remove()</code> method.</p>
	 * @param initialValue the initial value to return until the source actually produces something.
	 * @return the iterable
	 */
	public IterableBuilder<T> mostRecent(final T initialValue) {
		return IterableBuilder.from(Reactive.mostRecent(o, initialValue));
	}
	/**
	 * Returns an observable which remains connected to the <code>source</code>
	 * observable as long as there is at least one registration to this output observable.
	 * <p>The <code>observer</code> and <code>observable</code> parameters should denote
	 * the same object which implements both Observable and Observer interfaces.</p>
	 * @param <U> the result element type
	 * @param observer the observer that listens for Ts. Should be the same object as observable.
	 * @param observable the observable that will produce Us. Should be the same object as observable.
	 * @return the new observable
	 */
	public <U> ObservableBuilder<U> multicast(
			final Observer<? super T> observer, final Observable<? extends U> observable) {
		return from(Reactive.multicast(o, observer, observable));
	}
	/**
	 * Returns an observable which never fires.
	 * @return the observable
	 */
	@Nonnull 
	public ObservableBuilder<T> never() {
		return from(Reactive.<T>never());
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
	 * @return the iterable
	 */
	public IterableBuilder<T> next() {
		return IterableBuilder.from(Reactive.next(o));
	}
	/**
	 * Wrap the given observable object in a way that any of its observers receive callbacks on
	 * the given thread pool.
	 * @param pool the target observable
	 * @return the new observable
	 */
	@Nonnull 
	public ObservableBuilder<T> observeOn(
			@Nonnull final Scheduler pool) {
		return from(Reactive.observeOn(o, pool));
	}
	/**
	 * Returns an Observable which traverses the entire
	 * source Observable and creates an ordered list
	 * of elements. Once the source Observable completes,
	 * the elements are streamed to the output.
	 * @param <U> the source element type, must be self comparable
	 * @return the new iterable
	 */
	@Nonnull
	public <U extends Comparable<? super U>> ObservableBuilder<U> orderBy(
			) {
		return from(Reactive.orderBy((Observable<U>)o));
	}
	/**
	 * Returns an Observable which traverses the entire
	 * source Observable and creates an ordered list
	 * of elements. Once the source Observable completes,
	 * the elements are streamed to the output.
	 * @param comparator the value comparator
	 * @return the new iterable
	 */
	@Nonnull
	public ObservableBuilder<T> orderBy(
			@Nonnull final Comparator<? super T> comparator
			) {
		return from(Reactive.orderBy(o, comparator));
	}
	/**
	 * Returns an Observable which traverses the entire
	 * source Observable and creates an ordered list
	 * of elements. Once the source Observable completes,
	 * the elements are streamed to the output.
	 * @param <U> the key type for the ordering, must be self comparable
	 * @param keySelector the key selector for comparison
	 * @return the new iterable
	 */
	@Nonnull
	public <U extends Comparable<? super U>> ObservableBuilder<T> orderBy(
			@Nonnull final Func1<? super T, ? extends U> keySelector
			) {
		return from(Reactive.orderBy(o, keySelector));
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
	 * @param <U> the key type for the ordering
	 * @param keySelector the key selector for comparison
	 * @param keyComparator the key comparator function
	 * @return the new iterable
	 */
	@Nonnull
	public <U> ObservableBuilder<T> orderBy(
			@Nonnull final Func1<? super T, ? extends U> keySelector,
			@Nonnull final Comparator<? super U> keyComparator
			) {
		return from(Reactive.orderBy(o, keySelector, keyComparator));
	}
	/**
	 * Returns an observable which shares all registration to the source observable and
	 * each observer will only see the last notification.
	 * <p>Basically a replay with buffer size 1.</p>
	 * @return the observable
	 */
	public ObservableBuilder<T> prune() {
		return from(Reactive.prune(o));
	}
	/**
	 * Returns an observable which shares all registration to the source observable and
	 * each observer will only see the last notification.
	 * <p>Basically a replay with buffer size 1.</p>
	 * @param <U> the return element type
	 * @param selector the output stream selector
	 * @return the observable
	 */
	public <U> ObservableBuilder<U> prune(
			final Func1<? super Observable<? extends T>, ? extends Observable<U>> selector
	) {
		return from(Reactive.prune(o, selector));
	}
	/**
	 * Returns an observable which shares all registration to the source observable and
	 * each observer will only see the last notification.
	 * <p>Basically a replay with buffer size 1.</p>
	 * @param <U> the return element type
	 * @param selector the output stream selector
	 * @param scheduler the scheduler for replaying the single value
	 * @return the observable
	 */
	public <U> ObservableBuilder<U> prune(
			final Func1<? super Observable<? extends T>, ? extends Observable<U>> selector,
			final Scheduler scheduler
	) {
		return from(Reactive.prune(o, selector, scheduler));
	}
	/**
	 * Returns an observable which shares all registration to the source observable and
	 * each observer will only see the last notification.
	 * <p>Basically a replay with buffer size 1.</p>
	 * @param scheduler the scheduler for replaying the single value
	 * @return the observable
	 */
	public ObservableBuilder<T> prune(
			final Scheduler scheduler
	) {
		return from(Reactive.prune(o, scheduler));
	}
	/**
	 * Returns an observable which shares a single subscription to the underlying source.
	 * @return the new observable
	 */
	public ObservableBuilder<T> publish() {
		return from(Reactive.publish(o));
	}
	/**
	 * Returns an observable which shares a single subscription to the underlying source.
	 * @param <U> the result type
	 * @param selector the selector function for the return stream
	 * @return the new observable
	 */
	public <U> ObservableBuilder<U> publish(
			final Func1<? super Observable<? extends T>, ? extends Observable<U>> selector
	) {
		return from(Reactive.publish(o, selector));
	}
	/**
	 * Returns an observable which shares a single subscription to the underlying source.
	 * @param <U> the result type
	 * @param selector the selector function for the return stream
	 * @param scheduler the scheduler where the values will be replayed
	 * @return the new observable
	 */
	public <U> ObservableBuilder<U> publish(
			final Func1<? super Observable<? extends T>, ? extends Observable<U>> selector,
			final Scheduler scheduler
	) {
		return from(Reactive.publish(o, selector, scheduler));
	}
	/**
	 * Returns an observable which shares a single subscription to the underlying source.
	 * @param <U> the result type
	 * @param selector the selector function for the return stream
	 * @param initialValue the initial stream value
	 * @return the new observable
	 */
	public <U> ObservableBuilder<U> publish(
			final Func1<? super Observable<? extends T>, ? extends Observable<U>> selector,
			final U initialValue
	) {
		return from(Reactive.publish(o, selector, initialValue));
	}
	/**
	 * Returns an observable which shares a single subscription to the underlying source.
	 * @param <U> the result type
	 * @param selector the selector function for the return stream
	 * @param initialValue the initial stream value
	 * @param scheduler the scheduler where the values will be replayed
	 * @return the new observable
	 */
	public <U> ObservableBuilder<U> publish(
			final Func1<? super Observable<? extends T>, ? extends Observable<U>> selector,
			final U initialValue,
			final Scheduler scheduler
	) {
		return from(Reactive.publish(o, selector, initialValue, scheduler));
	}
	/**
	 * Returns an observable which shares a single subscription to the underlying source.
	 * @param scheduler the scheduler where the values will be replayed
	 * @return the new observable
	 */
	public ObservableBuilder<T> publish(
			final Scheduler scheduler
	) {
		return from(Reactive.publish(o, scheduler));
	}
	/**
	 * Returns an observable which shares a single subscription to the underlying source.
	 * @param initialValue the initial stream value
	 * @return the new observable
	 */
	public ObservableBuilder<T> publish(
			final T initialValue
	) {
		return from(Reactive.publish(o, initialValue));
	}
	/**
	 * Returns an observable which shares a single subscription to the underlying source.
	 * @param initialValue the initial stream value
	 * @param scheduler the scheduler where the values will be replayed
	 * @return the new observable
	 */
	public ObservableBuilder<T> publish(
			final T initialValue,
			final Scheduler scheduler
	) {
		return from(Reactive.publish(o, initialValue, scheduler));
	}
	/** 
	 * Creates an observable which generates numbers from start.
	 * @param start the start value.
	 * @param count the count
	 * @param step the stepping
	 * @return the observable
	 */
	@Nonnull 
	public static ObservableBuilder<BigDecimal> range(
			@Nonnull final BigDecimal start, 
			final int count, 
			@Nonnull final BigDecimal step) {
		return from(Reactive.range(start, count, step));
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
	public static ObservableBuilder<BigDecimal> range(
			@Nonnull final BigDecimal start, 
			final int count, 
			@Nonnull final BigDecimal step, 
			@Nonnull final Scheduler pool) {
		return from(Reactive.range(start, count, step, pool));
	}
	/** 
	 * Creates an observable which generates numbers from start.
	 * @param start the start value.
	 * @param count the count
	 * @return the observable
	 */
	@Nonnull 
	public static ObservableBuilder<BigInteger> range(
			@Nonnull final BigInteger start,
			@Nonnull final BigInteger count) {
		return from(Reactive.range(start, count));
	}
	/** 
	 * Creates an observable which generates BigInteger numbers from start.
	 * @param start the start value.
	 * @param count the count
	 * @param pool the execution thread pool.
	 * @return the observable
	 */
	@Nonnull 
	public static ObservableBuilder<BigInteger> range(
			@Nonnull final BigInteger start, 
			@Nonnull final BigInteger count, 
			final Scheduler pool) {
		return from(Reactive.range(start, count, pool));
	}
	/** 
	 * Creates an observable which generates numbers from start.
	 * @param start the start value.
	 * @param count the count
	 * @param step the stepping
	 * @return the observable
	 */
	@Nonnull 
	public static ObservableBuilder<Double> range(
			final double start, 
			final int count, 
			final double step) {
		return from(Reactive.range(start, count, step));
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
	public static ObservableBuilder<Double> range(
			final double start, 
			final int count, 
			final double step, 
			@Nonnull final Scheduler pool) {
		return from(Reactive.range(start, count, step, pool));
	}
	/** 
	 * Creates an observable which generates numbers from start.
	 * @param start the start value.
	 * @param count the count
	 * @param step the stepping
	 * @return the observable
	 */
	@Nonnull 
	public ObservableBuilder<Float> range(
			final float start, 
			final int count, 
			final float step) {
		return from(Reactive.range(start, count, step));
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
	public ObservableBuilder<Float> range(
			final float start, 
			final int count, 
			final float step, 
			@Nonnull final Scheduler pool) {
		return from(Reactive.range(start, count, step, pool));
	}
	/** 
	 * Creates an observable which generates numbers from start.
	 * @param start the start value.
	 * @param count the count
	 * @return the observable
	 */
	@Nonnull 
	public ObservableBuilder<Integer> range(
			final int start, 
			final int count) {
		return from(Reactive.range(start, count));
	}
	/** 
	 * Creates an observable which generates numbers from start.
	 * @param start the start value.
	 * @param count the count
	 * @param pool the execution thread pool.
	 * @return the observable
	 */
	public ObservableBuilder<Integer> range(
			final int start, 
			final int count, 
			@Nonnull final Scheduler pool) {
		return from(Reactive.range(start, count, pool));
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
	public ObservableBuilder<T> relayUntil(
			@Nonnull final Func0<Boolean> condition) {
		return from(Reactive.relayUntil(o, condition));
	}
	/**
	 * Relay the stream of Ts until condition turns into false.
	 * @param <T> the type of the values
	 * @param source the source of Ts
	 * @param condition the condition that must hold to relay Ts
	 * @return the new observable
	 */
	@Nonnull 
	public ObservableBuilder<T> relayWhile(
			@Nonnull final Func0<Boolean> condition) {
		return from(Reactive.relayWhile(o, condition));
	}
	/**
	 * Unwrap the values within a timeinterval observable to its normal value.
	 * @return the raw observables of Ts
	 */
	@Nonnull 
	public ObservableBuilder<T> removeTimeInterval() {
		return from(Reactive.removeTimeInterval((Observable<TimeInterval<T>>)o));
	}
	/**
	 * Unwrap the values within a timestamped observable to its normal value.
	 * @return the raw observables of Ts
	 */
	@Nonnull 
	public ObservableBuilder<T> removeTimestamped() {
		return from(Reactive.removeTimestamped((Observable<Timestamped<T>>)o));
	}
	/**
	 * Creates an observable which repeatedly calls the given function which generates the Ts indefinitely.
	 * The generator runs on the default pool. Note that observers must unregister to stop the infinite loop.
	 * @param <T> the type of elements to produce
	 * @param func the function which generates elements
	 * @return the observable
	 */
	@Nonnull 
	public static <T> ObservableBuilder<T> repeat(
			@Nonnull final Func0<? extends T> func) {
		return from(Reactive.repeat(func));
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
	public static <T> ObservableBuilder<T> repeat(
			@Nonnull final Func0<? extends T> func, 
			final int count) {
		return from(Reactive.repeat(func, count));
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
	public static <T> ObservableBuilder<T> repeat(
			@Nonnull final Func0<? extends T> func, 
			final int count, 
			@Nonnull final Scheduler pool) {
		return from(Reactive.repeat(func, count, pool));
	}
	/**
	 * Creates an observable which repeatedly calls the given function which generates the Ts indefinitely.
	 * The generator runs on the pool. Note that observers must unregister to stop the infinite loop.
	 * @param <T> the type of elements to produce
	 * @param func the function which generates elements
	 * @param pool the pool where the generator loop runs
	 * @return the observable
	 */
	public static <T> ObservableBuilder<T> repeat(
			@Nonnull final Func0<? extends T> func, 
			@Nonnull final Scheduler pool) {
		return from(Reactive.repeat(func, pool));
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
	public ObservableBuilder<T> repeat(
		int count) {
		return from(Reactive.repeat(o, count));
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
	public static <T> ObservableBuilder<T> repeat(final T value) {
		return from(Reactive.repeat(value));
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
	public ObservableBuilder<T> repeat(
			final T value, 
			final int count) {
		return from(Reactive.repeat(value, count));
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
	public ObservableBuilder<T> repeat(
			final T value, 
			final int count, 
			@Nonnull final Scheduler pool) {
		return from(Reactive.repeat(value, count, pool));
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
	public ObservableBuilder<T> repeat(
			final T value, 
			@Nonnull final Scheduler pool) {
		return from(Reactive.repeat(value, pool));
	}
	/**
	 * Creates an observable which shares the source observable and replays all source Ts
	 * to any of the registering observers.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @return the new observable
	 */
	public ObservableBuilder<T> replay() {
		return from(Reactive.replay(o));
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
	public <U> ObservableBuilder<U> replay(
			final Func1<? super Observable<? extends T>, ? extends Observable<U>> selector,
			final int bufferSize
	) {
		return from(Reactive.replay(o, selector, bufferSize));
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
	 * @return the new observer
	 */
	public <U> ObservableBuilder<U> replay(
			final Func1<? super Observable<? extends T>, ? extends Observable<U>> selector,
			final int bufferSize,
			final long timeSpan,
			final TimeUnit unit
	) {
		return from(Reactive.replay(o, selector, bufferSize, timeSpan, unit));
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
	 * @return the new observer
	 */
	public <U> ObservableBuilder<U> replay(
			final Func1<? super Observable<? extends T>, ? extends Observable<U>> selector,
			final int bufferSize,
			final long timeSpan,
			final TimeUnit unit,
			final Scheduler scheduler
	) {
		return from(Reactive.replay(o, selector, bufferSize, timeSpan, unit, scheduler));
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
	 * @return the new observer
	 */
	public <U> ObservableBuilder<U> replay(
			final Func1<? super Observable<? extends T>, ? extends Observable<U>> selector,
			final long timeSpan,
			final TimeUnit unit
	) {
		return from(Reactive.replay(o, selector, timeSpan, unit));
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
	 * @return the new observer
	 */
	public <U> ObservableBuilder<U> replay(
			final Func1<? super Observable<? extends T>, ? extends Observable<U>> selector,
			final long timeSpan,
			final TimeUnit unit,
			final Scheduler scheduler
	) {
		return from(Reactive.replay(o, selector, timeSpan, unit, scheduler));
	}
	/**
	 * Creates an observable which shares the source observable and replays the buffered source Ts
	 * to any of the registering observers.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param bufferSize the target buffer size
	 * @return the new observable
	 */
	public ObservableBuilder<T> replay(
			final int bufferSize
	) {
		return from(Reactive.replay(o, bufferSize));
	}
	/**
	 * Creates an observable which shares the source observable and replays the bufferSize source Ts
	 * to any of the registering observers. After the periodic timespan, the buffer is reset.
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @param bufferSize the buffer size
	 * @param timeSpan the window length
	 * @param unit the time unit
	 * @return the new observer
	 */
	public ObservableBuilder<T> replay(
			final int bufferSize,
			final long timeSpan,
			final TimeUnit unit
	) {
		return from(Reactive.replay(o, bufferSize, timeSpan, unit));
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
	 * @return the new observer
	 */
	public ObservableBuilder<T> replay(
			final int bufferSize,
			final long timeSpan,
			final TimeUnit unit,
			final Scheduler scheduler
	) {
		return from(Reactive.replay(o, bufferSize, timeSpan, unit, scheduler));
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
	public ObservableBuilder<T> replay(
			final int bufferSize,
			final Scheduler scheduler
	) {
		return from(Reactive.replay(o, bufferSize, scheduler));
	}
	/**
	 * Creates an observable which shares the source observable and replays all source Ts
	 * to any of the registering observers. After the periodic timespan, the buffer is reset.
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @param timeSpan the window length
	 * @param unit the time unit
	 * @return the new observer
	 */
	public ObservableBuilder<T> replay(
			final long timeSpan,
			final TimeUnit unit
	) {
		return from(Reactive.replay(o, timeSpan, unit));
	}
	/**
	 * Creates an observable which shares the source observable and replays all source Ts
	 * to any of the registering observers. After the periodic timespan, the buffer is reset.
	 * @param <T> the source element type
	 * @param source the source of Ts
	 * @param timeSpan the window length
	 * @param unit the time unit
	 * @param scheduler the target scheduler
	 * @return the new observer
	 */
	public ObservableBuilder<T> replay(
			final long timeSpan,
			final TimeUnit unit,
			final Scheduler scheduler
	) {
		return from(Reactive.replay(o, timeSpan, unit, scheduler));
	}
	/**
	 * Creates an observable which shares the source observable and replays all source Ts
	 * to any of the registering observers.
	 * @param <T> the element type
	 * @param source the source of Ts
	 * @param scheduler the scheduler from where the historical elements are emitted
	 * @return the new observable
	 */
	public ObservableBuilder<T> replay(
			final Scheduler scheduler
	) {
		return from(Reactive.replay(o, scheduler));
	}
	/**
	 * Creates an observable which shares the source observable returned by the selector and replays all source Ts
	 * to any of the registering observers.
	 * @param <U> the return element type
	 * @param selector the output stream selector
	 * @param bufferSize the target buffer size
	 * @param scheduler the scheduler from where the historical elements are emitted
	 * @return the new observable
	 */
	public <U> ObservableBuilder<U> replay(
			final Func1<? super Observable<? extends T>, ? extends Observable<U>> selector,
			final int bufferSize,
			final Scheduler scheduler
	) {
		return from(Reactive.replay(o, selector, bufferSize, scheduler));
	}
	/**
	 * Returns the observable sequence for the supplied source observable by
	 * invoking the selector function with it.
	 * @param <U> the output element type
	 * @param selector the selector which returns an observable of Us for the given <code>source</code>
	 * @return the new observable
	 */
	public <U> ObservableBuilder<U> replay(
		final Func1<? super Observable<T>, ? extends Observable<U>> selector
	) {
		return from(Reactive.replay(o, selector));
	}
	/**
	 * Returns an observable which listens to elements from a source until it signals an error()
	 * or finish() and continues with the next observable. The registration happens only when the
	 * previous observables finished in any way.
	 * FIXME not sure how to close previous registrations
	 * @param <T> the type of the elements
	 * @param sources the list of observables
	 * @return the observable
	 */
	@Nonnull 
	public ObservableBuilder<T> resumeAlways(
			@Nonnull final Iterable<? extends Observable<? extends T>> sources) {
		return from(Reactive.resumeAlways(Interactive.startWith(sources, o)));
	}
	/**
	 * It tries to submit the values of first observable, but when it throws an exeption,
	 * the next observable within source is used further on. Basically a failover between the Observables.
	 * If the current source finish() then the result observable calls finish().
	 * If the last of the sources calls error() the result observable calls error()
	 * FIXME not sure how to close previous registrations
	 * @param sources the available source observables.
	 * @return the failover observable
	 */
	@Nonnull 
	public ObservableBuilder<T> resumeOnError(
			@Nonnull final Iterable<? extends Observable<? extends T>> sources) {
		return from(Reactive.resumeOnError(Interactive.startWith(sources, o)));
	}
	/**
	 * Restarts the observation until the source observable terminates normally.
	 * @return the repeating observable
	 */
	@Nonnull 
	public ObservableBuilder<T> retry() {
		return from(Reactive.retry(o));
	}
	/**
	 * Restarts the observation until the source observable terminates normally or the <code>count</code> retry count was used up.
	 * FIXME if the retry count is zero and yet another error comes, what should happen? finish or this time submit the error?
	 * @param count the retry count
	 * @return the repeating observable
	 */
	@Nonnull 
	public ObservableBuilder<T> retry(
			final int count) {
		return from(Reactive.retry(o, count));
	}
	/**
	 * Blocks until the observable calls finish() or error(). Values are submitted to the given action.
	 * @param action the action to invoke for each value
	 * @throws InterruptedException if the current thread is interrupted while waiting on
	 * the observable.
	 */
	public void run(
			@Nonnull final Action1<? super T> action) throws InterruptedException {
		Reactive.run(o, action);
	}
	/**
	 * Blocks until the observable calls finish() or error(). Events are submitted to the given observer.
	 * @param observer the observer to invoke for each event
	 * @throws InterruptedException if the current thread is interrupted while waiting on
	 * the observable.
	 */
	public void run(
			@Nonnull final Observer<? super T> observer) throws InterruptedException {
		Reactive.run(o, observer);
	}
	/**
	 * Blocks until the observable calls finish() or error(). Values are ignored.
	 * @throws InterruptedException if the current thread is interrupted while waiting on
	 * the observable.
	 */
	public void run() throws InterruptedException {
		Reactive.run(o);
	}
	/**
	 * Periodically sample the given source observable, which means tracking the last value of
	 * the observable and periodically submitting it to the output observable.
	 * FIXME the error() and finish() are instantly propagated
	 * @param time the time value to wait
	 * @param unit the time unit
	 * @return the sampled observable
	 */
	@Nonnull 
	public ObservableBuilder<T> sample(
			final long time, 
			@Nonnull final TimeUnit unit) {
		return from(Reactive.sample(o, time, unit));
	}
	/**
	 * Periodically sample the given source observable, which means tracking the last value of
	 * the observable and periodically submitting it to the output observable.
	 * FIXME the error() and finish() are instantly propagated
	 * @param time the time value to wait
	 * @param unit the time unit
	 * @param pool the scheduler pool where the periodic submission should happen.
	 * @return the sampled observable
	 */
	@Nonnull 
	public ObservableBuilder<T> sample(
			final long time, 
			@Nonnull final TimeUnit unit, 
			@Nonnull final Scheduler pool) {
		return from(Reactive.sample(o, time, unit, pool));
	}
	/**
	 * Creates an observable which accumultates the given source and submits each intermediate results to its subscribers.
	 * Example:<br>
	 * <code>range(0, 5).accumulate((x, y) => x + y)</code> produces a sequence of [0, 1, 3, 6, 10];<br>
	 * basically the first event (0) is just relayed and then every pair of values are simply added together and relayed
	 * @param accumulator the accumulator which takest the current accumulation value and the current observed value 
	 * and returns a new accumulated value
	 * @return the observable
	 */
	@Nonnull 
	public ObservableBuilder<T> scan(
			@Nonnull final Func2<? super T, ? super T, ? extends T> accumulator) {
		return from(Reactive.scan(o, accumulator));
	}
	/**
	 * Creates an observable which accumultates the given source and submits each intermediate results to its subscribers.
	 * Example:<br>
	 * <code>range(0, 5).accumulate(1, (x, y) => x + y)</code> produces a sequence of [1, 2, 4, 7, 11];<br>
	 * basically the accumulation starts from zero and the first value (0) that comes in is simply added 
	 * @param seed the initial value of the accumulation
	 * @param accumulator the accumulator which takest the current accumulation value and the current observed value 
	 * and returns a new accumulated value
	 * @return the observable
	 */
	@Nonnull 
	public ObservableBuilder<T> scan(
			final T seed, 
			@Nonnull final Func2<? super T, ? super T, ? extends T> accumulator) {
		return from(Reactive.scan(o, seed, accumulator));
	}
	/**
	 * Creates an observable which accumultates the given source and submits each intermediate results to its subscribers.
	 * Example:<br>
	 * <code>range(1, 5).accumulate0(1, (x, y) => x + y)</code> produces a sequence of [1, 2, 4, 7, 11, 16];<br>
	 * basically, it submits the seed value (1) and computes the current aggregate with the current value(1). 
	 * @param seed the initial value of the accumulation
	 * @param accumulator the accumulator which takest the current accumulation value and the current observed value 
	 * and returns a new accumulated value
	 * @return the observable
	 */
	@Nonnull 
	public ObservableBuilder<T> scan0(
			final T seed, 
			@Nonnull final Func2<? super T, ? super T, ? extends T> accumulator) {
		return from(Reactive.scan0(o, seed, accumulator));
	}
	/**
	 * Use the mapper to transform the T source into an U source.
	 * @param <U> the type of the new observable
	 * @param mapper the mapper from Ts to Us
	 * @return the observable on Us
	 */
	@Nonnull 
	public <U> ObservableBuilder<U> select(
			@Nonnull final Func1<? super T, ? extends U> mapper) {
		return from(Reactive.select(o, mapper));
	}
	/**
	 * Transforms the elements of the source observable into Us by using a selector which receives an index indicating
	 * how many elements have been transformed this far.
	 * @param <U> the output element type
	 * @param selector the selector taking an index and the current T
	 * @return the transformed observable
	 */
	public <U> ObservableBuilder<U> select(
			@Nonnull final Func2<? super Integer, ? super T, ? extends U> selector) {
		return from(Reactive.select(o, selector));
	}
	/**
	 * Transform the given source of Ts into Us in a way that the 
	 * selector might return an observable ofUs for a single T.
	 * The observable is fully channelled to the output observable.
	 * FIXME not sure how to do it
	 * @param <U> the output element type
	 * @param selector the selector to return an Iterable of Us 
	 * @return the 
	 */
	@Nonnull 
	public <U> ObservableBuilder<U> selectMany(
			@Nonnull final Func1<? super T, ? extends Observable<? extends U>> selector) {
		return from(Reactive.selectMany(o, selector));
	}
	/**
	 * Creates an observable in which for each of Ts an observable of Vs are
	 * requested which in turn will be transformed by the resultSelector for each
	 * pair of T and V giving an U.
	 * FIXME concurrency related questions
	 * @param <U> the intermediate element type
	 * @param <V> the output element type
	 * @param collectionSelector the selector which returns an observable of intermediate Vs
	 * @param resultSelector the selector which gives an U for a T and V
	 * @return the observable of Us
	 */
	@Nonnull 
	public <U, V> ObservableBuilder<V> selectMany(
			@Nonnull final Func1<? super T, ? extends Observable<? extends U>> collectionSelector, 
			@Nonnull final Func2<? super T, ? super U, ? extends V> resultSelector) {
		return from(Reactive.selectMany(o, collectionSelector, resultSelector));
	}
	/**
	 * Creates an observable of Us in a way when a source T arrives, the observable of 
	 * Us is completely drained into the output. This is done again and again for
	 * each arriving Ts.
	 * @param <U> the output type
	 * @param provider the source of Us
	 * @return the observable for Us
	 */
	@Nonnull 
	public <U> ObservableBuilder<U> selectMany(
			@Nonnull Observable<? extends U> provider) {
		return from(Reactive.selectMany(o, provider));
	}
	/**
	 * Transform the given source of Ts into Us in a way that the selector might return zero to multiple elements of Us for a single T.
	 * The iterable is flattened and submitted to the output
	 * @param <U> the output element type
	 * @param selector the selector to return an Iterable of Us 
	 * @return the 
	 */
	@Nonnull 
	public <U> ObservableBuilder<U> selectManyIterable(
			@Nonnull final Func1<? super T, ? extends Iterable<? extends U>> selector) {
		return from(Reactive.selectManyIterable(o, selector));
	}
	/**
	 * Compares two sequences and returns whether they are produce the same
	 * elements in terms of the null-safe object equality.
	 * <p>The equality only stands if the two sequence produces the same
	 * amount of values and those values are pairwise equal. If one of the sequences
	 * terminates before the other, the equality test will return false.</p>
	 * @param second the second source of Ts
	 * @return the new observable
	 */
	public ObservableBuilder<Boolean> sequenceEqual(
			final Observable<? extends T> second) {
		return from(Reactive.sequenceEqual(o, second));
	}
	/**
	 * Compares two sequences and returns whether they are produce the same
	 * elements in terms of the comparer function.
	 * <p>The equality only stands if the two sequence produces the same
	 * amount of values and those values are pairwise equal. If one of the sequences
	 * terminates before the other, the equality test will return false.</p>
	 * @param second the second source of Ts
	 * @param comparer the equality comparison function
	 * @return the new observable
	 */
	public ObservableBuilder<Boolean> sequenceEqual(
			final Observable<? extends T> second,
			final Func2<? super T, ? super T, Boolean> comparer) {
		return from(Reactive.sequenceEqual(o, second, comparer));
	}
	/**
	 * Returns the single element of the given observable source.
	 * If the source is empty, a NoSuchElementException is thrown.
	 * If the source has more than one element, a TooManyElementsException is thrown.
	 * @return the single element
	 */
	@Nonnull 
	public T single() {
		return Reactive.single(o);
	}
	/**
	 * Skips the given amount of next() messages from source and relays
	 * the rest.
	 * @param count the number of messages to skip
	 * @return the new observable
	 */
	@Nonnull 
	public ObservableBuilder<T> skip(
			final int count) {
		return from(Reactive.skip(o, count));
	}
	/**
	 * Skips the last <code>count</code> elements from the source observable.
	 * @param count the number of elements to skip at the end
	 * @return the new observable
	 */
	@Nonnull 
	public ObservableBuilder<T> skipLast(
			final int count) {
		return from(Reactive.skipLast(o, count));
	}
	/**
	 * Skip the source elements until the signaller sends its first element.
	 * FIXME: If the signaller sends an error or only finish(), the relaying is never enabled?
	 * FIXME: once the singaller fires, it gets deregistered
	 * @param <U> the element type of the signaller, irrelevant
	 * @param signaller the source of Us
	 * @return the new observable
	 */
	@Nonnull 
	public <U> ObservableBuilder<T> skipUntil(
			@Nonnull final Observable<? extends U> signaller) {
		return from(Reactive.skipUntil(o, signaller));
	}
	/**
	 * Skips the Ts from source while the specified condition returns true.
	 * If the condition returns false, all subsequent Ts are relayed, 
	 * ignoring the condition further on. Errors and completion
	 * is relayed regardless of the condition.
	 * @param condition the condition that must turn false in order to start relaying
	 * @return the new observable
	 */
	@Nonnull 
	public ObservableBuilder<T> skipWhile(
			@Nonnull final Func1<? super T, Boolean> condition) {
		return from(Reactive.skipWhile(o, condition));
	}
	/**
	 * Invokes the action asynchronously on the given pool and
	 * relays its finish() or error() messages.
	 * @param action the action to invoke
	 * @return the observable
	 */
	@Nonnull 
	public static ObservableBuilder<Void> from(
			@Nonnull final Action0 action) {
		return from(Reactive.start(action));
	}
	
	/**
	 * Invokes the action asynchronously on the given pool and
	 * relays its finish() or error() messages.
	 * @param action the action to invoke
	 * @param pool the pool where the action should run
	 * @return the observable
	 */
	@Nonnull 
	public static ObservableBuilder<Void> from(
			@Nonnull final Action0 action, 
			@Nonnull final Scheduler pool) {
		return from(Reactive.start(action, pool));
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
	public static <T> ObservableBuilder<T> from(
			@Nonnull final Func0<? extends T> func) {
		return from(Reactive.start(func));
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
	public static <T> ObservableBuilder<T> from(
			@Nonnull final Func0<? extends T> func, 
			@Nonnull final Scheduler pool) {
		return from(Reactive.start(func, pool));
	}
	/**
	 * Start with the given iterable of values before relaying the Ts from the
	 * source. The iterable values are emmitted on the default pool.
	 * @param values the values to start with
	 * @return the new observable
	 */
	@Nonnull 
	public ObservableBuilder<T> startWith(
			@Nonnull Iterable<? extends T> values) {
		return from(Reactive.startWith(o, values));
	}
	/**
	 * Start with the given iterable of values before relaying the Ts from the
	 * source. The iterable values are emmitted on the default pool.
	 * @param values the values to start with
	 * @return the new observable
	 */
	@Nonnull 
	public ObservableBuilder<T> startWith(
			@Nonnull T... values) {
		return from(Reactive.startWith(o, Interactive.toIterable(values)));
	}
	/**
	 * Start with the given iterable of values before relaying the Ts from the
	 * source. The iterable values are emmitted on the given pool.
	 * @param values the values to start with
	 * @param pool the pool where the iterable values should be emitted
	 * @return the new observable
	 */
	@Nonnull 
	public ObservableBuilder<T> startWith(
			@Nonnull Iterable<? extends T> values, 
			@Nonnull Scheduler pool) {
		return from(Reactive.startWith(o, values, pool));
	}
	/**
	 * Start with the given iterable of values before relaying the Ts from the
	 * source. The iterable values are emmitted on the default pool.
	 * @param pool the pool where the iterable values should be emitted
	 * @param values the values to start with
	 * @return the new observable
	 */
	@Nonnull 
	public ObservableBuilder<T> startWith(
			@Nonnull Scheduler pool,
			@Nonnull T... values) {
		return from(Reactive.startWith(o, Interactive.toIterable(values), pool));
	}
	/**
	 * Start with the given iterable of values before relaying the Ts from the
	 * source. The value is emmitted on the default pool.
	 * @param value the single value to start with
	 * @return the new observable
	 */
	@Nonnull 
	public ObservableBuilder<T> startWith(
			T value) {
		return from(Reactive.startWith(o, value));
	}
	/**
	 * Start with the given iterable of values before relaying the Ts from the
	 * source. The value is emmitted on the given pool.
	 * @param value the value to start with
	 * @param pool the pool where the iterable values should be emitted
	 * @return the new observable
	 */
	@Nonnull 
	public ObservableBuilder<T> startWith(
			T value, 
			@Nonnull Scheduler pool) {
		return from(Reactive.startWith(o, value, pool));
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
	public ObservableBuilder<T> registerOn(
			@Nonnull final Scheduler pool) {
		return from(Reactive.registerOn(o, pool));
	}
	/**
	 * Computes and signals the sum of the values of the BigDecimal source.
	 * The source may not send nulls.
	 * @return the observable for the sum value
	 */
	@Nonnull 
	public ObservableBuilder<BigDecimal> sumBigDecimal() {
		return from(Reactive.sumBigDecimal((Observable<BigDecimal>)o));
	}
	/**
	 * Computes and signals the sum of the values of the BigInteger source.
	 * The source may not send nulls.
	 * @return the observable for the sum value
	 */
	@Nonnull 
	public ObservableBuilder<BigInteger> sumBigInteger() {
		return from(Reactive.sumBigInteger((Observable<BigInteger>)o));
	}
	/**
	 * Computes and signals the sum of the values of the Double source.
	 * The source may not send nulls.
	 * @return the observable for the sum value
	 */
	@Nonnull 
	public ObservableBuilder<Double> sumDouble() {
		return from(Reactive.sumDouble((Observable<Double>)o));
	}
	/**
	 * Computes and signals the sum of the values of the Float source.
	 * The source may not send nulls.
	 * @return the observable for the sum value
	 */
	@Nonnull 
	public ObservableBuilder<Float> sumFloat() {
		return from(Reactive.sumFloat((Observable<Float>)o));
	}
	
	/**
	 * Computes and signals the sum of the values of the Integer source.
	 * The source may not send nulls. An empty source produces an empty sum
	 * @return the observable for the sum value
	 */
	@Nonnull 
	public ObservableBuilder<Integer> sumInt() {
		return from(Reactive.sumInt((Observable<Integer>)o));
	}
	/**
	 * Computes and signals the sum of the values of the Integer source by using
	 * a double intermediate representation.
	 * The source may not send nulls. An empty source produces an empty sum
	 * @return the observable for the sum value
	 */
	@Nonnull 
	public ObservableBuilder<Double> sumIntAsDouble() {
		return from(Reactive.sumIntAsDouble((Observable<Integer>)o));
	}
	/**
	 * Computes and signals the sum of the values of the Long source.
	 * The source may not send nulls.
	 * @return the observable for the sum value
	 */
	@Nonnull 
	public ObservableBuilder<Long> sumLong() {
		return from(Reactive.sumLong((Observable<Long>)o));
	}
	/**
	 * Computes and signals the sum of the values of the Long sourceby using
	 * a double intermediate representation.
	 * The source may not send nulls.
	 * @return the observable for the sum value
	 */
	@Nonnull 
	public ObservableBuilder<Double> sumLongAsDouble() {
		return from(Reactive.sumLongAsDouble((Observable<Long>)o));
	}
	/**
	 * Returns an observer which relays Ts from the source observables in a way, when
	 * a new inner observable comes in, the previous one is deregistered and the new one is
	 * continued with. Basically, it is an unbounded ys.takeUntil(xs).takeUntil(zs)...
	 * @param sources the source of multiple observables of Ts.
	 * @return the new observable
	 */
	public ObservableBuilder<T> switchToNext(final Observable<? extends Observable<? extends T>> sources) {
		return from(Reactive.switchToNext(Reactive.startWith(sources, o)));
	}	
	/**
	 * Creates an observable which takes the specified number of
	 * Ts from the source, unregisters and completes.
	 * @param count the number of elements to relay
	 * @return the new observable
	 */
	@Nonnull 
	public ObservableBuilder<T> take(
			final int count) {
		return from(Reactive.take(o, count));
	}
	/**
	 * Returns an observable which returns the last <code>count</code>
	 * elements from the source observable.
	 * @param count the number elements to return
	 * @return the new observable
	 */
	public ObservableBuilder<T> takeLast(
			final int count) {
		return from(Reactive.takeLast(o, count));
	}
	/**
	 * Creates an observable which takes values from the source until
	 * the signaller produces a value. If the signaller never signals,
	 * all source elements are relayed.
	 * @param <U> the signaller element type, irrelevant
	 * @param signaller the source of Us
	 * @return the new observable
	 */
	@Nonnull 
	public <U> ObservableBuilder<T> takeUntil(
			@Nonnull final Observable<U> signaller) {
		return from(Reactive.takeUntil(o, signaller));
	}
	/**
	 * Creates an observable which takes values from source until
	 * the predicate returns false for the current element, then skips the remaining values.
	 * @param predicate the predicate
	 * @return the new observable
	 */
	@Nonnull 
	public ObservableBuilder<T> takeWhile(
			@Nonnull final Func1<? super T, Boolean> predicate) {
		return from(Reactive.takeWhile(o, predicate));
	}
	/**
	 * Creates and observable which fires the last value
	 * from source when the given timespan elapsed without a new
	 * value occurring from the source. It is basically how Content Assistant
	 * popup works after the user pauses in its typing. Uses the default scheduler.
	 * @param delay how much time should elapse since the last event to actually forward that event
	 * @param unit the delay time unit
	 * @return the observable
	 */
	@Nonnull 
	public ObservableBuilder<T> throttle(
			final long delay, 
			@Nonnull final TimeUnit unit) {
		return from(Reactive.throttle(o, delay, unit));
	}
	/**
	 * Creates and observable which fires the last value
	 * from source when the given timespan elapsed without a new
	 * value occurring from the source. It is basically how Content Assistant
	 * popup works after the user pauses in its typing.
	 * @param delay how much time should elapse since the last event to actually forward that event
	 * @param unit the delay time unit
	 * @param pool the pool where the delay-watcher should operate
	 * @return the observable
	 */
	@Nonnull 
	public ObservableBuilder<T> throttle(
			final long delay, 
			@Nonnull final TimeUnit unit, 
			@Nonnull final Scheduler pool) {
		return from(Reactive.throttle(o, delay, unit, pool));
	}
	/**
	 * Creates an observable which instantly sends the exception to
	 * its subscribers while running on the default pool.
	 * @param <T> the element type, irrelevant
	 * @param ex the exception to throw
	 * @return the new observable
	 */
	@Nonnull 
	public static <T> ObservableBuilder<T> throwException(
			@Nonnull final Throwable ex) {
		return from(Reactive.<T>throwException(ex));
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
	public static <T> ObservableBuilder<T> throwException(
			@Nonnull final Throwable ex, 
			@Nonnull final Scheduler pool) {
		return from(Reactive.<T>throwException(ex, pool));
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
	public static ObservableBuilder<Long> tick(
			final long start, 
			final long end, 
			final long delay, 
			@Nonnull final TimeUnit unit) {
		return from(Reactive.tick(start, end, delay, unit));
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
	public static ObservableBuilder<Long> tick(
			final long start, 
			final long end, 
			final long delay, 
			@Nonnull final TimeUnit unit, 
			@Nonnull final Scheduler pool) {
		return from(Reactive.tick(start, end, delay, unit, pool));
	}
	/**
	 * Returns an observable which produces an ordered sequence of numbers with the specified delay.
	 * It uses the default scheduler pool.
	 * @param delay the delay value
	 * @param unit the time unit of the delay
	 * @return the observer
	 */
	@Nonnull 
	public static ObservableBuilder<Long> tick(
			@Nonnull final long delay, 
			@Nonnull final TimeUnit unit) {
		return from(Reactive.tick(delay, unit));
	}
	/**
	 * Creates an observable which relays events if they arrive
	 * from the source observable within the specified amount of time
	 * or it singlals a java.util.concurrent.TimeoutException.
	 * @param time the maximum allowed timespan between events
	 * @param unit the time unit
	 * @return the observer.
	 */
	@Nonnull 
	public ObservableBuilder<T> timeout(
			final long time, 
			@Nonnull final TimeUnit unit) {
		return from(Reactive.timeout(o, time, unit));
	}
	/**
	 * Creates an observable which relays events if they arrive
	 * from the source observable within the specified amount of time
	 * or it switches to the <code>other</code> observable.
	 * FIXME not sure if the timeout should happen only when
	 * distance between elements get to large or just the first element
	 * does not arrive within the specified timespan.
	 * @param time the maximum allowed timespan between events
	 * @param unit the time unit
	 * @param other the other observable to continue with in case a timeout occurs
	 * @return the observer.
	 */
	@Nonnull 
	public ObservableBuilder<T> timeout(
			final long time, 
			@Nonnull final TimeUnit unit,
			@Nonnull final Observable<? extends T> other) {
		return from(Reactive.timeout(o, time, unit, other));
	}
	/**
	 * Creates an observable which relays events if they arrive
	 * from the source observable within the specified amount of time
	 * or it switches to the <code>other</code> observable.
	 * FIXME not sure if the timeout should happen only when
	 * distance between elements get to large or just the first element
	 * does not arrive within the specified timespan.
	 * @param time the maximum allowed timespan between events
	 * @param unit the time unit
	 * @param other the other observable to continue with in case a timeout occurs
	 * @param pool the scheduler pool for the timeout evaluation
	 * @return the observer.
	 */
	@Nonnull 
	public ObservableBuilder<T> timeout(
			final long time, 
			@Nonnull final TimeUnit unit,
			@Nonnull final Observable<? extends T> other,
			@Nonnull final Scheduler pool) {
		return from(Reactive.timeout(o, time, unit, other, pool));
	}
	/**
	 * Creates an observable which relays events if they arrive
	 * from the source observable within the specified amount of time
	 * or it singlals a java.util.concurrent.TimeoutException.
	 * FIXME not sure if the timeout should happen only when
	 * distance between elements get to large or just the first element
	 * does not arrive within the specified timespan.
	 * @param time the maximum allowed timespan between events
	 * @param unit the time unit
	 * @param pool the scheduler pool for the timeout evaluation
	 * @return the observer.
	 */
	@Nonnull 
	public ObservableBuilder<T> timeout(
			final long time, 
			@Nonnull final TimeUnit unit, 
			@Nonnull final Scheduler pool) {
		return from(Reactive.timeout(o, time, unit, pool));
	}
	/**
	 * Creates an array from the observable sequence elements by using the given
	 * array for the template to create a dynamicly typed array of Ts.
	 * <p><b>Exception semantics:</b> if the source throws an exception, that exception
	 * is forwarded (e.g., no partial array is created).</p>
	 * @param a the template array, noes not change its value
	 * @return the observable
	 */
	public ObservableBuilder<T[]> toArray(
			@Nonnull final T[] a) {
		return from(Reactive.toArray(o, a));
	}
	/**
	 * Creates an array from the observable sequence elements.
	 * <p><b>Exception semantics:</b> if the source throws an exception, that exception
	 * is forwarded (e.g., no partial array is created).</p>
	 * @param source the source of anything
	 * @return the object array
	 */
	@Nonnull
	public ObservableBuilder<Object[]> toArray() {
		return from(Reactive.toArray(o));
	}
	/**
	 * Collect the elements of the source observable into a single list.
	 * @return the new observable
	 */
	public ObservableBuilder<List<T>> toList() {
		return from(Reactive.toList(o));
	}
	/**
	 * Maps the given source of Ts by using the key extractor and
	 * returns a single Map of them. The keys are compared against each other
	 * by the <code>Object.equals()</code> semantics.
	 * <p><b>Exception semantics:</b> if the source throws an exception, that exception
	 * is forwarded (e.g., no partial map is created).</p>
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param keySelector the key selector
	 * @param valueSelector the value selector
	 * @return the new observable
	 */
	public <K, V> ObservableBuilder<Map<K, V>> toMap(
			final Func1<? super T, ? extends K> keySelector,
			final Func1<? super T, ? extends V> valueSelector
	) {
		return from(Reactive.toMap(o, keySelector, valueSelector));
	}
	/**
	 * Maps the given source of Ts by using the key and value extractor and
	 * returns a single Map of them.
	 * <p><b>Exception semantics:</b> if the source throws an exception, that exception
	 * is forwarded (e.g., no partial map is created).</p>
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param keySelector the key selector
	 * @param valueSelector the value selector
	 * @param keyComparer the comparison function for keys
	 * @return the new observable
	 */
	public <K, V> ObservableBuilder<Map<K, V>> toMap(
			final Func1<? super T, ? extends K> keySelector,
			final Func1<? super T, ? extends V> valueSelector,
			final Func2<? super K, ? super K, Boolean> keyComparer
	) {
		return from(Reactive.toMap(o, keySelector, valueSelector, keyComparer));
	}
	/**
	 * Maps the given source of Ts by using the key extractor and
	 * returns a single Map of them. 
	 * <p><b>Exception semantics:</b> if the source throws an exception, that exception
	 * is forwarded (e.g., no partial map is created).</p>
	 * @param <K> the key type
	 * @param keySelector the key selector
	 * @param keyComparer the key comparer function
	 * @return the new observable
	 */
	public <K> ObservableBuilder<Map<K, T>> toMap(
			final Func1<? super T, ? extends K> keySelector,
			final Func2<? super K, ? super K, Boolean> keyComparer
	) {
		return from(Reactive.toMap(o, keySelector, keyComparer));
	}
	/**
	 * Maps the given source of Ts by using the key extractor and
	 * returns a single Map of them. The keys are compared against each other
	 * by the <code>Object.equals()</code> semantics.
	 * <p><b>Exception semantics:</b> if the source throws an exception, that exception
	 * is forwarded (e.g., no partial map is created).</p>
	 * @param <K> the key type
	 * @param keySelector the key selector
	 * @return the new observable
	 */
	public <K> ObservableBuilder<Map<K, T>> toMap(
			final Observable<T> source,
			final Func1<? super T, ? extends K> keySelector
	) {
		return from(Reactive.toMap(o, keySelector));
	}
	/**
	 * Maps the given source of Ts by using the key  extractor and
	 * returns a single multi-map of them. The keys are compared against each other
	 * by the <code>Object.equals()</code> semantics.
	 * <p><b>Exception semantics:</b> if the source throws an exception, that exception
	 * is forwarded (e.g., no partial map is created).</p>
	 * @param <K> the key type
	 * @param keySelector the key selector
	 * @param collectionSupplier the function which retuns a collection to hold the Vs.
	 * @return the new observable
	 */
	public <K> ObservableBuilder<Map<K, Collection<T>>> toMultiMap(
			final Func1<? super T, ? extends K> keySelector,
			final Func0<? extends Collection<T>> collectionSupplier
	) {
		return from(Reactive.toMultiMap(o, keySelector, collectionSupplier));
	}
	/**
	 * Maps the given source of Ts by using the key extractor and
	 * returns a single multi-map of them.
	 * <p><b>Exception semantics:</b> if the source throws an exception, that exception
	 * is forwarded (e.g., no partial map is created).</p>
	 * @param <K> the key type
	 * @param keySelector the key selector
	 * @param collectionSupplier the function which retuns a collection to hold the Vs.
	 * @param keyComparer the comparison function for keys
	 * @return the new observable
	 */
	public <K> ObservableBuilder<Map<K, Collection<T>>> toMultiMap(
			final Func1<? super T, ? extends K> keySelector,
			final Func0<? extends Collection<T>> collectionSupplier,
			final Func2<? super K, ? super K, Boolean> keyComparer
	) {
		return from(Reactive.toMultiMap(o, keySelector, collectionSupplier, keyComparer));
	}
	/**
	 * Maps the given source of Ts by using the key and value extractor and
	 * returns a single multi-map of them. The keys are compared against each other
	 * by the <code>Object.equals()</code> semantics.
	 * <p><b>Exception semantics:</b> if the source throws an exception, that exception
	 * is forwarded (e.g., no partial map is created).</p>
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param keySelector the key selector
	 * @param collectionSupplier the function which retuns a collection to hold the Vs.
	 * @param valueSelector the value selector
	 * @return the new observable
	 * @see Functions#listSupplier()
	 * @see Functions#setSupplier()
	 */
	public <K, V> ObservableBuilder<Map<K, Collection<V>>> toMultiMap(
			final Func1<? super T, ? extends K> keySelector,
			final Func0<? extends Collection<V>> collectionSupplier,
			final Func1<? super T, ? extends V> valueSelector
	) {
		return from(Reactive.toMultiMap(o, keySelector, collectionSupplier, valueSelector));
	}
	/**
	 * Maps the given source of Ts by using the key and value extractor and
	 * returns a single multi-map of them.
	 * <p><b>Exception semantics:</b> if the source throws an exception, that exception
	 * is forwarded (e.g., no partial map is created).</p>
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param keySelector the key selector
	 * @param collectionSupplier the function which retuns a collection to hold the Vs.
	 * @param valueSelector the value selector
	 * @param keyComparer the comparison function for keys
	 * @return the new observable
	 */
	public <K, V> ObservableBuilder<Map<K, Collection<V>>> toMultiMap(
			final Func1<? super T, ? extends K> keySelector,
			final Func0<? extends Collection<V>> collectionSupplier,
			final Func1<? super T, ? extends V> valueSelector,
			final Func2<? super K, ? super K, Boolean> keyComparer
	) {
		return from(Reactive.toMultiMap(o, keySelector, collectionSupplier, valueSelector, keyComparer));
	}
	/**
	 * Filters objects from source which are assignment compatible with T.
	 * Note that due java erasure complex generic types can't be filtered this way in runtime (e.g., List&lt;String>.class is just List.class).
	 * FIXME is this what cast stands for?
	 * @param token the token to test agains the elements
	 * @return the observable containing Ts
	 */
	@Nonnull 
	public ObservableBuilder<T> typedAs(
			@Nonnull final Class<T> token) {
		return from(Reactive.typedAs(o, token));
	}
	/**
	 * Creates a filtered observable where only Ts are relayed which satisfy the clause.
	 * The clause receives the index and the current element to test.
	 * The clauseFactory is used for each individual registering observer.
	 * This can be used to create memorizing filter functions such as distinct.
	 * @param clauseFactory the filter clause, the first parameter receives the current index, the second receives the current element
	 * @return the new observable
	 */
	@Nonnull 
	public ObservableBuilder<T> where(
			@Nonnull final Func0<Func2<Integer, ? super T, Boolean>> clauseFactory) {
		return from(Reactive.where(o, clauseFactory));
	}
	/**
	 * Creates a filtered observable where only Ts are relayed which satisfy the clause.
	 * @param clause the filter clause
	 * @return the new observable
	 */
	@Nonnull 
	public ObservableBuilder<T> where(
			@Nonnull final Observable<? extends T> source, 
			@Nonnull final Func1<? super T, Boolean> clause) {
		return from(Reactive.where(o, clause));
	}
	/**
	 * Creates a filtered observable where only Ts are relayed which satisfy the clause.
	 * The clause receives the index and the current element to test.
	 * @param clause the filter clause, the first parameter receives the current index, the second receives the current element
	 * @return the new observable
	 */
	@Nonnull 
	public ObservableBuilder<T> where(
			@Nonnull final Func2<Integer, ? super T, Boolean> clause) {
		return from(Reactive.where(o, clause));
	}
	/**
	 * Splits the source stream into separate observables once
	 * the windowClosing fires an event.
	 * @param <U> the closing event type, irrelevant
	 * @param windowClosing the source of the window splitting events
	 * @return the observable on sequences of observables of Ts
	 */
	@Nonnull 
	public <U> ObservableBuilder<Observable<T>> window(
			@Nonnull final Func0<? extends Observable<U>> windowClosing) {
		return from(Reactive.window(o, windowClosing));
	}
	/**
	 * Splits the source stream into separate observables on
	 * each windowClosing event.
	 * FIXME not sure how to implement
	 * @param <U> the closing event type, irrelevant
	 * @param windowClosing the source of the window splitting events
	 * @param pool the pool where the first group is signalled from directly after
	 * the registration
	 * @return the observable on sequences of observables of Ts
	 */
	@Nonnull 
	public <U> ObservableBuilder<Observable<T>> window(
			@Nonnull final Func0<? extends Observable<U>> windowClosing, 
			@Nonnull final Scheduler pool) {
		return from(Reactive.window(o, windowClosing, pool));
	}
	/**
	 * Project the source elements into observable windows of size <code>count</code>
	 * and skip some initial values.
	 * @param count the count of elements
	 * @return the new observable
	 */
	public ObservableBuilder<Observable<T>> window(
			int count
	) {
		return from(Reactive.window(o, count));
	}
	/**
	 * Project the source elements into observable windows of size <code>count</code>
	 * and skip some initial values.
	 * @param count the count of elements
	 * @param skip the elements to skip
	 * @return the new observable
	 */
	public ObservableBuilder<Observable<T>> window(
			int count,
			int skip
	) {
		return from(Reactive.window(o, count, skip));
	}
	/**
	 * Project the source elements into observable windows of size <code>count</code>
	 * and skip some initial values.
	 * FIXME implement
	 * @param count the count of elements
	 * @param skip the elements to skip
	 * @param scheduler the scheduler 
	 * @return the new observable
	 */
	public ObservableBuilder<Observable<T>> window(
			final int count,
			final int skip,
			final Scheduler scheduler
	) {
		return from(Reactive.window(o, count, skip, scheduler));
	}
	/**
	 * Projects each value of T into an observable which are closed by
	 * either the <code>count</code> limit or the ellapsed timespan.
	 * @param count the maximum count of the elements in each window
	 * @param timeSpan the maximum time for each window
	 * @param unit the time unit
	 * @return the new observable
	 */
	public ObservableBuilder<Observable<T>> window(
		final int count,
		final long timeSpan,
		final TimeUnit unit
	) {
		return from(Reactive.window(o, count, timeSpan, unit));
	}
	/**
	 * Projects each value of T into an observable which are closed by
	 * either the <code>count</code> limit or the ellapsed timespan.
	 * @param count the maximum count of the elements in each window
	 * @param timeSpan the maximum time for each window
	 * @param unit the time unit
	 * @param scheduler the scheduler
	 * @return the new observable
	 */
	public ObservableBuilder<Observable<T>> window(
		final int count,
		final long timeSpan,
		final TimeUnit unit,
		final Scheduler scheduler
	) {
		return from(Reactive.window(o, count, timeSpan, unit, scheduler));
	}
	/**
	 * Project the source elements into observable windows of size <code>count</code>
	 * and skip some initial values.
	 * @param count the count of elements
	 * @param scheduler the scheduler 
	 * @return the new observable
	 */
	public ObservableBuilder<Observable<T>> window(
			int count,
			Scheduler scheduler
	) {
		return from(Reactive.window(o, count, scheduler));
	}
	/**
	 * Project each of the source Ts into observable sequences separated by
	 * the timespan and initial timeskip values.
	 * @param timeSpan the timespan between window openings
	 * @param timeSkip the initial delay to open the first window
	 * @param unit the time unit
	 * @return the observable
	 */
	public ObservableBuilder<Observable<T>> window(
		final long timeSpan,
		final long timeSkip,
		final TimeUnit unit
	) {
		return from(Reactive.window(o, timeSpan, timeSkip, unit));
	}
	/**
	 * Project each of the source Ts into observable sequences separated by
	 * the timespan and initial timeskip values.
	 * FIXME implement
	 * @param timeSpan the timespan between window openings
	 * @param timeSkip the initial delay to open the first window
	 * @param unit the time unit
	 * @param scheduler the scheduler
	 * @return the observable
	 */
	public ObservableBuilder<Observable<T>> window(
		final long timeSpan,
		final long timeSkip,
		final TimeUnit unit,
		final Scheduler scheduler
	) {
		return from(Reactive.window(o, timeSpan, timeSkip, unit, scheduler));
	}
	/**
	 * Project each of the source Ts into observable sequences separated by
	 * the timespan and initial timeskip values.
	 * @param timeSpan the timespan between window openings
	 * @param unit the time unit
	 * @return the observable
	 */
	public ObservableBuilder<Observable<T>> window(
		final long timeSpan,
		final TimeUnit unit
	) {
		return from(Reactive.window(o, timeSpan, unit));
	}
	/**
	 * Project each of the source Ts into observable sequences separated by
	 * the timespan and initial timeskip values.
	 * @param timeSpan the timespan between window openings
	 * @param unit the time unit
	 * @param scheduler the scheduler
	 * @return the observable
	 */
	public ObservableBuilder<Observable<T>> window(
		final long timeSpan,
		final TimeUnit unit,
		final Scheduler scheduler
	) {
		return from(Reactive.window(o, timeSpan, unit, scheduler));
	}
	/**
	 * Splits the source stream into separate observables
	 * by starting at windowOpening events and closing at windowClosing events.
	 * FIXME not sure how to implement
	 * @param <U> the opening event type, irrelevant
	 * @param <V> the closing event type, irrelevant
	 * @param windowOpening te source of the window opening events
	 * @param windowClosing the source of the window splitting events
	 * @return the observable on sequences of observables of Ts
	 */
	@Nonnull 
	public <U, V> ObservableBuilder<Observable<T>> window(
			@Nonnull final Observable<? extends U> windowOpening, 
			@Nonnull final Func1<? super U, ? extends Observable<V>> windowClosing) {
		return from(Reactive.window(o, windowOpening, windowClosing));
	}
	/**
	 * Creates an observable which waits for events from left
	 * and combines it with the next available value from the right iterable,
	 * applies the selector function and emits the resulting T.
	 * The error() and finish() signals are relayed to the output.
	 * The result is finished if the right iterator runs out of 
	 * values before the left iterator. 
	 * @param <U> the value type streamed on the left observable
	 * @param <V> the value type streamed on the right iterable
	 * @param right the right iterable of Vs
	 * @param selector the selector taking the left Us and right Vs.
	 * @return the resulting observable 
	 */
	@Nonnull 
	public <U, V> ObservableBuilder<V> zip(
			@Nonnull final Iterable<? extends U> right, 
			@Nonnull final Func2<? super T, ? super U, ? extends V> selector) {
		return from(Reactive.zip(o, right, selector));
	}
	/**
	 * Creates an observable which waits for events from left
	 * and combines it with the next available value from the right observable,
	 * applies the selector function and emits the resulting T.
	 * Basically it emmits a T when both an U and V is available.
	 * The output stream throws error or terminates if any of the streams 
	 * throws or terminates.
	 * FIXME not sure how to implement this, and how to close and signal
	 * @param <U> the value type streamed on the right iterable
	 * @param <V> the result type
	 * @param right the right iterable of Vs
	 * @param selector the selector taking the left Us and right Vs.
	 * @return the resulting observable 
	 */
	@Nonnull 
	public <U, V> ObservableBuilder<V> zip(
			@Nonnull final Observable<? extends U> right, 
			@Nonnull final Func2<T, U, V> selector) {
		return from(Reactive.zip(o, right, selector));
	}
	/**
	 * Combine the incoming Ts of the various observables into a single list of Ts like
	 * using Reactive.zip() on more than two sources.
	 * @param srcs the iterable of observable sources.
	 * @return the new observable
	 */
	public ObservableBuilder<List<T>> combineFirst(
			final List<? extends Observable<? extends T>> srcs) {
		List<Observable<? extends T>> list = new ArrayList<Observable<? extends T>>(srcs.size() + 1);
		list.add(o);
		list.addAll(srcs);
		return from(Reactive.combine(list));
	}
	/**
	 * Combine the incoming Ts of the various observables into a single list of Ts like
	 * using Reactive.zip() on more than two sources.
	 * @param srcs the iterable of observable sources.
	 * @return the new observable
	 */
	public ObservableBuilder<List<T>> combineLast(
			final List<? extends Observable<? extends T>> srcs) {
		List<Observable<? extends T>> list = new ArrayList<Observable<? extends T>>(srcs.size() + 1);
		list.addAll(srcs);
		list.add(o);
		return from(Reactive.combine(list));
	}
	/**
	 * Combine a stream of Ts with a constant T whenever the src fires.
	 * The observed list contains the values of src as the first value, constant as the second.
	 * @param constant the constant T to combine with
	 * @return the new observer
	 */
	public ObservableBuilder<List<T>> combineFirst(
			final T constant) {
		return from(Reactive.combine(o, constant));
	}
	/**
	 * Combine a constant T with a stream of Ts whenever the src fires.
	 * The observed sequence contains the constant as first, the src value as second.
	 * @param <T> the element type
	 * @param constant the constant T to combine with
	 * @param src the source of Ts
	 * @return the new observer
	 */
	public ObservableBuilder<List<T>> combineLast(
		final T constant) {
		return from(Reactive.combine(constant, o));
	}
}
