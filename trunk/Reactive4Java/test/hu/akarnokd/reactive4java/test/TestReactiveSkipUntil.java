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

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * Test the Reactive.skipUntil operator.
 * @author akarnokd, 2013.01.08.
 * @since 0.97
 */
public class TestReactiveSkipUntil {

	/**
	 * Simple test to skip anything for a second.
	 */
	@Test
	public void skipUntilSimple() {
		Observable<Long> sequence = Reactive.tick(1, 6, 400, TimeUnit.MILLISECONDS);
		Observable<Long> timer = Reactive.delay(Reactive.singleton(1L), 1, TimeUnit.SECONDS);
		
		Observable<Long> result = Reactive.takeUntil(sequence, timer);
		
		TestUtil.assertEqual(Arrays.asList(3, 4, 5), result);
	}

}
