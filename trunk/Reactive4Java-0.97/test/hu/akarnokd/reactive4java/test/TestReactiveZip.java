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
import static hu.akarnokd.reactive4java.reactive.Reactive.zip;
import static hu.akarnokd.reactive4java.util.Functions.pairUp;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Pair;
import hu.akarnokd.reactive4java.query.ObservableBuilder;
import hu.akarnokd.reactive4java.reactive.Reactive;
import hu.akarnokd.reactive4java.util.Functions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
	/** Run with different speed. */
	@Test
	public void zipDifferentSpeed() {
		
		Observable<Long> a = Reactive.tick(0, 2, 300, TimeUnit.MILLISECONDS);
		Observable<Long> b = Reactive.tick(0, 2, 500, TimeUnit.MILLISECONDS);
		
		Observable<Pair<Long, Long>> result = Reactive.zip(a, b, Functions.<Long, Long>pairUp());
		
		List<Pair<Long, Long>> expected = new ArrayList<Pair<Long, Long>>();
		expected.add(Pair.of(0L, 0L));
		expected.add(Pair.of(1L, 1L));
		
		
		TestUtil.assertEqual(expected, result);
	}
	/** Run with different speed. */
	@Test/*(timeout = 1500)*/
	public void zipTerminateEarly() {
		
		Observable<Long> a = Reactive.tick(0, 1, 300, TimeUnit.MILLISECONDS);
		Observable<Long> b = Reactive.tick(0, 100, 1000, TimeUnit.MILLISECONDS);
		
		Observable<Pair<Long, Long>> result = Reactive.zip(a, b, Functions.<Long, Long>pairUp());
		
		List<Pair<Long, Long>> expected = new ArrayList<Pair<Long, Long>>();
		expected.add(Pair.of(0L, 0L));
		
		TestUtil.assertEqual(expected, result);
	}
	/**
	 * Tests zip().
	 */
	@Test
	public void zipSameCountMany() {
		ObservableBuilder<Integer> a = from(0, 1, 2);
		ObservableBuilder<Integer> b = from(10, 11, 12);
		ObservableBuilder<Integer> c = from(20, 21, 22);
		
		List<List<Integer>> expected = new ArrayList<List<Integer>>();
		expected.add(TestUtil.newList(0, 10, 20));
		expected.add(TestUtil.newList(1, 11, 21));
		expected.add(TestUtil.newList(2, 12, 22));
		
		@SuppressWarnings("unchecked")
		List<ObservableBuilder<Integer>> sources = TestUtil.newList(a, b, c);
		
		TestUtil.assertEqual(expected, zip(sources));
	}
	/**
	 * Tests zip().
	 */
	@Test
	public void zipDifferentCountMany() {
		final int a0 = 0;
		final int b0 = 1;
		final int a1 = 2;
		final int b1 = 3;
		ObservableBuilder<Integer> a = from(a0, a1, 0);
		ObservableBuilder<Integer> b = from(b0, b1);
		ObservableBuilder<Integer> c = from(b0, b1);
		
		List<List<Integer>> expected = new ArrayList<List<Integer>>();
		expected.add(TestUtil.newList(a0, b0, b0));
		expected.add(TestUtil.newList(a1, b1, b1));
		
		@SuppressWarnings("unchecked")
		List<ObservableBuilder<Integer>> sources = TestUtil.newList(a, b, c);
		
		TestUtil.assertEqual(expected, zip(sources));
		
	}
	/** Run with different speed. */
	@Test
	public void zipDifferentSpeedMany() {
		
		Observable<Long> a = Reactive.tick(0, 2, 300, TimeUnit.MILLISECONDS);
		Observable<Long> b = Reactive.tick(0, 2, 500, TimeUnit.MILLISECONDS);
		Observable<Long> c = Reactive.tick(0, 2, 700, TimeUnit.MILLISECONDS);
		
		List<List<Long>> expected = new ArrayList<List<Long>>();
		expected.add(TestUtil.newList(0L, 0L, 0L));
		expected.add(TestUtil.newList(1L, 1L, 1L));
		
		@SuppressWarnings("unchecked")
		List<Observable<Long>> sources = TestUtil.newList(a, b, c);
		
		TestUtil.assertEqual(expected, zip(sources));
	}
	/** Run with different speed. */
	@Test/*(timeout = 1500)*/
	public void zipTerminateEarlyMany() {
		
		Observable<Long> a = Reactive.tick(0, 1, 300, TimeUnit.MILLISECONDS);
		Observable<Long> b = Reactive.tick(0, 1, 400, TimeUnit.MILLISECONDS);
		Observable<Long> c = Reactive.tick(0, 100, 1000, TimeUnit.MILLISECONDS);
		
		List<List<Long>> expected = new ArrayList<List<Long>>();
		expected.add(TestUtil.newList(0L, 0L, 0L));
		
		@SuppressWarnings("unchecked")
		List<Observable<Long>> sources = TestUtil.newList(a, b, c);
		
		TestUtil.assertEqual(expected, zip(sources));
	}
}
