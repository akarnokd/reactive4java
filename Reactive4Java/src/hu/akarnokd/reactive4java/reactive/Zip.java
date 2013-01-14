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

import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Func2;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;
import hu.akarnokd.reactive4java.base.Pair;
import hu.akarnokd.reactive4java.util.Closeables;
import hu.akarnokd.reactive4java.util.CompositeCloseable;
import hu.akarnokd.reactive4java.util.DefaultObserverEx;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;

/**
 * Helper class for zip operators.
 * @author akarnokd, 2013.01.14.
 * @since 0.97
 */
public final class Zip {
	/** Helper class. */
	private Zip() { }
	/**
	 * Pairwise merges the iterable and observable source sequences
	 * and applies a selector function to produce the final observable
	 * values.
	 * <p>The resulting sequence terminates if no more pairs can be
	 * established, i.e., streams of length 1 and 2 zipped will produce
	 * only 1 item.</p>
	 * <p>Exception semantics: errors from the source observable are
	 * propagated as-is.</p>
	 * @author akarnokd, 2013.01.14.
	 * @param <T> the iterable's element type
	 * @param <U> the observable's element type
	 * @param <V> the result element type
	 */
	public static class ObservableAndIterable<T, U, V> implements Observable<V> {
		/** */
		private final Observable<? extends T> left;
		/** */
		private final Iterable<? extends U> right;
		/** */
		private final Func2<? super T, ? super U, ? extends V> selector;
		/**
		 * Constructor.
		 * @param left the iterable side
		 * @param right the observable side
		 * @param selector the result selector function
		 */
		public ObservableAndIterable(
				Observable<? extends T> left, 
				Iterable<? extends U> right, 
				Func2<? super T, ? super U, ? extends V> selector) {
			this.left = left;
			this.right = right;
			this.selector = selector;
		}
		@Override
		@Nonnull
		public Closeable register(@Nonnull final Observer<? super V> observer) {
			final Iterator<? extends U> it = right.iterator();
			if (it.hasNext()) {
				/** Simple combining observer. */
				class LeftObserver extends DefaultObserverEx<T> {
					@Override
					protected void onNext(T value) {
						if (it.hasNext()) {
							observer.next(selector.invoke(value, it.next()));
							if (!it.hasNext()) {
								finish();
							}
						}
					}

					@Override
					protected void onError(Throwable ex) {
						observer.error(ex);
					}

					@Override
					protected void onFinish() {
						observer.finish();
					}
					
				}
				return new LeftObserver().registerWith(left);
			}
			return Closeables.emptyCloseable();
		}
	}
	/**
	 * Pairwise merges the two observable sequences and emits
	 * the value by the selector.
	 * <p>The resulting sequence terminates if no more pairs can be
	 * established, i.e., streams of length 1 and 2 zipped will produce
	 * only 1 item.</p>
	 * <p>Exception semantics: errors from the source observable are
	 * propagated as-is.</p>
	 * @author akarnokd, 2013.01.14.
	 * @param <T> the left observable's element type
	 * @param <U> the right observable's element type
	 * @param <V> the result element type
	 */
	public static class TwoObservable<T, U, V> implements Observable<V> {
		/** */
		protected final Observable<? extends T> left;
		/** */
		protected final Observable<? extends U> right;
		/** */
		protected final Func2<? super T, ? super U, ? extends V> selector;
		/**
		 * Constructor.
		 * @param left the first observable
		 * @param right the second observable
		 * @param selector the result selector
		 */
		public TwoObservable(
				Observable<? extends T> left, 
				Observable<? extends U> right,
				Func2<? super T, ? super U, ? extends V> selector) {
			this.left = left;
			this.right = right;
			this.selector = selector;
		}

