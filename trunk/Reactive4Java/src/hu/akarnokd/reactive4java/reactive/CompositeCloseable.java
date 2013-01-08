/*
 * Copyright 2011-2012 David Karnok
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

import hu.akarnokd.reactive4java.base.Closeables;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

/**
 * A composite closeable which maintains a list of sub-closeables and has its
 * own alive state. If the composite is closed, any maintained closeable is
 * closed and any subsequently added closeable will be closed instantly.
 * @author akarnokd, 2013.01.07.
 * @since 0.97
 */
public class CompositeCloseable implements Closeable {
	/** The lock guarding the items and done. */
	protected final Lock lock = new ReentrantLock();
	/** The list of items. */
	@GuardedBy("lock")
	protected final List<Closeable> items;
	/** The done indicator. */
	@GuardedBy("lock")
	protected boolean done;
	/**
	 * Constructs a composite closeable where the backing list has the
	 * supplied capacity.
	 * @param capacity the capacity value >= 1
	 */
	public CompositeCloseable(int capacity) {
		items = new ArrayList<Closeable>(capacity);
	}
	/**
	 * Constructs the composite closeable with the supplied initial elements.
	 * @param closeables the closeable elements to start with
	 */
	public CompositeCloseable(@Nonnull Closeable... closeables) {
		this(Arrays.asList(closeables));
	}
	/**
	 * Constructs the composite closeable with the supplied initial elements.
	 * @param closeables the closeable elements to start with
	 */
	public CompositeCloseable(@Nonnull Iterable<? extends Closeable> closeables) {
		items = new ArrayList<Closeable>();
		for (Closeable c : closeables) {
			items.add(c);
		}
	}
	@Override
	public void close() throws IOException {
		boolean closeOnce = false;
		List<Closeable> itemsOnce = null;
		lock.lock();
		try {
			if (!done) {
				done = true;
				closeOnce = true;
				itemsOnce = new ArrayList<Closeable>(items);
				items.clear();
			}
		} finally {
			lock.unlock();
		}
		if (closeOnce) {
			Closeables.close(itemsOnce);
		}
	}
	/**
	 * Adds new closeables to this composite object.
	 * If the composite is already closed, the closeables are immediately closed.
	 * @param closeables the array of closeables
	 */
	public void add(Closeable... closeables) {
		add(Arrays.asList(closeables));
	}
	/**
	 * Adds new closeables to this composite object.
	 * If the composite is already closed, the closeables are immediately closed.
	 * Close exceptions are silently ignored.
	 * @param closeables the sequence of closeables
	 */
	public void add(Iterable<? extends Closeable> closeables) {
		boolean isDone = false;
		lock.lock();
		try {
			isDone = done;
			if (!done) {
				for (Closeable c : closeables) {
					items.add(c);
				}
			}
		} finally {
			lock.unlock();
		}
		if (isDone) {
			Closeables.closeSilently(closeables);
		}
	}
	/** @return test if this closeable is already closed. */
	public boolean isClosed() {
		lock.lock();
		try {
			return done;
		} finally {
			lock.unlock();
		}
	}
	/**
	 * @return Returns the currently maintained number of closeables.
	 */
	public int size() {
		lock.lock();
		try {
			return items.size();
		} finally {
			lock.unlock();
		}
	}
	/**
	 * Removes and closes the specified closeable instance if contained within this composite.
	 * @param c the closeable
	 * @return true if the instance was removed
	 * @throws IOException if the close method threw it
	 */
	public boolean remove(Closeable c) throws IOException {
		boolean shouldClose = false;
		if (c != null) {
			lock.lock();
			try {
				shouldClose = items.remove(c);
			} finally {
				lock.unlock();
			}
			if (shouldClose) {
				c.close();
			}
		}
		return shouldClose;
	}
	/**
	 * Removes the given closeable from this composite but does not close it.
	 * @param c the closeable instance
	 * @return true if the closeable was in this container
	 */
	public boolean delete(Closeable c) {
		lock.lock();
		try {
			return items.remove(c);
		} finally {
			lock.unlock();
		}
	}
	/**
	 * Closes the inner closeables but does not  close this composite.
	 * @throws IOException the exception(s) thrown by the close() calls
	 */
	public void clear() throws IOException {
		List<Closeable> itemsOnce = null;
		lock.lock();
		try {
			itemsOnce = new ArrayList<Closeable>(items);
			items.clear();
		} finally {
			lock.unlock();
		}
		Closeables.close(itemsOnce);
	}
	/**
	 * Returns true if the given closeable is in this composite.
	 * @param c the closeable to test
	 * @return true if is in this closeable
	 */
	public boolean contains(Closeable c) {
		lock.lock();
		try {
			return items.contains(c);
		} finally {
			lock.unlock();
		}
	}
	/**
	 * Close this composite and ignore any IOExceptions.
	 */
	public void closeSilently() {
		Closeables.closeSilently(this);
	}
}
