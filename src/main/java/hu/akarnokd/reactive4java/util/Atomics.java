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
package hu.akarnokd.reactive4java.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

/**
 * Utility classes to perform some AtomicXYZ related operations
 * not supported by the classes themselves. 
 * @author akarnokd, 2013.01.12.
 * @since 0.97
 */
public final class Atomics {
	/** Utility class. */
	private Atomics() { }
	/**
	 * Atomically compares the content of <code>ref</code> with the <code>compareWith</code>
	 * value by using nullsafe object equality and 
	 * replaces it with the newValue if they equal. The method
	 * returns the old value of ref regardless of whether the exchange actually happened.
	 * @param ref the target of the exchange
	 * @param compareWith the value to compare against
	 * @param newValue the the new value to replace with
	 * @return the old value of the ref
	 * @param <T> the element type
	 */
	public static <T> T compareExchange(
			@Nonnull AtomicReference<T> ref, T compareWith, T newValue) {
		T old = null;
		
		do {
			old = ref.get();
			if (old != compareWith && (old == null || !old.equals(compareWith))) {
				break;
			}
		} while (!ref.compareAndSet(old, newValue));
		
		return old;
			
	}
	/**
	 * Atomically compares the content of <code>ref</code> with the <code>compareWith</code>
	 * value and 
	 * replaces it with the newValue if they equal. The method
	 * returns the old value of ref regardless of whether the exchange actually happened.
	 * @param ref the target of the exchange
	 * @param compareWith the value to compare against
	 * @param newValue the the new value to replace with
	 * @return the old value of the ref
	 */
	public static boolean compareExchange(
			@Nonnull AtomicBoolean ref, boolean compareWith, boolean newValue) {
		boolean old = false;
		
		do {
			old = ref.get();
			if (old != compareWith) {
				break;
			}
		} while (!ref.compareAndSet(old, newValue));
		
		return old;
	}
	/**
	 * Atomically compares the content of <code>ref</code> with the <code>compareWith</code>
	 * value and 
	 * replaces it with the newValue if they equal. The method
	 * returns the old value of ref regardless of whether the exchange actually happened.
	 * @param ref the target of the exchange
	 * @param compareWith the value to compare against
	 * @param newValue the the new value to replace with
	 * @return the old value of the ref
	 */
	public static int compareExchange(
			@Nonnull AtomicInteger ref, int compareWith, int newValue) {
		int old = 0;
		
		do {
			old = ref.get();
			if (old != compareWith) {
				break;
			}
		} while (!ref.compareAndSet(old, newValue));
		
		return old;
	}
	/**
	 * Atomically compares the content of <code>ref</code> with the <code>compareWith</code>
	 * value and 
	 * replaces it with the newValue if they equal. The method
	 * returns the old value of ref regardless of whether the exchange actually happened.
	 * @param ref the target of the exchange
	 * @param compareWith the value to compare against
	 * @param newValue the the new value to replace with
	 * @return the old value of the ref
	 */
	public static long compareExchange(
			@Nonnull AtomicLong ref, long compareWith, long newValue) {
		long old = 0;
		
		do {
			old = ref.get();
			if (old != compareWith) {
				break;
			}
		} while (!ref.compareAndSet(old, newValue));
		
		return old;
	}
}
