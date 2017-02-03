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
import static hu.akarnokd.reactive4java.reactive.Reactive.empty;
import static hu.akarnokd.reactive4java.reactive.Reactive.single;
import static org.junit.Assert.assertEquals;
import hu.akarnokd.reactive4java.base.TooManyElementsException;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.reactive.Reactive;
import hu.akarnokd.reactive4java.util.Functions;

import java.util.NoSuchElementException;

import org.junit.Assert;

import org.junit.Test;

/**
 * Test the Reactive.single operator.
 * @author akarnokd, 2013.01.09.
 * @since 0.97
 */
public class TestReactiveSingle {

	/**
	 * Tests single() in case of 0 element.
	 */
	@Test(expected = NoSuchElementException.class)
	public void singleNoSuchElement() {
		single(empty());
	}

	/**
	 * Tests single() in case of 1 element.
	 */
	@Test
	public void singleOk() {
		Integer expected = 42;
		Observable<Integer> o = from(expected);
		assertEquals(expected, single(o));
	}
	/**
	 * Tests single() in case of more than 1 elements.
	 */
	@Test(expected = TooManyElementsException.class)
	public void singleTooManyElements() {
		single(from(1, 2));
	}
	/**
	 * Test for empty source with constant.
	 */
	@Test
	public void singleDefault() {
		Observable<Integer> source = Reactive.empty();
		
		Integer result = Reactive.single(source, 1);
		
		Assert.assertEquals((Integer)1, result);
	}
	/**
	 * Test for empty source with default supplier.
	 */
	@Test
	public void singleDefaultFunction() {
		Observable<Integer> source = Reactive.empty();
		
		Integer result = Reactive.single(source, Functions.constant0(1));
		
		Assert.assertEquals((Integer)1, result);
	}
}
