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

/**
 * An abstract interface for defining
 * minimum scheduling capabilities.
 * @author akarnokd, 2011.02.02.
 */
public interface Scheduler {
	/** 
	 * Schedule for ASAP execution.
	 * @param run the runnable task
	 * @return the cancel handler 
	 */
	Closeable schedule(Runnable run);
	/**
	 * Schedule a single execution of the runnable task
	 * with the given delay of nanoseconds.
	 * FIXME GWT probably doesn't support the TimeUnit enum
	 * @param run the task to run
	 * @param delay the initial delay in nanoseconds, 
	 * implementations might not have the capability to 
	 * schedule in this resolution
	 * @return the cancel handler
	 */
	Closeable schedule(Runnable run, long delay);
	/**
	 * Schedule a repeaded execution of the given task with
	 * the given initialDelay (in nanoseconds) and betweenDelay
	 * (in nanoseconds). The expected semantics from the scheduler
	 * is to run the task at a fixed rate 
	 * (i.e., <code>ExecutorService.scheduleAtFixedRate()</code>).
	 * Note: Implementations might not have the capability to 
	 * schedule nanosecond resolution.
	 * @param run the task to run
	 * @param initialDelay the initial delay before the first run
	 * @param betweenDelay the delay between task runs after the 
	 * @return the cancel handler
	 */
	Closeable schedule(Runnable run, long initialDelay, long betweenDelay);
}
