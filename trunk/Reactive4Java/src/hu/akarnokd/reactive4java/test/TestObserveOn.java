/*
 * Copyright 2011 David Karnok
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

import hu.akarnokd.reactive4java.base.Scheduler;
import hu.akarnokd.reactive4java.reactive.Observable;
import hu.akarnokd.reactive4java.reactive.Reactive;

/**
 * @author akarnokd, 2011.01.29.
 */
public final class TestObserveOn {

	/**
	 * Test class.
	 */
	private TestObserveOn() {
	}

	/**
	 * @param args no arguments
	 * @throws Exception ignored
	 */
	public static void main(String[] args) throws Exception {
		Observable<Integer> xs = Reactive.range(0, 10);
		Scheduler exec = Reactive.getDefaultScheduler();
		Observable<Integer> ys = Reactive.observeOn(xs, exec);
		
		ys.register(Reactive.println());
		
//		exec.shutdown();
//		exec.awaitTermination(Long.MAX_VALUE, TimeUnit.MICROSECONDS);
	}

}
