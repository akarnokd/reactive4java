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
import java.util.concurrent.locks.Lock;

import javax.annotation.Nonnull;

/**
 * Extension to the default observer for cases when the observer itself
 * needs to track sub-observers and their close handlers.
 * <p>Note when overriding onClose, you should always call <code>super.onClose()</code>
 * to close any remaining subObservers.</p>
 * @author akarnokd, 2011.02.11.
 * @param <T> the element type
 */
public abstract class DefaultObserverEx<T> extends DefaultObserver<T> {
	/** The container for the tagged observers. */
	@Nonnull
	protected final TaggedCompositeCloseable subObservers;
	/**
	 * Constructor. Sets up the observer to complete once the error or finish
	 * messages are received.
	 */
	public DefaultObserverEx() {
		super(true);
		subObservers = new TaggedCompositeCloseable(lock);
	}
	/**
	 * Constructor.
	 * @param complete should the observer close its sub-resources automatically on error/finish?
	 */
	public DefaultObserverEx(boolean complete) {
		super(complete);
		subObservers = new TaggedCompositeCloseable(lock);
	}

	/**
	 * Constructor.
	 * @param lock the external lock to use when synchronizing the message methods
	 * @param complete should the observer close its sub-resources automatically on error/finish?
	 */
	public DefaultObserverEx(@Nonnull Lock lock, boolean complete) {
		super(lock, complete);
		subObservers = new TaggedCompositeCloseable(this.lock);
	}
	/**
	 * Adds or replaces a new closeable with a token to the sub-observer list.
	 * If this observer is already in completed state, the handler is closed immediately.
	 * @param token the reference token
	 * @param handler the closeable handler
	 */
	public void add(Object token, Closeable handler) {
		subObservers.add(token, handler);
	}
	/**
	 * Called internally with the global lock held to ensure any dependent registrations succeed to
	 * store the closeable reference before returning.
	 */
	protected void onRegister() { }
	/**
	 * While holding the global lock, executes the onRegister method then 
	 * registers this instance with the supplied source observable with the <code>this</code> token.
	 * If the this token is already registered, the previous handle is closed.
	 * @param source the source observable
	 * @return this
	 */
	@Nonnull 
	public Closeable registerWith(@Nonnull Observable<? extends T> source) {
		init();
		boolean comp = false;
		lock.lock();
		try {
			comp = completed;
			if (!comp) {
				onRegister();
			}
		} finally {
			lock.unlock();
		}
		if (!comp) {
			try {
				subObservers.add(this, source.register(this));
			} catch (Throwable t) {
				error(t);
			}
		}
		return this;
	}
	/**
	 * Removes and closes the close handler associated with the token.
	 * @param token the token to the closeable handler
	 */
	public void remove(@Nonnull Object token) {
		subObservers.remove(token);
	}
	@Override
	protected void onClose() {
		Closeables.closeSilently(subObservers);
	}
	/**
	 * Wraps the supplied observer into a DefaultObservableEx and
	 * simply forwards onNext, onError, onFinish events to the raw
	 * next, error and finish methods.
	 * @param <T> the observed element type
	 * @param observer the observer to wrap
	 * @return the wrapper default observer ex
	 */
	public static <T> DefaultObserverEx<T> wrap(@Nonnull final Observer<? super T> observer) {
		return new DefaultObserverEx<T>() {
			@Override
			protected void onNext(T value) {
				observer.next(value);
			}

			@Override
			protected void onError(@Nonnull Throwable ex) {
				observer.error(ex);
			}

			@Override
			protected void onFinish() {
				observer.finish();
			}
			
		};
	}
	/**
	 * Implementations might override this method to initialize some other
	 * resources outside the onRegister's lock, before onRegister.
	 */
	protected void init() { }
}
