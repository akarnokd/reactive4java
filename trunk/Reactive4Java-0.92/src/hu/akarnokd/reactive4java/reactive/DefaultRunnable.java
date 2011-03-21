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
package hu.akarnokd.reactive4java.reactive;

import java.util.concurrent.CancellationException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;

/**
 * A default runnable implementation which may
 * use an internal or external lock to synchronize
 * access to its alternative onRun method. 
 * FIXME might need an option to share a completed flag?!
 * @author akarnokd, 2011.02.06.
 */
public abstract class DefaultRunnable implements Runnable {
	/** The lock protecting the runnable. */
	protected final Lock lock;
	/**
	 * Creates a new instance with a fair reentrant lock.
	 */
	public DefaultRunnable() {
		this(new ReentrantLock(true));
	}
	/**
	 * Creates a new instance with the supplied lock.
	 * @param lock the lock object 
	 */
	public DefaultRunnable(@Nonnull final Lock lock) {
		if (lock == null) {
			throw new IllegalArgumentException("lock is null");
		}
		this.lock = lock;
	}
	@Override
	public final void run() {
		lock.lock();
		try {
			if (!cancelled()) {
				onRun();
			}
		} finally {
			lock.unlock();
		}
	}
	/**
	 * @return returns the current thread's interrupted state, which indicate
	 * a cancel request from an outside source.
	 */
	public boolean cancelled() {
		return Thread.currentThread().isInterrupted();
	}
	/**
	 * An internal cancel method which ensures that if
	 * this runnable is within a periodic schedule, it won't execute the next time.
	 * <p>Note that this method throws a <code>CancellationException</code> which, when
	 * propagated back to the scheduler pool, will disable the wrapper task.
	 * Therefore, ensure that you do not accidentally catch this exception by a
	 * <code>catch (Throwable ex)</code> clause.</p>
	 */
	protected void cancel() {
		throw new CancellationException(); // FIXME there seems to be no more elegant way than this 
	}
	/**
	 * The alternative run method, which will be called by the original run() method under lock.
	 */
	protected abstract void onRun();


}
