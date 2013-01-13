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

package hu.akarnokd.reactive4java.scheduler;

import hu.akarnokd.reactive4java.base.Scheduler;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

/**
 * A scheduler which creates separate new threads for each task.
 * To stop a repeating schedule, throw a CancellationException.
 * @author akarnokd, 2011.10.05.
 */
public class NewThreadScheduler implements Scheduler {
	/** The tread names. */
	@Nonnull
	private String name = "NewThreadScheduler";
	/** Counts the new threads started. */
	@Nonnull 
	private final AtomicInteger counter = new AtomicInteger();
	/**
	 * Constructor. The threads are named as {@code NewThreadScheduler-#}
	 */
	public NewThreadScheduler() {
	}
	/**
	 * Constructor.
	 * @param name the name prefix used when creating new threads.
	 */
	public NewThreadScheduler(@Nonnull String name) {
		this.name = name;
	}
	@Override
	@Nonnull 
	public Closeable schedule(@Nonnull Runnable run) {
		final Thread t = new Thread(run, name + "-" + counter.incrementAndGet());
		t.start();
		return new Closeable() {
			@Override
			public void close() throws IOException {
				t.interrupt();
			}
		};
	}

	@Override
	@Nonnull 
	public Closeable schedule(
			@Nonnull final Runnable run, 
			final long delay, 
			@Nonnull final TimeUnit unit) {
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
	@Nonnull 
	public Closeable schedule(
			@Nonnull final Runnable run, 
			final long initialDelay,
			final long betweenDelay, 
			@Nonnull final TimeUnit unit) {
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
