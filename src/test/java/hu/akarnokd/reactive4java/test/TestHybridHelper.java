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
package hu.akarnokd.reactive4java.test;

import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;
import hu.akarnokd.reactive4java.util.AsyncSubject;
import hu.akarnokd.reactive4java.util.Closeables;
import hu.akarnokd.reactive4java.util.ObserverAdapter;

import java.io.Closeable;

import org.junit.Assert;

/**
 * Utility class to test a hybrid java-reactive observable implementation on all
 * 4 stream traversal cases.
 * All test transmit a simple integer of 1 and asserts its reception.
 * @author akarnokd, 2013.01.11.
 * @since 0.97
 */
public final class TestHybridHelper {
	/** Helper class. */
	private TestHybridHelper() { }
	/**
	 * Test the java to java communication through an java-observer interface.
	 * @param observer the java-observer
	 * @param observableImpl the implementation as java-observable
	 * @throws InterruptedException if the wait is interrupted
	 */
	public static void testJavaToJava(
			java.util.Observer observer, 
			java.util.Observable observableImpl) 
	throws InterruptedException {
		final AsyncSubject<Integer> waiter = new AsyncSubject<Integer>();
		
		java.util.Observer o = new java.util.Observer() {
			@Override
			public void update(java.util.Observable o, Object arg) {
				waiter.next((Integer)arg);
				waiter.finish();
			}
		};
		
		observableImpl.addObserver(o);
		try {
			observableImpl.notifyObservers(1);
			
			Assert.assertEquals((Integer)1, waiter.get());
		} finally {
			observableImpl.deleteObserver(o);
		}

	}
	/**
	 * Test a stream from java to reactive.
	 * @param observableImpl the java-observable
	 * @param observable the reactive-observable
	 * @throws InterruptedException if interrupted
	 */
	public static void testJavaToReactive(
			java.util.Observable observableImpl, 
			Observable<Integer> observable) throws InterruptedException {
		
		final AsyncSubject<Integer> waiter = new AsyncSubject<Integer>();

		Closeable c = observable.register(new ObserverAdapter<Integer>() {
			@Override
			public void next(Integer value) {
				waiter.next(value);
				waiter.finish();
			}
		});
		try {
			observableImpl.notifyObservers(1);
			
			Assert.assertEquals((Integer)1, waiter.get());
		} finally {
			Closeables.closeSilently(c);
		}
	}
	/**
	 * Test a stream from reactive to java.
	 * @param observer the reactive-observer
	 * @param observableImpl the java-observable
	 * @throws InterruptedException if interrupted
	 */
	public static void testReactiveToJava(
			Observer<Integer> observer,
			java.util.Observable observableImpl) throws InterruptedException {
		
		final AsyncSubject<Integer> waiter = new AsyncSubject<Integer>();

		java.util.Observer o = new java.util.Observer() {
			@Override
			public void update(java.util.Observable o, Object arg) {
				waiter.next((Integer)arg);
				waiter.finish();
			}
		};
		
		observableImpl.addObserver(o);
		try {
			observer.next(1);
			
			Assert.assertEquals((Integer)1, waiter.get());
		} finally {
			observableImpl.deleteObserver(o);
		}
	}
	/**
	 * Test a stream from reactive to java.
	 * @param observer the reactive-observer
	 * @param observable the reactive-observable
	 * @throws InterruptedException if interrupted
	 */
	public static void testReactiveToReactive(
			Observer<Integer> observer,
			Observable<Integer> observable) throws InterruptedException {
		
		final AsyncSubject<Integer> waiter = new AsyncSubject<Integer>();

		Closeable c = observable.register(new ObserverAdapter<Integer>() {
			@Override
			public void next(Integer value) {
				waiter.next(value);
				waiter.finish();
			}
		});
		try {
			observer.next(1);
			
			Assert.assertEquals((Integer)1, waiter.get());
		} finally {
			Closeables.closeSilently(c);
		}
	}
}
