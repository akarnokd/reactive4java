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

import hu.akarnokd.reactive4java.base.ConnectableObservable;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;
import hu.akarnokd.reactive4java.base.Subject;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

/**
 * Default implementation of the connectable observable which can be disconnected
 * from its source independently from the registered observers.
 * @author akarnokd, 2013.01.09.
 * @param <T> the type of values observed from the source
 * @param <U> the type of values emmitted to registered observers
 * @since 0.97
 */
public class DefaultConnectableObservable<T, U> implements
		ConnectableObservable<U> {
	/** The subject that is connected to the source. */
	@Nonnull 
	protected final Subject<? super T, ? extends U> subject;
	/** The source observable. */
	@Nonnull 
	protected final Observable<? extends T> source;
	/** The lock. */
	@Nonnull 
	protected final Lock lock;
	/** The active connection's close reference. */
	@GuardedBy("lock")
	@Nullable
	protected Closeable connection;
	/**
	 * Creates an observable which can be connected and disconnected from the source.
	 * <p>Uses fair ReentrantLock.</p>
	 * @param source the underlying observable source of Ts
	 * @param subject the observer that receives values from the source in case it is connected
	 */
	public DefaultConnectableObservable(
			@Nonnull Observable<? extends T> source, 
			@Nonnull Subject<? super T, ? extends U> subject) {
		this(source, subject, new ReentrantLock(R4JConfigManager.get().useFairLocks()));
	}
	/**
	 * Creates an observable which can be connected and disconnected from the source.
	 * @param source the underlying observable source of Ts
	 * @param subject the observer that receives values from the source in case it is connected
	 * @param lock the lock to use
	 */
	public DefaultConnectableObservable(
			@Nonnull Observable<? extends T> source, 
			@Nonnull Subject<? super T, ? extends U> subject, 
			@Nonnull Lock lock) {
		this.subject = subject;
		this.source = source;
		this.lock = lock;
		
	}
	@Override
	public Closeable connect() {
		lock.lock();
		try {
			if (connection == null) {
				final Closeable c = source.register(subject);
				connection = new InnerConnection(c);
			}
			return connection;
		} finally {
			lock.unlock();
		}
	}
	@Override
	@Nonnull
	public Closeable register(@Nonnull Observer<? super U> observer) {
		return subject.register(observer);
	}
	/**
	 * The inner connection that nulls out the 
	 * parent class' connection and deregisters the subject, but only once.
	 * @author akarnokd, 2013.01.09.
	 */
	protected class InnerConnection implements Closeable {
		/** The subject's close handler. */
		@Nonnull 
		protected Closeable c;
		/**
		 * Constructor. Stores the subject's close handler.
		 * @param c the closeable
		 */
		public InnerConnection(@Nonnull Closeable c) {
			this.c = c;
		}
		@Override
		public void close() throws IOException {
			Closeable toClose = null;
			lock.lock();
			try {
				if (c != null) {
					toClose = c;
					c = null;
					connection = null;
				}
			} finally {
				lock.unlock();
			}
			if (toClose != null) {
				toClose.close();
			}
		}
	}
}
