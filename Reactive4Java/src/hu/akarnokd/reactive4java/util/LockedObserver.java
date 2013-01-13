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
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;

/**
 * Class to wrap an observer's method calls via a lock to prevent
 * concurrent callback of its methods.
 * The class implements the Closeable interface and forwards
 * this close call to the wrapped observer, if it supports 
 * Closeable as well (otherwise, its no-op).
 * 
 * @author akarnokd, 2013.01.09.
 * @param <T> the element type
 * @since 0.97
 */
public class LockedObserver<T> implements Observer<T>, Closeable {
	/** The wrapped observer. */
	protected final Observer<T> observer;
	/** The lock object. */
	protected final Lock lock;
	/**
	 * Constructor, sets the wrapped observer and uses a fair ReentrantLock.
	 * @param o the observer
	 */
	public LockedObserver(@Nonnull Observer<T> o) {
		this(o, new ReentrantLock(true));
	}
	/**
	 * Constructor, sets the wrapped observer and uses the provided lock.
	 * @param o the observer
	 * @param l the lock
	 */
	public LockedObserver(@Nonnull Observer<T> o, @Nonnull Lock l) {
		this.observer = o;
		this.lock = l;
	}
	/** @return the lock object */
	public Lock getLock() {
		return lock;
	}
	@Override
	public void close() throws IOException {
		lock.lock();
		try {
			Closeables.close(observer);
		} finally {
			lock.unlock();
		}
	}
	@Override
	public void next(T value) {
		lock.lock();
		try {
			observer.next(value);
		} finally {
			lock.unlock();
		}
	}
	@Override
	public void error(@Nonnull Throwable ex) {
		lock.lock();
		try {
			observer.error(ex);
		} finally {
			lock.unlock();
		}
	}
	@Override
	public void finish() {
		lock.lock();
		try {
			observer.finish();
		} finally {
			lock.unlock();
		}
	}
}
