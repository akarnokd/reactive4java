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

import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;
import hu.akarnokd.reactive4java.base.Subject;

import java.io.Closeable;

import javax.annotation.Nonnull;

/**
 * Utility class to wrap existing pairs of Observers and Observables under the Subject
 * interface.
 * @author akarnokd, 2013.01.09.
 * @since 0.97
 */
public final class Subjects {
	/** Utility class. */
	private Subjects() { }
	/**
	 * Wraps the observer and observable instances into a Subject.
	 * @param <T> the type the observer uses
	 * @param <U> the type the observable uses
	 * @param observer the observer object
	 * @param observable the observable object
	 * @return the subject relaying to both
	 */
	@Nonnull 
	public static <T, U> Subject<T, U> newSubject(
			@Nonnull final Observer<? super T> observer, 
			@Nonnull final Observable<? extends U> observable) {
		return new Subject<T, U>() {

			@Override
			public void next(T value) {
				observer.next(value);
			}

			@Override
			public void error(@Nonnull Throwable ex) {
				observer.error(ex);
			}

			@Override
			public void finish() {
				observer.finish();
			}

			@Override
			@Nonnull
			public Closeable register(@Nonnull Observer<? super U> observer) {
				return observable.register(observer);
			}
		};
	}
	/**
	 * Creates a new subject which simply forwards its observed values
	 * to the registered observers.
	 * @param <T> the element type
	 * @return the new subject
	 */
	@Nonnull 
	public static <T> Subject<T, T> newSubject() {
		return new DefaultObservable<T>();
	}
	/**
	 * Creates a new subject which forwards its observed values after
	 * applying the selector function.
	 * @param <T> the observed element type
	 * @param <U> the forwarded element type
	 * @param selector the selector to use
	 * @return the new subject
	 */
	@Nonnull 
	public static <T, U> Subject<T, U> newSubject(
			@Nonnull final Func1<? super T, ? extends U> selector) {
		return new Subject<T, U>() {
			final DefaultObservable<U> observable = new DefaultObservable<U>();

			@Override
			public void next(T value) {
				observable.next(selector.invoke(value));
			}

			@Override
			public void error(@Nonnull Throwable ex) {
				observable.error(ex);
			}

			@Override
			public void finish() {
				observable.finish();
			}
			@Override
			@Nonnull
			public Closeable register(@Nonnull Observer<? super U> observer) {
				return observable.register(observer);
			}
			
		};
	}
}
