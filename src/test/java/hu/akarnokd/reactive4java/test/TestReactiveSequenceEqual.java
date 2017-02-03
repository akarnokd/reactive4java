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

import static hu.akarnokd.reactive4java.query.ObservableBuilder.from;
import static hu.akarnokd.reactive4java.reactive.Reactive.concat;
import static hu.akarnokd.reactive4java.reactive.Reactive.empty;
import static hu.akarnokd.reactive4java.reactive.Reactive.sequenceEqual;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.reactive.Reactive;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * Test for Reactive.sequenceEqual operator.
 * @author akarnokd, 2013.01.08.
 * @since 0.97
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
		
		TestUtil.assertEqual(Arrays.asList(false), values);
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
	@Test
	public void testMixedEqual() throws Exception {
		RuntimeException ex = new RuntimeException();
		
		Observable<Integer> value1 = Reactive.singleton(1);
		Observable<Integer> value2 = Reactive.throwException(ex);
		
		Observable<Boolean> result = Reactive.sequenceEqual(value1, value2);
		
		List<Boolean> values = TestUtil.waitForAll(result);
		
		TestUtil.assertEqual(Arrays.asList(false), values);
	}
	/**
	 * Tests the commutativity of sequenceEqual().
	 */
	@Test
	public void sequenceEqualCommutative() {
		Observable<Integer> prefix = from(1, 2);
		Observable<Integer> o = concat(prefix, from(3, 4));
		TestUtil.assertEqual(sequenceEqual(prefix, o), sequenceEqual(o, prefix));
	}
	/**
	 * Tests sequenceEqual() in case of an empty and non-empty sequence.
	 */
	@Test
	public void sequenceEqualNotBecauseEmpty() {
		TestUtil.assertNotEqual(from(1, 2), empty());
	}
	/**
	 * Tests sequenceEqual() in case of different sequences.
	 */
	@Test
	public void sequenceEqualNotBecauseJustPrefix() {
		Observable<Integer> prefix = from(1, 2);
		Observable<Integer> o = concat(prefix, from(3, 4));
		TestUtil.assertNotEqual(o, prefix);
	}
	/**
	 * Tests sequenceEqual() in case of equal sequences.
	 */
	@Test
	public void sequenceEqualOk() {
		Observable<Integer> o = from(1, 2);
		TestUtil.assertEqual(o, o);
	}

}
