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

import hu.akarnokd.reactive4java.base.Functions;
import hu.akarnokd.reactive4java.base.TooManyElementsException;
import hu.akarnokd.reactive4java.reactive.Observable;
import hu.akarnokd.reactive4java.reactive.Reactive;

import java.util.NoSuchElementException;

import org.junit.Test;

/**
 * Test the Reactive.single operator.
 * @author akarnokd, 2013.01.09.
 * @since 0.97
 */
public class TestReactiveSingleAsync {

	/**
	 * Tests singleAsync() in case of 0 element.
	 * @throws Exception on error
	 */
	@Test(expected = NoSuchElementException.class)
	public void singleNoSuchElement() throws Exception {
		Observable<Integer> source = Reactive.empty();
		
		Observable<Integer> result = Reactive.singleAsync(source);
		
		TestUtil.waitForAll(result);
	}

	/**
	 * Tests singleAsync() in case of 1 element.
	 */
	@Test
	public void singleOk() {
		Observable<Integer> source = Reactive.singleton(42);
		
		Observable<Integer> result = Reactive.singleAsync(source);
		
		TestUtil.assertSingle(42, result);
	}
	/**
	 * Tests singleAsync() in case of more than 1 elements.
	 * @throws Exception on error
	 */
	@Test(expected = TooManyElementsException.class)
	public void singleTooManyElements() throws Exception {
		Observable<Integer> source = Reactive.range(0, 10);
		
		Observable<Integer> result = Reactive.singleAsync(source);
		
		TestUtil.waitForAll(result);
	}
	/**
	 * Test for empty source with constant.
	 */
	@Test
	public void singleDefault() {
		Observable<Integer> source = Reactive.empty();
		
		Observable<Integer> result = Reactive.singleAsync(source, 1);
		
		TestUtil.assertSingle(1, result);
	}
	/**
	 * Test for empty source with default supplier.
	 */
	@Test
	public void singleDefaultFunction() {
		Observable<Integer> source = Reactive.empty();
		
		Observable<Integer> result = Reactive.singleAsync(source, Functions.constant0(1));
		
		TestUtil.assertSingle(1, result);
	}
}
