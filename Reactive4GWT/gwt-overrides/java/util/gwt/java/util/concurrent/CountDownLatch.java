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
package java.util.concurrent;

/**
 * The GWT version of the j.u.c.CountDownLatch.
 * @author akarnokd, 2011.02.19.
 */
public class CountDownLatch {
	/**
	 * Constructor.
	 * @param value the initial countdown value
	 */
	public CountDownLatch(int value) {
		
	}
	/**
	 * Count down.
	 */
	public void countDown() {
		
	}
	/**
	 * Await the latch to become zero.
	 * @throws InterruptedException on interruption
	 */
	public void await() throws InterruptedException {
		
	}
	/**
	 * Await the latch to become zero.
	 * @param time the time to wait
	 * @param unit the time unit
	 * @return true if timeout happened
	 * @throws InterruptedException on interruption
	 */
	public boolean await(long time, TimeUnit unit) throws InterruptedException {
		return false;
	}
}
