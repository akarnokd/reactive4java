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
package hu.akarnokd.reactive4java.util;

import hu.akarnokd.reactive4java.base.Scheduler;

import java.io.Closeable;
import java.io.IOException;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nonnull;

/**
 * Scheduler which runs tasks on the current thread.
 * Timed or repeated tasks may block operations indefinitely.
 * @author akarnokd, 2011.02.07.
 */
public class CurrentThreadScheduler implements Scheduler {
	/** The relative order for zero delay invocations. */
	public final AtomicLong sequence = new AtomicLong();
	/** The delayed runnable class. */
	class DelayedRunnable {
		/** The relative sequence for same-delay invocations .*/
		public final long id = sequence.getAndIncrement();
		/** The actual runnable. */
		public final Runnable run;
		/** The delay . */
		public final long delay;
		/** The delay unit. */
		public final TimeUnit unit;
		/**
		 * Constructor. Sets the task and the preferred delay.
		 * @param task the task
		 * @param delay the delay
		 * @param unit the delay time unit
		 */
		public DelayedRunnable(@Nonnull Runnable task, long delay, @Nonnull TimeUnit unit) {
			this.run = task;
			this.delay = delay;
			this.unit = unit;
		}
	}
	/** The delayed runnable class. */
	class RepeatedRunnable extends DelayedRunnable {
		/** The in-between delay. */
		public final long betweenDelay;
		/**
		 * Constructor. sets the task and preferred delays.
		 * @param task the target tasks.
		 * @param initialDelay the initial delay
		 * @param betweenDelay the between delay
		 * @param unit the delay time unit
		 */
		public RepeatedRunnable(
				@Nonnull Runnable task, 
				long initialDelay, 
				long betweenDelay, 
				@Nonnull TimeUnit unit) {
			super(task, initialDelay, unit);
			this.betweenDelay = betweenDelay; 
		}
	}
	/** The in progress value. */
	protected int wip;
	/** The priority queue for the tasks. */
	protected PriorityQueue<DelayedRunnable> tasks = new PriorityQueue<DelayedRunnable>(128, new Comparator<DelayedRunnable>() {
		@Override
		public int compare(DelayedRunnable o1, DelayedRunnable o2) {
			return o1.delay < o2.delay ? -1
					: (o1.delay > o2.delay ? 1 
					: (o1.id < o2.id ? -1 : (o1.id > o2.id ? 1 : 0))	
			);
		}
	});
	/** The main scheduler loop. */
	void schedulerLoop() {
		if (wip == 1) {
			try {
				while (true) {
					DelayedRunnable dr = tasks.poll();
					if (dr == null) {
						break;
					}
					if (dr.delay > 0) {
						dr.unit.sleep(dr.delay);
					}
					try {
						dr.run.run();
						if (dr instanceof RepeatedRunnable) {
							RepeatedRunnable rr = (RepeatedRunnable) dr;
							tasks.add(new RepeatedRunnable(rr.run, rr.betweenDelay, rr.betweenDelay, rr.unit));
						}
					} catch (Throwable ex) {
						// any exception interpreted as cancel running
					} finally {
						wip--;
					}
				}
			} catch (InterruptedException ex) {
				
			}
		}
	}
	@Override
	public Closeable schedule(Runnable run) {
		final DelayedRunnable dr = new DelayedRunnable(run, 0, TimeUnit.MILLISECONDS);
		tasks.add(dr);
		wip++;
		schedulerLoop();
		return new Closeable() {
			@Override
			public void close() throws IOException {
				tasks.remove(dr);
			}
		};
	}
	
	@Override
	public Closeable schedule(Runnable run, long delay, TimeUnit unit) {
		final DelayedRunnable dr = new DelayedRunnable(run, delay, unit);
		tasks.add(dr);
		wip++;
		schedulerLoop();
		return new Closeable() {
			@Override
			public void close() throws IOException {
				tasks.remove(dr);
			}
		};
	}

	@Override
	public Closeable schedule(Runnable run, long initialDelay, long betweenDelay, TimeUnit unit) {
		final RepeatedRunnable dr = new RepeatedRunnable(run, initialDelay, betweenDelay, unit);
		tasks.add(dr);
		wip++;
		schedulerLoop();
		return new Closeable() {
			@Override
			public void close() throws IOException {
				tasks.remove(dr);
			}
		};
	}

}