		@Override
		@Nonnull
		public Closeable register(@Nonnull final Observer<? super V> observer) {
			
			final Lock lock = new ReentrantLock(true);
			final CompositeCloseable composite = new CompositeCloseable();
			
			final Object nullSentinel = new Object();
			/* GuardedBy("lock") */
			final Queue<Object> queue1 = new LinkedList<Object>();
			/* GuardedBy("lock") */
			final Queue<Object> queue2 = new LinkedList<Object>();
			/** The active parties. */
			final AtomicInteger wip = new AtomicInteger(2);
			/** The left observer. */
			class LeftObserver extends DefaultObserverEx<T> {
				/**
				 * Constructor.
				 * @param lock the shared lock
				 */
				public LeftObserver(Lock lock) {
					super(lock, false);
				}

				@Override
				@SuppressWarnings("unchecked")
				protected void onNext(T value) {
					Object u = queue2.poll();
					if (u == null) {
						if (wip.get() > 1) {
							queue1.add(value != null ? value : nullSentinel);
						}
					} else {
						if (u == nullSentinel) {
							u = null;
						}
						V v = selector.invoke(value, (U)u);
						observer.next(v);
					}
					if (wip.get() == 1 && queue2.isEmpty()) {
						observer.finish();
						Closeables.closeSilently(composite);
					}
				}

				@Override
				protected void onError(Throwable ex) {
					observer.error(ex);
					Closeables.closeSilently(composite);
				}

				@Override
				protected void onFinish() {
					int n = wip.decrementAndGet();
					if (n == 0 || (n == 1 && queue1.isEmpty())) {
						observer.finish();
						Closeables.closeSilently(composite);
					}
				}
			}
			/** The right observer. */
			class RightObserver extends DefaultObserverEx<U> {
				/**
				 * Constructor.
				 * @param lock the shared lock
				 */
				public RightObserver(Lock lock) {
					super(lock, false);
				}

				@Override
				@SuppressWarnings("unchecked")
				protected void onNext(U value) {
					Object t = queue1.poll();
					if (t == null) {
						if (wip.get() > 1) {
							queue2.add(value != null ? value : nullSentinel);
						}
					} else {
						if (t == nullSentinel) {
							t = null;
						}
						V v = selector.invoke((T)t, value);
						observer.next(v);
						
					}
					if (wip.get() == 1 && queue1.isEmpty()) {
						observer.finish();
						Closeables.closeSilently(composite);
					}
				}

				@Override
				protected void onError(Throwable ex) {
					observer.error(ex);
					Closeables.closeSilently(composite);
				}

				@Override
				protected void onFinish() {
					int n = wip.decrementAndGet();
					if (n == 0 || (n == 1 && queue2.isEmpty())) {
						observer.finish();
						Closeables.closeSilently(composite);
					}
				}
				
			}
			
			LeftObserver lo = new LeftObserver(lock);
			RightObserver ro = new RightObserver(lock);
			composite.add(lo, ro);
			lo.registerWith(left);
			ro.registerWith(right);
			
			return composite;
		}
	}
	/**
	 * Merges the values across multiple sources and applies the selector
	 * function.
	 * <p>The resulting sequence terminates if no more pairs can be
	 * established, i.e., streams of length 1 and 2 zipped will produce
	 * only 1 item.</p>
	 * <p>Exception semantics: errors from the source observable are
	 * propagated as-is.</p>
	 * @author akarnokd, 2013.01.14.
	 * @param <T> the common element type
	 * @param <V> the result element type
	 */
	public static class ManyObservables<T, U> implements Observable<U> {
		/** */
		protected final Iterable<? extends Observable<? extends T>> sources;
		/** */
		protected final Func1<? super List<T>, ? extends U> selector;
		/**
		 * Constructor.
		 * @param sources the sources
		 * @param selector the result selector
		 */
		public ManyObservables(
				Iterable<? extends Observable<? extends T>> sources,
				Func1<? super List<T>, ? extends U> selector) {
			this.sources = sources;
			this.selector = selector;
		}

		@Override
		@Nonnull
		public Closeable register(@Nonnull final Observer<? super U> observer) {

			final Lock lock = new ReentrantLock(true);
			
			final CompositeCloseable composite = new CompositeCloseable();
			
			final Object nullSentinel = new Object();
			
			final List<BlockingQueue<T>> itemQueues = new ArrayList<BlockingQueue<T>>();

			final AtomicInteger wip = new AtomicInteger();
			
			final AtomicReference<List<T>> row = new AtomicReference<List<T>>();
			
			/** The individual source observer. */
			class ItemObserver extends DefaultObserverEx<T> {
				/** The index to work with. */
				protected final int index;
				/**
				 * Constructor.
				 * @param lock the common lock
				 * @param index the item index
				 */
				public ItemObserver(Lock lock, int index) {
					super(lock, false);
					this.index = index;
				}
				@Override
				protected void onNext(T value) {
				}
				/**
				 * Prepare the current row.
				 */
				protected void prepareRow() {
					List<T> r = row.get();
					if (r == null) {
						r = new ArrayList<T>();
						row.set(r);
					}
					while (r.size() < index + 1) {
						r.add(null);
					}
				}
				/** @return Check if the other queues are empty. */
				protected boolean othersEmpty() {
					for (int i = 0; i < itemQueues.size(); i++) {
						if (i != index) {
							if (!itemQueues.get(i).isEmpty()) {
								return false;
							}
						}
					}
					return true;
				}
				@Override
				protected void onError(Throwable ex) {
					observer.error(ex);
					Closeables.closeSilently(composite);
				}
				/** Notify observer and terminate. */
				protected void done() {
					observer.finish();
					Closeables.closeSilently(composite);
				}
				@Override
				protected void onFinish() {
					int n = wip.decrementAndGet();
					if (n == 1 || (n > 1 && othersEmpty())) {
						done();
					}
				}
				
			}
			
			List<Pair<ItemObserver, ? extends Observable<? extends T>>> regs = new ArrayList<Pair<ItemObserver, ? extends Observable<? extends T>>>();
			int i = 0;
			for (Observable<? extends T> o : sources) {
				
				ItemObserver io = new ItemObserver(lock, i);
				BlockingQueue<T> q = new LinkedBlockingQueue<T>(i);
				
				itemQueues.add(q);
				composite.add(io);

				regs.add(Pair.of(io, o));
				
				i++;
			}
			
			for (Pair<ItemObserver, ? extends Observable<? extends T>> p : regs) {
				p.first.registerWith(p.second);
			}
			
			return composite;
		}
		
	}
}
