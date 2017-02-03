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

import hu.akarnokd.reactive4java.base.Func2;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.reactive.Reactive;
import hu.akarnokd.reactive4java.util.Functions;

import java.util.Arrays;

import org.junit.Test;

/**
 * Test the select operators.
 * @author akarnokd, 2013.01.16.
 * @since 0.97
 */
public class TestReactiveSelect {
	/** Plain test. */
	@Test
	public void testSimple() {
		Observable<Integer> source = Reactive.range(0, 5);
		
		Observable<Integer> result = Reactive.select(source, Functions.incrementInt());
		
		TestUtil.assertEqual(Arrays.asList(1, 2, 3, 4, 5), result);
	}

	/** Test simple indexing. */
	@Test
	public void testSimpleIndexing() {
		Observable<Integer> source = Reactive.range(0, 5);
		
		Observable<Integer> result = Reactive.select(source, new Func2<Integer, Integer, Integer>() {
			@Override
			public Integer invoke(Integer param1, Integer param2) {
				return param1 * param2;
			}
		});
		
		TestUtil.assertEqual(Arrays.asList(0 * 0, 1 * 1, 2 * 2, 3 * 3, 4 * 4), result);
	}
	/** Test simple indexing. */
	@Test
	public void testSimpleLongIndexing() {
		Observable<Integer> source = Reactive.range(0, 5);
		
		Observable<Integer> result = Reactive.selectLong(source, new Func2<Integer, Long, Integer>() {
			@Override
			public Integer invoke(Integer param1, Long param2) {
				return (int)(param1 * param2);
			}
		});
		
		TestUtil.assertEqual(Arrays.asList(0 * 0, 1 * 1, 2 * 2, 3 * 3, 4 * 4), result);
	}

}
