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

import static hu.akarnokd.reactive4java.query.IterableBuilder.from;
import static hu.akarnokd.reactive4java.test.TestScheduler.onNext;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.util.Closeables;

import java.io.Closeable;
import java.util.List;

import org.junit.Test;

/**
 * @author Denes Harmath, 2012.09.22.
 */
public class TestTestScheduler {

	/**
	 * Simple test based on http://blogs.msdn.com/b/rxteam/archive/2012/06/14/testing-rx-queries-using-virtual-time-scheduling.aspx.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void singletonReimplementation() {
		TestScheduler scheduler = new TestScheduler();
		int value = 42;
		int event1 = 10;
		int event2 = 20;
		final Observable<Integer> observable = scheduler.createObservable(true, onNext(value, event1), TestScheduler.<Integer>onFinish(event2));
		final TestableObserver<Integer> observer1 = scheduler.createObserver();
		int subscription1 = 190;
		Closeable closeable1 = scheduler.schedule(new Runnable() {
			@Override
			public void run() {
				observable.register(observer1);
			}
		}, subscription1, MILLISECONDS);
		final TestableObserver<Integer> observer2 = scheduler.createObserver();
		int subscription2 = 220;
		Closeable closeable2 = scheduler.schedule(new Runnable() {
			@Override
			public void run() {
				observable.register(observer2);
			}
		}, subscription2, MILLISECONDS);
		scheduler.start();
		Closeables.closeSilently(closeable1);
		Closeables.closeSilently(closeable2);

		List<?> expected = from(
				onNext(value, subscription1 + event1), 
				TestScheduler.<Integer>onFinish(subscription1 + event2)).toList();
		List<?> actual = observer1.getEvents();
		
		TestUtil.assertEqual(expected, actual);
		
		expected = from(onNext(value, subscription2 + event1), TestScheduler.<Integer>onFinish(subscription2 + event2)).toList();
		actual = observer2.getEvents();
		
		TestUtil.assertEqual(expected, actual);
	}

}
