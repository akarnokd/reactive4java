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

import com.google.gwt.user.client.Timer;

/**
 * The default implementation of the Scheduler
 * interface used by the <code>Reactive</code> operators
 * for a GWT environment.
 * @author akarnokd, 2011.02.02.
 */
public class DefaultGwtScheduler implements Scheduler {

	@Override
	public Closeable schedule(final Runnable run) {
		return schedule(run, 0);
	}

	@Override
	public Closeable schedule(final Runnable run, long delay) {
		final Timer timer = new Timer() {
			@Override
			public void run() {
				run.run();
			}
		};
		timer.schedule((int)(delay / 1000000L));
		return new Closeable() {
			@Override
			public void close() throws IOException {
				timer.cancel();
			}
		};
	}

	@Override
	public Closeable schedule(final Runnable run, 
			long initialDelay, final long betweenDelay) {
		final Timer outerTimer = new Timer() {
			/** The inner timer. */
			final Timer timer = new Timer() {
				@Override
				public void run() {
					try {
						run.run();
					} catch (Throwable ex) {
						timer.cancel();
					}
				}
			};
			@Override
			public void run() {
				timer.run();
				timer.scheduleRepeating((int)(betweenDelay / 1000000L));
			}
			@Override
			public void cancel() {
				timer.cancel();
				super.cancel();
			}
		};
		outerTimer.schedule((int)(initialDelay / 1000000L));
		return new Closeable() {
			@Override
			public void close() throws IOException {
				outerTimer.cancel();
			}
		};
	}
	
}
