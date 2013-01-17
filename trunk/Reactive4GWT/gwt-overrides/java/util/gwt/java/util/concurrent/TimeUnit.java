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

package java.util.concurrent;

/**
 * OpenJDK TimeUnit minus the unsupported operations.
 * <p>Some operations and style fixes added.</p>
 * @author Charles Fry
 * @author David Karnok
 */
public enum TimeUnit {
	/** Unit in nanoseconds. */
	NANOSECONDS {
		@Override
		public long toNanos(long d)   { return d; }
		@Override
		public long toMicros(long d)  { return d / (C1 / C0); }
		@Override
		public long toMillis(long d)  { return d / (C2 / C0); }
		@Override
		public long toSeconds(long d) { return d / (C3 / C0); }
		@Override
		public long toMinutes(long d) { return d / (C4 / C0); }
		@Override
		public long toHours(long d)   { return d / (C5 / C0); }
		@Override
		public long toDays(long d)    { return d / (C6 / C0); }
		@Override
		public long convert(long d, TimeUnit u) { return u.toNanos(d); }
		@Override
		int excessNanos(long d, long m) { return (int)(d - (m * C2)); }
	},
	/** Unit in microseconds. */
	MICROSECONDS {
		@Override
		public long toNanos(long d)   { return x(d, C1 / C0, MAX / (C1 / C0)); }
		@Override
		public long toMicros(long d)  { return d; }
		@Override
		public long toMillis(long d)  { return d / (C2 / C1); }
		@Override
		public long toSeconds(long d) { return d / (C3 / C1); }
		@Override
		public long toMinutes(long d) { return d / (C4 / C1); }
		@Override
		public long toHours(long d)   { return d / (C5 / C1); }
		@Override
		public long toDays(long d)    { return d / (C6 / C1); }
		@Override
		public long convert(long d, TimeUnit u) { return u.toMicros(d); }
		@Override
		int excessNanos(long d, long m) { return (int)((d * C1) - (m * C2)); }
	},
	/** Unit in milliseconds. */
	MILLISECONDS {
		@Override
		public long toNanos(long d)   { return x(d, C2 / C0, MAX / (C2 / C0)); }
		@Override
		public long toMicros(long d)  { return x(d, C2 / C1, MAX / (C2 / C1)); }
		@Override
		public long toMillis(long d)  { return d; }
		@Override
		public long toSeconds(long d) { return d / (C3 / C2); }
		@Override
		public long toMinutes(long d) { return d / (C4 / C2); }
		@Override
		public long toHours(long d)   { return d / (C5 / C2); }
		@Override
		public long toDays(long d)    { return d / (C6 / C2); }
		@Override
		public long convert(long d, TimeUnit u) { return u.toMillis(d); }
		@Override
		int excessNanos(long d, long m) { return 0; }
	},
	/** Unit in seconds. */
	SECONDS {
		@Override
		public long toNanos(long d)   { return x(d, C3 / C0, MAX / (C3 / C0)); }
		@Override
		public long toMicros(long d)  { return x(d, C3 / C1, MAX / (C3 / C1)); }
		@Override
		public long toMillis(long d)  { return x(d, C3 / C2, MAX / (C3 / C2)); }
		@Override
		public long toSeconds(long d) { return d; }
		@Override
		public long toMinutes(long d) { return d / (C4 / C3); }
		@Override
		public long toHours(long d)   { return d / (C5 / C3); }
		@Override
		public long toDays(long d)    { return d / (C6 / C3); }
		@Override
		public long convert(long d, TimeUnit u) { return u.toSeconds(d); }
		@Override
		int excessNanos(long d, long m) { return 0; }
	},
	/** Unit in minutes. */
	MINUTES {
		@Override
		public long toNanos(long d)   { return x(d, C4 / C0, MAX / (C4 / C0)); }
		@Override
		public long toMicros(long d)  { return x(d, C4 / C1, MAX / (C4 / C1)); }
		@Override
		public long toMillis(long d)  { return x(d, C4 / C2, MAX / (C4 / C2)); }
		@Override
		public long toSeconds(long d) { return x(d, C4 / C3, MAX / (C4 / C3)); }
		@Override
		public long toMinutes(long d) { return d; }
		@Override
		public long toHours(long d)   { return d / (C5 / C4); }
		@Override
		public long toDays(long d)    { return d / (C6 / C4); }
		@Override
		public long convert(long d, TimeUnit u) { return u.toMinutes(d); }
		@Override
		int excessNanos(long d, long m) { return 0; }
	},
	/** Unit in hours. */
	HOURS {
		@Override
		public long toNanos(long d)   { return x(d, C5 / C0, MAX / (C5 / C0)); }
		@Override
		public long toMicros(long d)  { return x(d, C5 / C1, MAX / (C5 / C1)); }
		@Override
		public long toMillis(long d)  { return x(d, C5 / C2, MAX / (C5 / C2)); }
		@Override
		public long toSeconds(long d) { return x(d, C5 / C3, MAX / (C5 / C3)); }
		@Override
		public long toMinutes(long d) { return x(d, C5 / C4, MAX / (C5 / C4)); }
		@Override
		public long toHours(long d)   { return d; }
		@Override
		public long toDays(long d)    { return d / (C6 / C5); }
		@Override
		public long convert(long d, TimeUnit u) { return u.toHours(d); }
		@Override
		int excessNanos(long d, long m) { return 0; }
	},
	/** Unit in days. */
	DAYS {
		@Override
		public long toNanos(long d)   { return x(d, C6 / C0, MAX / (C6 / C0)); }
		@Override
		public long toMicros(long d)  { return x(d, C6 / C1, MAX / (C6 / C1)); }
		@Override
		public long toMillis(long d)  { return x(d, C6 / C2, MAX / (C6 / C2)); }
		@Override
		public long toSeconds(long d) { return x(d, C6 / C3, MAX / (C6 / C3)); }
		@Override
		public long toMinutes(long d) { return x(d, C6 / C4, MAX / (C6 / C4)); }
		@Override
		public long toHours(long d)   { return x(d, C6 / C5, MAX / (C6 / C5)); }
		@Override
		public long toDays(long d)    { return d; }
		@Override
		public long convert(long d, TimeUnit u) { return u.toDays(d); }
		@Override
		int excessNanos(long d, long m) { return 0; }
	};

