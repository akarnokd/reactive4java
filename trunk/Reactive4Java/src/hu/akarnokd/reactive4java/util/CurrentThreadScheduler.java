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
package hu.akarnokd.reactive4java.util;

import hu.akarnokd.reactive4java.base.Scheduler;

import java.io.Closeable;
import java.io.IOException;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * Scheduler which runs tasks on the current thread.
 * Timed or repeated tasks may block operations indefinitely.
 * @author akarnokd, 2011.02.07.
 */
public class CurrentThreadScheduler implements Scheduler {
	/** The delayed runnable class. */
	static class DelayedRunnable {
		/** The actual runnable. */
		public final Runnable run;
		/** The delay in nanoseconds. */
		public long delay;
		/**
		 * Constructor. Sets the task and the preferred delay.
		 * @param task the task
		 * @param delay the delay
		 */
		public DelayedRunnable(@Nonnull Runnable task, long delay) {
			this.run = task;
			this.delay = delay;
		}
	}
	/** The delayed runnable class. */
	static class RepeatedRunnable extends DelayedRunnable {
		/** The in-between delay. */
		public final long betweenDelay;
		/**
		 * Constructor. sets the task and preferred delays.
		 * @param task the target tasks.
		 * @param initialDelay the initial delay
		 * @param betweenDelay the between delay
		 */
		public RepeatedRunnable(@Nonnull Runnable task, long initialDelay, long betweenDelay) {
			super(task, initialDelay);
			this.betweenDelay = betweenDelay; 
		}
	}
	/** The priority queue for the tasks. */
	protected PriorityQueue<DelayedRunnable> tasks = new PriorityQueue<DelayedRunnable>(128, new Comparator<DelayedRunnable>() {
		@Override
		public int compare(DelayedRunnable o1, DelayedRunnable o2) {
			return o1.delay < o2.delay ? -1 : (o1.delay > o2.delay ? 1 : 0);
		}
	});
	/** The main scheduler loop. */
	void schedulerLoop() {
		if (tasks.size() == 1) {
			try {
				while (true) {
					DelayedRunnable dr = tasks.poll();
					if (dr == null) {
						break;
					}
					if (dr.delay > 0) {
						TimeUnit.NANOSECONDS.sleep(dr.delay);
					}
					try {
						dr.run.run();
						if (dr instanceof RepeatedRunnable) {
							dr.delay = ((RepeatedRunnable) dr).betweenDelay;
							tasks.add(dr);
						}
					} catch (CancellationException ex) {
						
					}
				}
			} catch (InterruptedException ex) {
				
			}
		}
	}
	@Override
	public Closeable schedule(Runnable run) {
		final DelayedRunnable dr = new DelayedRunnable(run, 0);
		tasks.add(dr);
		schedulerLoop();
		return new Closeable() {
			@Override
			public void close() throws IOException {
				tasks.remove(dr);
			}
		};
	}
	
	@Override
	public Closeable schedule(Runnable run, long delay) {
		final DelayedRunnable dr = new DelayedRunnable(run, delay);
		tasks.add(dr);
		schedulerLoop();
		return new Closeable() {
			@Override
			public void close() throws IOException {
				tasks.remove(dr);
			}
		};
	}

	@Override
	public Closeable schedule(Runnable run, long initialDelay, long betweenDelay) {
		final RepeatedRunnable dr = new RepeatedRunnable(run, initialDelay, betweenDelay);
		tasks.add(dr);
		schedulerLoop();
		return new Closeable() {
			@Override
			public void close() throws IOException {
				tasks.remove(dr);
			}
		};
	}

}
