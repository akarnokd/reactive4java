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
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * The scheduler uses a cached thread pool executor (via {@link Executors#newCachedThreadPool()}) as its backing thread pool.
 * Delayed executions are performed via the TimeUnit.sleep() methods on the pool thread (consuming resources).
 * To stop a repeating schedule, throw a CancellationException.
 * @author akarnokd, 2011.10.05.
 * @since 0.95
 */
public class CachedThreadPoolScheduler implements Scheduler {
	/** The thread pool. */
	protected final ExecutorService pool;
	/**
	 * Constructor. Initializes the backing thread pool
	 */
	public CachedThreadPoolScheduler() {
		pool = Executors.newCachedThreadPool();
	}
	@Override
	public Closeable schedule(Runnable run) {
		final Future<?> f = pool.submit(run);
		return new Closeable() {
			@Override
			public void close() throws IOException {
				f.cancel(true);
			}
		};
	}

	@Override
	public Closeable schedule(final Runnable run, final long delay, final TimeUnit unit) {
		Runnable task = new Runnable() {
			@Override
			public void run() {
				try {
					unit.sleep(delay);
					run.run();
				} catch (InterruptedException ex) {
					// ignore and quit
				} catch (CancellationException ex) {
					// ignored
				}
			}
		};
		return schedule(task);
	}

	@Override
	public Closeable schedule(final Runnable run, final long initialDelay,
			final long betweenDelay, final TimeUnit unit) {
		Runnable task = new Runnable() {
			@Override
			public void run() {
				try {
					unit.sleep(initialDelay);
					while (!Thread.currentThread().isInterrupted()) {
						run.run();
						unit.sleep(betweenDelay);
					}
				} catch (InterruptedException ex) {
					// ignore and quit
				}
			}
		};
		return schedule(task);
	}

}
