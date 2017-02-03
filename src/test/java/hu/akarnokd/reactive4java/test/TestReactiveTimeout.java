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
import hu.akarnokd.reactive4java.reactive.Reactive;
import hu.akarnokd.reactive4java.util.DefaultObservable;
import hu.akarnokd.reactive4java.util.Functions;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * Test the reacive timeout operators.
 * @author akarnokd, 2013.01.17.
 */
public class TestReactiveTimeout {
	/**
	 * 
	 * @throws Exception on error
	 */
	@Test
	public void testInitialTimeout() throws Exception {
		Observable<Integer> source = new DefaultObservable<Integer>();
		
		Observable<Integer> other = Reactive.singleton(-1);
		
		Observable<Long> tick = Reactive.tick(0, 1, 1, TimeUnit.SECONDS);
		
		Observable<Integer> result = Reactive.timeout(source, tick, Functions.constant(tick), other);
		
		TestUtil.assertEqual(Arrays.asList(-1), result);
	}
	/**
	 * 
	 * @throws Exception on error
	 */
	@Test
	public void testSubsequentTimeout() throws Exception {
		Observable<Integer> source = Reactive.longToInt(Reactive.tick(0, 2, 1500, TimeUnit.MILLISECONDS));
		
		Observable<Integer> other = Reactive.singleton(-1);
		
		Observable<Long> tick = Reactive.tick(0, 1, 1, TimeUnit.SECONDS);
		
		Observable<Integer> result = Reactive.timeout(source, Functions.constant(tick), other);
		
		TestUtil.assertEqual(Arrays.asList(0, -1), result);
	}

}
