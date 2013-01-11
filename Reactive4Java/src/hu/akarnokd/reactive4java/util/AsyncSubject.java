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
import hu.akarnokd.reactive4java.base.Option;
import hu.akarnokd.reactive4java.base.Subject;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

/**
 * Represents the result (last value or exception) of an asynchronous operation
 * delivered through observation.
 * @author akarnokd, 2013.01.10.
 * @since 0.97
 * @param <T> the element type
 */
public class AsyncSubject<T> implements Subject<T, T> {
	/** The lock. */
	protected final Lock lock;
	/** Indication that a value was produced. */
	@GuardedBy("lock")
	protected boolean hasValue;
	/** The last value produced. */
	@GuardedBy("lock")
	protected T value;
	/** The exception produced. */
	@GuardedBy("lock")
	protected Throwable error;
	/** Indicator that the source finished. */
	@GuardedBy("lock")
	protected boolean done;
	/** The map of registered observers. */
	@GuardedBy("lock")
	protected Map<Closeable, Observer<? super T>> observers = new HashMap<Closeable, Observer<? super T>>();
	/**
	 * Creates an AsyncSubject with a fair reentrant lock.
	 */
	public AsyncSubject() {
		this(new ReentrantLock(true));
	}
	/**
	 * Creates an AsyncSubject with the supplied lock.
	 * @param lock the lock to use
	 */
	public AsyncSubject(Lock lock) {
		this.lock = lock;
	}

	@Override
	public void error(Throwable ex) {
		Map<Closeable, Observer<? super T>> os = null;
		lock.lock();
		try {
			if (!done) {
				os = this.observers;
				this.observers = new HashMap<Closeable, Observer<? super T>>();
				done = true;
				this.error = ex;
			}
		} finally {
			lock.unlock();
		}
		if (os != null) {
			for (Observer<? super T> o : os.values()) {
				o.error(ex);
			}
		}
	}
	@Override
	public void finish() {
		Map<Closeable, Observer<? super T>> os = null;
		lock.lock();
		T v = null;
		boolean hv = false;
		try {
			if (!done) {
				os = this.observers;
				this.observers = new HashMap<Closeable, Observer<? super T>>();
				done = true;
				v = this.value;
				hv = this.hasValue;
			}
		} finally {
			lock.unlock();
		}
		if (os != null) {
			for (Observer<? super T> o : os.values()) {
				if (hv) {
					o.next(v);
				}
				o.finish();
			}
		}
	}
	@Override
	public void next(T value) {
		lock.lock();
		try {
			if (!done) {
				this.value = value;
				this.hasValue = true;
			}
		} finally {
			lock.unlock();
		}
	}
	@Override
	@Nonnull
	public Closeable register(@Nonnull final Observer<? super T> observer) {
		T v = null;
		boolean hv = false;
		Throwable ex = null;
		
		lock.lock();
		try {
			if (!done) {
				Closeable handle = new Closeable() {
					@Override
					public void close() throws IOException {
						unregister(this);
					}
				};
				observers.put(handle, observer);
				return handle;
			}
			ex = error;
			v = value;
			hv = hasValue;
		} finally {
			lock.unlock();
		}
		if (ex != null) {
			observer.error(ex);
		} else
		if (hv) {
			observer.next(v);
		}
		observer.finish();
		
		return Closeables.emptyCloseable();
	}
	/**
	 * Remove the registration of the observable identified by the closeable.
	 * @param c the registration token to remove
	 */
	protected void unregister(Closeable c) {
		lock.lock();
		try {
			observers.remove(c);
		} finally {
			lock.unlock();
		}
	}
	/**
	 * @return true if the asynchronous operation is complete
	 */
	public boolean isDone() {
		lock.lock();
		try {
			return done;
		} finally {
			lock.unlock();
		}
	}
	/**
	 * Returns the results of the asynchronous operation in the form
	 * of an option, blocking and waiting indefinitely if necessary.
	 * @return the option value
	 * @throws InterruptedException in case the wait was interrupted 
	 */
	@Nonnull
	public Option<T> getOption() throws InterruptedException {
		if (!isDone()) {
			final CountDownLatch latch = new CountDownLatch(1);
			register(Observers.newAsyncAwaiter(latch));
			latch.await();
		}
		lock.lock();
		try {
			if (error != null) {
				return Option.error(error);
			} else
			if (hasValue) {
				return Option.some(value);
			}
			return Option.none();
		} finally {
			lock.unlock();
		}
	}
	/**
	 * Returns the results of the asynchronous operation in the form
	 * of an option, waiting at most the supplied amount of time,
	 * returning null if timeout occurred.
	 * @param time the time value
	 * @param unit the time unit
	 * @return the option value if the operation completed within time or null
	 * if timeout happened.
	 * @throws InterruptedException in case the wait was interrupted 
	 */
	public Option<T> getOption(long time, TimeUnit unit) throws InterruptedException {
		if (!isDone()) {
			final CountDownLatch latch = new CountDownLatch(1);
			register(Observers.newAsyncAwaiter(latch));
			if (!latch.await(time, unit)) {
				return null;
			}
		}
		lock.lock();
		try {
			if (error != null) {
				return Option.error(error);
			} else
			if (hasValue) {
				return Option.some(value);
			}
			return Option.none();
		} finally {
			lock.unlock();
		}
	}
	/**
	 * Returns the result of the asynchronous operation, throws
	 * a RuntimeException if the operation had errors, or throws NoSuchElementException if no result
	 * was produced, waiting indefinitely if necessary.
	 * @return the result
	 * @throws InterruptedException in case the wait was interrupted 
	 */
	public T get() throws InterruptedException {
		Option<T> result = getOption();
		if (Option.isNone(result)) {
			throw new NoSuchElementException();
		}
		return result.value();
	}
	/**
	 * Returns the result of the asynchronous operation, throws
	 * its exception, throws NoSuchElementException if no result
	 * was produced or throws TimeoutException in case of timeout, waiting at most the specified time.
	 * @param time the time to wait
	 * @param unit the time unit
	 * @return the result
	 * @throws InterruptedException in case the wait was interrupted 
	 * @throws TimeoutException in case the wait timed out
	 */
	public T get(long time, TimeUnit unit) throws InterruptedException, TimeoutException {
		Option<T> result = getOption(time, unit);
		if (Option.isNone(result)) {
			throw new NoSuchElementException();
		}
		if (result != null) {
			return result.value();
		}
		throw new TimeoutException();
	}
}
