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
import static hu.akarnokd.reactive4java.reactive.Reactive.concat;
import static hu.akarnokd.reactive4java.reactive.Reactive.count;
import static hu.akarnokd.reactive4java.reactive.Reactive.empty;
import static hu.akarnokd.reactive4java.reactive.Reactive.single;
import static hu.akarnokd.reactive4java.reactive.Reactive.skip;
import static hu.akarnokd.reactive4java.reactive.Reactive.skipLast;
import static hu.akarnokd.reactive4java.reactive.Reactive.skipWhile;
import static hu.akarnokd.reactive4java.util.Functions.equal;
import hu.akarnokd.reactive4java.query.ObservableBuilder;
import hu.akarnokd.reactive4java.base.Observable;

import org.junit.Test;

/**
 * Test the Reactive.skipXXX operators.
 * @author akarnokd, 2013.01.09.
 * @since 0.97
 */
public class TestReactiveSkip {

	/**
	 * Tests skipLast().
	 */
	@Test
	public void skipLastOk() {
		Observable<Integer> prefix = from(1, 2);
		Observable<Integer> postfix = from(3, 4);
		Observable<Integer> o = concat(prefix, postfix);
		Integer count = single(count(postfix));
		TestUtil.assertEqual(prefix, skipLast(o, count));
	}

	/**
	 * Tests skip().
	 */
	@Test
	public void skipOk() {
		Observable<Integer> prefix = from(1, 2);
		Observable<Integer> postfix = from(3, 4);
		Observable<Integer> o = concat(prefix, postfix);
		Integer count = single(count(prefix));
		TestUtil.assertEqual(postfix, skip(o, count));
	}

	/**
	 * Tests skipWhile() with all elements skipped.
	 */
	@Test
	public void skipWhileAll() {
		Integer value = 42;
		TestUtil.assertEqual(empty(), skipWhile(from(value, value), equal(value)));
	}

	/**
	 * Tests skipWhile() with no elements skipped.
	 */
	@Test
	public void skipWhileNone() {
		Integer value = 42;
		ObservableBuilder<Integer> o = from(0, value);
		TestUtil.assertEqual(o, skipWhile(o, equal(value)));
	}

	/**
	 * Tests skipWhile() with some elements skipped.
	 */
	@Test
	public void skipWhileSome() {
		Integer value = 42;
		Observable<Integer> prefix = from(value, value);
		Observable<Integer> postfix = from(0, value);
		Observable<Integer> o = concat(prefix, postfix);
		TestUtil.assertEqual(postfix, skipWhile(o, equal(value)));
	}

}
