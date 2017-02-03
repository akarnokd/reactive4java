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

import hu.akarnokd.reactive4java.base.CloseableIterable;
import hu.akarnokd.reactive4java.base.CloseableIterator;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.reactive.Reactive;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;

import org.junit.Test;

/**
 * Test the behavior of the Reactive.latest operator.
 * @author akarnokd, 2013.01.12.
 * @since 0.97
 */
public class TestReactiveLatest {
	/** 
	 * Receive two values and wait for the finish event.
	 * @throws Exception on error 
	 */
	@Test(timeout = 4000)
	public void testSimpleReception() throws Exception {
		Observable<Long> source = Reactive.tick(0, 3, 1, TimeUnit.SECONDS);
		
		CloseableIterable<Long> result = Reactive.latest(source);
		
		CloseableIterator<Long> it = result.iterator();
		try {
			Assert.assertEquals((Long)0L, it.next());
			Assert.assertEquals((Long)1L, it.next());
			Thread.sleep(1500);
			Assert.assertFalse(it.hasNext());
		} finally {
			it.close();
		}
		
	}

}
