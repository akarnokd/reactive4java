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

import hu.akarnokd.reactive4java.base.Action0;
import hu.akarnokd.reactive4java.base.Action0E;
import hu.akarnokd.reactive4java.base.Func1;
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
	 * <p>Note that since java-observables are not generic, ClassCastException
	 * might occur if the transmitted value has incompatible class.</p>
	 * @param <T> the element type
	 * @param javaObservable the java-observable to be used
	 * @return the reactive-observable
	 */
	@Nonnull
	public static <T> Observable<T> toObservable(
			@Nonnull final java.util.Observable javaObservable) {
		return new OriginalObservableWrapper<T>(javaObservable);
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
	 * @param <T> the observable sequence type
	 * @param source the source sequence of anything
	 * @return the java-observable
	 * @see HybridSubject
	 */
	@Nonnull
	public static <T> OriginalObservableWrapper<T> toOriginalObservable(
			@Nonnull final Observable<T> source) {
		return toOriginalObservable(source, true);
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
	 * @param <T> the observable sequence type
	 * @param source the source sequence of anything
	 * @param unregisterObservers should the registered observers deregistered
	 * in case of an error or finish message or closing the observer?
	 * @return the java-observable
	 * @see HybridSubject
	 */
	@Nonnull
	public static <T> OriginalObservableWrapper<T> toOriginalObservable(
			@Nonnull final Observable<T> source,
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
		
		return new OriginalObservableWrapper<T>(javaObservable, Actions.asAction0E(c), unregisterObservers);
	}
	/**
	 * Create an observable instance by submitting a function which takes responsibility
	 * for registering observers.
	 * @param <T> the type of the value to observe
	 * @param register the function to manage new registrations
	 * @return the observable instance
	 */
	@Nonnull
	public static <T> Observable<T> createWithAction(
			@Nonnull final Func1<Observer<? super T>, ? extends Action0> register) {
		return new Observable<T>() {
			@Override
			public Closeable register(Observer<? super T> observer) {
				final Action0 a = register.invoke(observer);
				return Closeables.toCloseable(a);
			}
		};
	}
	/**
	 * Create an observable instance by submitting a function which takes responsibility
	 * for registering observers and returns a custom Closeable to terminate the registration.
	 * @param <T> the type of the value to observe
	 * @param registerer the function to manage new registrations
	 * @return the observable instance
	 */
	@Nonnull
	public static <T> Observable<T> create(
			@Nonnull final Func1<Observer<? super T>, ? extends Closeable> registerer) {
		return new Observable<T>() {
			@Override
			public Closeable register(Observer<? super T> observer) {
				return registerer.invoke(observer);
			}
		};
	}
	/**
	 * Create an observable instance by submitting a function which takes responsibility
	 * for registering observers and returns a custom Closeable to terminate the registration.
	 * @param <T> the type of the value to observe
	 * @param registerer the function to manage new registrations
	 * @return the observable instance
	 */
	@Nonnull
	public static <T> Observable<T> createWithActionE(
			@Nonnull final Func1<Observer<? super T>, ? extends Action0E<? extends IOException>> registerer) {
		return new Observable<T>() {
			@Override
			public Closeable register(Observer<? super T> observer) {
				return Closeables.toCloseable(registerer.invoke(observer));
			}
		};
	}
}
