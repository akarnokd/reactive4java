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

import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;
import hu.akarnokd.reactive4java.util.DefaultObserverEx;

import java.io.Closeable;

import javax.annotation.Nonnull;

/**
 * Helper class for containment testing operators.
 * @author akarnokd, 2013.01.13.
 * @since 0.97
 */
public final class Containment {
	/** Helper class. */
	private Containment() { }
	/**
	 * @author akarnokd, 2013.01.13.
	 * @param <T>
	 */
	public static final class Any<T> implements Observable<Boolean> {
		/**
		 * 
		 */
		private final Observable<T> source;
		/**
		 * 
		 */
		private final Func1<? super T, Boolean> predicate;

		/**
		 * @param source
		 * @param predicate
		 */
		public Any(Observable<T> source, Func1<? super T, Boolean> predicate) {
			this.source = source;
			this.predicate = predicate;
		}

		@Override
		@Nonnull
		public Closeable register(@Nonnull final Observer<? super Boolean> observer) {
			DefaultObserverEx<T> obs = new DefaultObserverEx<T>(true) {
				@Override
				public void onError(@Nonnull Throwable ex) {
					observer.error(ex);
					close();
				}

				@Override
				public void onFinish() {
					observer.next(false);
					observer.finish();
					close();
				}

				@Override
				public void onNext(T value) {
					if (predicate.invoke(value)) {
						observer.next(true);
						observer.finish();
						close();
					}
				}

			};
			return obs.registerWith(source);
		}
	}
	/**
	 * @author akarnokd, 2013.01.13.
	 * @param <T>
	 */
	public static final class All<T> implements Observable<Boolean> {
		/**
		 * 
		 */
		private final Func1<? super T, Boolean> predicate;
		/**
		 * 
		 */
		private final Observable<? extends T> source;

		/**
		 * @param predicate
		 * @param source
		 */
		public All(Observable<? extends T> source,
				Func1<? super T, Boolean> predicate) {
			this.predicate = predicate;
			this.source = source;
		}

		@Override
		@Nonnull
		public Closeable register(@Nonnull final Observer<? super Boolean> observer) {
			DefaultObserverEx<T> o = new DefaultObserverEx<T>(true) {
				/** Indicate if we returned early. */
				boolean done;
				@Override
				public void onError(@Nonnull Throwable ex) {
					observer.error(ex);
				}
				@Override
				public void onFinish() {
					if (!done) {
						done = true;
						observer.next(true);
						observer.finish();
					}
				}
				@Override
				public void onNext(T value) {
					if (!predicate.invoke(value)) {
						done = true;
						observer.next(false);
						observer.finish();
					}
				}
			};
			return o.registerWith(source);
		}
	}

}
