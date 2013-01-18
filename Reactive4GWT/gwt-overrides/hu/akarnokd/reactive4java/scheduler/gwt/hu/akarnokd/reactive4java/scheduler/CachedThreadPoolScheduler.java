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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

//#GWT-IGNORE-FILE

/**
 * The scheduler uses a cached thread pool executor (via {@link Executors#newCachedThreadPool()}) as its backing thread pool.
 * Delayed executions are performed via the TimeUnit.sleep() methods on the pool thread (consuming resources).
 * To stop a repeating schedule, throw a CancellationException.
 * @author akarnokd, 2011.10.05.
 * @since 0.95
 */
public class CachedThreadPoolScheduler 

extends DefaultScheduler {






































































}
