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

import hu.akarnokd.reactive4java.util.SingleCloseable;

import java.io.IOException;

import org.junit.Assert;

import org.junit.Test;

/**
 * Test if the SingleCloseable conforms to its contract.
 * @author akarnokd, 2013.01.12.
 * @since 0.97
 */
public class TestSingleCloseable {
	/** 
	 * Simple close operation.
	 * @throws IOException due to Closeable.close(). 
	 */
	@Test
	public void simpleClosing() throws IOException {
		SingleCloseable sc = new SingleCloseable();
		
		DebugCloseable mc = new DebugCloseable();
		
		sc.set(mc);
		
		Assert.assertFalse("Shouldn't be closed yet", mc.isClosed());

		sc.close();
		
		Assert.assertTrue("Should be closed now", mc.isClosed());

		Assert.assertTrue("Container should be closed now", sc.isClosed());

	}
	/** 
	 * Assignment to a closed container.
	 * @throws IOException due to Closeable.close(). 
	 */
	@Test
	public void alreadyClosed() throws IOException {
		SingleCloseable sc = new SingleCloseable();
		
		DebugCloseable mc = new DebugCloseable();

		sc.close();
		
		sc.set(mc);
		
		Assert.assertTrue("Should be closed now", mc.isClosed());

		Assert.assertTrue("Container should be closed now", sc.isClosed());

	}
	/** 
	 * Check if reassignment throws an exception.
	 * @throws IOException due to Closeable.close(). 
	 */
	@Test(expected = IllegalStateException.class)
	public void rejectAssignment() throws IOException {
		SingleCloseable sc = new SingleCloseable();
		
		DebugCloseable mc = new DebugCloseable();

		sc.set(mc);
		
		DebugCloseable mc2 = new DebugCloseable();

		sc.set(mc2);
		
		// we shouldn't get here
		sc.close();
	}
	/** 
	 * Check if rejected is closed.
	 * @throws IOException due to Closeable.close(). 
	 */
	@Test
	public void rejectedNotClosed() throws IOException {
		SingleCloseable sc = new SingleCloseable();
		
		DebugCloseable mc = new DebugCloseable();

		sc.set(mc);
		
		DebugCloseable mc2 = new DebugCloseable();

		try {
			sc.set(mc2);
		} catch (IllegalStateException ex) {
			// expected
		}
		
		Assert.assertFalse("Shouldn't be closed now", mc2.isClosed());

		sc.close();
	}
	/** 
	 * Check if after closing the container, it should not reference the original closeable
	 * anymore.
	 * @throws IOException due to Closeable.close(). 
	 */
	@Test
	public void notReferencing() throws IOException {
		SingleCloseable sc = new SingleCloseable();
		
		DebugCloseable mc = new DebugCloseable();
		
		sc.set(mc);
		
		Assert.assertEquals(mc, sc.get());
		
		sc.close();
		
		Assert.assertNotSame(mc, sc.get());

	}
}
