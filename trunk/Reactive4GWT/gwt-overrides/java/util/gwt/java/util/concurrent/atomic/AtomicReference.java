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

package java.util.concurrent.atomic;

/**
 * GWT emulated version of {@link AtomicReference}.  It's a thin wrapper
 * around an object typed <code>T</code>.
 * @param <T> the element type
 * @author Hayward Chan
 */
public class AtomicReference<T> {
	/** The value. */
	private T value;
	/**
	 * Constructor.
	 * @param initialValue the initial value
	 */
	public AtomicReference(T initialValue) {
		value = initialValue;
	}
	/**
	 * Constructor.
	 */
	public AtomicReference() {
	}
	/** @return the current value */
	public final T get() {
		return value;
	}
	/**
	 * Set a new value.
	 * @param newValue the new value to set
	 */
	public final void set(T newValue) {
		value = newValue;
	}
	/**
	 * Set a new value lazily.
	 * @param newValue the new value to set
	 */
	public final void lazySet(T newValue) {
		set(newValue);
	}
	/**
	 * Get the current value and set the new value.
	 * @param newValue the new value
	 * @return the current value
	 */
	public final T getAndSet(T newValue) {
		T current = value;
		value = newValue;
		return current;
	}
	/**
	 * Compare the current value with the expected
	 * value and if they are reference-equal, set
	 * a new value.
	 * @param expect the expected value
	 * @param update the new value
	 * @return the success indicator
	 */
	public final boolean compareAndSet(T expect, T update) {
		if (value == expect) {
			value = update;
			return true;
		} else {
			return false;
		}
	}

	@Override 
	public String toString() {
		return String.valueOf(value);
	}

}
