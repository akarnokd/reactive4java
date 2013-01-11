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

import hu.akarnokd.reactive4java.base.CloseableObservable;
import hu.akarnokd.reactive4java.base.ConnectableObservable;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.reactive.Reactive;
import hu.akarnokd.reactive4java.util.Subjects;
import hu.akarnokd.reactive4java.util.Closeables;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * Tests the Reactive.multicast operators.
 * @author akarnokd, 2013.01.10.
 */
public class TestReactiveMulticast {
	/** Test for the simple, always connected case. */
	@Test(timeout = 10000)
	public void testContinuous() {
		Observable<Long> source = Reactive.tick(0, 5, 1, TimeUnit.SECONDS);
		
		final ConnectableObservable<Long> conn = Reactive.multicast(source, Subjects.<Long>newSubject());

		conn.connect();
		
		Observable<Long> result = Reactive.timeoutFinish(conn, 2, TimeUnit.SECONDS);
		
		TestUtil.assertEqual(Arrays.asList(0L, 1L, 2L, 3L, 4L), result);
	}
	/** Test for the simple, always connected case. */
	@Test /* (timeout = 10000) */
	public void testEarlyDisconnect() {
		Observable<Long> source = Reactive.tick(0, 5, 1, TimeUnit.SECONDS);
		
		final ConnectableObservable<Long> conn = Reactive.multicast(source, Subjects.<Long>newSubject());

		final Closeable c = conn.connect();
		
		Reactive.getDefaultScheduler().schedule(new Runnable() {
			@Override
			public void run() {
				Closeables.closeSilently(c);
			}
		}, 3500, TimeUnit.MILLISECONDS);

		Observable<Long> result = Reactive.timeoutFinish(conn, 2, TimeUnit.SECONDS);

		TestUtil.assertEqual(Arrays.asList(0L, 1L, 2L), result);
	}
	/** 
	 * Test for the simple, always connected case.
	 * @throws IOException on error 
	 */
	@Test /* (timeout = 10000) */
	public void testLateConnect() throws IOException {
		CloseableObservable<Long> source = Reactive.activeTick(0, 5, 1, TimeUnit.SECONDS);
		
		final ConnectableObservable<Long> conn = Reactive.multicast(source, 
				Subjects.<Long>newSubject());

		
		Reactive.getDefaultScheduler().schedule(new Runnable() {
			@Override
			public void run() {
				conn.connect();
			}
		}, 3500, TimeUnit.MILLISECONDS);

		Observable<Long> result = Reactive.timeoutFinish(conn, 2, TimeUnit.SECONDS);

		TestUtil.assertEqual(Arrays.asList(3L, 4L), result);
		
		source.close();
	}

}
