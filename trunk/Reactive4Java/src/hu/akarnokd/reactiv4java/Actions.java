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
 * Helper class for Action interfaces.
 * @author karnokd
 *
 */
public final class Actions {

	/** Utility class. */
	private Actions() {
		
	}
	/** A helper action with one parameter which does nothing. */
	private static final Action1<Void> NO_ACTION_1 = new Action1<Void>() {
		@Override
		public void invoke(Void value) {
			
		}
	};
	/** A helper action without parameters which does nothing. */
	private static final Action0 NO_ACTION_0 = new Action0() {
		@Override
		public void invoke() {
			
		}
	};
	/**
	 * Returns an action which does nothing with its parameter.
	 * @param <T> the type of the parameter (irrelevant)
	 * @return the action
	 */
	@SuppressWarnings("unchecked")
	public static <T> Action1<T> noAction1() {
		return (Action1<T>)NO_ACTION_1;
	}
	/** @return returns an empty action which does nothing. */
	public static Action0 noAction0() {
		return NO_ACTION_0;
	}
}