	/** Handy constants for conversion methods. */
	static final long C0 = 1L;
	/** Handy constants for conversion methods. */
	static final long C1 = C0 * 1000L;
	/** Handy constants for conversion methods. */
	static final long C2 = C1 * 1000L;
	/** Handy constants for conversion methods. */
	static final long C3 = C2 * 1000L;
	/** Handy constants for conversion methods. */
	static final long C4 = C3 * 60L;
	/** Handy constants for conversion methods. */
	static final long C5 = C4 * 60L;
	/** Handy constants for conversion methods. */
	static final long C6 = C5 * 24L;
	/** Handy constants for conversion methods. */
	static final long MAX = Long.MAX_VALUE;

	/**
	 * Scale d by m, checking for overflow.
	 * This has a short name to make above code more readable.
	 * @param d the base value
	 * @param m the scaler value
	 * @param over the overflow limiter
	 * @return the scaled value
	 */
	static long x(long d, long m, long over) {
		if (d >  over) {
			return Long.MAX_VALUE;
		}
		if (d < -over) {
			return Long.MIN_VALUE;
		}
		return d * m;
	}
	/**
	 * Convert the given source duration and unit into this unit.
	 * @param sourceDuration the source duration
	 * @param sourceUnit the source unit
	 * @return the new duration in this unit
	 */
	public abstract long convert(long sourceDuration, TimeUnit sourceUnit);
	/**
	 * Convert to nanoseconds.
	 * @param duration the duration in the current unit
	 * @return the duration in nanoseconds
	 */
	public abstract long toNanos(long duration);
	/**
	 * Convert to microseconds.
	 * @param duration the duration in the current unit
	 * @return the duration in microseconds
	 */
	public abstract long toMicros(long duration);
	/**
	 * Convert to milliseconds.
	 * @param duration the duration in the current unit
	 * @return the duration in milliseconds
	 */
	public abstract long toMillis(long duration);
	/**
	 * Convert to seconds.
	 * @param duration the duration in the current unit
	 * @return the duration in seconds
	 */
	public abstract long toSeconds(long duration);
	/**
	 * Convert to minutes.
	 * @param duration the duration in the current unit
	 * @return the duration in minutes
	 */
	public abstract long toMinutes(long duration);
	/**
	 * Convert to hours.
	 * @param duration the duration in the current unit
	 * @return the duration in hours
	 */
	public abstract long toHours(long duration);
	/**
	 * Convert to days.
	 * @param duration the duration in the current unit
	 * @return the duration in days
	 */
	public abstract long toDays(long duration);
	/**
	 * The helper method for the remainder of nanoseconds?
	 * @param d the duration
	 * @param m the base
	 * @return the number of excess nanoseconds
	 */
	abstract int excessNanos(long d, long m);
	/**
	 * Sleep for the specified amount of time based on the current unit.
	 * In GWT, this is a no-op.
	 * @param duration the duration
	 * @throws InterruptedException when the sleep is interrupted.
	 */
	public void sleep(long duration) throws InterruptedException {

	}
}

