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
/*
 * Copyright (C) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.util.concurrent.atomic;

/**
 * GWT emulated version of {@link AtomicInteger}.  It's a thin wrapper
 * around the primitive {@code int}.
 *
 * @author Hayward Chan
 */
public class AtomicInteger extends Number implements java.io.Serializable {

	/** */
	private static final long serialVersionUID = 831715854452574825L;
	/** The value. */
	private int value;
	/**
	 * Constructor.
	 * @param initialValue the initial value
	 */
	public AtomicInteger(int initialValue) {
		value = initialValue;
	}
	/** Constructor. */
	public AtomicInteger() {
	}
	/** @return the current value. */
	public final int get() {
		return value;
	}
	/** 
	 * Set a new value.
	 * @param newValue the new value
	 */
	public final void set(int newValue) {
		value = newValue;
	}
	/**
	 * Set a new value lazily.
	 * @param newValue the new value
	 */
	public final void lazySet(int newValue) {
		set(newValue);
	}
	/**
	 * Get the current value and set a new value.
	 * @param newValue the new value to set
	 * @return the current value
	 */
	public final int getAndSet(int newValue) {
		int current = value;
		value = newValue;
		return current;
	}
	/**
	 * Set a new value only if the current value is
	 * equal to the expected value.
	 * @param expect the expected value
	 * @param update the new value
	 * @return success indicator
	 */
	public final boolean compareAndSet(int expect, int update) {
		if (value == expect) {
			value = update;
			return true;
		} else {
			return false;
		}
	}
	/**
	 * @return get the current and increment the value
	 */
	public final int getAndIncrement() {
		return value++;
	}

	/**
	 * @return get the current and decrement the value
	 */
	public final int getAndDecrement() {
		return value--;
	}
	/**
	 * @param delta the difference to add
	 * @return get the current and add the delta value
	 */
	public final int getAndAdd(int delta) {
		int current = value;
		value += delta;
		return current;
	}
	/**
	 * @return increment and get the new value
	 */
	public final int incrementAndGet() {
		return ++value;
	}
	/**
	 * @return decrement and get the new value
	 */
	public final int decrementAndGet() {
		return --value;
	}
	/**
	 * Add the delta and get the new value.
	 * @param delta the delta to add
	 * @return the new value
	 */
	public final int addAndGet(int delta) {
		value += delta;
		return value;
	}

	@Override public String toString() {
		return Integer.toString(value);
	}
	/** @return value as integer */
	public int intValue() {
		return value;
	}

	/** @return value as long */
	public long longValue() {
		return /* (long) */ value;
	}

	/** @return value as float */
	public float floatValue() {
		return /* (float) */ value;
	}
	/** @return value as double */
	public double doubleValue() {
		return /* (double) */ value;
	}
}
