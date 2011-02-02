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
package hu.akarnokd.reactiv4java;

import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The default implementation of the Scheduler
 * interface used by the Observables methods.
 * @author akarnokd, 2011.02.02.
 */
public class DefaultScheduler implements Scheduler {
	/** The common observable pool where the Observer methods get invoked by default. */
	final ExecutorService pool;
	/** The defalt scheduler pool for delayed observable actions. */
	final ScheduledExecutorService scheduledPool;
	/**
	 * Creates two separate pools for the immediate and scheduled executions.
	 * <ul>
	 * <li>ThreadPoolExecutor, having
	 * <ul>
	 * <li>1 core thread</li>
	 * <li>1 second idle timeout</li>
	 * <li>core threads may timeout</li>
	 * <li>unbounded worker queue</li>
	 * <li>no rejection handler</li>
	 * </ul>
	 * </li>
	 * <li>ScheduledThreadPoolExecutor
	 * <ul>
	 * <li>1 core thread</li>
	 * <li>1 second idle timeout</li>
	 * <li>core threads may timeout</li>
	 * <li>unbounded worker queue</li>
	 * <li>no rejection handler</li>
	 * <li>if running on Java 7 or above: remove on cancel policy set to true</li>
	 * </ul></li>
	 * </ul>
	 */
	public DefaultScheduler() {
		ThreadPoolExecutor threadpool = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		threadpool.allowCoreThreadTimeOut(true);
		
		
		ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);
		scheduler.setKeepAliveTime(1, TimeUnit.SECONDS);
		scheduler.allowCoreThreadTimeOut(true);
		
		/* 
		 * the setRemoveOnCancelPolicy() was introduced in Java 7 to
		 * allow the option to remove tasks from work queue if its initial delay hasn't
		 * elapsed -> therfore, if no other tasks are present, the scheduler might go idle earlier
		 * instead of waiting for the initial delay to pass to discover there is nothing to do.
		 * Because the library is currenlty aimed at Java 6, we use a reflection to set this policy
		 * on a Java 7 runtime. 
		 */
		try {
			Method m = scheduler.getClass().getMethod("setRemoveOnCancelPolicy", Boolean.TYPE);
			m.invoke(scheduler, true);
		} catch (InvocationTargetException ex) {
			
		} catch (NoSuchMethodException e) {
		} catch (SecurityException e) {
		} catch (IllegalAccessException e) {
		} catch (IllegalArgumentException e) {
		}
		pool = threadpool;
		scheduledPool = scheduler;
	}
	/**
	 * Creates a default scheduler with the defined normal and scheduled pools.
	 * Note that since ScheduledExecutorService implements ExecutorService, you
	 * may pass in the same ScheduledExecutorService instance to fulfill both purposes.
	 * @param exec the executor service
	 * @param scheduled the scheduled executor service
	 */
	public DefaultScheduler(ExecutorService exec, ScheduledExecutorService scheduled) {
		if (exec == null) {
			throw new IllegalArgumentException("exec is null");
		}
		if (scheduled == null) {
			throw new IllegalArgumentException("scheduled is null");
		}
		this.pool = exec;
		this.scheduledPool = scheduled;
	}
	/**
	 * Creates a default scheduler with the defined normal service and a
	 * default scheduled executor service.
	 * <ul>
	 * <li>ScheduledThreadPoolExecutor
	 * <ul>
	 * <li>1 core thread</li>
	 * <li>1 second idle timeout</li>
	 * <li>core threads may timeout</li>
	 * <li>unbounded worker queue</li>
	 * <li>no rejection handler</li>
	 * <li>if running on Java 7 or above: remove on cancel policy set to true</li>
	 * </ul></li>
	 * </ul>
	 * Note that since ScheduledExecutorService implements ExecutorService, you
	 * may pass in the a ScheduledExecutorService instance to fulfill both purposes.
	 * @param exec the executor service
	 */
	public DefaultScheduler(ExecutorService exec) {
		if (exec == null) {
			throw new IllegalArgumentException("exec is null");
		}
		this.pool = exec;
		ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);
		scheduler.setKeepAliveTime(1, TimeUnit.SECONDS);
		scheduler.allowCoreThreadTimeOut(true);
		
		/* 
		 * the setRemoveOnCancelPolicy() was introduced in Java 7 to
		 * allow the option to remove tasks from work queue if its initial delay hasn't
		 * elapsed -> therfore, if no other tasks are present, the scheduler might go idle earlier
		 * instead of waiting for the initial delay to pass to discover there is nothing to do.
		 * Because the library is currenlty aimed at Java 6, we use a reflection to set this policy
		 * on a Java 7 runtime. 
		 */
		try {
			Method m = scheduler.getClass().getMethod("setRemoveOnCancelPolicy", Boolean.TYPE);
			m.invoke(scheduler, true);
		} catch (InvocationTargetException ex) {
			
		} catch (NoSuchMethodException e) {
		} catch (SecurityException e) {
		} catch (IllegalAccessException e) {
		} catch (IllegalArgumentException e) {
		}
		scheduledPool = scheduler;
	}
	/**
	 * Creates a default scheduler with the defined scheduled pools and the 
	 * default executor service.
	 * <ul>
	 * <li>ThreadPoolExecutor, having
	 * <ul>
	 * <li>1 core thread</li>
	 * <li>1 second idle timeout</li>
	 * <li>core threads may timeout</li>
	 * <li>unbounded worker queue</li>
	 * <li>no rejection handler</li>
	 * </ul>
	 * </li>
	 * </ul>
	 * Note that since ScheduledExecutorService implements ExecutorService, you
	 * may pass in the same ScheduledExecutorService instance to fulfill both purpose.
	 * @param scheduled the scheduled executor service
	 */
	public DefaultScheduler(ScheduledExecutorService scheduled) {
		if (scheduled == null) {
			throw new IllegalArgumentException("scheduled is null");
		}
		ThreadPoolExecutor threadpool = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		threadpool.allowCoreThreadTimeOut(true);
		this.pool = threadpool;
		this.scheduledPool = scheduled;
	}
	/**
	 * Helper class which invokes <code>Future.cancel(true)</code> on
	 * the wrapped future.
	 * @author akarnokd, 2011.02.02.
	 */
	static class FutureCloser implements Closeable {
		/** The wrapped future. */
		final Future<?> future;
		/**
		 * Constructor. 
		 * @param future the future to close
		 */
		FutureCloser(Future<?> future) {
			if (future == null) {
				throw new IllegalArgumentException("future is null");
			}
			this.future = future;
		}
		@Override
		public void close() {
			future.cancel(true);
		}
	}
	@Override
	public Closeable schedule(Runnable run) {
		return new FutureCloser(pool.submit(run));
	}

	@Override
	public Closeable schedule(Runnable run, long delay) {
		return new FutureCloser(scheduledPool.schedule(run, delay, TimeUnit.NANOSECONDS));
	}

	@Override
	public Closeable schedule(Runnable run, long initialDelay, long betweenDelay) {
		return new FutureCloser(scheduledPool.scheduleAtFixedRate(run, initialDelay, betweenDelay, TimeUnit.NANOSECONDS));
	}
	/**
	 * Shutdown both pools.
	 */
	public void shutdown() {
		pool.shutdown();
		scheduledPool.shutdown();
	}
	/**
	 * Shutdown both pools now.
	 * @return the list of runnable tasks awaiting executions in both pools
	 */
	public List<Runnable> shutdownNow() {
		List<Runnable> result = new ArrayList<Runnable>();
		result.addAll(pool.shutdownNow());
		result.addAll(scheduledPool.shutdownNow());
		return result;
	}
}
