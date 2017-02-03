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

import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;

import java.io.Closeable;

import javax.annotation.Nonnull;

/**
 * Helper class for Reactive.count operators.
 * @author akarnokd, 2013.01.15.
 * @since 0.97
 */
public final class Count {
	/** Helper class. */
	private Count() { }
	/**
	 * Counts the number of elements in the observable source as long.
	 * @param <T> the element type
	 * @author akarnokd, 2013.01.15.
	 */
	public static final class AsLong<T> implements Observable<Long> {
		/** */
		private final Observable<T> source;

		/**
		 * Constructor.
		 * @param source the source observable
		 */
		public AsLong(Observable<T> source) {
			this.source = source;
		}

		@Override
		@Nonnull 
		public Closeable register(@Nonnull final Observer<? super Long> observer) {
			return source.register(new Observer<T>() {
				/** The counter. */
				long count;
				@Override
				public void error(@Nonnull Throwable ex) {
					observer.error(ex);
				}

				@Override
				public void finish() {
					observer.next(count);
					observer.finish();
				}

				@Override
				public void next(T value) {
					count++;
				}

			});
		}
	}
	/**
	 * Counts the number of elements in the observable source as an int.
	 * @param <T> the element type
	 * @author akarnokd, 2013.01.15.
	 */
	public static final class AsInt<T> implements Observable<Integer> {
		/** */
		private final Observable<T> source;

		/**
		 * Constructor.
		 * @param source the source observable
		 */
		public AsInt(Observable<T> source) {
			this.source = source;
		}

		@Override
		@Nonnull 
		public Closeable register(@Nonnull final Observer<? super Integer> observer) {
			return source.register(new Observer<T>() {
				/** The counter. */
				int count;
				@Override
				public void error(@Nonnull Throwable ex) {
					observer.error(ex);
				}

				@Override
				public void finish() {
					observer.next(count);
					observer.finish();
				}

				@Override
				public void next(T value) {
					count++;
				}

			});
		}
	}

}
