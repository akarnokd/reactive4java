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
package hu.akarnokd.reactive4java.base;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Utility methods for various closeable objects.
 * @author akarnokd, 2011.10.26.
 * @since 0.95
 */
public final class Closeables {
	/** Utility class. */
	private Closeables() {
		// utility class
	}
	/** An empty closeable. */
	private static final Closeable EMPTY_CLOSEABLE = new Closeable() {
		@Override
		public void close() throws IOException {
			// NO OP
		}
	};
	/**
	 * Returns an empty closeable object which does nothing. 
	 * @return an empty, no-op closeable instance. 
	 */
	public static Closeable emptyCloseable() {
		return EMPTY_CLOSEABLE;
	}
	/**
	 * Wraps two or more closeables into one closeable.
	 * <code>IOException</code>s thrown from the closeables are suppressed.
	 * @param c0 the first closeable
	 * @param c1 the second closeable
	 * @param closeables the rest of the closeables
	 * @return the composite closeable
	 * @since 0.97
	 */
	@Nonnull 
	public static Closeable newCloseable(
			@Nonnull final Closeable c0, 
			@Nonnull final Closeable c1, 
			@Nonnull final Closeable... closeables) {
		return new Closeable() {
			@Override
			public void close() throws IOException {
				try {
					c0.close();
				} catch (IOException ex) {
					
				}
				try {
					c1.close();
				} catch (IOException ex) {
					
				}
				for (Closeable c : closeables) {
					try {
						c.close();
					} catch (IOException ex) {
						
					}
				}
			}
		};
	}
	/**
	 * Invoke the <code>close()</code> method on the closeable instance
	 * and throw away any <code>IOException</code> it might raise.
	 * @param c the closeable instance, <code>null</code>s are simply ignored
	 * @since 0.97
	 */
	public static void closeSilently(Closeable c) {
		if (c != null) {
			try {
				c.close();
			} catch (IOException ex) {
				// ignored
			}
		}
	}
	/**
	 * Creates a composite closeable from the array of closeables.
	 * <code>IOException</code>s thrown from the closeables are suppressed.
	 * @param closeables the closeables array
	 * @return the composite closeable
	 */
	@Nonnull 
	public static Closeable newCloseable(
			@Nonnull final Iterable<? extends Closeable> closeables) {
		return new Closeable() {
			@Override
			public void close() throws IOException {
				for (Closeable c : closeables) {
					closeSilently(c);
				}
			}
		};
	}
	/**
	 * Closes the given object if it implements the Closeable interface.
	 * @param o an object
	 * @since 0.97
	 */
	public static void closeSilently(@Nullable Object o) {
		if (o instanceof Closeable) {
			closeSilently((Closeable)o);
		}
	}
	/**
	 * Closes the given array of closeables and returns a MultiIOException
	 * of the thrown exceptions.
	 * @param closeables the array of closeables
	 * @throws MultiIOException if one or more close() calls threw 
	 */
	public static void close(Closeable... closeables) throws MultiIOException {
		close(Arrays.asList(closeables));
	}
	/**
	 * Closes the given sequence of closeables and returns a MultiIOException
	 * of the thrown exceptions.
	 * @param closeables the sequence of closeables
	 * @throws MultiIOException if one or more close() calls threw 
	 */
	public static void close(Iterable<? extends Closeable> closeables) throws MultiIOException {
		MultiIOException xout = null;
		for (Closeable c : closeables) {
			try {
				c.close();
			} catch (IOException ex) {
				xout = MultiIOException.createOrAdd(xout, ex);
			}
		}
		if (xout != null) {
			throw xout;
		}
	}
	/**
	 * If the target object implements Closeable, this method calls
	 * it, otherwise its a no-op.
	 * @param o the object to close
	 * @throws IOException the exception thrown by the close() method
	 */
	public static void close(Object o) throws IOException {
		if (o instanceof Closeable) {
			((Closeable)o).close();
		}
	}
}
