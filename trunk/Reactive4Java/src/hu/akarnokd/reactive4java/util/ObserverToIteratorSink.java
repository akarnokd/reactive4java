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

import hu.akarnokd.reactive4java.base.CloseableIterator;
import hu.akarnokd.reactive4java.base.Observer;

import java.io.Closeable;
import java.io.IOException;
import java.util.NoSuchElementException;

import javax.annotation.Nonnull;

/**
 * Base class to help transition from reactive to interactive
 * world.
 * @author akarnokd, 2013.01.12.
 * @since 0.97
 * @param <T> the observed type
 * @param <U> the returned value type
 */
public abstract class ObserverToIteratorSink<T, U> implements Observer<T>,
		CloseableIterator<U> {
	/** Indicate that the stream has finished. */
	protected boolean done;
	/** The original handle to the observer registration. */
	@Nonnull 
	protected final Closeable handle;
	/** The current value. */
	@Nonnull 
	protected final SingleOption<U> current = new SingleOption<U>();
	/**
	 * Constructor, saves the handle.
	 * @param handle the handle to close when the stream finishes.
	 */
	public ObserverToIteratorSink(@Nonnull Closeable handle) {
		this.handle = handle;
	}
	@Override
	public boolean hasNext() {
		if (!done) {
			if (current.isEmpty()) {
				if (!tryNext(current)) {
					done = true;
					Closeables.closeSilently(this);
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public U next() {
		if (hasNext()) {
			if (current.hasError()) {
				done = true;
				Closeables.closeSilently(this);
			}
			return current.take();
		}
		throw new NoSuchElementException();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
	@Override
	public void close() throws IOException {
		handle.close();
	}
	/** Closes this iterator and suppresses exceptions. */
	protected void done() {
		Closeables.closeSilently(this);
	}
	/**
	 * Try to get the next value.
	 * @param out the output where to put the value
	 * @return true if value was available
	 */
	public abstract boolean tryNext(@Nonnull SingleOption<? super U> out);
}
