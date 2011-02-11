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

import hu.akarnokd.reactive4java.base.Func2;
import hu.akarnokd.reactive4java.base.Functions;
import hu.akarnokd.reactive4java.reactive.Observable;
import hu.akarnokd.reactive4java.reactive.Reactive;

import java.util.concurrent.TimeUnit;


/**
 * Test Reactive operators, B.
 * @author akarnokd
 */
public final class TestC {

	/**
	 * Utility class.
	 */
	private TestC() {
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

		Reactive.run(
			Reactive.join(
				Reactive.tick(0, 10, 1, TimeUnit.SECONDS),
				Reactive.tick(0, 10, 3, TimeUnit.SECONDS),
				Functions.<Observable<Long>, Long>constant(Reactive.tick(0, 1, 20, TimeUnit.SECONDS)),
				Functions.<Observable<Long>, Long>constant(Reactive.tick(0, 1, 20, TimeUnit.SECONDS)),
				new Func2<String, Long, Long>() {
					@Override
					public String invoke(Long param1, Long param2) {
						return param1 + " and " + param2;
					}
				}
			),
			Reactive.println()
		);
		
		System.out.printf("%nMain finished%n");
	}

}
