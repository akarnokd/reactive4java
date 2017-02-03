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
import hu.akarnokd.reactive4java.base.Action2;
import hu.akarnokd.reactive4java.base.Func0;
import hu.akarnokd.reactive4java.base.Func2;
import hu.akarnokd.reactive4java.base.Pred2;

import java.util.Comparator;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

/**
 * Class holding a Func2 object and providing various relevant methods 
 * of the {@code Functions} utility class as instance methods.
 * <p>The class itself is of type {@code Func2<T, U, V>} and can be used where this type is needed.</p>
 * @author akarnokd, 2012.02.02.
 * @param <T> the function first parameter type
 * @param <U> the function second parameter type
 * @param <V> the return type
 * @since 0.96.1
 */
public class Func2Builder<T, U, V> implements Func2<T, U, V> {
	/** The wrapped function. */
	@Nonnull
	protected final Func2<T, U, V> f;
	/**
	 * Construct an instance of this builder with the wrapped function.
	 * @param f the function to wrap
	 */
	protected Func2Builder(@Nonnull Func2<T, U, V> f) {
		this.f = f;
	}
	/**
	 * Wrap the given function into a function builder instance.
	 * @param <T> the function first parameter type
	 * @param <U> the function second parameter type
	 * @param <V> the return type
	 * @param f the function to wrap
	 * @return the function builder
	 */
	@Nonnull 
	public static <T, U, V> Func2Builder<T, U, V> from(@Nonnull Func2<T, U, V> f) {
		return new Func2Builder<T, U, V>(f);
	}
	/**
	 * Wraps the given Func0 function into a Func2 object which ignores
	 * its parameter T.
	 * @param <T> the function first parameter type
	 * @param <U> the function second parameter type
	 * @param <V> the return type
	 * @param f the function to wrap
	 * @return the function builder
	 */
	@Nonnull 
	public static <T, U, V> Func2Builder<T, U, V> from(@Nonnull Func0<V> f) {
		return from(Functions.<T, U, V>asFunc2(f));
	}
	/**
	 * Wraps the given value and the function returns this value
	 * regardless of the parameters.
	 * @param <T> the function parameter type, irrelevant
	 * @param <U> the function parameter type, irrelevant
	 * @param <V> the return type
	 * @param value the value to return
	 * @return the function builder
	 */
	@Nonnull 
	public static <T, U, V> Func2Builder<T, U, V> from(final V value) {
		return from(Functions.<T, U, V>constant2(value));
	}
	@Override
	public V invoke(T param1, U param2) {
		return f.invoke(param1, param2);
	}
	/**
	 * @param <T> the parameter type (irrelevant)
	 * @param <U> the parameter type (irrelevant)
	 * @return a function builder which returns always true.
	 */
	@Nonnull 
	public static <T, U> Func2Builder<T, U, Boolean> alwaysTrue() {
		return from(Functions.<T, U>alwaysTrue2());
	}
	/** 
	 * @param <T> the parameter type (irrelevant)
	 * @param <U> the parameter type (irrelevant)
	 * @return a function builder which retuns always false. 
	 */
	@Nonnull 
	public static <T, U> Func2Builder<T, U, Boolean> alwaysFalse() {
		return from(Functions.<T, U>alwaysFalse2());
	}
	/**
	 * Convert this function into a zero parameter function builder by fixing the parameter
	 * to the given values.
	 * @param param1 the fixed parameter value
	 * @param param2 the fixed parameter value
	 * @return the function builder
	 */
	@Nonnull 
	public Func0Builder<V> toFunc0(final T param1, final U param2) {
		return Func0Builder.from(new Func0<V>() {
			@Override
			public V invoke() {
				return f.invoke(param1, param2);
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
	public Func2Builder<T, U, Boolean> not() {
		return from(Functions.not((Func2<T, U, Boolean>)f));
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
	public Func2Builder<T, U, Boolean> and(@Nonnull final Func2<? super T, ? super U, Boolean> func) {
		return from(Functions.and((Func2<T, U, Boolean>)f, func));
	}
	/**
	 * Returns a function which produces the logical AND value of this and the other function.
	 * <p><b>Note: this function will throw a ClassCastException if the current function return type
	 * is not Boolean.</b></p>
	 * @param func the function to AND with
	 * @return the function builder
	 */
	@Nonnull 
	public Func2Builder<T, U, Boolean> and(@Nonnull final Func0<Boolean> func) {
		return from(new Pred2<T, U>() {
			@Override
			public Boolean invoke(T param1, U param2) {
				return ((Boolean)f.invoke(param1, param2)) && func.invoke();
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
	public Func2Builder<T, U, Boolean> or(
			@Nonnull Func2<? super T, ? super U, Boolean> func) {
		return from(Functions.or((Func2<T, U, Boolean>)f, func));
	}
	/**
	 * Returns a function which produces the logical AND value of this and the other function.
	 * <p><b>Note: this function will throw a ClassCastException if the current function return type
	 * is not Boolean.</b></p>
	 * @param func the function to AND with
	 * @return the function builder
	 */
	@Nonnull 
	public Func2Builder<T, U, Boolean> or(@Nonnull final Func0<Boolean> func) {
		return from(new Pred2<T, U>() {
			@Override
			public Boolean invoke(T param1, U param2) {
				return ((Boolean)f.invoke(param1, param2)) || func.invoke();
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
	public Func2Builder<T, U, Boolean> xor(
			@Nonnull Func2<? super T, ? super U, Boolean> func) {
		return from(Functions.xor((Func2<T, U, Boolean>)f, func));
	}
	/**
	 * Returns a function which produces the logical AND value of this and the other function.
	 * <p><b>Note: this function will throw a ClassCastException if the current function return type
	 * is not Boolean.</b></p>
	 * @param func the function to AND with
	 * @return the function builder
	 */
	@Nonnull 
	public Func2Builder<T, U, Boolean> xor(@Nonnull final Func0<Boolean> func) {
		return from(new Pred2<T, U>() {
			@Override
			public Boolean invoke(T param1, U param2) {
				return ((Boolean)f.invoke(param1, param2)) ^ func.invoke();
			}
		});
	}
	/**
	 * Construct a function which invokes the given action and
	 * returns a constant value.
	 * @param <T> the function first parameter type
	 * @param <U> the function second parameter type
	 * @param <V> the return type
	 * @param action the action to invoke on each function invocation
	 * @param result the return value by this function
	 * @return the function builder
	 */
	@Nonnull 
	public static <T, U, V> Func2Builder<T, U, V> from(@Nonnull Action0 action, V result) {
		return from(Functions.<T, U, V>asFunc2(action, result));
	}
	/**
	 * Construct a function which invokes the given runnable and
	 * returns a constant value.
	 * @param <T> the function first parameter type
	 * @param <U> the function second parameter type
	 * @param <V> the return type
	 * @param run the runnable to wrap
	 * @param result the return value by this function
	 * @return the function builder
	 */
	@Nonnull 
	public static <T, U, V> Func2Builder<T, U, V> from(
			@Nonnull Runnable run, V result) {
		return from(Functions.<T, U, V>asFunc2(run, result));
	}
	/**
	 * Construct a function which invokes the given action and
	 * returns a constant value.
	 * @param <T> the function first parameter type
	 * @param <U> the function second parameter type
	 * @param <V> the return type
	 * @param action the action to invoke on each function invocation
	 * @param result the return value by this function
	 * @return the function builder
	 */
	@Nonnull 
	public static <T, U, V> Func2Builder<T, U, V> from(
			@Nonnull Action2<? super T, ? super U> action, V result) {
		return from(Functions.<T, U, V>asFunc2(action, result));
	}
	/**
	 * Wrap the given function into a new builder.
	 * @param <T> the function first parameter type
	 * @param <U> the function second parameter type
	 * @param <V> the return type
	 * @param f the function to wrap
	 * @return the function builder
	 */
	@Nonnull 
	public static <T, U, V> Func2Builder<T, U, V> from(
			@Nonnull Callable<? extends V> f) {
		return from(Functions.<T, U, V>asFunc2(f));
	}
	/**
	 * Wraps the given comparator into a 2 parameter function.
	 * @param <T> the value type
	 * @param comp the comparator to wrap
	 * @return the function builder
	 */
	@Nonnull 
	public static <T> Func2Builder<T, T, Integer> from(
			@Nonnull Comparator<? super T> comp) {
		return from(Functions.<T>asFunc2(comp));
	}
	/**
	 * Convert this two parameter function into a comparator.
	 * <p><b>Note, if the parameter types are not the same or the return type is not Integer
	 * you might expect to get a ClassCastException.</b></p>
	 * @return the comparator representing this function
	 */
	@Nonnull 
	public Comparator<T> toComparator() {
		return new Comparator<T>() {
			@Override
			@SuppressWarnings("unchecked")
			public int compare(T o1, T o2) {
				return (Integer)f.invoke(o1, (U)o2);
			}
		};
	}
	/**
	 * Returns a matrix indexer function.
	 * @param <T> the element type
	 * @param matrix the matrix to index
	 * @return the function builder
	 */
	@Nonnull 
	public static <T> Func2Builder<Integer, Integer, T> from(
			@Nonnull T[][] matrix) {
		return from(Functions.asFunc2(matrix));
	}
	/**
	 * Returns a matrix indexer function.
	 * @param matrix the matrix to index
	 * @return the function builder
	 */
	@Nonnull 
	public static Func2Builder<Integer, Integer, Integer> from(
			@Nonnull int[][] matrix) {
		return from(Functions.asFunc2(matrix));
	}
	/**
	 * Returns a matrix indexer function.
	 * @param matrix the matrix to index
	 * @return the function builder
	 */
	@Nonnull 
	public static Func2Builder<Integer, Integer, Double> from(
			@Nonnull double[][] matrix) {
		return from(Functions.asFunc2(matrix));
	}
	/**
	 * Returns a matrix indexer function.
	 * @param matrix the matrix to index
	 * @return the function builder
	 */
	@Nonnull 
	public static Func2Builder<Integer, Integer, Long> from(
			@Nonnull long[][] matrix) {
		return from(Functions.asFunc2(matrix));
	}
}
