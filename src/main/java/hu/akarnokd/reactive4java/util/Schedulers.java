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
import hu.akarnokd.reactive4java.scheduler.DefaultScheduler;

import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

/**
 * Utility class for schedulers, and manages
 * the global default scheduler.
 * @author akarnokd, 2013.01.12.
 */
public final class Schedulers {
	/** The common observable pool where the Observer methods get invoked by default. */
	@Nonnull
	static final AtomicReference<Scheduler> DEFAULT_SCHEDULER = new AtomicReference<Scheduler>();
	/** Setup default scheduler. */
	static {
		restoreDefault();
	}
	/** Helper class. */
	private Schedulers() { }
	/**
	 * @return Returns the default scheduler.
	 */
	@Nonnull
	public static Scheduler getDefault() {
		return DEFAULT_SCHEDULER.get();
	}
	/**
	 * Changes the default scheduler.
	 * @param scheduler the new scheduler, non-null
	 */
	public static void setDefault(@Nonnull Scheduler scheduler) {
		if (scheduler == null) {
			throw new IllegalArgumentException("scheduler is null");
		}
		DEFAULT_SCHEDULER.set(scheduler);
	}
	/**
	 * Restore the default scheduler back to the <code>DefaultScheduler</code>
	 * used when this class was initialized.
	 */
	public static void restoreDefault() {
		DEFAULT_SCHEDULER.set(new DefaultScheduler());
	}
	/** @return the scheduler designated to constant time operations. */
	public static Scheduler constantTimeOperations() {
		return getDefault(); // for now
	}
	/**
	 * @return the value of the nanosecond resolution timer
	 */
	public static long now() {
		// #GWT-IGNORE-START
		return System.nanoTime();
		// #GWT-IGNORE-END
		// #GWT-ACCEPT-START
		//return System.currentTimeMillis() * 1000000;
		// #GWT-ACCEPT-END
	}
}
