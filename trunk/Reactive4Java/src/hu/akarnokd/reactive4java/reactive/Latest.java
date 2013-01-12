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
 * Returns an iterable sequence which returns the latest element
 * from the observable sequence, consuming it only once.
 * <p>Note that it is possible one doesn't receive the
 * last value of a fixed-length observable sequence in case 
 * the last next() call is is quickly followed by a finish() event.</p>
 * <p>The returned iterator throws <code>UnsupportedOperationException</code> for its <code>remove()</code> method.</p>
 * @param <T> the type of the values
 * @author akarnokd, 2013.01.12.
 * @since 0.97
 */
public final class Latest<T> extends
		ObservableToIterableAdapter<T, T> {
	/**
	 * Constructor.
	 * @param observable the source sequence
	 */
	public Latest(Observable<? extends T> observable) {
		super(observable);
	}

	@Override
	protected ObserverToIteratorSink<T, T> run(Closeable handle) {
		return new ObserverToIteratorSink<T, T>(handle) {
			/** The signal that value is available. */
			protected final Semaphore semaphore = new Semaphore(0, true);
			/** The lock protecting the fields. */
			protected final Lock lock = new ReentrantLock(true);
			@GuardedBy("lock")
			/** There is an event already received but not processed? */
			protected boolean eventAvailable;
			@GuardedBy("lock")
			/** The received value. */
			protected T value;
			@GuardedBy("lock")
			/** The received error. */
			protected Throwable error;
			/** The last observation kind. */
			@GuardedBy("lock")
			protected ObservationKind kind;
			@Override
			public void next(T value) {
				boolean hadNoValue = false;
				lock.lock();
				try {
					hadNoValue = !eventAvailable;
					
					eventAvailable = true;
					
					kind = ObservationKind.NEXT;
					this.value = value;
					
				} finally {
					lock.unlock();
				}
				if (hadNoValue) {
					semaphore.release();
				}
			}
			@Override
			public void error(Throwable ex) {
				done();
				boolean hadNoValue = false;
				lock.lock();
				try {
					hadNoValue = !eventAvailable;
					
					eventAvailable = true;
					
					kind = ObservationKind.ERROR;
					this.error = ex;
					
				} finally {
					lock.unlock();
				}
				if (hadNoValue) {
					semaphore.release();
				}
			}
			@Override
			public void finish() {
				done();
				boolean hadNoValue = false;
				lock.lock();
				try {
					hadNoValue = !eventAvailable;
					eventAvailable = true;
					
					kind = ObservationKind.FINISH;
					
				} finally {
					lock.unlock();
				}
				if (hadNoValue) {
					semaphore.release();
				}
			}
			@Override
			public boolean tryNext(SingleOption<? super T> out) {
				
				T v = null;
				Throwable e = null; 
				ObservationKind k = null;
				
				try {
					semaphore.acquire();
				} catch (InterruptedException ex) {
					out.addError(ex);
					return true;
				}
					
				lock.lock();
				try {
					k = kind;
					
					switch (k) {
					case NEXT:
						v = value;
						break;
					case ERROR:
						e = error;
						break;
					default:
					}
					
					eventAvailable = false;
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
