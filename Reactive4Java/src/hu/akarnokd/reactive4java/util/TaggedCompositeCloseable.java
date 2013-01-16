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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

/**
 * A composite, modifyable holder of Closeable values with tag objects.
 * The container ensures that once it is closed, attempts to
 * add new closeables will instantly close them.
 * @author akarnokd, 2013.01.14.
 * @since 0.97
 */
public class TaggedCompositeCloseable implements Closeable, Cancelable {
	/** The lock protecting the inner structures. */
	protected final Lock lock;
	/** The sub-observer registration holder. The key is a use-site created object. */
	@GuardedBy("lock")
	@Nonnull 
	protected Map<Object, Closeable> items;
	/** Flag to indicate this item container is closed. */
	@GuardedBy("lock")
	protected boolean done;
	/**
	 * Construct a new instance with a fair reentrant lock.
	 */
	public TaggedCompositeCloseable() {
		this(new ReentrantLock());
	}
	/**
	 * Constructs an instance with the given shared lock.
	 * @param lock the lock to use
	 */
	public TaggedCompositeCloseable(Lock lock) {
		this.lock = lock;
		init();
	}
	/** Initializes the items map. */
	protected void init() {
		items = new HashMap<Object, Closeable>();
	}
	@Override
	public void close() throws IOException {
		List<Closeable> os = null;
		lock.lock();
		try {
			if (!done) {
				done = true;
				os = new ArrayList<Closeable>(items.values());
				// free the items
				init();
			}
		} finally {
			lock.unlock();
		}
		if (os != null) {
			Closeables.close(os);
		}
	}
	/**
	 * Adds or replaces a new closeable by the given token.
	 * If the container is already closed, the supplied
	 * closeable is closed silently immediately.
	 * @param token the token identifying the closeable
	 * @param c the closeable instance
	 */
	public void add(@Nonnull Object token, @Nonnull Closeable c) {
		Closeable old = null;
		boolean shouldClose = false;
		lock.lock();
		try {
			shouldClose = done;
			if (!shouldClose) {
				old = items.put(token, c);
			}
		} finally {
			lock.unlock();
		}
		Closeables.closeSilently(old);
		if (shouldClose) {
			Closeables.closeSilently(c);
		}
	}
	/**
	 * Remove and close the closeable identified by the token.
	 * If the token is not among the items, this method
	 * does nothing.
	 * @param token the token to remove
	 */
	public void remove(@Nonnull Object token) {
		Closeable old = null;
		lock.lock();
		try {
			old = items.remove(token);
		} finally {
			lock.unlock();
		}
		Closeables.closeSilently(old);
	}
	@Override
	public boolean isClosed() {
		lock.lock();
		try {
			return done;
		} finally {
			lock.unlock();
		}
	}
}
