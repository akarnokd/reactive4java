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

package hu.akarnokd.reactive4java.base;

import javax.annotation.Nonnull;

/**
 * Helper class for Action interfaces.
 * @author akarnokd
 *
 */
public final class Actions {

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
	 * Wrap the supplied no-parameter function into an action.
	 * The function's return value is ignored.
	 * @param <T> the return type of the function, irrelevant
	 * @param run the original runnable
	 * @return the Action0 wrapping the runnable
	 */
	@Nonnull 
	public static <T> Action0 asAction0(
			@Nonnull final Func0<T> run) {
		return new Action0() {
			@Override
			public void invoke() {
				run.invoke();
			}
		};
	}
	/**
	 * Wrap the supplied runnable into an action.
	 * @param run the original runnable
	 * @return the Action0 wrapping the runnable
	 */
	@Nonnull 
	public static Action0 asAction0(
			@Nonnull final Runnable run) {
		return new Action0() {
			@Override
			public void invoke() {
				run.run();
			}
		};
	}
	/**
	 * Wrap the supplied one-parameter function into an action.
	 * The function's return value is ignored.
	 * @param <T> the parameter type
	 * @param <U> the return type, irrelevant
	 * @param run the original runnable
	 * @return the Action0 wrapping the runnable
	 */
	@Nonnull 
	public static <T, U> Action1<T> asAction1(
			@Nonnull final Func1<T, U> run) {
		return new Action1<T>() {
			@Override
			public void invoke(T param) {
				run.invoke(param);
			}
		};
	}
	/**
	 * Wrap the supplied runnable into an action.
	 * @param <T> the parameter type
	 * @param run the original runnable
	 * @return the Action0 wrapping the runnable
	 */
	@Nonnull 
	public static <T> Action1<T> asAction1(
			@Nonnull final Runnable run) {
		return new Action1<T>() {
			@Override
			public void invoke(T param) {
				run.run();
			}
		};
	}
	/**
	 * Wrap the given action into a runnable instance.
	 * @param action the target action
	 * @return the wrapper runnable
	 */
	@Nonnull 
	public static Runnable asRunnable(
			@Nonnull final Action0 action) {
		return new Runnable() {
			@Override
			public void run() {
				action.invoke();
			}
		};
	}
	/** @return returns an empty action which does nothing. */
	@Nonnull 
	public static Action0 noAction0() {
		return NO_ACTION_0;
	}
	/**
	 * Returns an action which does nothing with its parameter.
	 * @param <T> the type of the parameter (irrelevant)
	 * @return the action
	 */
	@SuppressWarnings("unchecked")
	@Nonnull 
	public static <T> Action1<T> noAction1() {
		return (Action1<T>)NO_ACTION_1;
	}
	/** Utility class. */
	private Actions() {
		
	}
}
