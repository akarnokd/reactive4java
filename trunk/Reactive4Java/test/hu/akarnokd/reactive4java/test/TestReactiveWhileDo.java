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
import hu.akarnokd.reactive4java.base.Pred0;
import hu.akarnokd.reactive4java.reactive.Reactive;
import hu.akarnokd.reactive4java.util.Closeables;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test the Reactive.doWhile operator.
 * @author akarnokd, 2013.01.13.
 */
public class TestReactiveWhileDo {
	/** Simple test. */
	@Test
	public void testSimple() {
		Observable<Integer> source = Reactive.range(0, 5);
		
		Pred0 twice = new Pred0() {
			/** Count backwards. */
			int count = 2;
			@Override
			public Boolean invoke() {
				return count-- > 0;
			}
		};
		
		Observable<Integer> result = Reactive.whileDo(source, twice);
		
		List<Integer> expected = Arrays.asList(0, 1, 2, 3, 4, 0, 1, 2, 3, 4);
		
		TestUtil.assertEqual(expected, result);
	}
	/** Simple test. */
	@Test(expected = RuntimeException.class)
	public void testException() {
		Observable<Integer> ex = Reactive.throwException(new RuntimeException());
		Observable<Integer> source = Reactive.concat(Reactive.range(0, 5), ex);
		
		Pred0 twice = new Pred0() {
			/** Count backwards. */
			int count = 1;
			@Override
			public Boolean invoke() {
				return count-- > 0;
			}
		};
		
		Observable<Integer> result = Reactive.whileDo(source, twice);
		
		List<Integer> expected = Arrays.asList(0, 1, 2, 3, 4, 0, 1, 2, 3, 4);
		
		TestUtil.assertEqual(expected, result);
	}
	/**
	 * Test if prolonged repeat causes any issues.
	 * @throws InterruptedException if interrupted
	 * @throws IOException on close?
	 */
	@Test
	public void testLongRepeat() throws InterruptedException, IOException {
		final int n = 10000;
		Observable<Integer> source = Reactive.singleton(1);

		Pred0 twice = new Pred0() {
			/** Count backwards. */
			int count = n;
			@Override
			public Boolean invoke() {
				return count-- > 0;
			}
		};

		final AtomicInteger cnt = new AtomicInteger();
		final CountDownLatch compl = new CountDownLatch(1);
		Reactive.whileDo(source, twice)
				.register(new Observer<Integer>() {
					@Override
					public void error(Throwable ex) {
						ex.printStackTrace();
						compl.countDown();
					}
					@Override
					public void finish() {
						compl.countDown();
					}
					@Override
					public void next(Integer value) {
						cnt.incrementAndGet();
					}
					
				});
		
		compl.await();
		
		Assert.assertEquals(n, cnt.get());
	}
	/**
	 * Test if prolonged repeat causes any issues.
	 * @throws InterruptedException if interrupted
	 * @throws IOException on close?
	 */
	@Test
	public void testLongRepeat2() throws InterruptedException, IOException {
		final int n = 10000;

		Pred0 twice = new Pred0() {
			/** Count backwards. */
			int count = n;
			@Override
			public Boolean invoke() {
				return count-- > 0;
			}
		};
		Observable<Integer> source = new Observable<Integer>() {
			@Override
			public Closeable register(Observer<? super Integer> observer) {
				observer.next(1);
				observer.finish();
				return Closeables.emptyCloseable();
			}
		};

		final CountDownLatch compl = new CountDownLatch(1);
		
		final AtomicInteger cnt = new AtomicInteger();
		Reactive.whileDo(source, twice)
				.register(new Observer<Integer>() {
					@Override
					public void error(Throwable ex) {
						ex.printStackTrace();
						compl.countDown();
					}
					@Override
					public void finish() {
						compl.countDown();
					}
					@Override
					public void next(Integer value) {
						cnt.incrementAndGet();
					}
					
				});
		
		compl.await();
		
		Assert.assertEquals(n, cnt.get());
	}
}
