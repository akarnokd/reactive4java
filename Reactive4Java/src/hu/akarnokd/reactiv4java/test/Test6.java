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

import hu.akarnokd.reactiv4java.Action0;
import hu.akarnokd.reactiv4java.Func0;
import hu.akarnokd.reactiv4java.Functions;
import hu.akarnokd.reactiv4java.Interactives;
import hu.akarnokd.reactiv4java.Observable;
import hu.akarnokd.reactiv4java.Observables;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;


/**
 * Test program for the MinLinq stuff.
 * @author akarnokd
 */
public final class Test6 {

	/**
	 * Utility class.
	 */
	private Test6() {
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

		run(Observables.skipUntil(
					Observables.tick(0, 10, 1, TimeUnit.SECONDS), 
					Observables.tick(0, 1, 3, TimeUnit.SECONDS)
			));
		
		run(Observables.skipWhile(
					Observables.range(0, 10),
					Functions.lessThan(5)
			));

		run(Observables.start(new Action0() {
			@Override
			public void invoke() {
				System.err.println("Hello world");
			}
		}));
		
		run(Observables.start(new Func0<String>() {
			@Override
			public String invoke() {
				return "Hello world Function!";
			}
		}));
		
		run(Observables.startWith(Observables.range(5, 10), Interactives.range(0, 5)));
		
		run(Observables.sumInt(Observables.range(0, 101)));
		
		run(Observables.sumBigDecimal(Observables.range(BigDecimal.ZERO, 100, new BigDecimal("0.001"))));
	}

}
