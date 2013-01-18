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

import hu.akarnokd.reactive4java.base.Cancelable;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

/**
 * Represents a closeable resource that only allows closing
 * when all dependent closeable objects have been closed.
 * <p>Can be used in cases where one has to wait for
 * all work items to complete before closing the main handle.</p>
 * @author akarnokd, 2013.01.16.
 * @since 0.97
 */
public class RefCountCloseable implements Closeable, Cancelable {
	/** The lock guarding the internal variables. */
	protected final Lock lock = new ReentrantLock(R4JConfigManager.get().useFairLocks());
	/** The closeable to close when all dependent closeables are closed. */
	@GuardedBy("lock")
	protected Closeable closeable;
	/** Indicate that the primary handle is closed already. */
	@GuardedBy("lock")
	protected boolean primaryClosed;
	/** The number of open dependent closeables. */
	@GuardedBy("lock")
	protected int count;
	/**
	 * Constructor.
	 * @param closeable the closeable to manage
	 */
	public RefCountCloseable(@Nonnull Closeable closeable) {
		this.closeable = closeable;
	}
	@Override
	public boolean isClosed() {
		lock.lock();
		try {
			return closeable == null;
		} finally {
			lock.unlock();
		}
	}
	/**
	 * Returns a new dependent closeable instance.
	 * If all of these dependent closeables have been closed,
	 * the underlying closeable will be closed once.
	 * @return the closeable
	 */
	public Closeable getCloseable() {
		lock.lock();
		try {
			if (closeable == null) {
				return Closeables.emptyCloseable();
			}
			count++;
			return new InnerCloseable(this);
		} finally {
			lock.unlock();
		}
	}
	@Override
	public void close() throws IOException {
		Closeable c = null;
		lock.lock();
		try {
			if (closeable != null) {
				if (primaryClosed) {
					primaryClosed = true;
					if (count == 0) {
						c = closeable;
						closeable = null;
					}
				}
			}
		} finally {
			lock.unlock();
		}
		if (c != null) {
			c.close();
		}
	}
	/** 
	 * Releases an dependent closeable.
	 * @throws IOException the propagated close exception 
	 */
	protected void release() throws IOException {
		Closeable c = null;
		lock.lock();
		try {
			if (closeable != null) {
				count--;
				if (primaryClosed) {
					if (count == 0) {
						c = closeable;
						closeable = null;
					}
				}
			}
		} finally {
			lock.unlock();
		}
		if (c != null) {
			c.close();
		}
	}
	/**
	 * The dependent closeable implementation.
	 * @author akarnokd, 2013.01.16.
	 */
	protected static class InnerCloseable implements Closeable {
		/** The parent instance. */
		protected AtomicReference<RefCountCloseable> parent;
		/**
		 * Constructor.
		 * @param parent the parent instance.
		 */
		public InnerCloseable(RefCountCloseable parent) {
			this.parent = new AtomicReference<RefCountCloseable>(parent);
		}
		@Override
		public void close() throws IOException {
			RefCountCloseable p = parent.getAndSet(null);
			if (p != null) {
				p.release();
			}
		}
	}
}
