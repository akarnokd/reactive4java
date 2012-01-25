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
import hu.akarnokd.reactive4java.base.Pair;
import hu.akarnokd.reactive4java.base.Scheduler;
import hu.akarnokd.reactive4java.interactive.GroupedIterable;
import hu.akarnokd.reactive4java.interactive.Interactive;
import hu.akarnokd.reactive4java.reactive.Observable;
import hu.akarnokd.reactive4java.reactive.Reactive;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * An iterable builder which offers methods to chain the
 * sequence of some Interactive operations.
 * <p>This builder is the dual of the 
 * {@link hu.akarnokd.reactive4java.query.ObservableBuilder} class.</p>
 * @author akarnokd, Jan 25, 2012
 * @param <T> the element type
 * @since 0.96
 */
public final class IterableBuilder<T> implements Iterable<T> {
	/**
	 * Defers the source iterable creation to registration time and
	 * calls the given <code>func</code> for the actual source.
	 * @param <T> the element type
	 * @param func the function that returns an iterable.
	 * @return the new iterable
	 */
	public static <T> IterableBuilder<T> defer(
			@Nonnull final Func0<? extends Iterable<T>> func) {
		return from(Interactive.defer(func));
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
	public static <T> IterableBuilder<T> dematerialize(@Nonnull Iterable<? extends Option<? extends T>> source) {
		return from(Interactive.dematerialize(source));
	}
	/**
	 * Creates a new iterable builder instance by wrapping the given
	 * array. Changes to the array will be visible through
	 * the iterator.
	 * @param <T> the element type
	 * @param from the source index inclusive
	 * @param to the destination index exclusive
	 * @param ts the array of ts
	 * @return the created iterable builder
	 */
	public static <T> IterableBuilder<T> from(int from, int to, @Nonnull final T... ts) {
		return from(Interactive.toIterable(from, to, ts));
	}
	/**
	 * Creates a new iterable builder instance by wrapping the given
	 * source sequence.
	 * @param <T> the element type
	 * @param source the source sequence
	 * @return the created iterable builder
	 */
	public static <T> IterableBuilder<T> from(@Nonnull final Iterable<T> source) {
		return new IterableBuilder<T>(source);
	}
	/**
	 * Creates a new iterable builder by wrapping the given observable.
	 * <p>The resulting iterable does not support the {@code remove()} method.</p>
	 * @param <T> the element type
	 * @param source the source observable
	 * @return the created iterable builder
	 */
	public static <T> IterableBuilder<T> from(@Nonnull Observable<T> source) {
		return from(Reactive.toIterable(source));
	}
	/**
	 * Creates a new iterable builder instance by wrapping the given
	 * array. Changes to the array will be visible through
	 * the iterator.
	 * @param <T> the element type
	 * @param ts the array of ts
	 * @return the created iterable builder
	 */
	public static <T> IterableBuilder<T> from(@Nonnull final T... ts) {
		return from(Interactive.toIterable(ts));
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
	public static <T> IterableBuilder<T> generate(T seed, @Nonnull Func1<? super T, Boolean> predicate, @Nonnull Func1<? super T, ? extends T> next) {
		return from(Interactive.generate(seed, predicate, next));
	}
	/**
	 * Creates an integer iteratable builder which returns numbers from the start position in the count size.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param start the starting value.
	 * @param count the number of elements to return, negative count means counting down from the start.
	 * @return the iterator.
	 */
	public static IterableBuilder<Integer> range(int start, int count) {
		return from(Interactive.range(start, count));
	}
	/**
	 * Creates an long iterable builder which returns numbers from the start position in the count size.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param start the starting value.
	 * @param count the number of elements to return, negative count means counting down from the start.
	 * @return the iterator.
	 */
	public static IterableBuilder<Long> range(long start, long count) {
		return from(Interactive.range(start, count));
	}
	/**
	 * Creates an iterable builder which repeats the given
	 * value indefinitely.
	 * <p>The resulting iterable does not support the {@code remove()} method.</p>
	 * @param <T> the element type
	 * @param t the value to repeat
	 * @return the created iterable builder
	 */
	public static <T> IterableBuilder<T> repeat(final T t) {
		return from(Interactive.repeat(t));
	}
	/**
	 * Returns an iterable builder which repeats the given single value the specified number of times. 
	 * <p>The returned iterable does not support the {@code remove()} method.</p>
	 * @param <T> the value type
	 * @param t the value to repeat
	 * @param count the repeat amount
	 * @return the iterable
	 * @since 0.96
	 */
	public static <T> IterableBuilder<T> repeat(final T t, int count) {
		return from(Interactive.repeat(t, count));
	}
	/** The backing iterable. */
	protected final Iterable<T> it;
	/**
	 * Constructor.
	 * @param source the backing iterable
	 */
	protected IterableBuilder(@Nonnull Iterable<T> source) {
		this.it = source;
	}
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
	 * @param <U> the itermediate aggregation type
	 * @param <V> the resulting aggregation type
	 * @param sum the function which takes the current itermediate value, 
	 * the current source value and should produce a new intermediate value.
	 * for the first element of T, the U parameter will receive null
	 * @param divide the function which takes the last intermediate value and a total count of Ts seen and should return the final aggregation value.
	 * @return the new iterable
	 */
	public <U, V> IterableBuilder<V> aggregate(
			@Nonnull final Func2<? super U, ? super T, ? extends U> sum, 
			@Nonnull final Func2<? super U, ? super Integer, ? extends V> divide) {
		return from(Interactive.aggregate(it, sum, divide));
	}
	/**
	 * Returns an iterable which contains true if all
	 * elements of the source iterable satisfy the predicate.
	 * The operator might return a false before fully iterating the source.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param predicate the predicate
	 * @return the new iterable
	 */
	public IterableBuilder<Boolean> all(@Nonnull final Func1<? super T, Boolean> predicate) {
		return from(Interactive.all(it, predicate));
	}
	/**
	 * Determines if the given source has any elements at all.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @return the new iterable with a single true or false
	 */
	public IterableBuilder<Boolean> any() {
		return from(Interactive.any(it));
	}
	/**
	 * Tests if there is any element of the source that satisfies the given predicate function.
	 * @param predicate the predicate tester function
	 * @return the new iterable
	 */
	public IterableBuilder<Boolean> any(
			@Nonnull final Func1<? super T, Boolean> predicate
	) {
		return from(Interactive.any(it, predicate));
	}
	/**
	 * Returns a pair of the maximum argument and value from the given sequence.
	 * @param <V> the value type for the comparison, must be self comparable
	 * @param valueSelector the value selector function
	 * @return the pair of the first maximum element and value, null if the sequence was empty
	 * @since 0.96
	 */
	public <V extends Comparable<? super V>> Pair<T, V> argAndMax(
			@Nonnull Func1<? super T, ? extends V> valueSelector) {
		return Interactive.argAndMax(it, valueSelector);
	}
	/**
	 * Returns a pair of the maximum argument and value from the given sequence.
	 * @param <V> the value type
	 * @param valueSelector the selector to extract the value from T
	 * @param valueComparator the comparator to compare two values
	 * @return the first pair of max argument and value or null if the source sequence was empty
	 * @since 0.96
	 */
	public <V> Pair<T, V> argAndMax(
			@Nonnull Func1<? super T, ? extends V> valueSelector, 
			@Nonnull Comparator<? super V> valueComparator) {
		return Interactive.argAndMax(it, valueSelector, valueComparator);
	}
	/**
	 * Returns a pair of the minimum argument and value from the given sequence.
	 * @param <V> the value type for the comparison, must be self comparable
	 * @param valueSelector the value selector function
	 * @return the pair of the first minimum element and value, null if the sequence was empty
	 * @since 0.96
	 */
	public <V extends Comparable<? super V>> Pair<T, V> argAndMin(
			@Nonnull Func1<? super T, ? extends V> valueSelector) {
		return Interactive.argAndMin(it, valueSelector);
	}
	/**
	 * Returns a pair of the minimum argument and value from the given sequence.
	 * @param <V> the value type
	 * @param valueSelector the selector to extract the value from T
	 * @param valueComparator the comparator to compare two values
	 * @return the first pair of minimum argument and value or null if the source sequence was empty
	 * @since 0.96
	 */
	public <V> Pair<T, V> argAndMin(
			@Nonnull Func1<? super T, ? extends V> valueSelector, 
			@Nonnull Comparator<? super V> valueComparator) {
		return Interactive.argAndMin(it, valueSelector, valueComparator);
	}
	/**
	 * Returns an iterable which buffers the source elements 
	 * into <code>bufferSize</code> lists.
	 * FIXME what to do on empty source or last chunk?
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param bufferSize the buffer size.
	 * @return the new iterable
	 */
	public IterableBuilder<List<T>> buffer(int bufferSize) {
		return from(Interactive.buffer(it, bufferSize));
	}
	/**
	 * Casts the source iterable into a different typ by using a type token.
	 * If the source contains a wrong element, the <code>next()</code>
	 * will throw a <code>ClassCastException</code>.
	 * <p>The returned iterator forwards all <code>remove()</code> calls
	 * to the source.</p>
	 * @param <U> the result element type
	 * @param token the type token
	 * @return the new iterable
	 */
	public <U> IterableBuilder<U> cast(@Nonnull final Class<U> token) {
		return from(Interactive.cast(it, token));
	}
	/**
	 * Concatenate this iterable with the other iterable in a way, that calling the second <code>iterator()</code> 
	 * only happens when there is no more element in the first iterator.
	 * <p>The returned iterator forwards all <code>remove()</code> calls
	 * to the current source (first or next).
	 * @param other the second iterable
	 * @return the new iterable
	 */
	public IterableBuilder<T> concat(@Nonnull Iterable<? extends T> other) {
		return from(Interactive.concat(it, other));
	}
	/**
	 * Concatenate this iterable with the sequence of array values.
	 * @param values the array values
	 * @return the created iterable builder
	 */
	public IterableBuilder<T> concat(@Nonnull final T... values) {
		return concat(Interactive.toIterable(values));
	}
	/**
	 * Creates an iterable builder which contains the concatenation
	 * of this iterable and the rest iterable provided.
	 * @param others the other iterables
	 * @return the created iterable
	 */
	public IterableBuilder<T> concatAll(@Nonnull Iterable<? extends Iterable<? extends T>> others) {
		return from(Interactive.concat(Interactive.startWith(others, it)));
	}
	public IterableBuilder<Boolean> contains(final T value) {
		return from(Interactive.contains(it, value));
	}
	public IterableBuilder<Integer> count() {
		return from(Interactive.count(it));
	}
	public IterableBuilder<Long> countLong() {
		return from(Interactive.countLong(it));
	}
	public IterableBuilder<T> distinct() {
		return from(Interactive.distinct(it));
	}
	public <U> IterableBuilder<T> distinct(@Nonnull final Func1<T, U> keySelector) {
		return from(Interactive.distinct(it, keySelector));
	}
	public IterableBuilder<T> distinctSet() {
		return from(Interactive.distinctSet(it));
	}
	public <U> IterableBuilder<T> distinctSet(@Nonnull Func1<? super T, ? extends U> keySelector) {
		return from(Interactive.distinctSet(it, keySelector, Functions.<T>identity()));
	}
	public IterableBuilder<T> endWith(final T value) {
		return from(Interactive.endWith(it, value));
	}
	public IterableBuilder<T> finish(@Nonnull Action0 action) {
		return from(Interactive.finish(it, action));
	}
	public T first() {
		return Interactive.first(it);
	}
	public <K, V> IterableBuilder<GroupedIterable<K, V>> groupBy(@Nonnull final Func1<T, K> keySelector, final Func1<T, V> valueSelector) {
		return from(Interactive.groupBy(it, keySelector, valueSelector));
	}
	public IterableBuilder<T> invoke(@Nonnull Action1<? super T> action) {
		return from(Interactive.invoke(it, action));
	}
	public IterableBuilder<Boolean> isEmpty() {
		return from(Interactive.isEmpty(it));
	}
	@Override
	public Iterator<T> iterator() {
		return it.iterator();
	}
	public IterableBuilder<String> join(String separator) {
		return from(Interactive.join(it, separator));
	}
	public T last() {
		return Interactive.last(it);
	}
	public IterableBuilder<Option<T>> materialize() {
		return from(Interactive.materialize(it));
	}
	public IterableBuilder<T> max(@Nonnull Comparator<? super T> comparator) {
		return from(Interactive.max(it, comparator));
	}
	public IterableBuilder<List<T>> maxBy(@Nonnull Comparator<? super T> comparator) {
		return from(Interactive.maxBy(it, comparator));
	}
	public <U extends Comparable<? super U>> IterableBuilder<List<T>> mayBy(@Nonnull Func1<? super T, U> keySelector) {
		return from(Interactive.maxBy(it, keySelector));
	}
	public <U> IterableBuilder<List<T>> mayBy(@Nonnull Func1<? super T, U> keySelector, @Nonnull Comparator<? super U> keyComparator) {
		return from(Interactive.maxBy(it, keySelector, keyComparator));
	}
	public IterableBuilder<T> memoize(int bufferSize) {
		return from(Interactive.memoize(it, bufferSize));
	}
	public IterableBuilder<T> memoizeAll() {
		return from(Interactive.memoizeAll(it));
	}
	public IterableBuilder<T> min(@Nonnull Comparator<? super T> comparator) {
		return from(Interactive.min(it, comparator));
	}
	public IterableBuilder<List<T>> minBy(@Nonnull Comparator<? super T> comparator) {
		return from(Interactive.minBy(it, comparator));
	}
	public <U extends Comparable<? super U>> IterableBuilder<List<T>> minBy(@Nonnull Func1<? super T, U> keySelector) {
		return from(Interactive.minBy(it, keySelector));
	}
	public <U> IterableBuilder<List<T>> minBy(@Nonnull Func1<? super T, U> keySelector, @Nonnull Comparator<? super U> keyComparator) {
		return from(Interactive.minBy(it, keySelector, keyComparator));
	}
	public IterableBuilder<T> orderBy(@Nonnull Comparator<? super T> comparator) {
		return from(Interactive.orderBy(it, comparator));
	}
	public <U extends Comparable<? super U>> IterableBuilder<T> orderBy(@Nonnull Func1<? super T, ? extends U> keySelector) {
		return from(Interactive.orderBy(it, keySelector));
	}
	public <U> IterableBuilder<T> orderBy(@Nonnull Func1<? super T, ? extends U> keySelector, @Nonnull Comparator<? super U> keyComparator) {
		return from(Interactive.orderBy(it, keySelector, keyComparator));
	}
	public <U> IterableBuilder<U> prune(@Nonnull Func1<? super Iterable<? extends T>, ? extends Iterable<U>> func) {
		return from(Interactive.prune(it, func));
	}
	public <U> IterableBuilder<U> publish(@Nonnull final Func1<? super Iterable<? super T>, ? extends Iterable<? extends U>> func, 
			final U initial) {
		return from(Interactive.publish(it, func, initial));
	}
	public <U> IterableBuilder<U> publish(@Nonnull final Func1<? super Iterable<T>, ? extends Iterable<U>> func) {
		return from(Interactive.publish(it, func));
	}
	public <U> IterableBuilder<U> replay(@Nonnull final Func1<? super Iterable<T>, ? extends Iterable<U>> func) {
		return from(Interactive.replay(it, func));
	}
	public <U> IterableBuilder<U> replay(@Nonnull final Func1<? super Iterable<T>, ? extends Iterable<U>> func, 
			final int bufferSize) {
		return from(Interactive.replay(it, func, bufferSize));
	}
	public <U> IterableBuilder<U> scan(@Nonnull final Func2<? super U, ? super T, ? extends U> aggregator) {
		return from(Interactive.scan(it, aggregator));
	}
	public <U> IterableBuilder<U> scan(final U seed, 
			@Nonnull final Func2<? super U, ? super T, ? extends U> aggregator) {
		return from(Interactive.scan(it, seed, aggregator));
	}
	/**
	 * Creates an iterable which is a transforms the source
	 * elements by using the selector function.
	 * The function receives the current index and the current element.
	 * @param <U> the output element type
	 * @param selector the selector function
	 * @return the new iterable
	 */
	public <U> IterableBuilder<U> select(final Func1<? super T, ? extends U> selector) {
		return from(Interactive.select(it, selector));
	}
	/**
	 * Creates an iterable which is a transforms the source
	 * elements by using the selector function.
	 * The function receives the current index and the current element.
	 * <p>The returned iterator forwards all <code>remove()</code> calls
	 * to the source.</p>
	 * @param <U> the output element type
	 * @param selector the selector function
	 * @return the new iterable
	 */
	public <U> IterableBuilder<U> select(@Nonnull final Func2<Integer, ? super T, ? extends U> selector) {
		return from(Interactive.select(it, selector));
	}
	/**
	 * Creates an iterable which returns a stream of Us for each source Ts.
	 * The iterable stream of Us is returned by the supplied selector function.
	 * <p>The returned iterator forwards all <code>remove()</code> calls
	 * to the current source (which might not accept it).
	 * @param <U> the output element type
	 * @param selector the selector for multiple Us for each T
	 * @return the new iterable
	 */
	public <U> IterableBuilder<U> selectMany(@Nonnull final Func1<? super T, ? extends Iterable<? extends U>> selector) {
		return from(Interactive.selectMany(it, selector));
	}
	public IterableBuilder<T> share() {
		return from(Interactive.share(it));
	}
	public IterableBuilder<T> skipLast(final int num) {
		return from(Interactive.skipLast(it, num));
	}
	public IterableBuilder<T> startWith(T value) {
		return from(Interactive.startWith(it, value));
	}
	public IterableBuilder<T> take(int num) {
		return from(Interactive.take(it, num));
	}
	public IterableBuilder<T> takeLast(int num) {
		return from(Interactive.takeLast(it, num));
	}
	/**
	 * Iterates over and returns all elements in a list.
	 * @return the list of the values from this iterable
	 */
	@Nonnull
	public List<T> toList() {
		List<T> result = new ArrayList<T>();
		for (T t : it) {
			result.add(t);
		}
		return result;
	}
	/**
	 * Returns an object array of all elements in this
	 * iterable.
	 * @return the object array
	 */
	@Nonnull
	public Object[] toArray() {
		return toList().toArray();
	}
	/**
	 * Returns all elements from this iterable into either
	 * the given array or a new array if the size requires.
	 * @param a the output array
	 * @return the output array
	 */
	@Nonnull
	public T[] toArray(T[] a) {
		return toList().toArray(a);
	}
	/**
	 * Converts this iterable into an observable builder
	 * which uses the default scheduler of {@link hu.akarnokd.reactive4java.reactive.Reactive} to emit values.
	 * @return the observable builder
	 */
	public ObservableBuilder<T> toObservable() {
		return ObservableBuilder.from(it);
	}
	/**
	 * Converts this iterable into an observable builder
	 * which uses the supplied Scheduler to emit values. 
	 * @param scheduler the scheduler
	 * @return the observable builder
	 */
	public ObservableBuilder<T> toObservable(@Nonnull Scheduler scheduler) {
		return ObservableBuilder.from(it, scheduler);
	}
	/**
	 * Creates an iterable which filters this iterable with the
	 * given predicate factory function. The predicate returned by the factory receives an index
	 * telling how many elements were processed thus far.
	 * Use this construct if you want to use some memorizing predicat function (e.g., filter by subsequent distinct, filter by first occurrences only)
	 * which need to be invoked per iterator() basis.
	 * <p>The returned iterator forwards all <code>remove()</code> calls
	 * to the source.</p>
	 * @param filter the predicate function
	 * @return the new iterable
	 */
	public IterableBuilder<T> where(final Func1<? super T, Boolean> filter) {
		return from(Interactive.where(it, filter));
	}
	/**
	 * Creates an iterable which filters this iterable with the
	 * given predicate factory function. The predicate returned by the factory receives an index
	 * telling how many elements were processed thus far.
	 * @param predicate the predicate
	 * @return the new iterable
	 */
	public IterableBuilder<T> where(@Nonnull final Func2<Integer, ? super T, Boolean> predicate) {
		return from(Interactive.where(it, predicate));
	}
	/**
	 * Pairs each element from this and the oher iterable source and
	 * combines them into a new value by using the <code>combiner</code>
	 * function.
	 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code>
	 * for its <code>remove()</code> method.</p>
	 * @param <U> the right source type
	 * @param <V> the result type
	 * @param right the right source
	 * @param combiner the combiner function
	 * @return the new iterable
	 */
	public <U, V> IterableBuilder<V> zip(@Nonnull final Iterable<? extends U> right, 
			@Nonnull final Func2<? super T, ? super U, ? extends V> combiner) {
		return from(Interactive.zip(it, right, combiner));
	}
}
