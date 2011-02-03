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

package hu.akarnokd.reactive4java;

import java.io.Closeable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A helper class which ensures that each of its queued
 * elements get processed in sequence even on a multi-threaded pool.
 * The class implements Closeable which may be called to cancel
 * any on-going processing and remove any queued tasks.
 * @author akarnokd, 2011.01.31.
 * @param <T> the element type to process
 */
public final class SingleLaneExecutor<T> implements Closeable {
	/** The executor pool. */
	final Scheduler pool;
	/** Keeps track of the queue size. */
	final AtomicInteger wip = new AtomicInteger();
	/** The queue of items. */
	final BlockingQueue<T> queue = new LinkedBlockingQueue<T>();
	/** The action to invoke for each element. */
	final Action1<? super T> action;
	/** The queue processor. */
	final Runnable processor = new Runnable() {
		@Override
		public synchronized void run() { // ensure that only one instance is running
			int count = 0;
			do {
				List<T> list = new LinkedList<T>();
				queue.drainTo(list);
				count = list.size();
				for (T t : list) {
					if (Thread.currentThread().isInterrupted()) {
						wip.addAndGet(-count);
						return;
					}
					action.invoke(t);
				}
			} while (wip.addAndGet(-count) > 0);
		}
	};
	/** The future of the currently running processor. */
	final AtomicReference<Future<?>> future = new AtomicReference<Future<?>>();
	/**
	 * Constructor. 
	 * @param pool the executor service to use as the pool.
	 * @param action the action to invoke when processing a queue item
	 */
	public SingleLaneExecutor(Scheduler pool, Action1<? super T> action) {
		if (pool == null) {
			throw new IllegalArgumentException("pool is null");
		}
		if (action == null) {
			throw new IllegalArgumentException("action is null");
		}
		this.action = action;
		this.pool = pool;
	}
	/**
	 * Add an item to the queue and start the processor if necessary.
	 * @param item the item to add.
	 */
	public void add(T item) {
		queue.add(item);
		if (wip.incrementAndGet() == 1) {
			FutureTask<Void> f =  new FutureTask<Void>(processor, null);
			future.set(f);
			pool.schedule(f);
		}
	}
	/**
	 * Add the iterable series of items. The items are added via add() method,
	 * and might start the processor if necessary.
	 * @param items the iterable of items
	 */
	public void add(Iterable<? extends T> items) {
		for (T item : items) {
			add(item);
		}
	}
	@Override
	public void close() {
		Future<?> f = future.getAndSet(null);
		if (f != null && !f.isDone()) {
			f.cancel(true);
		}
		// drain remaining elements as of now
		List<T> left = new LinkedList<T>();
		queue.drainTo(left);
		wip.addAndGet(-left.size());
	}
}
