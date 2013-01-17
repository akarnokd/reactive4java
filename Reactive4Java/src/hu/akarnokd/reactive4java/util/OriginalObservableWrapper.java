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
import hu.akarnokd.reactive4java.base.CloseableObservable;

import java.io.Closeable;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import javax.annotation.Nonnull;

/**
 * Helper class that wraps a Java Observable and makes its 
 * services availabel as java- and reactive- observable.
 * Data incoming through the observer interfaces are routed to the underlying
 * java-observable. The class implements Observable&lt;Object>, therefore
 * reactive-observers can be registered as well.
 * @author akarnokd, 2013.01.11.
 * @since 0.97
 * @param <T> the reactive-observer's type
 */
public class OriginalObservableWrapper<T> 
// #GWT-IGNORE-START
extends Observable 
// #GWT-IGNORE-END
implements CloseableObservable<T> {
	// #GWT-IGNORE-START
	/** The observable. */
	@Nonnull 
	protected final Observable observable;
	/** The close action. */
	@Nonnull 
	protected final Action0E<? extends IOException> closeAction;
	/**
	 * Wraps the given observable and sets a close
	 * action to remove all observers.
	 * @param observable the java-observable to wrap
	 */
	public OriginalObservableWrapper(
			@Nonnull Observable observable) {
		this(observable, Actions.noAction0(), true);
	}
	/**
	 * Wraps the given observable which calls the supplied
	 * action in case of a close event and unregisters all observables.
	 * @param observable the java-observable to wrap
	 * @param action the close action
	 */
	public OriginalObservableWrapper(
			@Nonnull Observable observable, 
			@Nonnull Action0 action) {
		this(observable, action, true);
	}
	/**
	 * Wraps the given observable and sets up this observable
	 * to call the closeAction on close() and remove all
	 * registered observers if unregisterAll is true.
	 * @param observable the java-observable to wrap
	 * @param closeAction the close action
	 * @param unregisterAll undegister all observers on close?
	 */
	public OriginalObservableWrapper(
			@Nonnull Observable observable, 
			@Nonnull Action0 closeAction, boolean unregisterAll) {
		this(observable, Actions.<IOException>asAction0E(closeAction), unregisterAll);
	}
	/**
	 * Wraps the given observable which calls the supplied
	 * action in case of a close event and unregisters all observables.
	 * @param observable the java-observable to wrap
	 * @param action the close action
	 */
	public OriginalObservableWrapper(
			@Nonnull Observable observable, 
			@Nonnull Action0E<? extends IOException> action) {
		this(observable, action, true);
	}
	/**
	 * Wraps the given observable and sets up this observable
	 * to call the closeAction on close() and remove all
	 * registered observers if unregisterAll is true.
	 * @param observable the java-observable to wrap
	 * @param closeAction the close action
	 * @param unregisterAll undegister all observers on close?
	 */
	public OriginalObservableWrapper(
			@Nonnull Observable observable, 
			@Nonnull final Action0E<? extends IOException> closeAction, 
			boolean unregisterAll) {
		Action0E<? extends IOException> ca = closeAction;
		if (unregisterAll) {
			ca = new Action0E<IOException>() {
				@Override
				public void invoke() throws IOException {
					try {
						closeAction.invoke();
					} finally {
						deleteObservers();
					}
				}
			};
		}
		
		this.observable = observable;
		this.closeAction = ca;
	}
	
	@Override
	public synchronized void addObserver(Observer o) {
		observable.addObserver(o);
	}
	@Override
	public synchronized void deleteObserver(Observer o) {
		observable.deleteObserver(o);
	}
	@Override
	public synchronized void deleteObservers() {
		observable.deleteObservers();
	}
	@Override
	public synchronized int countObservers() {
		return observable.countObservers();
	}
	@Override
	public void notifyObservers() {
		observable.notifyObservers();
	}
	@Override
	public void notifyObservers(Object arg) {
		observable.notifyObservers(arg);
	}
	@Override
	public synchronized boolean hasChanged() {
		return observable.hasChanged();
	}
	@Override
	public void close() throws IOException {
		closeAction.invoke();
	}
	@Override
	@Nonnull
	public Closeable register(
			@Nonnull hu.akarnokd.reactive4java.base.Observer<? super T> observer) {
		return Observers.registerWith(this, observer);
	}
	/**
	 * Registers a java-observer and returns a handle to it.
	 * The observer can be unregistered via this handle or the regular deleteObserver().
	 * <p>The convenience method is to have symmetric means
	 * for both observer kinds to interact with this observable.<p>
	 * @param observer the observer to register
	 * @return the unregistration handle
	 */
	@Nonnull 
	public Closeable register(@Nonnull final Observer observer) {
		Closeable handle = new Closeable() {
			@Override
			public void close() throws IOException {
				deleteObserver(observer);
			}
		};
		addObserver(observer);
		return handle;
	}
	// #GWT-IGNORE-END
	// #GWT-ACCEPT-START
//	@Override
//	public void close() throws IOException {
//	}
//	@Nonnull 
//	public Closeable register(@Nonnull final Observer observer) {
//		return Closeables.emptyCloseable();
//	}	
	// #GWT-ACCEPT-END
}
