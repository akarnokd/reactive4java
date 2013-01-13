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

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

/**
 * A closeable container which closes the previously managed
 * closeable if a new closeable is assigned to it.
 * <p>Closeables assigned to this container after it was
 * closed is ignored and the incoming closeable is closed instantly.</p>
 * <p>The implementation is thread-safe.</p>
 * @author akarnokd, 2013.01.12.
 * @since 0.97
 */
public class SequentialCloseable implements Closeable {
	/** The lock guarding the contents. */
	protected final Lock lock = new ReentrantLock(true);
	/** The current closeable. */
	@GuardedBy("lock")
	protected Closeable current;
	/** Indicate that this container was closed. */
	@GuardedBy("lock")
	protected boolean done;
	@Override
	public void close() throws IOException {
		Closeable c = null;
		lock.lock();
		try {
			if (!done) {
				done = true;
				c = current;
				current = null;
			}
		} finally {
			lock.unlock();
		}
		if (c != null) {
			c.close();
		}
	}
	/**
	 * @return the currently managed closeable
	 */
	@Nullable
	public Closeable get() {
		lock.lock();
		try {
			return current;
		} finally {
			lock.unlock();
		}
	}
	/**
	 * Replaces the current closeable with the new closeable.
	 * The old closeable is closed.
	 * If the container is already closed, the c will
	 * be closed immediately.
	 * @param c the closeable
	 */
	public void set(@Nullable Closeable c) {
		Closeable old = null;
		boolean isDone = false;
		lock.lock();
		try {
			isDone = done;
			if (!done) {
				old = current;
				current = c;
			}
		} finally {
			lock.unlock();
		}
		Closeables.closeSilently(old);
		if (isDone && c != null) {
			Closeables.closeSilently(c);
		}
	}
	/** @return true if this container is already closed. */
	public boolean isClosed() {
		lock.lock();
		try {
			return done;
		} finally {
			lock.unlock();
		}
	}
}
