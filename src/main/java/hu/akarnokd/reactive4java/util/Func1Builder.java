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
package hu.akarnokd.reactive4java.util;

import hu.akarnokd.reactive4java.base.Action0;
import hu.akarnokd.reactive4java.base.Action1;
import hu.akarnokd.reactive4java.base.Func0;
import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Pred1;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

/**
 * Class holding a Func1 object and providing various relevant methods 
 * of the {@code Functions} utility class as instance methods.
 * <p>The class itself is of type {@code Func1<T, U>} and can be used where this type is needed.</p>
 * @author akarnokd, 2012.02.02.
 * @param <T> the function parameter type
 * @param <U> the return type
 * @since 0.96.1
 */
public final class Func1Builder<T, U> implements Func1<T, U> {
	/** The wrapped function. */
	@Nonnull 
	protected final Func1<T, U> f;
	/**
	 * Construct a function builder by wrapping the given function.
	 * @param f the function to wrap
	 */
	protected Func1Builder(@Nonnull Func1<T, U> f) {
		this.f = f;
	}
	/**
	 * Wrap the given function into a new builder.
	 * @param <T> the parameter type
	 * @param <U> the return type
	 * @param f the function to wrap
	 * @return the function builder
	 */
	@Nonnull 
	public static <T, U> Func1Builder<T, U> from(@Nonnull Func1<T, U> f) {
		return new Func1Builder<T, U>(f);
	}
	/**
	 * Wraps the given Func0 function into a Func1 object which ignores
	 * its parameter T.
	 * @param <T> the function parameter type (irrelevant)
	 * @param <U> the function return type
	 * @param f the function to wrap
	 * @return the function builder
	 */
	@Nonnull 
	public static <T, U> Func1Builder<T, U> from(@Nonnull Func0<U> f) {
		return from(Functions.<T, U>asFunc1(f));
	}
	/**
	 * Wraps the given value and the function returns this value
	 * regardless of the parameters.
	 * @param <T> the function parameter type, irrelevant
	 * @param <U> the return type
	 * @param value the value to return
	 * @return the function builder
	 */
	@Nonnull 
	public static <T, U> Func1Builder<T, U> from(final U value) {
		return from(Functions.<T, U>constant(value));
	}
	/**
	 * Returns a function builder which will return the indexth element
	 * from the supplied array.
	 * @param <T> the element type
	 * @param values the values to return from
	 * @return the function builder
	 */
	@Nonnull 
	public static <T> Func1Builder<Integer, T> from(@Nonnull final T... values) {
		return from(new Func1<Integer, T>() {
			@Override
			public T invoke(Integer param1) {
				return values[param1];
			}
		});
	}
	/**
	 * Returns a function builder which will return the indexth element
	 * from the supplied list.
	 * @param <T> the element type
	 * @param values the values to return from
	 * @return the function builder
	 */
	@Nonnull 
	public static <T> Func1Builder<Integer, T> from(@Nonnull final List<? extends T> values) {
		return from(Functions.asFunc1(values));
	}
	/**
	 * Returns a function builder which will return true if its
	 * parameter is contained in the given set of values.
	 * @param <T> the element type
	 * @param values the values to return from
	 * @return the function builder
	 */
	@Nonnull 
	public static <T> Func1Builder<T, Boolean> from(@Nonnull final Set<? super T> values) {
		return from(Functions.asFunc1(values));
	}
	/**
	 * Creates a function builder which relays all invocations to the supplied
	 * map's get method.
	 * @param <T> the key type
	 * @param <U> the value type
	 * @param map the map to wrap
	 * @return the function builder
	 */
	@Nonnull 
	public static <T, U> Func1Builder<T, U> from(@Nonnull final Map<? super T, ? extends U> map) {
		return from(Functions.asFunc1(map));
	}
	/**
	 * @param <T> the parameter type (irrelevant)
	 * @return a function builder which returns always true.
	 */
	@Nonnull 
	public static <T> Func1Builder<T, Boolean> alwaysTrue() {
		return from(Functions.<T>alwaysTrue1());
	}
	/** 
	 * @param <T> the parameter type (irrelevant)
	 * @return a function builder which retuns always false. 
	 */
	@Nonnull 
	public static <T> Func1Builder<T, Boolean> alwaysFalse() {
		return from(Functions.<T>alwaysFalse1());
	}
	@Override
	public U invoke(T param1) {
		return f.invoke(param1);
	}
	/**
	 * Convert this function into a zero parameter function builder by fixing the parameter
	 * to the given value.
	 * @param param1 the fixed parameter value
	 * @return the function builder
	 */
	@Nonnull 
	public Func0Builder<U> toFunc0(final T param1) {
		return Func0Builder.from(new Func0<U>() {
			@Override
			public U invoke() {
				return f.invoke(param1);
			}
		});
	}
	/**
	 * Returns a function which takes the logical not of the wrapped boolean returning function.
	 * <p><b>Note: this function will throw a ClassCastException if the current function return type
	 * is not Boolean.</b></p>
	 * @return the function builder.
	 */
	@SuppressWarnings("unchecked")
	@Nonnull 
	public Func1Builder<T, Boolean> not() {
		return from(Functions.not((Func1<T, Boolean>)f));
	}
	/**
	 * Returns a function which produces the logical AND value of this and the other function.
	 * <p><b>Note: this function will throw a ClassCastException if the current function return type
	 * is not Boolean.</b></p>
	 * @param func the function to AND with
	 * @return the function builder
	 */
	@SuppressWarnings("unchecked")
	@Nonnull 
	public Func1Builder<T, Boolean> and(@Nonnull final Func1<? super T, Boolean> func) {
		return from(Functions.and((Func1<T, Boolean>)f, func));
	}
	/**
	 * Returns a function which produces the logical AND value of this and the other function.
	 * <p><b>Note: this function will throw a ClassCastException if the current function return type
	 * is not Boolean.</b></p>
	 * @param func the function to AND with
	 * @return the function builder
	 */
	@Nonnull 
	public Func1Builder<T, Boolean> and(@Nonnull final Func0<Boolean> func) {
		return from(new Pred1<T>() {
			@Override
			public Boolean invoke(T param1) {
				return ((Boolean)f.invoke(param1)) && func.invoke();
			}
		});
	}
	/**
	 * Returns a function which produces the logical OR value of this and the other function.
	 * <p><b>Note: this function will throw a ClassCastException if the current function return type
	 * is not Boolean.</b></p>
	 * @param func the function to OR with
	 * @return the function builder
	 */
	@SuppressWarnings("unchecked")
	@Nonnull 
	public Func1Builder<T, Boolean> or(Func1<? super T, Boolean> func) {
		return from(Functions.or((Func1<T, Boolean>)f, func));
	}
	/**
	 * Returns a function which produces the logical AND value of this and the other function.
	 * <p><b>Note: this function will throw a ClassCastException if the current function return type
	 * is not Boolean.</b></p>
	 * @param func the function to AND with
	 * @return the function builder
	 */
	@Nonnull 
	public Func1Builder<T, Boolean> or(@Nonnull final Func0<Boolean> func) {
		return from(new Pred1<T>() {
			@Override
			public Boolean invoke(T param1) {
				return ((Boolean)f.invoke(param1)) || func.invoke();
			}
		});
	}
	/**
	 * Returns a function which produces the logical XOR value of this and the other function.
	 * <p><b>Note: this function will throw a ClassCastException if the current function return type
	 * is not Boolean.</b></p>
	 * @param func the function to XOR with
	 * @return the function builder
	 */
	@SuppressWarnings("unchecked")
	@Nonnull 
	public Func1Builder<T, Boolean> xor(@Nonnull Func1<? super T, Boolean> func) {
		return from(Functions.xor((Func1<T, Boolean>)f, func));
	}
	/**
	 * Returns a function which produces the logical AND value of this and the other function.
	 * <p><b>Note: this function will throw a ClassCastException if the current function return type
	 * is not Boolean.</b></p>
	 * @param func the function to AND with
	 * @return the function builder
	 */
	@Nonnull 
	public Func1Builder<T, Boolean> xor(@Nonnull final Func0<Boolean> func) {
		return from(new Pred1<T>() {
			@Override
			public Boolean invoke(T param1) {
				return ((Boolean)f.invoke(param1)) ^ func.invoke();
			}
		});
	}
	/**
	 * Compose this function with the other function by supplying the output of this
	 * function as input for the other function and return its result,
	 * e.g., {@code func(this(T))}.
	 * @param <V> the new return type
	 * @param func the function to compose with
	 * @return the function builder with the new return type
	 */
	@Nonnull 
	public <V> Func1Builder<T, V> composeTo(@Nonnull final Func1<? super U, ? extends V> func) {
		return from(new Func1<T, V>() {
			@Override
			public V invoke(T param1) {
				return func.invoke(f.invoke(param1));
			}
		});
	}
	/**
	 * Compose this function with the other function by supplying the output of this
	 * function as input for the other function and return its result,
	 * e.g., {@code this(func(V))}.
	 * @param <V> the outer parameter type
	 * @param func the function to compose with
	 * @return the function builder with the new return type
	 */
	@Nonnull 
	public <V> Func1Builder<V, U> composeFrom(@Nonnull final Func1<? super V, ? extends T> func) {
		return from(new Func1<V, U>() {
			@Override
			public U invoke(V param1) {
				return f.invoke(func.invoke(param1));
			}
		});
	}
	/**
	 * Construct a function which invokes the given action and
	 * returns a constant value.
	 * @param <T> the function parameter type
	 * @param <U> the return type
	 * @param action the action to invoke on each function invocation
	 * @param result the return value by this function
	 * @return the function builder
	 */
	@Nonnull 
	public static <T, U> Func1Builder<T, U> from(@Nonnull Action0 action, U result) {
		return from(Functions.<T, U>asFunc1(action, result));
	}
	/**
	 * Construct a function which invokes the given runnable and
	 * returns a constant value.
	 * @param <T> the function parameter type
	 * @param <U> the return type
	 * @param run the runnable to wrap
	 * @param result the return value by this function
	 * @return the function builder
	 */
	@Nonnull 
	public static <T, U> Func1Builder<T, U> from(@Nonnull Runnable run, U result) {
		return from(Functions.<T, U>asFunc1(run, result));
	}
	/**
	 * Construct a function which invokes the given action and
	 * returns a constant value.
	 * @param <T> the function parameter type
	 * @param <U> the return type
	 * @param action the action to invoke on each function invocation
	 * @param result the return value by this function
	 * @return the function builder
	 */
	@Nonnull 
	public static <T, U> Func1Builder<T, U> from(@Nonnull Action1<? super T> action, U result) {
		return from(Functions.<T, U>asFunc1(action, result));
	}
	/**
	 * Wrap the given function into a new builder.
	 * @param <T> the parameter type
	 * @param <U> the return type
	 * @param f the function to wrap
	 * @return the function builder
	 */
	@Nonnull 
	public static <T, U> Func1Builder<T, U> from(@Nonnull Callable<? extends U> f) {
		return from(Functions.<T, U>asFunc1(f));
	}
	/**
	 * Returns an indexer function which returns the corresponding array element.
	 * @param values the array of values
	 * @return the function builder
	 */
	@Nonnull 
	public static Func1Builder<Integer, Integer> from(@Nonnull int... values) {
		return from(Functions.asFunc1(values));
	}
	/**
	 * Returns an indexer function which returns the corresponding array element.
	 * @param values the array of values
	 * @return the function builder
	 */
	@Nonnull 
	public static Func1Builder<Integer, Double> from(@Nonnull double... values) {
		return from(Functions.asFunc1(values));
	}
	/**
	 * Returns an indexer function which returns the corresponding array element.
	 * @param values the array of values
	 * @return the function builder
	 */
	@Nonnull 
	public static Func1Builder<Integer, Long> from(@Nonnull long... values) {
		return from(Functions.asFunc1(values));
	}
	/**
	 * Returns an indexer function which returns the corresponding list element.
	 * @param values the list of numbers
	 * @return the function builder
	 */
	@Nonnull 
	public static Func1Builder<Integer, Double> fromDoubles(@Nonnull List<? extends Number> values) {
		return from(Functions.asDoubleFunc1(values));
	}
	/**
	 * Returns an indexer function which returns the corresponding list element.
	 * @param values the list of numbers
	 * @return the function builder
	 */
	@Nonnull 
	public static Func1Builder<Integer, Integer> fromInts(@Nonnull List<? extends Number> values) {
		return from(Functions.asIntFunc1(values));
	}
	/**
	 * Returns an indexer function which returns the corresponding list element.
	 * @param values the list of numbers
	 * @return the function builder
	 */
	@Nonnull 
	public static Func1Builder<Integer, Long> fromLongs(@Nonnull List<? extends Number> values) {
		return from(Functions.asLongFunc1(values));
	}
}
