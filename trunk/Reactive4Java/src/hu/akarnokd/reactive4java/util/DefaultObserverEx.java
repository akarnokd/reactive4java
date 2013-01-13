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
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

/**
 * Extension to the default observer for cases when the observer itself
 * needs to track sub-observers and their close handlers.
 * <p>Note when overriding onClose, you should always call <code>super.onClose()</code>
 * to close any remaining subObservers.</p>
 * @author akarnokd, 2011.02.11.
 * @param <T> the element type
 */
public abstract class DefaultObserverEx<T> extends DefaultObserver<T> {
	/** The sub-observer registration holder. The key is a use-site created object. */
	@GuardedBy("lock")
	@Nonnull 
	protected final Map<Object, Closeable> subObservers = new IdentityHashMap<Object, Closeable>();
	/**
	 * Constructor. Sets up the observer to complete once the error or finish
	 * messages are received.
	 */
	public DefaultObserverEx() {
		super(true);
	}
	/**
	 * Constructor.
	 * @param complete should the observer close its sub-resources automatically on error/finish?
	 */
	public DefaultObserverEx(boolean complete) {
		super(complete);
	}

	/**
	 * Constructor.
	 * @param lock the external lock to use when synchronizing the message methods
	 * @param complete should the observer close its sub-resources automatically on error/finish?
	 */
	public DefaultObserverEx(@Nonnull Lock lock, boolean complete) {
		super(lock, complete);
	}
	/**
	 * Add a new closeable with a token to the sub-observer list.
	 * If this observer is already in completed state, the handler is closed immediately.
	 * @param token the reference token
	 * @param handler the closeable handler
	 */
	public void add(Object token, Closeable handler) {
		boolean shouldClose = false;
		lock.lock();
		try {
			shouldClose = completed;
			if (!completed) {
				subObservers.put(token, handler);
			}
		} finally {
			lock.unlock();
		}
		if (shouldClose) {
			Closeables.closeSilently(handler);
		}
	}
	/**
	 * Called internally with the global lock held to ensure any dependent registrations succeed to
	 * store the closeable reference before returning.
	 */
	protected void onRegister() { }
	/**
	 * Registers itself with the given source observable and stores the registration info by the given token.
	 * @param token the reference token
	 * @param source the target observable
	 */
	public void add(@Nonnull Object token, @Nonnull Observable<? extends T> source) {
		lock.lock();
		try {
			if (!completed) {
				Closeable c = source.register(this);
				if (!completed) {
					subObservers.put(token, c);
				}
			}
		} finally {
			lock.unlock();
		}
	}
	/**
	 * While holding the global lock, executes the onRegister method then 
	 * registers this instance with the supplied source observable with the <code>this</code> token.
	 * If the this token is already registered, the previous handle is closed.
	 * @param source the source observable
	 * @return this
	 */
	@Nonnull 
	public Closeable registerWith(@Nonnull Observable<? extends T> source) {
		Closeable closePrevious = null;
		lock.lock();
		try {
			if (!completed) {
				onRegister();
				if (!completed) {
					Closeable c = source.register(this);
					if (!completed) {
						closePrevious = subObservers.put(this, c);
					}
				}
			}
		} finally {
			lock.unlock();
		}
		Closeables.closeSilently(closePrevious);
		return this;
	}
	/**
	 * Removes and closes the handler associated with the token.
	 * The call should be called with the <code>lock</code> being held.
	 * @param token the token to remove
	 */
	protected void removeInner(@Nonnull Object token) {
		Closeable c = subObservers.remove(token);
		Closeables.closeSilently(c);
	}
	/**
	 * Removes and closes the close handler associated with the token.
	 * @param token the token to the closeable handler
	 */
	public void remove(@Nonnull Object token) {
		lock.lock();
		try {
			removeInner(token);
		} finally {
			lock.unlock();
		}
	}
	/**
	 * Replaces (atomically) an old token with a new token and closeable handler.
	 * If the observer is in finished state, the new handler is immediately closed. 
	 * @param oldToken the old token
	 * @param newToken the new token
	 * @param newHandler the new closeable handler
	 */
	public void replace(@Nonnull Object oldToken, @Nonnull Object newToken, @Nonnull Closeable newHandler) {
		Closeable oldHandler = null;
		boolean shouldCloseNew = false;
		lock.lock();
		try {
			shouldCloseNew = completed;
			if (!completed) {
				oldHandler = subObservers.get(oldToken);
				subObservers.put(newToken, newHandler);
			}
		} finally {
			lock.unlock();
		}
		Closeables.closeSilently(oldHandler);
		if (shouldCloseNew) {
			Closeables.closeSilently(newHandler);
		}
	}
	@Override
	protected void onClose() {
		List<Object> cs = new ArrayList<Object>(subObservers.keySet());
		for (Object c : cs) {
			removeInner(c);
		}
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
}
