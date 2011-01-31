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

import hu.akarnokd.reactiv4java.Functions;
import hu.akarnokd.reactiv4java.Observable;
import hu.akarnokd.reactiv4java.Observables;

import java.util.concurrent.TimeUnit;


/**
 * Test program for the MinLinq stuff.
 * @author akarnokd
 */
public final class Test7 {

	/**
	 * Utility class.
	 */
	private Test7() {
		// utility class
	}
	/** 
	 * Run the observable with a print attached. 
	 * @param observable the source observable
	 * @throws InterruptedException when the current thread is interrupted while
	 * waiting on the observable completion
	 */
	static void run(Observable<?> observable) throws InterruptedException {
		Observables.run(observable, Observables.print());
	}
	
	/**
	 * @param args no arguments
	 * @throws Exception on error
	 */
	public static void main(String[] args) throws Exception {
		run(
			Observables.takeUntil(
				Observables.tick(1, TimeUnit.SECONDS),
				Observables.tick(5, TimeUnit.SECONDS)
			)
		);

		run(
			Observables.take(
				Observables.tick(1, TimeUnit.SECONDS),
				10
			)
		);
		
		run(Observables.takeWhile(Observables.tick(1, TimeUnit.SECONDS), Functions.lessThan(5L)));
		
		
		run(
			Observables.addTimestamped(
				Observables.throttle(
					Observables.concat(
						Observables.tick(0, 10, 200, TimeUnit.MILLISECONDS),
						Observables.tick(10, 5, 1, TimeUnit.SECONDS)
					),
				500, TimeUnit.MILLISECONDS)
			)
		);
		
		System.out.printf("%nMain finished%n");
	}

}
