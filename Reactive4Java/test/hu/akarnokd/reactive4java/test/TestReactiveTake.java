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

import static hu.akarnokd.reactive4java.base.Functions.equal;
import static hu.akarnokd.reactive4java.query.ObservableBuilder.from;
import static hu.akarnokd.reactive4java.reactive.Reactive.concat;
import static hu.akarnokd.reactive4java.reactive.Reactive.count;
import static hu.akarnokd.reactive4java.reactive.Reactive.empty;
import static hu.akarnokd.reactive4java.reactive.Reactive.single;
import static hu.akarnokd.reactive4java.reactive.Reactive.take;
import static hu.akarnokd.reactive4java.reactive.Reactive.takeLast;
import static hu.akarnokd.reactive4java.reactive.Reactive.takeWhile;
import hu.akarnokd.reactive4java.reactive.Observable;

import org.junit.Test;

/**
 * Test the Reactive.takeXXX operators.
 * @author Denes Harmath, 2012.07.16.
 * @since 0.97
 */
public class TestReactiveTake {
	/**
	 * Tests take().
	 */
	@Test
	public void takeOk() {
		Observable<Integer> prefix = from(1, 2);
		Observable<Integer> postfix = from(3, 4);
		Observable<Integer> o = concat(prefix, postfix);
		Integer count = single(count(prefix));
		TestUtil.assertEqual(prefix, take(o, count));
	}
	/**
	 * Tests takeLast().
	 */
	@Test
	public void takeLastOk() {
		Observable<Integer> prefix = from(1, 2);
		Observable<Integer> postfix = from(3, 4);
		Observable<Integer> o = concat(prefix, postfix);
		Integer count = single(count(postfix));
		TestUtil.assertEqual(postfix, takeLast(o, count));
	}
	/**
	 * Tests takeWhile() with some elements taken.
	 */
	@Test
	public void takeWhileSome() {
		Integer value = 42;
		Observable<Integer> prefix = from(value, value);
		Observable<Integer> postfix = from(0, value);
		Observable<Integer> o = concat(prefix, postfix);
		TestUtil.assertEqual(prefix, takeWhile(o, equal(value)));
	}
	/**
	 * Tests takeWhile() with all elements taken.
	 */
	@Test
	public void takeWhileAll() {
		Integer value = 42;
		Observable<Integer> o = from(value, value);
		TestUtil.assertEqual(o, takeWhile(o, equal(value)));
	}
	/**
	 * Tests takeWhile() with no elements taken.
	 */
	@Test
	public void takeWhileNone() {
		Integer value = 42;
		TestUtil.assertEqual(empty(), takeWhile(from(0, value), equal(value)));
	}
}

