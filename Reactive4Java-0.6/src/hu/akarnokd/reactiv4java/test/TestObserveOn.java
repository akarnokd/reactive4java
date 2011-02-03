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
package hu.akarnokd.reactiv4java.test;

import hu.akarnokd.reactiv4java.Observable;
import hu.akarnokd.reactiv4java.Observables;
import hu.akarnokd.reactiv4java.Scheduler;

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
		Observable<Integer> xs = Observables.range(0, 10);
		Scheduler exec = Observables.getDefaultScheduler();
		Observable<Integer> ys = Observables.observeOn(xs, exec);
		
		ys.register(Observables.println());
		
//		exec.shutdown();
//		exec.awaitTermination(Long.MAX_VALUE, TimeUnit.MICROSECONDS);
	}

}
