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

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * Test Reactive operators, 9.
 * @author akarnokd
 */
public final class Test9 {

	/**
	 * Utility class.
	 */
	private Test9() {
		// utility class
	}
	/**
	 * @param args no arguments
	 * @throws Exception on error
	 */
	public static void main(String[] args) throws Exception {
		ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);
		scheduler.setKeepAliveTime(1, TimeUnit.SECONDS);
		scheduler.allowCoreThreadTimeOut(true);
		scheduler.setRemoveOnCancelPolicy(true);
		
		Future<?> future = scheduler.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				System.out.println("This should not be visible");
			}
		}, 10, 10, TimeUnit.SECONDS);
		
		Thread.sleep(1500);
		
		if (future.cancel(true)) {
			System.out.println("Future cancelled");
		}

		System.out.printf("%nMain finished%n");
	}

}
