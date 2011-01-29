/*
 * Copyright 2011 David Karnok
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
package hu.akarnokd.reactiv4java;

import java.util.Date;

/**
 * An immutable record holding a value and timestamp pairs.
 * @author akarnokd, 2011.01.29.
 * @param <T> the contained type
 */
public final class Timestamped<T> {
	/** The value. */
	private final T value;
	/** The timestamp. */
	private final long timestamp;
	/**
	 * Construct a new timestamped value.
	 * @param value the value
	 * @param timestamp the timestamp
	 */
	public Timestamped(T value, long timestamp) {
		this.value = value;
		this.timestamp = timestamp;
	}
	/** @return the contained value */
	public T value() {
		return value;
	}
	/** @return the associated timestamp. */
	public long timestamp() {
		return timestamp;
	}
	/**
	 * A type inference helper to construct a new timestamped value.
	 * @param <T> the type of the value
	 * @param value the value
	 * @param timestamp the timestamp
	 * @return the timestamped object
	 */
	public static <T> Timestamped<T> of(T value, long timestamp) {
		return new Timestamped<T>(value, timestamp);
	}
	@Override
	public String toString() {
		return value + " @ " + new Date(timestamp);
	}
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Timestamped) {
			Timestamped<?> that = (Timestamped<?>)obj;
			return (this.value == that.value || (this.value != null && this.value.equals(that.value)))
			&& this.timestamp == that.timestamp;
		}
		return false;
	}
	@Override
	public int hashCode() {
		return (17 + (value != null ? value.hashCode() : 0)) * 31 + (int)((timestamp >> 32) ^ (timestamp & 0xFFFFFFFFL));
	}
}
