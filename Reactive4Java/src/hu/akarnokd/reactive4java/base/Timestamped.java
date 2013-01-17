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

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.annotation.Nonnull;

// #GWT-ACCEPT-START
//import com.google.gwt.i18n.client.DateTimeFormat;
// #GWT-ACCEPT-END
/**
 * An immutable record holding a value and timestamp pairs.
 * @author akarnokd, 2011.01.29.
 * @param <T> the contained type
 */
public final class Timestamped<T> implements Comparable<Timestamped<T>> {
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
	@Nonnull
	public static <T> Timestamped<T> of(T value, long timestamp) {
		return new Timestamped<T>(value, timestamp);
	}
	/**
	 * A type inference helper to construct a new timestamped value from another
	 * timestamped value by keeping the value and assigning a new value.
	 * @param <T> the type of the value
	 * @param value the value
	 * @param timestamp the timestamp
	 * @return the timestamped object
	 */
	@Nonnull
	public static <T> Timestamped<T> of(
			@Nonnull Timestamped<T> value,
			long timestamp) {
		return new Timestamped<T>(value.value(), timestamp);
	}
	/**
	 * A type inference helper to construct a new timestamped value where the timestamp
	 * is the System.currentTimeMillis().
	 * @param <T> the type of the value
	 * @param value the value
	 * @return the timestamped object
	 */
	@Nonnull
	public static <T> Timestamped<T> of(T value) {
		return of(value, System.currentTimeMillis());
	}
	/**
	 * A type inference helper to construct a new timestamped value from another timestamped value
	 * where the new timestamp is the System.currentTimeMillis().
	 * @param <T> the type of the value
	 * @param value the value
	 * @return the timestamped object
	 */
	public static <T> Timestamped<T> of(
			@Nonnull Timestamped<T> value) {
		return of(value, System.currentTimeMillis());
	}
	@Override
	public String toString() {
		// #GWT-IGNORE-START
		return value + " @ " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(timestamp));
		// #GWT-IGNORE-END
		// #GWT-ACCEPT-START
		//return DateTimeFormat.getFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(timestamp));
		// #GWT-ACCEPT-END
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
	@Override
	public int compareTo(Timestamped<T> that) {
		long t1 = this.timestamp();
		long t2 = that.timestamp();
		return t1 < t2 ? -1 : (t1 == t2) ? 0 : 1;
	}
}
