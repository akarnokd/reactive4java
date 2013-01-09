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

import hu.akarnokd.reactive4java.base.Closeables;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

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
	public DefaultObserverEx(Lock lock, boolean complete) {
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
	public void add(Object token, Observable<? extends T> source) {
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
	public Closeable registerWith(Observable<? extends T> source) {
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
	protected void removeInner(Object token) {
		Closeable c = subObservers.remove(token);
		Closeables.closeSilently(c);
	}
	/**
	 * Removes and closes the close handler associated with the token.
	 * @param token the token to the closeable handler
	 */
	public void remove(Object token) {
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
	public void replace(Object oldToken, Object newToken, Closeable newHandler) {
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
}
