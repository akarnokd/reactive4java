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

package hu.akarnokd.reactive4java.test.old;

import hu.akarnokd.reactive4java.base.Action0;
import hu.akarnokd.reactive4java.base.Func0;
import hu.akarnokd.reactive4java.base.Functions;
import hu.akarnokd.reactive4java.interactive.Interactive;
import hu.akarnokd.reactive4java.reactive.Observable;
import hu.akarnokd.reactive4java.reactive.Reactive;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;


/**
 * Test Reactive operators, 6.
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
		Reactive.run(observable, Reactive.print());
	}
	
	/**
	 * @param args no arguments
	 * @throws Exception on error
	 */
	public static void main(String[] args) throws Exception {

		run(Reactive.skipUntil(
					Reactive.tick(0, 10, 1, TimeUnit.SECONDS), 
					Reactive.tick(0, 1, 3, TimeUnit.SECONDS)
			));
		
		run(Reactive.skipWhile(
					Reactive.range(0, 10),
					Functions.lessThan(5)
			));

		run(Reactive.start(new Action0() {
			@Override
			public void invoke() {
				System.err.println("Hello world");
			}
		}));
		
		run(Reactive.start(new Func0<String>() {
			@Override
			public String invoke() {
				return "Hello world Function!";
			}
		}));
		
		run(Reactive.startWith(Reactive.range(5, 10), Interactive.range(0, 5)));
		
		run(Reactive.sumInt(Reactive.range(0, 101)));
		
		run(Reactive.sumBigDecimal(Reactive.range(BigDecimal.ZERO, 100, new BigDecimal("0.001"))));
	}

}
