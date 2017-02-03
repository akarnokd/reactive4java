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
import hu.akarnokd.reactive4java.util.Observers;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * Test the Reactive.tick operator.
 * @author akarnokd, 2013.01.10.
 */
public class TestReactiveTick {
	/** Test if the tick produces the required amount of values. */
	@Test
	public void tickValues() {
		Observable<Long> source = Reactive.tick(0, 10, 100, TimeUnit.MILLISECONDS);
		
		TestUtil.assertEqual(Interactive.range(0L, 10L), source);
	}
	/** 
	 * Test if the tick produces the required amount of values near the specified time.
	 * @throws Exception on error 
	 */
	@Test(timeout = 6500)
	public void tickValuesInTime() throws Exception {
		Observable<Long> source = Reactive.tick(0, 5, 1, TimeUnit.SECONDS);
		
		TestUtil.assertEqual(Interactive.range(0L, 5L), source);
	}
	/** 
	 * Test if the tick produces the required amount of values near the specified time.
	 * @throws Exception on error 
	 */
	@Test(timeout = 6500)
	public void tickValuesInTimePrint() throws Exception {
		Observable<Long> source = Reactive.tick(0, 5, 1, TimeUnit.SECONDS);
		
		Reactive.run(Reactive.addTimestamped(source), Observers.println());
	}

}
