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
package hu.akarnokd.reactive4java.swing;

import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;
import hu.akarnokd.reactive4java.base.Scheduler;
import hu.akarnokd.reactive4java.reactive.Reactive;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

/**
 * Utility class to wrap typical Swing events into observables.
 * @author akarnokd, 2011.02.01.
 */
public final class SwingObservables {
	/** Utility class. */
	private SwingObservables() {
		// utility class
	}
	/** The common observable pool where the Observer methods get invoked by default. */
	static final AtomicReference<Scheduler> DEFAULT_EDT_SCHEDULER = new AtomicReference<Scheduler>(new DefaultEdtScheduler());
	/**
	 * Invoke the methodPrefix + paramType.getSimpleName method (i.e., "add" and ActionListener.class will produce addActionListener)
	 * on the object and wrap any exception into IllegalArgumentException.
	 * @param <T> the method parameter type
	 * @param <U> a type which extends the method parameter type T.
	 * @param o the target object
	 * @param methodPrefix the method name prefix
	 * @param paramType the required parameter type
	 * @param value the value to invoke with
	 * @return the value
	 */
	static <T, U extends T> U invoke(
			@Nonnull Object o, 
			@Nonnull String methodPrefix, 
			@Nonnull Class<T> paramType, 
			@Nonnull U value) {
		if (o == null) {
			throw new IllegalArgumentException("o is null");
		}
		try {
			Method m = o.getClass().getMethod(methodPrefix + paramType.getSimpleName(), paramType);
			m.invoke(o, value);
		} catch (NoSuchMethodException ex) {
			throw new IllegalArgumentException(ex);
		} catch (IllegalAccessException ex) {
			throw new IllegalArgumentException(ex);
		} catch (InvocationTargetException ex) {
			throw new IllegalArgumentException(ex);
		}
		return value;
	}
	/**
	 * Create a dynamic observer for the given listener interface by
	 * proxying all method calls. None of the methods of the listener interface should require something meaningful to be returned, i.e., they all
	 * must be <code>void</code>, return <code>Void</code> (or the original call site should accept <code>null</code>s).
	 * Note that due this proxying effect, the handler invocation may be 100 times slower than a direct implementation
	 * @param <T> the listener interface type
	 * @param listener the list interface class
	 * @param bindTo the target observer, use the DefaultObservable
	 * @return the proxy instance
	 */
	@Nonnull 
	public static <T> T create(
			@Nonnull Class<T> listener, 
			@Nonnull final Observer<? super Dynamic> bindTo) {
		final InvocationHandler handler = new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args)
					throws Throwable {
				bindTo.next(new Dynamic(method.getName(), args));
				return null;
			}
		};
		
		return listener.cast(Proxy.newProxyInstance(listener.getClassLoader(), new Class<?>[] { listener }, handler));
	}
	/**
	 * Wrap the observable to the Event Dispatch Thread for listening to events.
	 * @param <T> the value type to observe
	 * @param observable the original observable
	 * @return the new observable
	 */
	@Nonnull 
	public static <T> Observable<T> observeOnEdt(@Nonnull Observable<T> observable) {
		return Reactive.observeOn(observable, DEFAULT_EDT_SCHEDULER.get());
	}
	/**
	 * @return the current default pool used by the Observables methods
	 */
	@Nonnull 
	public static Scheduler getDefaultEdtScheduler() {
		return DEFAULT_EDT_SCHEDULER.get();
	}
	/**
	 * Replace the current default scheduler with the specified  new scheduler.
	 * This method is threadsafe
	 * @param newScheduler the new scheduler
	 * @return the current scheduler
	 */
	@Nonnull 
	public static Scheduler replaceDefaultEdtScheduler(
			@Nonnull Scheduler newScheduler) {
		if (newScheduler == null) {
			throw new IllegalArgumentException("newScheduler is null");
		}
		return DEFAULT_EDT_SCHEDULER.getAndSet(newScheduler);
	}
	/**
	 * Restore the default scheduler back to the <code>DefaultScheduler</code>
	 * used when this class was initialized.
	 */
	public static void restoreDefaultEdtScheduler() {
		DEFAULT_EDT_SCHEDULER.set(new DefaultEdtScheduler());
	}
	/**
	 * Wrap the observable to the Event Dispatch Thread for subscribing to events.
	 * @param <T> the value type to observe
	 * @param observable the original observable
	 * @return the new observable
	 */
	@Nonnull 
	public static <T> Observable<T> subscribeOnEdt(Observable<T> observable) {
		return Reactive.registerOn(observable, DEFAULT_EDT_SCHEDULER.get());
	}
}
