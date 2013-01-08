/*
 * Copyright 2011-2012 David Karnok
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

import hu.akarnokd.reactive4java.reactive.Observable;
import hu.akarnokd.reactive4java.reactive.Reactive;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * Test for Reactive.sequenceEqual operator.
 * @author akarnokd, 2013.01.08.
 */
public class TestReactiveSequenceEqual {

	/**
	 * Test if equality is properly detected.
	 * @throws Exception on error
	 */
	@Test(timeout = 2000)
	public void testEqual() throws Exception {
		Observable<Integer> value1 = Reactive.range(1, 10);
		Observable<Integer> value2 = Reactive.range(1, 10);
		
		Observable<Boolean> result = Reactive.sequenceEqual(value1, value2);
		
		List<Boolean> values = TestUtil.waitForAll(result);
		
		TestUtil.assertEqual(Arrays.asList(true), values);
	}
	/**
	 * Test if difference is properly detected.
	 * @throws Exception on error
	 */
	@Test(timeout = 2000)
	public void testNotEqual() throws Exception {
		Observable<Integer> value1 = Reactive.range(1, 10);
		Observable<Integer> value2 = Reactive.range(1, 5);
		
		Observable<Boolean> result = Reactive.sequenceEqual(value1, value2);
		
		List<Boolean> values = TestUtil.waitForAll(result);
		
		TestUtil.assertNotEqual(Arrays.asList(false), values);
	}
	/**
	 * Test if equality is properly detected in case of empty sources.
	 * @throws Exception on error
	 */
	@Test(timeout = 2000)
	public void testEmptyEqual() throws Exception {
		Observable<Integer> value1 = Reactive.empty();
		Observable<Integer> value2 = Reactive.empty();
		
		Observable<Boolean> result = Reactive.sequenceEqual(value1, value2);
		
		List<Boolean> values = TestUtil.waitForAll(result);
		
		TestUtil.assertEqual(Arrays.asList(true), values);
	}
	/**
	 * Test if equality is properly detected between exceptions.
	 * @throws Exception on error
	 */
	@Test(timeout = 2000)
	public void testErrorEqual() throws Exception {
		RuntimeException ex = new RuntimeException();
		
		Observable<Integer> value1 = Reactive.throwException(ex);
		Observable<Integer> value2 = Reactive.throwException(ex);
		
		Observable<Boolean> result = Reactive.sequenceEqual(value1, value2);
		
		List<Boolean> values = TestUtil.waitForAll(result);
		
		TestUtil.assertEqual(Arrays.asList(true), values);
	}
	/**
	 * Test if difference is properly detected.
	 * @throws Exception on error
	 */
	@Test(timeout = 2000)
	public void testMixedEqual() throws Exception {
		RuntimeException ex = new RuntimeException();
		
		Observable<Integer> value1 = Reactive.singleton(1);
		Observable<Integer> value2 = Reactive.throwException(ex);
		
		Observable<Boolean> result = Reactive.sequenceEqual(value1, value2);
		
		List<Boolean> values = TestUtil.waitForAll(result);
		
		TestUtil.assertEqual(Arrays.asList(true), values);
	}

}
