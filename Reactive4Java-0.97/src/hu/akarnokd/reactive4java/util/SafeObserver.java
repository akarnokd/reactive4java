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

import javax.annotation.Nonnull;

/**
 * A simple wrapping observer which closes the specified
 * handle in case of error or finish message, or
 * an exception raised by the wrapped next.
 * @author akarnokd, 2013.01.16.
 * @since 0.97
 * @param <T> the element type
 */
public class SafeObserver<T> implements Observer<T> {
	/** The observer. */
	protected final Observer<? super T> o;
	/** The close handle. */
	protected final Closeable handle;
	/**
	 * Constructor.
	 * @param o the observer to wrap
	 * @param handle the close handle
	 */
	public SafeObserver(@Nonnull Observer<? super T> o, @Nonnull Closeable handle) {
		this.o = o;
		this.handle = handle;
	}

	@Override
	public void error(@Nonnull Throwable ex) {
		try {
			o.error(ex);
		} finally {
			Closeables.closeSilently(handle);
		}
	}

	@Override
	public void finish() {
		try {
			o.finish();
		} finally {
			Closeables.closeSilently(handle);
		}
	}

	@Override
	public void next(T value) {
		boolean success = false;
		try {
			o.next(value);
			success = true;
		} finally {
			if (!success) {
				Closeables.closeSilently(handle);
			}
		}
	}

}
