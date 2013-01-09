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

/**
 * A simple reference object holding something that can be changed. Not thread-safe and not meant to be.
 * @author akarnokd
 * @param <T> the type of the contained object
 */
public final class Ref<T> {
	/** The contained object. */
	private T value;
	/**
	 * Initializes a null element.
	 */
	public Ref() {

	}
	/**
	 * Initializes the ref with the given value.
	 * @param value the value
	 */
	public Ref(T value) {
		this.value = value;
	}
	/** @return retrieve the content. */
	public T get() {
		return value;
	}
	/**
	 * Set the new content.
	 * @param newValue the new value to set
	 */
	public void set(T newValue) {
		this.value = newValue;
	}
	/**
	 * Replace the current contents with the new value and return the old value.
	 * @param newValue the new value
	 * @return the old value
	 */
	public T replace(T newValue) {
		T curr = value;
		set(newValue);
		return curr;
	}
	/**
	 * Construct a new reference of an initial value (type inference helper).
	 * @param <U> the type of the value
	 * @param value the initial value
	 * @return the reference holding the value
	 */
	public static <U> Ref<U> of(U value) {
		return new Ref<U>(value);
	}
	/**
	 * Construct a new reference in empty mode.
	 * @param <U> the type of the contents
	 * @return the reference
	 */
	public static <U> Ref<U> of() {
		return new Ref<U>();
	}
}
