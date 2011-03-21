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
import java.util.concurrent.TimeUnit;

import com.google.gwt.user.client.Timer;

/**
 * The default implementation of the Scheduler
 * interface used by the <code>Reactive</code> operators
 * for a GWT environment.
 * @author akarnokd, 2011.02.20.
 */
public class DefaultScheduler implements Scheduler {

	@Override
	public Closeable schedule(final Runnable run) {
		final Timer timer = new Timer() {
			@Override
			public void run() {
				try {
					run.run();
				} finally {
					Thread.interrupted(); // clear interrupt flag
				}
			}
		};
		timer.schedule(1);
		return new Closeable() {
			@Override
			public void close() throws IOException {
				Thread.currentThread().interrupt();
				timer.cancel();
			}
		};
	}

	@Override
	public Closeable schedule(final Runnable run, long delay, TimeUnit unit) {
		final Timer timer = new Timer() {
			@Override
			public void run() {
				try {
					run.run();
				} finally {
					Thread.interrupted(); // clear interrupt flag
				}
			}
		};
		timer.schedule((int)unit.convert(delay, TimeUnit.MILLISECONDS));
		return new Closeable() {
			@Override
			public void close() throws IOException {
				Thread.currentThread().interrupt();
				timer.cancel();
			}
		};
	}

	@Override
	public Closeable schedule(final Runnable run, 
			long initialDelay, final long betweenDelay, final TimeUnit unit) {
		final Timer outerTimer = new Timer() {
			/** The inner timer. */
			final Timer timer = new Timer() {
				@Override
				public void run() {
					try {
						run.run();
					} catch (Throwable ex) {
						Thread.currentThread().interrupt();
					}
					if (Thread.interrupted()) {
						timer.cancel();
					}
				}
			};
			@Override
			public void run() {
				try {
					run.run();
					if (!Thread.interrupted()) {
						timer.scheduleRepeating((int)unit.convert(betweenDelay, TimeUnit.MILLISECONDS));
					}
				} catch (Throwable ex) {
					
				}
			}
			@Override
			public void cancel() {
				Thread.currentThread().interrupt();
				timer.cancel();
				super.cancel();
			}
		};
		outerTimer.schedule((int)unit.convert(initialDelay, TimeUnit.MILLISECONDS));
		return new Closeable() {
			@Override
			public void close() throws IOException {
				outerTimer.cancel();
			}
		};
	}
	
}
