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

import org.junit.Test;

/**
 * Test the Reactive.firstAsync operator.
 * @author akarnokd, 2013.01.09.
 * @since 0.97
 */
public class TestReactiveLastAsync {
	/** Test an surely existing value. */
	@Test
	public void existing() {
		Observable<Integer> source = Reactive.range(0, 10);
		
		Observable<Integer> result = Reactive.lastAsync(source);
		
		TestUtil.assertSingle(9, result);
	}
	/** 
	 * Test an nonexistent value. 
	 * @throws Exception on error. 
	 */
	@Test(expected = NoSuchElementException.class)
	public void nonexistent() throws Exception {
		Observable<Integer> source = Reactive.empty();
		
		Observable<Integer> result = Reactive.lastAsync(source);
		
		TestUtil.waitForAll(result);
	}
	/** 
	 * Test an default value in case of empty source. 
	 * @throws Exception on error. 
	 */
	@Test
	public void nonexistentDefault() throws Exception {
		Observable<Integer> source = Reactive.empty();
		
		Observable<Integer> result = Reactive.lastAsync(source, 1);
		
		TestUtil.assertSingle(1, result);
	}
	/** 
	 * Test an default supplied value in case of empty source. 
	 * @throws Exception on error. 
	 */
	@Test
	public void nonexistentDefaultFunction() throws Exception {
		Observable<Integer> source = Reactive.empty();
		
		Observable<Integer> result = Reactive.lastAsync(source, Functions.constant0(1));
		
		TestUtil.assertSingle(1, result);
	}
}
