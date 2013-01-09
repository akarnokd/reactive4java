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

/**
 * Utility class to wrap existing pairs of Observers and Observables under the Subject
 * interface.
 * @author akarnokd, 2013.01.09.
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
	public static <T, U> Subject<T, U> newSubject(
			@Nonnull final Observer<? super T> observer, 
			@Nonnull final Observable<? extends U> observable) {
		return new Subject<T, U>() {

			@Override
			public void next(T value) {
				observer.next(value);
			}

			@Override
			public void error(Throwable ex) {
				observer.error(ex);
			}

			@Override
			public void finish() {
				observer.finish();
			}

			@Override
			@Nonnull
			public Closeable register(Observer<? super U> observer) {
				return observable.register(observer);
			}
			
		};
	}
}
