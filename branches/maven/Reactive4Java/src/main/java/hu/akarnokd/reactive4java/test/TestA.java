/*
 * Copyright 2011-2012 David Karnok
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
import hu.akarnokd.reactive4java.interactive.Interactive;
import hu.akarnokd.reactive4java.reactive.Observable;
import hu.akarnokd.reactive4java.reactive.Reactive;

import java.util.concurrent.TimeUnit;


/**
 * Test Reactive operators, A.
 * @author akarnokd
 */
public final class TestA {

	/**
	 * Utility class.
	 */
	private TestA() {
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
		run(
			Reactive.timeout(
				Reactive.tick(200, TimeUnit.SECONDS), 5, TimeUnit.SECONDS)
		);
		
		run(
				Reactive.timeout(
					Reactive.tick(200, TimeUnit.SECONDS), 
					2, 
					TimeUnit.SECONDS,
					Reactive.tick(0, 5, 1, TimeUnit.SECONDS)
				)
			);
		
		run(
			Reactive.zip(
				Reactive.tick(0, 5, 1, TimeUnit.SECONDS), 
				Interactive.range(0, 3),
				new Func2<Long, Integer, Long>() {
					@Override
					public Long invoke(Long param1, Integer param2) {
						return param1 * 10 + param2;
					}
				}
			)
		);

		run(
			Reactive.zip(
				Reactive.tick(0, 5, 1, TimeUnit.SECONDS), 
				Reactive.tick(0, 3, 100, TimeUnit.MILLISECONDS), 
				new Func2<Long, Long, Long>() {
					@Override
					public Long invoke(Long param1, Long param2) {
						return param1 * 10 + param2;
					}
				}
			)
		);

		System.out.printf("%nMain finished%n");
	}

}
