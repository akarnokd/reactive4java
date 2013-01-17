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
 * GWT emulated version of {@link AtomicBoolean}.  It's a thin wrapper
 * around the primitive {@code boolean}.
 *
 * @author David Karnok
 */
public class AtomicBoolean implements java.io.Serializable {
	/** */
	private static final long serialVersionUID = -7367643074559189825L;
	/** The value. */
	private boolean value;
	/**
	 * Constructor.
	 * @param initialValue the initial value
	 */
	public AtomicBoolean(boolean initialValue) {
		value = initialValue;
	}
	/** Constructor. */
	public AtomicBoolean() {
	}
	/** @return the current value. */
	public final boolean get() {
		return value;
	}
	/**
	 * Set a new value.
	 * @param newValue the value
	 */
	public final void set(boolean newValue) {
		value = newValue;
	}
	/**
	 * Lazily set the new value.
	 * @param newValue the value
	 */
	public final void lazySet(boolean newValue) {
		set(newValue);
	}
	/**
	 * Retrieve the old and set the new value.
	 * @param newValue the new value
	 * @return the old value
	 */
	public final boolean getAndSet(boolean newValue) {
		boolean current = value;
		value = newValue;
		return current;
	}
	/**
	 * Compare the current value with expected.
	 * and if they match, set the new value
	 * @param expect the expected current value
	 * @param update the new value to use
	 * @return the indicator of success
	 */
	public final boolean compareAndSet(boolean expect, boolean update) {
		if (value == expect) {
			value = update;
			return true;
		} else {
			return false;
		}
	}

	@Override 
	public String toString() {
		return Boolean.toString(value);
	}

}
