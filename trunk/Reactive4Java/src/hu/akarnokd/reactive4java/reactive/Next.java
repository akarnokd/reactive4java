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

import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.ObservationKind;
import hu.akarnokd.reactive4java.util.ObservableToIterableAdapter;
import hu.akarnokd.reactive4java.util.ObserverToIteratorSink;
import hu.akarnokd.reactive4java.util.SingleOption;

import java.io.Closeable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.concurrent.GuardedBy;

/**
 * Returns an iterable sequence which blocks until an element
 * becomes available from the source.
 * The iterable's (has)next() call is paired up with the observer's next() call,
 * therefore, values might be skipped if the iterable is not on its (has)next() call
 * at the time of reception.
 * <p>The returned iterator will throw an <code>UnsupportedOperationException</code> for its
 * <code>remove()</code> method.</p>
 * <p>Exception semantics: in case of exception received, the source is
 * disconnected and the exception is rethrown from the iterator's next method
 * as a wrapped RuntimeException if necessary.</p>
 * @author akarnokd, 2013.01.12.
 * @since 0.97
 * @param <T> the element type.
 */
public class Next<T> extends ObservableToIterableAdapter<T, T> {
	/**
	 * Constructor.
	 * @param observable the source obserable.
	 */
	public Next(Observable<? extends T> observable) {
		super(observable);
	}

	@Override
	protected ObserverToIteratorSink<T, T> run(Closeable handle) {
		return new ObserverToIteratorSink<T, T>(handle) {
			protected final Lock lock = new ReentrantLock(true);
			protected final Semaphore semaphore = new Semaphore(0);
			@GuardedBy("lock")
			protected boolean iteratorIsWaiting;
			@GuardedBy("lock")
			protected T value;
			@GuardedBy("lock")
			protected Throwable error;
			@GuardedBy("lock")
			protected ObservationKind kind;
			@Override
			public void next(T value) {
				set(ObservationKind.NEXT, value, null);
			}

			@Override
			public void error(Throwable ex) {
				done();
				set(ObservationKind.ERROR, null, ex);
			}

			@Override
			public void finish() {
				done();
				set(ObservationKind.FINISH, null, null);
			}
			/**
			 * Atomically sets the current event values
			 * if there is an iterator waiting for a value.
			 * @param kind the event kind
			 * @param value the potential value
			 * @param error the potential error
			 */
			protected void set(ObservationKind kind, T value, Throwable error) {
				lock.lock();
				try {
					if (iteratorIsWaiting) {
						this.kind = kind;
						this.value = value;
						this.error = error;
						
						semaphore.release();
					}
					iteratorIsWaiting = false;
				} finally {
					lock.unlock();
				}
			}
			@Override
			public boolean tryNext(SingleOption<? super T> out) {
				boolean finished = false;
				
				lock.lock();
				try {
					iteratorIsWaiting = true;
					
					finished = kind != ObservationKind.NEXT;
				} finally {
					lock.unlock();
				}
				
				if (!finished) {
					try {
						semaphore.acquire();
					} catch (InterruptedException ex) {
						out.addError(ex);
						return true;
					}
				}
				
				T v = null;
				Throwable e = null;
				ObservationKind k = null;
				
				lock.lock();
				try {
					v = value;
					e = error;
					k = kind;
				} finally {
					lock.unlock();
				}
				
				switch (k) {
				case NEXT:
					out.add(v);
					return true;
				case ERROR:
					out.addError(e);
					return true;
				default:
				}
				
				return false;
			}
			
		};
	}

}
