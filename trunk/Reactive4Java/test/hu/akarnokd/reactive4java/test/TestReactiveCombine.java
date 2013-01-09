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
import static hu.akarnokd.reactive4java.reactive.Reactive.combine;
import static java.util.Arrays.asList;
import hu.akarnokd.reactive4java.query.ObservableBuilder;

import java.util.List;

import org.junit.Test;

/**
 * Test the Reactive.combine operator.
 * @author akarnokd, 2013.01.09.
 * @since 0.97
 */
public class TestReactiveCombine {

	/**
	 * Tests combine() with observables.
	 */
	@Test
	public void combineObservables() {
		final int a0 = 0;
		final int b0 = 1;
		final int a1 = 2;
		final int b1 = 3;
		ObservableBuilder<Integer> a = from(a0, a1, 0);
		ObservableBuilder<Integer> b = from(b0, b1);
		@SuppressWarnings("unchecked")
		ObservableBuilder<List<Integer>> expected = from(asList(a0, b0), asList(a1, b1));
		@SuppressWarnings("unchecked")
		List<ObservableBuilder<Integer>> asList = asList(a, b);
		TestUtil.assertEqual(expected, combine(asList));
	}

	/**
	 * Tests combine() with value.
	 */
	@Test
	public void combineValue() {
		final int a0 = 0;
		final int a1 = 1;
		final int value = 3;
		ObservableBuilder<Integer> a = from(a0, a1);
		@SuppressWarnings("unchecked")
		ObservableBuilder<List<Integer>> expected = from(asList(a0, value), asList(a1, value));
		TestUtil.assertEqual(expected, combine(a, value));
	}

}
