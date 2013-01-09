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
import hu.akarnokd.reactive4java.reactive.Observable;
import hu.akarnokd.reactive4java.reactive.Reactive;

import java.util.NoSuchElementException;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Test the Reactive.first operator.
 * @author akarnokd, 2013.01.09.
 * @since 0.97
 */
public class TestReactiveLast {
	/** Test a surely existing value. */
	@Test
	public void existing() {
		Observable<Integer> source = Reactive.range(0, 10);
		
		Integer result = Reactive.last(source);
		
		Assert.assertEquals((Integer)9, result);
	}
	/** Test a surely existing value with default. */
	@Test
	public void existingDefault() {
		Observable<Integer> source = Reactive.singleton(0);
		
		Integer result = Reactive.last(source, 10);
		
		Assert.assertEquals((Integer)0, result);
	}
	/** Test a surely existing value with default. */
	@Test
	public void existingDefaultFunction() {
		Observable<Integer> source = Reactive.singleton(0);
		
		Integer result = Reactive.last(source, Functions.constant0(10));
		
		Assert.assertEquals((Integer)0, result);
	}
	/** Test a surely existing value. */
	@Test(expected = NoSuchElementException.class)
	public void nonexisting() {
		Observable<Integer> source = Reactive.empty();
		
		Integer result = Reactive.last(source);
		
		Assert.assertEquals((Integer)0, result);
	}
	/** Test a surely existing value with default. */
	@Test
	public void nonexistingDefault() {
		Observable<Integer> source = Reactive.empty();
		
		Integer result = Reactive.last(source, 10);
		
		Assert.assertEquals((Integer)10, result);
	}
	/** Test a surely existing value with default. */
	@Test
	public void nonexistingDefaultFunction() {
		Observable<Integer> source = Reactive.empty();
		
		Integer result = Reactive.last(source, Functions.constant0(10));
		
		Assert.assertEquals((Integer)10, result);
	}

}
