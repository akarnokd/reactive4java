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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * The default implementation of the Scheduler
 * interface used by the <code>Reactive</code> operators.
 * @author akarnokd, 2011.02.02.
 */
public class DefaultScheduler implements Scheduler {
	/** The default scheduler pool for delayed observable actions. */
	final ScheduledExecutorService pool;
	/**
	 * Creates a scheduler with a ScheduledThreadPoolExecutor. The
	 * pool will have the following attributes:
	 * <ul>
	 * <li><code>Runtime.getRuntime().availableProcessors()</code> core thread</li>
	 * <li>1 second idle timeout</li>
	 * <li>core threads may timeout</li>
	 * <li>unbounded worker queue</li>
	 * <li>no rejection handler</li>
	 * <li>if running on Java 7 or above: remove on cancel policy set to true</li>
	 * </ul>
	 */
	public DefaultScheduler() {
		this(Runtime.getRuntime().availableProcessors());
	}
	/**
	 * Creates a scheduler with a ScheduledThreadPoolExecutor. The
	 * pool will have the following attributes:
	 * <ul>
	 * <li><code>parallellism</code> core thread</li>
	 * <li>1 second idle timeout</li>
	 * <li>core threads may timeout</li>
	 * <li>unbounded worker queue</li>
	 * <li>no rejection handler</li>
	 * <li>if running on Java 7 or above: remove on cancel policy set to true</li>
	 * </ul>
	 * @param poolSize the number of core threads
	 * @since 0.97
	 */
	public DefaultScheduler(int poolSize) {
		ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(poolSize);
		scheduler.setKeepAliveTime(1, TimeUnit.SECONDS);
		scheduler.allowCoreThreadTimeOut(true);

		/*
		 * the setRemoveOnCancelPolicy() was introduced in Java 7 to
		 * allow the option to remove tasks from work queue if its initial delay hasn't
		 * elapsed -> therefore, if no other tasks are present, the scheduler might go idle earlier
		 * instead of waiting for the initial delay to pass to discover there is nothing to do.
		 * Because the library is currently aimed at Java 6, we use a reflection to set this policy
		 * on a Java 7+ runtime.
		 */
		try {
			java.lang.reflect.Method m = scheduler.getClass().getMethod("setRemoveOnCancelPolicy", Boolean.TYPE);
			m.invoke(scheduler, true);
		} catch (java.lang.reflect.InvocationTargetException ex) {
		} catch (NoSuchMethodException e) {
		} catch (SecurityException e) {
		} catch (IllegalAccessException e) {
		} catch (IllegalArgumentException e) {
		}
		pool = scheduler;
	}
	/**
     * Creates a scheduler instance with the given backing pool.
     * @param scheduled the backing scheduled executor service
	 */
	public DefaultScheduler(@Nonnull ScheduledExecutorService scheduled) {
//		if (scheduled == null) {
//			throw new IllegalArgumentException("scheduled is null");
//		}
		this.pool = scheduled;
	}
	/**
	 * Helper class which invokes <code>Future.cancel(true)</code> on
	 * the wrapped future.
	 * @author akarnokd, 2011.02.02.
	 */
	static class FutureCloser implements Closeable {
		/** The wrapped future. */
		@Nonnull 
		final Future<?> future;
		/**
		 * Constructor.
		 * @param future the future to close
		 */
		FutureCloser(@Nonnull Future<?> future) {
//			if (future == null) {
//				throw new IllegalArgumentException("future is null");
//			}
			this.future = future;
		}
		@Override
		public void close() {
			future.cancel(true);
		}
	}
	@Override
	@Nonnull 
	public Closeable schedule(@Nonnull Runnable run) {
		return toCloseable(pool.submit(run));
	}

	@Override
	@Nonnull 
	public Closeable schedule(
			@Nonnull Runnable run, 
			long delay, 
			@Nonnull TimeUnit unit) {
		return toCloseable(pool.schedule(run, delay, unit));
	}

	@Override
	@Nonnull 
	public Closeable schedule(
			@Nonnull Runnable run, 
			long initialDelay, 
			long betweenDelay, 
			@Nonnull TimeUnit unit) {
		return toCloseable(
				pool.scheduleAtFixedRate(run, initialDelay, betweenDelay, unit));
	}
	/**
	 * Factory function to turn the future into a closeable instance.
	 * @param future the future to work with
	 * @return the closeable instance
	 * @since 0.97
	 */
	@Nonnull 
	protected Closeable toCloseable(@Nonnull Future<?> future) {
		return new FutureCloser(future);
	}
	/**
	 * Shutdown both pools.
	 */
	public void shutdown() {
		pool.shutdown();
	}
	/**
	 * Shutdown both pools now.
	 * @return the list of runnable tasks awaiting executions in both pools
	 */
	public List<Runnable> shutdownNow() {
		List<Runnable> result = new ArrayList<Runnable>();
		result.addAll(pool.shutdownNow());
		return result;
	}
}
