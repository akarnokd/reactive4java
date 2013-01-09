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

package hu.akarnokd.reactive4java.test.old;

import hu.akarnokd.reactive4java.reactive.Observable;
import hu.akarnokd.reactive4java.reactive.Reactive;

import java.util.concurrent.TimeUnit;


/**
 * Test Reactive operators, 8.
 * @author akarnokd
 */
public final class Test8 {

	/**
	 * Utility class.
	 */
	private Test8() {
		// utility class
	}
	/** 
	 * Run the observable with a print attached. 
	 * @param observable the source observable
	 * @throws InterruptedException when the current thread is interrupted while
	 * waiting on the observable completion
	 */
	static void run(Observable<?> observable) throws InterruptedException {
		Reactive.run(observable, Reactive.print());
	}
	
	/**
	 * @param args no arguments
	 * @throws Exception on error
	 */
	public static void main(String[] args) throws Exception {
		Observable<Long> cc = Reactive.concat(
				Reactive.tick(0, 10, 200, TimeUnit.MILLISECONDS),
				Reactive.tick(10, 15, 1, TimeUnit.SECONDS)
			);
		
		run(cc);
		run(cc);
		
		run(
			Reactive.addTimestamped(
				Reactive.throttle(
					cc,
				500, TimeUnit.MILLISECONDS)
			)
		);
		
		run(Reactive.addTimeInterval(cc));
		
		System.out.printf("%nMain finished%n");
	}

}
