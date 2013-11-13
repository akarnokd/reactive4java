/*
 * Copyright 2013 akarnokd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hu.akarnokd.reactive4java8.base;

import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**
 * The default implementation of the Scheduler inferface
 * which is mapped to the ScheduledThreadPoolExecutor.
 * <p>Calling the close on the scheduler shuts down the underlying pool.</p>
 * 
 * @author akarnokd, 2013.11.09.
 */
public class DefaultScheduler implements Scheduler, Registration {
    private final ScheduledThreadPoolExecutor pool;
    /**
     * Constructor, creates a scheduled thread pool which
     * the available number of processors and 1 second timeout.
     */
    public DefaultScheduler() {
        this(new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors()));
        pool.setRemoveOnCancelPolicy(true);
        pool.setKeepAliveTime(1, TimeUnit.SECONDS);
    }
    /**
     * Constructor, creates a scheduled thread pool with the
     * given number of core pool threads.
     * @param corePoolSize the core pool size 
     */
    public DefaultScheduler(int corePoolSize) {
        this(new ScheduledThreadPoolExecutor(corePoolSize));
        pool.setRemoveOnCancelPolicy(true);
        pool.setKeepAliveTime(1, TimeUnit.SECONDS);
    }
    /**
     * Constructor, uses the specified scheduled thread pool.
     * @param pool the pool to use
     */
    public DefaultScheduler(ScheduledThreadPoolExecutor pool) {
        this.pool = Objects.requireNonNull(pool);
    }
    @Override
    public Registration schedule(Runnable run) {
        Future<?> f = pool.schedule(run, 0, TimeUnit.SECONDS);
        return () -> { f.cancel(true); };
    }

    @Override
    public Registration schedule(long initialDelay, long period, TimeUnit unit, Runnable run) {
        Future<?> f = pool.scheduleAtFixedRate(run, initialDelay, period, unit);
        return () -> { f.cancel(true); };
    }

    @Override
    public Registration schedule(long time, TimeUnit unit, Runnable run) {
        Future<?> f = pool.schedule(run, time, unit);
        return () -> { f.cancel(true); };
    }

    @Override
    public void close() {
        pool.shutdown();
    }
    
}
