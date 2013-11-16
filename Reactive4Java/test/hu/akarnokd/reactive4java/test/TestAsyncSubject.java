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
package hu.akarnokd.reactive4java.test;

import hu.akarnokd.reactive4java.reactive.Reactive;
import hu.akarnokd.reactive4java.util.AsyncSubject;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;

import org.junit.Test;

/**
 * Test the AsyncSubject behavior.
 * @author akarnokd, 2013.01.11.
 */
public class TestAsyncSubject {
	/** 
	 * Test simple instant return with value.
	 * @throws InterruptedException on interruption 
	 */
	@Test(timeout = 1000)
	public void testSimpleValue() throws InterruptedException {
		AsyncSubject<Integer> subject = new AsyncSubject<Integer>();
		subject.next(1);
		subject.finish();
		
		Assert.assertEquals((Integer)1, subject.get());
	}
	/** 
	 * Test simple return with last value value.
	 * @throws InterruptedException on interruption 
	 */
	@Test(timeout = 1000)
	public void testSimpleLastValue() throws InterruptedException {
		AsyncSubject<Integer> subject = new AsyncSubject<Integer>();
		Reactive.range(0, 10).register(subject);
		
		Assert.assertEquals((Integer)9, subject.get());
	}
	/** 
	 * Test simple instant return with value delayed by a second.
	 * @throws InterruptedException on interruption 
	 */
	@Test(timeout = 1500)
	public void testDelayedValue() throws InterruptedException {
		AsyncSubject<Long> subject = new AsyncSubject<Long>();
		Reactive.tick(1, 2, 1, TimeUnit.SECONDS).register(subject);
		
		Assert.assertEquals((Long)1L, subject.get());
	}
	/** 
	 * Test simple instant return with empty contents.
	 * @throws InterruptedException on interruption 
	 */
	@Test(timeout = 1000, expected = NoSuchElementException.class)
	public void testEmpty() throws InterruptedException {
		AsyncSubject<Integer> subject = new AsyncSubject<Integer>();
		subject.finish();

		subject.get();
	}
	/** 
	 * Test timeout of get.
	 * @throws Exception on error 
	 */
	@Test(timeout = 1500, expected = TimeoutException.class)
	public void testTimeout() throws Exception {
		AsyncSubject<Integer> subject = new AsyncSubject<Integer>();
		
		subject.get(1, TimeUnit.SECONDS);
	}
	/** 
	 * Test simple instant error.
	 * @throws InterruptedException on interruption 
	 */
	@Test(timeout = 1000, expected = NoSuchElementException.class)
	public void testSimpleError() throws InterruptedException {
		AsyncSubject<Integer> subject = new AsyncSubject<Integer>();
		subject.error(new NoSuchElementException());
		subject.finish();
		
		subject.get();
	}
}
