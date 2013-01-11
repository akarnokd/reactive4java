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

import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;

import java.io.Closeable;
import java.io.IOException;

import javax.annotation.Nonnull;

/**
 * Utility class that helps in creating various observable instances.
 * @author akarnokd, 2013.01.10.
 * @since 0.97
 */
public final class Observables {
	/** Utility class. */
	private Observables() { }
	/**
	 * Converts the original Java Observable into an reactive-Observable instance.
	 * Since Java Observables had no concept of error and termination, and
	 * were considered active observable, the returned reactive-observable
	 * never terminates or throws an error.
	 * @param javaObservable the java observable to be used
	 * @return the reactive-observable
	 */
	@Nonnull
	public static Observable<Object> toObservable(
			@Nonnull final java.util.Observable javaObservable) {
		return new Observable<Object>() {
			@Override
			@Nonnull
			public Closeable register(@Nonnull final Observer<? super Object> observer) {
				final java.util.Observer javaObserver = new java.util.Observer() {
					@Override
					public void update(java.util.Observable o, Object arg) {
						observer.next(arg);
					}
				};
				javaObservable.addObserver(javaObserver);
				return new Closeable() {
					@Override
					public void close() throws IOException {
						javaObservable.deleteObserver(javaObserver);
					}
				};
			}
		};
	}
	/**
	 * Converts the reactive-observable into the original Java Observable.
	 * <p>Since java-observables are always active, the source
	 * sequence is immediately registered. In order to have a java-observable
	 * that activates once the first observer registers, supply this method
	 * with a Reactive.refCount() wrapped observable. The returned
	 * java-observable implements Closeable which can be used to
	 * terminate the connection to source and deregister all observers</p>
	 * <p>Incoming error and finish events will close the connection
	 * to the source and deregister all observers.</p>
	 * @param source the source sequence of anything
	 * @return the java-observable
	 */
	@Nonnull
	public static OriginalObservableWrapper toJavaObservable(
			@Nonnull final Observable<?> source) {
		return toJavaObservable(source, true);
	}
	/**
	 * Converts the reactive-observable into the original Java Observable.
	 * <p>Since java-observables are always active, the source
	 * sequence is immediately registered. In order to have a java-observable
	 * that activates once the first observer registers, supply this method
	 * with a Reactive.refCount() wrapped observable. The returned
	 * java-observable implements Closeable which can be used to
	 * terminate the connection to source and deregister all observers</p>
	 * <p>Incoming error and finish events are ignored if unregisterObservers
	 * is false, or they will cause deregistration otherwise.</p>
	 * @param source the source sequence of anything
	 * @param unregisterObservers should the registered observers deregistered
	 * in case of an error or finish message or closing the observer?
	 * @return the java-observable
	 */
	@Nonnull
	public static OriginalObservableWrapper toJavaObservable(
			@Nonnull final Observable<?> source,
			final boolean unregisterObservers) {
		final java.util.Observable javaObservable = new java.util.Observable();
		
		Closeable c = source.register(new Observer<Object>() {
			@Override
			public void next(Object value) {
				javaObservable.notifyObservers(value);
			}
			@Override
			public void error(Throwable ex) {
				if (unregisterObservers) {
					javaObservable.deleteObservers();
				}
			}
			@Override
			public void finish() {
				if (unregisterObservers) {
					javaObservable.deleteObservers();
				}
			}
		});
		
		return new OriginalObservableWrapper(javaObservable, Actions.asAction0E(c), unregisterObservers);
	}
}
