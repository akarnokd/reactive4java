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

import hu.akarnokd.reactive4java.base.Action0;
import hu.akarnokd.reactive4java.base.Action1;
import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Functions;
import hu.akarnokd.reactive4java.reactive.Observable;
import hu.akarnokd.reactive4java.reactive.Observers;
import hu.akarnokd.reactive4java.reactive.Reactive;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * Test Reactive operators, B.
 * @author akarnokd
 */
public final class TestB {

	/**
	 * Utility class.
	 */
	private TestB() {
		// utility class
	}
	/** 
	 * Run the observable with a print attached. 
	 * @param observable the source observable
	 * @throws InterruptedException when the current thread is interrupted while
	 * waiting on the observable completion
	 */
	static void run(Observable<?> observable) throws InterruptedException {
		Reactive.run(observable, Observers.print());
	}
	
	/**
	 * @param args no arguments
	 * @throws Exception on error
	 */
	public static void main(String[] args)
	throws Exception {
	
		Reactive.run(Reactive.selectMany(
				Reactive.range(0, 10), 
				new Func1<Integer, Observable<Integer>>() {
			@Override
			public Observable<Integer> invoke(Integer param1) {
				return Reactive.range(0, param1);
			}
			
		}), Observers.println());
		
		
		run(Reactive.tick(0, 10, 1, TimeUnit.SECONDS));
		
		Observable<Observable<Long>> window = Reactive.window(
				Reactive.tick(0, 10, 1, TimeUnit.SECONDS), 
				Functions.constant0(Reactive.tick(0, 2, 2, TimeUnit.SECONDS)));
		
		final CountDownLatch cdl = new CountDownLatch(1);
		
		window.register(Observers.toObserver(new Action1<Observable<Long>>() {
			@Override
			public void invoke(Observable<Long> value) {
				System.out.println("New window");
				value.register(Observers.println());
			}
		},
		new Action1<Throwable>() {
			@Override
			public void invoke(Throwable value) {
				value.printStackTrace();
			}
		},
		new Action0() {
			@Override
			public void invoke() {
				System.out.println("Finished");
				cdl.countDown();
			}
		}
		));

		cdl.await();
		
		System.out.printf("%nMain finished%n");
	}

}
