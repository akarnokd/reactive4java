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

package hu.akarnokd.reactive4java.util;

import hu.akarnokd.reactive4java.base.Action1;
import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Func2;
import hu.akarnokd.reactive4java.base.Functions;
import hu.akarnokd.reactive4java.base.Option;
import hu.akarnokd.reactive4java.base.Ref;


/**
 * The functional observable helper class with the anamorphism, bind and catamorpism basic operators.
 * Inspired by http://blogs.bartdesmet.net/blogs/bart/archive/2010/01/01/the-essence-of-linq-minlinq.aspx
 * @author akarnokd
 */
public final class ActionObservable {
	/** Helper class. */
	private ActionObservable() {
		// helper class.
	}
	/**
	 * Returns an action to an action to receive an option of T.
	 * @param <T> the type of the value (irrelevant)
	 * @return the action to action to option
	 */
	public static <T> Action1<Action1<Option<T>>> empty() {
		return new Action1<Action1<Option<T>>>() {
			@Override
			public void invoke(Action1<Option<T>> value) {
				value.invoke(Option.<T>none());
			}
		};
	}
	/**
	 * Returns an action which takes an action which takes an option of T.
	 * @param <T> the type of the value
	 * @param value the value to return
	 * @return the action to action to option of T
	 */
	public static <T> Action1<Action1<Option<T>>> single(final T value) {
		return new Action1<Action1<Option<T>>>() {
			@Override
			public void invoke(Action1<Option<T>> action) {
				action.invoke(Option.some(value));
				action.invoke(Option.<T>none());
			}
		};
	}
	/**
	 * The binding option to convert a source stream of Ts into streaming Rs.
	 * @param <T> the source type
	 * @param <R> the converted type
	 * @param source the source of Ts
	 * @param selector the selector which converts Ts to Rs.
	 * @return the action to action that receives option of R
	 */
	public static <T, R> Action1<Action1<Option<R>>> bind(
			final Action1<Action1<Option<T>>> source, 
			final Func1<T, Action1<Action1<Option<R>>>> selector) {
		return new Action1<Action1<Option<R>>>() {
			@Override
			public void invoke(final Action1<Option<R>> o) {
				source.invoke(new Action1<Option<T>>() {
					@Override
					public void invoke(Option<T> x) {
						if (x == Option.none()) {
							o.invoke(Option.<R>none());
						} else {
							selector.invoke(x.value()).invoke(new Action1<Option<R>>() {
								@Override
								public void invoke(Option<R> y) {
									if (y != Option.none()) {
										o.invoke(y);
									}
								}
							});
						}
					}
				});
			}
		};
	}
	/**
	 * Anamorphism that creates Ts starting from the seed value until the condition holds.
	 * Its equivalent is a for loop: for (int i = 0; i &lt; 10; i++) 
	 * @param <T> the type of the values to generate
	 * @param seed the initial value
	 * @param condition the condition until the Ts should be produced
	 * @param next the way of compute the next T
	 * @return the function of founction of option of T
	 */
	public static <T> Action1<Action1<Option<T>>> ana(
			final T seed, 
			final Func1<T, Boolean> condition, 
			final Func1<T, T> next) {
		return new Action1<Action1<Option<T>>>() {
			@Override
			public void invoke(Action1<Option<T>> o) {
				for (T value = seed; condition.invoke(value); value = next.invoke(value)) {
					o.invoke(Option.some(value));
				}
				o.invoke(Option.<T>none());
			}
		};
	}
	/**
	 * The action of action of option of none provider implemented by using anamorphism.
	 * @param <T> the type of the value (irrelevant)
	 * @return the action to action to option
	 */
	public static <T> Action1<Action1<Option<T>>> emptyAna() {
		Func1<T, Boolean> condition = Functions.alwaysFalse();
		return ana(null, condition, null);
	}
	/**
	 * The action of action of option of only a single element provider implemented by using anamorphism.
	 * @param <T> the value type
	 * @param value the value to return only once
	 * @return the action to action to option of T
	 */
	public static <T> Action1<Action1<Option<T>>> singleAna(T value) {
		Func1<T, T> identity = Functions.identity();
		return ana(value, new Func1<T, Boolean>() {
			boolean once;
			@Override
			public Boolean invoke(T param1) {
				if (once) {
					return false;
				}
				once = true;
				return true;
			}
		}, identity);
	}
	/**
	 * A catamorphism which creates a single R out of the sequence of Ts by using an aggregator.
	 * The method is a greedy operation: it must wait all source values to arrive, therefore, do not use it on infinite sources.
	 * @param <T> the type of the sequence values
	 * @param <R> the intermediate and output types
	 * @param source the source of the sequence values
	 * @param seed the initial value of the aggregation (e.g., start from zero)
	 * @param aggregator the aggregator function which takes a sequence value and the previous output value and produces a new output value
	 * @return the aggregation result
	 */
	public static <T, R> R cata(
			Action1<Action1<Option<T>>> source, 
			R seed, 
			final Func2<R, T, R> aggregator) {
		final Ref<R> result = Ref.of(seed);
		source.invoke(new Action1<Option<T>>() {
			/** Indicate the aggregation end. */
			boolean end;
			@Override
			public void invoke(Option<T> value) {
				if (value != Option.none() && !end) {
					result.set(aggregator.invoke(result.get(), value.value()));
				} else {
					end = true;
				}
			}
		});
		return result.get();
	}
}

