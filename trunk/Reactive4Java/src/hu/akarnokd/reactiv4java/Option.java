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

/**
 * A class representing a value or nothing.
 * @author karnokd
 * @param <T> the type of the contained object
 */
public abstract class Option<T> {
//	/** @return does this option hold a value? */
//	public abstract boolean hasValue();
	/** @return query for the value. */
	public abstract T value();
	/**
	 * The helper class representing an option holding nothing.
	 * @author karnokd
	 *
	 * @param <T> the type of the nothing - not really used but required by the types
	 */
	public static final class None<T> extends Option<T> {
		/** Single instance! */
		private None() {
			
		}
//		@Override
//		public boolean hasValue() {
//			return false;
//		}
		@Override
		public T value() {
			throw new UnsupportedOperationException();
		}
		@Override
		public String toString() {
			return "None";
		}
	}
	/**
	 * A helper class representing an option holding something of T.
	 * @author karnokd
	 *
	 * @param <T> the type of the contained stuff
	 */
	public static final class Some<T> extends Option<T> {
		/** The value that is hold by this option. */
		private final T value;
		/**
		 * Construct the some with a value.
		 * @param value the value.
		 */
		private Some(T value) {
			this.value = value;
		}
//		@Override
//		public boolean hasValue() {
//			return true;
//		}
		@Override
		public T value() {
			return value;
		}
		@Override
		public String toString() {
			return "Some with " + value;
		}
//		/**
//		 * Construct a Some of type U.
//		 * @param <U> the type
//		 * @param value the value
//		 * @return the Some option
//		 */
//		public static <U> Some<U> of(U value) {
//			return new Some<U>(value);
//		}
	}
	/** The single instance of the nothingness. */
	private static final None<Void> NONE = new None<Void>();
	/**
	 * Returns a none of T.
	 * @param <T> the type of the T
	 * @return the None of T
	 */
	@SuppressWarnings("unchecked")
	public static <T> None<T> none() {
		return (None<T>)NONE;
	}
	/**
	 * Create a new Some instance with the supplied value.
	 * @param <T> the value type
	 * @param value the initial value
	 * @return the some object
	 */
	public static <T> Some<T> some(T value) {
		return new Some<T>(value);
	}
}
