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

import hu.akarnokd.reactive4java.base.Cancelable;
import hu.akarnokd.reactive4java.base.Scheduler;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

/**
 * A closeable instance which invokes the managed closeable's
 * close method on the given scheduler.
 * <p>The implementation is thread-safe.</p>
 * @author akarnokd, 2013.01.12.
 * @since 0.97
 */
public class ScheduledCloseable implements Closeable, Cancelable {
	/** The scheduler reference. */
	@Nonnull
	protected final Scheduler scheduler;
	/** The handle reference. */
	@Nonnull
	protected final AtomicReference<Closeable> current = new AtomicReference<Closeable>();
	/**
	 * Constructor.
	 * @param scheduler the scheduler to use
	 * @param handle the handle to close
	 */
	public ScheduledCloseable(@Nonnull Scheduler scheduler, @Nonnull Closeable handle) {
		this.scheduler = scheduler;
		this.current.set(handle);
	}
	@Override
	public void close() throws IOException {
		scheduler.schedule(new Runnable() {
			@Override
			public void run() {
				Closeable c = current.getAndSet(SENTINEL);
				if (c != SENTINEL) {
					Closeables.closeSilently(c);
				}
			}
		});
	}
	/** @return the scheduler used. */
	@Nonnull
	public Scheduler scheduler() {
		return scheduler;
	}
	/**
	 * @return the managed closeable
	 */
	@Nonnull
	public Closeable get() {
		Closeable c = current.get();
		
		// don't leak the sentinel
		if (c == SENTINEL) {
			return Closeables.emptyCloseable();
		}
		
		return c;
	}
	@Override
	public boolean isClosed() {
		return current.get() == SENTINEL;
	}
	/** 
	 * The empty sentinel to know when we have
	 * closed the previous instance and keep the 
	 * invariants of this SingleCloseable.
	 */
	@Nonnull 
	protected static final Closeable SENTINEL = new Closeable() {
		@Override
		public void close() throws IOException { }
	};
}
