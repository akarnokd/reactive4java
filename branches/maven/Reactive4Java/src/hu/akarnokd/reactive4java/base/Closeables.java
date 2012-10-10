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
package hu.akarnokd.reactive4java.base;

import java.io.Closeable;
import java.io.IOException;

import javax.annotation.Nonnull;

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
	 */
	@Nonnull 
	public static Closeable close(
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
	 */
	public static void close0(Closeable c) {
		if (c != null) {
			try {
				c.close();
			} catch (IOException ex) {
				
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
	public static Closeable closeAll(
			@Nonnull final Iterable<? extends Closeable> closeables) {
		return new Closeable() {
			@Override
			public void close() throws IOException {
				for (Closeable c : closeables) {
					close0(c);
				}
			}
		};
	}

}
