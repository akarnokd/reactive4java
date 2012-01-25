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
package hu.akarnokd.reactive4java.interactive;

import hu.akarnokd.reactive4java.base.Action0;
import hu.akarnokd.reactive4java.base.Action1;
import hu.akarnokd.reactive4java.base.Func0;
import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Func2;
import hu.akarnokd.reactive4java.base.Functions;
import hu.akarnokd.reactive4java.base.Option;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * An iterable builder which offers methods to chain the
 * sequence of some Interactive operations.
 * @author akarnokd, Jan 25, 2012
 * @param <T> the element type
 * @since 0.96
 */
public final class IterableBuilder<T> implements Iterable<T> {
	/** The backing iterable. */
	protected final Iterable<T> it;
	/**
	 * Constructor.
	 * @param source the backing iterable
	 */
	protected IterableBuilder(Iterable<T> source) {
		this.it = source;
	}
	@Override
	public Iterator<T> iterator() {
		return it.iterator();
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
	public static <T> IterableBuilder<T> repeat(final T t) {
		return from(Interactive.repeat(t));
	}
	public static <T> IterableBuilder<T> repeat(final T t, int count) {
		return from(Interactive.repeat(t, count));
	}
	public static IterableBuilder<Integer> range(int start, int count) {
		return from(Interactive.range(start, count));
	}
	public static IterableBuilder<Long> range(long start, long count) {
		return from(Interactive.range(start, count));
	}
	public <U> IterableBuilder<U> select(final Func1<? super T, ? extends U> selector) {
		return from(Interactive.select(it, selector));
	}
	public IterableBuilder<T> where(final Func1<? super T, Boolean> filter) {
		return from(Interactive.where(it, filter));
	}
	public List<T> toList() {
		List<T> result = new ArrayList<T>();
		for (T t : it) {
			result.add(t);
		}
		return result;
	}
	public <U, V> IterableBuilder<V> aggregate(
			@Nonnull final Func2<? super U, ? super T, ? extends U> sum, 
			@Nonnull final Func2<? super U, ? super Integer, ? extends V> divide) {
		return from(Interactive.aggregate(it, sum, divide));
	}
	public IterableBuilder<Boolean> any() {
		return from(Interactive.any(it));
	}
	public IterableBuilder<Boolean> any(
			@Nonnull final Func1<? super T, Boolean> predicate
	) {
		return from(Interactive.any(it, predicate));
	}
	public IterableBuilder<List<T>> buffer(int bufferSize) {
		return from(Interactive.buffer(it, bufferSize));
	}
	public <U> IterableBuilder<U> cast(@Nonnull final Class<U> token) {
		return from(Interactive.cast(it, token));
	}
	public IterableBuilder<T> concat(@Nonnull Iterable<? extends T> other) {
		return from(Interactive.concat(it, other));
	}
	public IterableBuilder<T> concat(final T... values) {
		return concat(Interactive.toIterable(values));
	}
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
	public static <T> IterableBuilder<T> defer(
			@Nonnull final Func0<? extends Iterable<T>> func) {
		return from(Interactive.defer(func));
	}
	public IterableBuilder<Option<T>> materialize() {
		return from(Interactive.materialize(it));
	}
	public static <T> IterableBuilder<T> dematerialize(@Nonnull Iterable<? extends Option<? extends T>> source) {
		return from(Interactive.dematerialize(source));
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
	public static <T> IterableBuilder<T> generate(T seed, @Nonnull Func1<? super T, Boolean> predicate, @Nonnull Func1<? super T, ? extends T> next) {
		return from(Interactive.generate(seed, predicate, next));
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
	public IterableBuilder<String> join(String separator) {
		return from(Interactive.join(it, separator));
	}
	public T last() {
		return Interactive.last(it);
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
	public <U> IterableBuilder<U> select(@Nonnull final Func2<Integer, ? super T, ? extends U> selector) {
		return from(Interactive.select(it, selector));
	}
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
	public IterableBuilder<T> where(@Nonnull final Func2<Integer, ? super T, Boolean> predicate) {
		return from(Interactive.where(it, predicate));
	}
	public <U, V> IterableBuilder<V> zip(@Nonnull final Iterable<? extends U> right, 
			@Nonnull final Func2<? super T, ? super U, ? extends V> combiner) {
		return from(Interactive.zip(it, right, combiner));
	}
}
