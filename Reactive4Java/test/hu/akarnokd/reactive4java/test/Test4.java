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

import hu.akarnokd.reactive4java.reactive.Observable;
import hu.akarnokd.reactive4java.reactive.Reactive;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

/**
 * Test Reactive operators, 4.
 * @author akarnokd
 */
public final class Test4 {

	/**
	 * Utility class.
	 */
	private Test4() {
		// utility class
	}

	/**
	 * @param args no arguments
	 * @throws Exception on error
	 */
	public static void main(String[] args) throws Exception {

		Observable<Long> tick = Reactive.tick(1, TimeUnit.SECONDS);
		
		Closeable c = tick.register(Reactive.println());
		
		Thread.sleep(5500);
		
		c.close();
		
		System.out.println(Reactive.last(Reactive.range(0, 10)));
		
		System.out.println(Reactive.latest(Reactive.range(0, 10)).iterator().next());
		
		for (Long t : Reactive.latest(Reactive.tick(0, 20, 1, TimeUnit.SECONDS))) {
			System.out.println(t);
			Thread.sleep(3000);
		}
		
		c = Reactive.sample(Reactive.tick(1L, TimeUnit.SECONDS), 3L, TimeUnit.SECONDS).register(Reactive.println());
		
		Thread.sleep(10000);
		
		c.close();
	}

}
