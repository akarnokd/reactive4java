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
package hu.akarnokd.reactive4java.base;

import javax.annotation.Nonnull;


/**
 * An immutable record holding a value and a
 * time interval value.
 * @author akarnokd, 2011.01.29.
 * @param <T> the contained type
 */
public final class TimeInterval<T> {
	/** The value. */
	private final T value;
	/** The timestamp. */
	private final long interval;
	/**
	 * Construct a value with a time inverval.
	 * @param value the value
	 * @param interval the time interval
	 */
	public TimeInterval(T value, long interval) {
		this.value = value;
		this.interval = interval;
	}
	/** @return the contained value */
	public T value() {
		return value;
	}
	/** @return the associated timestamp. */
	public long interval() {
		return interval;
	}
	/**
	 * A type inference helper to construct a new TimeInterval value.
	 * @param <T> the type of the value
	 * @param value the value
	 * @param interval the time interval
	 * @return the timestamped object
	 */
	@Nonnull 
	public static <T> TimeInterval<T> of(T value, long interval) {
		return new TimeInterval<T>(value, interval);
	}
	/**
	 * A type inference helper to construct a new TimeInterval value from another
	 * timestamped value by keeping the value and assigning a new value.
	 * @param <T> the type of the value
	 * @param value the value
	 * @param interval the time interval
	 * @return the timestamped object
	 */
	@Nonnull 
	public static <T> TimeInterval<T> of(TimeInterval<T> value, long interval) {
		return new TimeInterval<T>(value.value(), interval);
	}
	@Override
	public String toString() {
		return String.format("%s delta %,d", value, interval);
	}
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TimeInterval) {
			TimeInterval<?> that = (TimeInterval<?>)obj;
			return (this.value == that.value || (this.value != null && this.value.equals(that.value)))
			&& this.interval == that.interval;
		}
		return false;
	}
	@Override
	public int hashCode() {
		return (17 + (value != null ? value.hashCode() : 0)) * 31 + (int)((interval >> 32) ^ (interval & 0xFFFFFFFFL));
	}
}
