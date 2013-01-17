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
 * GWT emulated version of {@link AtomicLong}.  It's a thin wrapper
 * around the primitive {@code long}.
 *
 * @author Hayward Chan
 */
public class AtomicLong extends Number implements java.io.Serializable {

	/** */
	private static final long serialVersionUID = 831715854452574825L;
	/** The value. */
	private long value;
	/**
	 * Constructor.
	 * @param initialValue the initial value
	 */
	public AtomicLong(long initialValue) {
		value = initialValue;
	}
	/** Constructor. */
	public AtomicLong() {
	}
	/** @return the current value. */
	public final long get() {
		return value;
	}
	/** 
	 * Set a new value.
	 * @param newValue the new value
	 */
	public final void set(long newValue) {
		value = newValue;
	}
	/**
	 * Set a new value lazily.
	 * @param newValue the new value
	 */
	public final void lazySet(long newValue) {
		set(newValue);
	}
	/**
	 * Get the current value and set a new value.
	 * @param newValue the new value to set
	 * @return the current value
	 */
	public final long getAndSet(long newValue) {
		long current = value;
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
	public final boolean compareAndSet(long expect, long update) {
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
	public final long getAndIncrement() {
		return value++;
	}

	/**
	 * @return get the current and decrement the value
	 */
	public final long getAndDecrement() {
		return value--;
	}
	/**
	 * @param delta the difference to add
	 * @return get the current and add the delta value
	 */
	public final long getAndAdd(long delta) {
		long current = value;
		value += delta;
		return current;
	}
	/**
	 * @return increment and get the new value
	 */
	public final long incrementAndGet() {
		return ++value;
	}
	/**
	 * @return decrement and get the new value
	 */
	public final long decrementAndGet() {
		return --value;
	}
	/**
	 * Add the delta and get the new value.
	 * @param delta the delta to add
	 * @return the new value
	 */
	public final long addAndGet(long delta) {
		value += delta;
		return value;
	}

	@Override public String toString() {
		return String.valueOf(value);
	}
	/** @return value as integer */
	public int intValue() {
		return (int)value;
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
