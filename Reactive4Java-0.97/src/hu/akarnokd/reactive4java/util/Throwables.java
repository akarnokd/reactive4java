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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Utility methods for handling exceptions and throwables.
 * @author akarnokd, 2013.01.11.
 * @since 0.97
 */
public final class Throwables {
	/** Utility class. */
	private Throwables() { }
	/**
	 * Throws the given exception as unchecked by either casting it
	 * to RuntimeException or wrapping it into a RuntimeException, if not null.
	 * @param t the exception to throw as unchecked if not null
	 */
	public static void throwAsUnchecked(@Nullable Throwable t) {
		if (t != null) {
			if (t instanceof RuntimeException) {
				throw (RuntimeException)t;
			}
			throw new RuntimeException(t);
		}
	}
	/**
	 * Sets up the cause for the exception and throws it by either
	 * casting it to RuntimeException or wrapping it into a RuntimeException.
	 * @param t the exception to throw, null means no-op
	 * @param cause the cause for the exception
	 */
	public static void throwAsUncheckedWithCause(@Nullable Throwable t, @Nonnull Throwable cause) {
		if (t != null) {
			t.initCause(cause);
			throwAsUnchecked(t);
		}
	}
}
