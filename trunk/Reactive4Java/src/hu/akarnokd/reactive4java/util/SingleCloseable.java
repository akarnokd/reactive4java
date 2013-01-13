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
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents a closeable which allows only a single wrapped
 * closeable instance.
 * Once the wrapped closeable is set, any further attepmt
 * will throw an IllegalStateException.
 * <p>This class might help in situations where decoupling closeables
 * allows the referenced objects to be collected earlier by a GC.</p>
 * <p>The implementation is thread-safe.</p>
 * @author akarnokd, 2013.01.12.
 * @since 0.97
 */
public class SingleCloseable implements Closeable {
	/** The reference holder. */
	@Nonnull 
	protected final AtomicReference<Closeable> current = new AtomicReference<Closeable>();
	@Override
	public void close() throws IOException {
		Closeable old = current.getAndSet(SENTINEL);
		if (old != null) {
			old.close();
		}
	}
	/**
	 * Sets the managed closeable if not already set.
	 * Throws an IllegalStateException if this instance
	 * already manages a closeable.
	 * If this instance is already closed, setting
	 * a new closeable will immediately close it
	 * @param c the closeable to handle
	 */
	public void set(@Nonnull Closeable c) {
		Closeable old = Atomics.compareExchange(current, null, c);
		if (old == null) {
			return;
		}
		if (old != SENTINEL) {
			throw new IllegalStateException("SingleCloseable already assigned");
		}
		if (c != null) {
			Closeables.closeSilently(c);
		}
	}
	/**
	 * @return returns the managed closeable
	 */
	@Nullable
	public Closeable get() {
		Closeable c = current.get();
		// don't leak the sentinel
		if (c == SENTINEL) {
			return Closeables.emptyCloseable();
		}
		return c;
	}
	/** @return true if this closeable is closed. */
	public boolean isClosed() {
		return current.get() == SENTINEL;
	}
	/** 
	 * The empty sentinel to know when we have
	 * closed the previous instance and keep the 
	 * invariants of this SingleCloseable.
	 */
	@Nonnull 
	protected static final Closeable SENTINEL = new Closeable() {
		@Override
		public void close() throws IOException { }
	};
}
