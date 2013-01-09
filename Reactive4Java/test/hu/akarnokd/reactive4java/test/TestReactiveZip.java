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

import static hu.akarnokd.reactive4java.base.Functions.pairUp;
import static hu.akarnokd.reactive4java.query.ObservableBuilder.from;
import static hu.akarnokd.reactive4java.reactive.Reactive.zip;
import hu.akarnokd.reactive4java.base.Pair;
import hu.akarnokd.reactive4java.query.ObservableBuilder;

import org.junit.Test;

/**
 * Test the Reactive.zip operators.
 * @author akarnokd, 2013.01.09.
 * @since 0.97
 */
public class TestReactiveZip {
	/**
	 * Tests zip().
	 */
	@Test
	public void zipSameCount() {
		final int a0 = 0;
		final int b0 = 1;
		final int a1 = 2;
		final int b1 = 3;
		ObservableBuilder<Integer> a = from(a0, a1, 0);
		ObservableBuilder<Integer> b = from(b0, b1, 1);
		@SuppressWarnings("unchecked")
		ObservableBuilder<Pair<Integer, Integer>> expected = from(
				Pair.of(a0, b0), 
				Pair.of(a1, b1),
				Pair.of(0, 1));
		
		TestUtil.assertEqual(expected, zip(a, b, pairUp()));
	}
	/**
	 * Tests zip().
	 */
	@Test
	public void zipDifferentCount() {
		final int a0 = 0;
		final int b0 = 1;
		final int a1 = 2;
		final int b1 = 3;
		ObservableBuilder<Integer> a = from(a0, a1, 0);
		ObservableBuilder<Integer> b = from(b0, b1);
		@SuppressWarnings("unchecked")
		ObservableBuilder<Pair<Integer, Integer>> expected = from(Pair.of(a0, b0), Pair.of(a1, b1));
		TestUtil.assertEqual(expected, zip(a, b, pairUp()));
	}
}
