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

import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.reactive.Reactive;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;

import org.junit.Test;

/**
 * Test the Reactive.Amb operator's behavior.
 * @author akarnokd, 2013.01.08.
 * @since 0.97
 */
public class TestReactiveAmb {
	/** The left observable will fire first. */
	@Test
	public void ambLeftFirst() {
		Observable<Long> left = Reactive.tick(1, 2, 1, TimeUnit.SECONDS);
		Observable<Long> right = Reactive.tick(2, 3, 2, TimeUnit.SECONDS);
		
		Observable<Long> result = Reactive.amb(left, right);
		
		Long value = Reactive.first(result);
		if (value.longValue() != 1L) {
			Assert.fail("Left did not fire first?!");
		}
	}
	/** The right observable will fire first. */
	@Test
	public void ambRightFirst() {
		Observable<Long> left = Reactive.tick(1, 2, 2, TimeUnit.SECONDS);
		Observable<Long> right = Reactive.tick(2, 3, 1, TimeUnit.SECONDS);
		
		Observable<Long> result = Reactive.amb(left, right);
		
		Long value = Reactive.first(result);
		if (value.longValue() != 2L) {
			Assert.fail("Right did not fire first?!");
		}
	}
	/** 
	 * The left observable should bring the register to completion before
	 * the right is even registered.
	 */
	@Test(timeout = 2000)
	public void ambPremature() {
		Observable<Long> left = Reactive.singleton(1L);
		Observable<Long> right = Reactive.tick(2, 100, 1, TimeUnit.SECONDS);
		
		Observable<Long> result = Reactive.amb(left, right);
		
		Long value = Reactive.first(result);
		if (value.longValue() != 1L) {
			Assert.fail("Right did not fire first?!");
		}
	}
}
