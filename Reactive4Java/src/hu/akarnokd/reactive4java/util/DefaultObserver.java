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

import hu.akarnokd.reactive4java.base.Observer;

import java.io.Closeable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;

/**
 * The default implementation of the Observer interface
 * used by the Reactive operators.
 * <p>This implementation ensures that its onNext, onError and onFinish methods
 * are never interleaved and won't be executed after an error or finish message.
 * In addition, a Throwable thrown from the onNext() method is routed
 * through the error() method.</p>
 * <p>The close method is idempotent and can be called multiple times.</p>
 * <p>Extend this class when you need something more than a decorator or
 * filter around some Observer passed to the custom register method, e.g.,
 * observers with multiple sub-observers registered to multiple sources or
 * observers with an accompanied scheduled task.
 * @author akarnokd, 2011.01.29.
 * @param <T> the element type to observe
 */
public abstract class DefaultObserver<T> implements Observer<T>, Closeable {
	/** The lock that ensures sequential and exclusive runs for the observer's methods. */
	@Nonnull
	protected final Lock lock;
	/** The completion flag, it will be set by the close method once. */
	@javax.annotation.concurrent.GuardedBy("lock")
	protected boolean completed;
	/** Should the observer close() itself on error or finish()? */
	protected final boolean closeOnTermination;
	/**
	 * Constructor. Initializes the class with a fair reentrant lock.
	 * @param complete should set the completion status on an error or finish?
	 */
	public DefaultObserver(boolean complete) {
		this(new ReentrantLock(R4JConfigManager.get().useFairLocks()), complete);
	}
	/**
	 * Constructor. Initializes the class with a shared lock instance.
	 * @param lock the lock instance, nonnul
	 * @param complete should set the completion status on an error or finish?
	 */
	public DefaultObserver(@Nonnull final Lock lock, boolean complete) {
		if (lock == null) {
			throw new IllegalArgumentException("lock is null");
		}
		this.closeOnTermination = complete;
		this.lock = lock;
	}
	/**
	 * The alternative next() method, which is called by the original next() method
	 * under lock.
	 * @param value the value
	 */
	protected abstract void onNext(T value);
	/**
	 * The alternative error() method, which is called by the original error() method.
	 * @param ex the exception
	 */
	protected abstract void onError(@Nonnull Throwable ex);
	/** The alternative finish() method, which is called by the original finish() method. */
	protected abstract void onFinish();
	/**
	 * Called by close to close down any associated resources with this instance.
	 * <p>The <code>close()</code> method ensures that the lock is held this code executes.</p>
	 */
	protected void onClose() {

	}
	@Override
	public final void next(T value) {
		lock.lock();
		try {
			if (!completed) {
				try {
					onNext(value);
				} catch (Throwable t) {
					error(t);
				}
			}
		} finally {
			lock.unlock();
		}
	}
	@Override
	public final void close() {
		lock.lock();
		try {
			if (!completed) {
				completed = true;
				onClose(); // FIXME is there a deadlock possibility?
			}
		} finally {
			lock.unlock();
		}
	}
	@Override
	public final void error(@Nonnull Throwable ex) {
		lock.lock();
		try {
			if (!completed) {
				try {
					onError(ex);
				} finally {
					if (closeOnTermination) {
						close();
					}
				}
			}
		} finally {
			lock.unlock();
		}
	}
	@Override
	public final void finish() {
		lock.lock();
		try {
			if (!completed) {
				try {
					onFinish();
				} finally {
					if (closeOnTermination) {
						close();
					}
				}
			}
		} finally {
			lock.unlock();
		}
	}
	/**
	 * Returns the internal lock which might be
	 * shared among multiple instances.
	 * @return the lock
	 */
	public Lock getLock() {
		return lock;
	}
	/** @return the completion status. */
	public boolean isCompleted() {
		lock.lock();
		try {
			return completed;
		} finally {
			lock.unlock();
		}
	}
}
