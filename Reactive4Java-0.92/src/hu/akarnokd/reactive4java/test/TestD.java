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

import hu.akarnokd.reactive4java.reactive.Observable;
import hu.akarnokd.reactive4java.reactive.Reactive;


/**
 * Test Reactive operators, B.
 * @author akarnokd
 */
public final class TestD {

	/**
	 * Utility class.
	 */
	private TestD() {
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
	public static void main(String[] args)
	throws Exception {

		run(Reactive.sequenceEqual(Reactive.range(0, 5), Reactive.range(0, 10)));
		run(Reactive.sequenceEqual(Reactive.range(0, 5), Reactive.range(1, 5)));
		run(Reactive.sequenceEqual(Reactive.range(0, 5), Reactive.range(0, 5)));
		
		System.out.printf("%nMain finished%n");
	}

}