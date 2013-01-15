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
package hu.akarnokd.reactive4java.reactive;

import java.io.Closeable;

import javax.annotation.Nonnull;

import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;

/**
 * Helper class for where like operations.
 * @author akarnokd, 2013.01.15.
 * @since 0.97
 */
public final class Where {
	/** Helper class. */
	private Where() { }
	/**
	 * Filters the elements of the source sequence which
	 * is assignable to the provided type.
	 * @author akarnokd, 2013.01.15.
	 * @param <T> the target element type
	 */
	public static class OfType<T> implements Observable<T> {
		/** */
		private final Observable<?> source;
		/** */
		private final Class<T> clazz;
		/**
		 * Constructor.
		 * @param source the source sequence
		 * @param clazz the class token
		 */
		public OfType(@Nonnull Observable<?> source, @Nonnull Class<T> clazz) {
			this.source = source;
			this.clazz = clazz;
			
		}
		@Override
		@Nonnull
		public Closeable register(final Observer<? super T> observer) {
			return source.register(new Observer<Object>() {

				@Override
				public void next(Object value) {
					if (clazz.isInstance(value)) {
						observer.next(clazz.cast(value));
					}
				}

				@Override
				public void error(Throwable ex) {
					observer.error(ex);
				}

				@Override
				public void finish() {
					observer.finish();
				}
				
			});
		}
	}
}
