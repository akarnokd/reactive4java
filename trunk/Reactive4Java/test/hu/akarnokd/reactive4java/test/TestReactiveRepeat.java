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
import hu.akarnokd.reactive4java.interactive.Interactive;
import hu.akarnokd.reactive4java.reactive.Reactive;

import org.junit.Test;

/**
 * Test the Reactive.repeat behavior.
 * @author akarnokd, 2013.11.24.
 */
public class TestReactiveRepeat {
	/** Test the early termination of repeat through take. */
	@Test/*(timeout = 1000)*/
	public void testTakeSome() {
		Observable<Integer> source = Reactive.take(Reactive.repeat(
				Reactive.just(1), Integer.MAX_VALUE), 100);
		
		Iterable<Integer> expected = Interactive.repeat(1, 100);
		TestUtil.assertEqual(expected, source);
	}

}
