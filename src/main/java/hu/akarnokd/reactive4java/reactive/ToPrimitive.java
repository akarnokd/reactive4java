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

import hu.akarnokd.reactive4java.base.DoubleObservable;
import hu.akarnokd.reactive4java.base.DoubleObserver;
import hu.akarnokd.reactive4java.base.IntObservable;
import hu.akarnokd.reactive4java.base.IntObserver;
import hu.akarnokd.reactive4java.base.LongObservable;
import hu.akarnokd.reactive4java.base.LongObserver;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;

import java.io.Closeable;

import javax.annotation.Nonnull;

/**
 * Helper class for object-to-primitive conversion observables.
 * @author akarnokd, 2013.01.15.
 * @since 0.97
 */
public final class ToPrimitive {
	/** Helper class. */
	private ToPrimitive() {
		// TODO Auto-generated constructor stub
	}
	/**
	 * Converts an object-double to primitive-double observable sequence.
	 * @author akarnokd, 2013.01.15.
	 */
	public static final class ToDouble implements DoubleObservable {
		/** */
		private final Observable<Double> source;

		/**
		 * Constructor.
		 * @param source the object-double sequence
		 */
		public ToDouble(Observable<Double> source) {
			this.source = source;
		}

		@Override
		@Nonnull
		public Closeable register(@Nonnull final DoubleObserver observer) {
			return source.register(new Observer<Double>() {
				@Override
				public void error(@Nonnull Throwable ex) {
					observer.error(ex);
				}

				@Override
				public void finish() {
					observer.finish();
				}

				@Override
				public void next(Double value) {
					observer.next(value);
				}
				
			});
		}
	}
	/**
	 * Converts an object-long to primitive-long observable sequence.
	 * @author akarnokd, 2013.01.15.
	 */
	public static final class ToLong implements LongObservable {
		/** */
		private final Observable<Long> source;

		/**
		 * Constructor.
		 * @param source the long-object sequence
		 */
		public ToLong(Observable<Long> source) {
			this.source = source;
		}

		@Override
		@Nonnull
		public Closeable register(@Nonnull final LongObserver observer) {
			return source.register(new Observer<Long>() {
				@Override
				public void error(@Nonnull Throwable ex) {
					observer.error(ex);
				}

				@Override
				public void finish() {
					observer.finish();
				}

				@Override
				public void next(Long value) {
					observer.next(value);
				}
				
			});
		}
	}
	/**
	 * Converts an object-int to primitive-int observable sequence.
	 * @author akarnokd, 2013.01.15.
	 */
	public static final class ToInt implements IntObservable {
		/** */
		private final Observable<Integer> source;

		/**
		 * Constructor.
		 * @param source the object-int sequence
		 */
		public ToInt(Observable<Integer> source) {
			this.source = source;
		}

		@Override
		@Nonnull
		public Closeable register(@Nonnull final IntObserver observer) {
			return source.register(new Observer<Integer>() {
				@Override
				public void error(@Nonnull Throwable ex) {
					observer.error(ex);
				}

				@Override
				public void finish() {
					observer.finish();
				}

				@Override
				public void next(Integer value) {
					observer.next(value);
				}
				
			});
		}
	}

}
