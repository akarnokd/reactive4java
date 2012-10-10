/*
 * Copyright 2011-2012 David Karnok
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

import java.lang.ref.Reference;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

/**
 * Class holding a Func0 object and providing various relevant methods 
 * of the {@code Functions} utility class as instance methods.
 * <p>The class itself is of type {@code Func0<T>} and can be used where this type is needed.</p>
 * @author akarnokd, 2012.02.02.
 * @param <T> the return type
 * @since 0.96.1
 */
public final class Func0Builder<T> implements Func0<T> {
	/** The wrapped function. */
	@Nonnull 
	protected final Func0<T> f;
	/**
	 * Constructs an instance of this builder with the wrapped function.
	 * @param f the function to wrap
	 */
	protected Func0Builder(@Nonnull Func0<T> f) {
		this.f = f;
	}
	@Override
	public T invoke() {
		return f.invoke();
	}
	/**
	 * Construct a function builder from the given existing function.
	 * @param <T> the return type
	 * @param f the function to wrap
	 * @return the function builder
	 */
	public static <T> Func0Builder<T> from(Func0<T> f) {
		return new Func0Builder<T>(f);
	}
	/**
	 * Construct a function which returns a constant value.
	 * @param <T> the value type
	 * @param value the value returned by the function
	 * @return the function builder
	 */
	public static <T> Func0Builder<T> from(T value) {
		return from(Functions.constant0(value));
	}
	/**
	 * Construct a function which invokes the given action and
	 * returns a constant value.
	 * @param <T> the return type
	 * @param action the action to invoke on each function invocation
	 * @param result the return value by this function
	 * @return the function builder
	 */
	public static <T> Func0Builder<T> from(@Nonnull Action0 action, T result) {
		return from(Functions.asFunc0(action, result));
	}
	/**
	 * Construct a function which invokes the given runnable and
	 * returns a constant value.
	 * @param <T> the return type
	 * @param run the runnable to wrap
	 * @param result the return value by this function
	 * @return the function builder
	 */
	public static <T> Func0Builder<T> from(@Nonnull Runnable run, T result) {
		return from(Functions.asFunc0(run, result));
	}
	/**
	 * Construct a function from the given callable instance.
	 * @param <T> the return type
	 * @param call the callable instance
	 * @return the function builder
	 */
	public static <T> Func0Builder<T> from(@Nonnull Callable<? extends T> call) {
		return from(Functions.asFunc0(call));
	}
	/**
	 * Construct a function from the given atomic variable.
	 * @param value the atomic variable
	 * @return the function builder
	 */
	public static Func0Builder<Boolean> from(@Nonnull AtomicBoolean value) {
		return from(Functions.asFunc0(value));
	}
	/**
	 * Construct a function from the given atomic variable.
	 * @param value the atomic variable
	 * @return the function builder
	 */
	public static Func0Builder<Integer> from(@Nonnull AtomicInteger value) {
		return from(Functions.asFunc0(value));
	}
	/**
	 * Construct a function from the given atomic variable.
	 * @param value the atomic variable
	 * @return the function builder
	 */
	public static Func0Builder<Long> from(@Nonnull AtomicLong value) {
		return from(Functions.asFunc0(value));
	}
	/**
	 * Construct a function from the given atomic variable reference.
	 * @param <T> the value type of the reference
	 * @param value the atomic variable
	 * @return the function builder
	 */
	public static <T> Func0Builder<T> from(@Nonnull AtomicReference<? extends T> value) {
		return from(Functions.asFunc0(value));
	}
	/**
	 * @return a function builder which returns always true.
	 */
	public static Func0Builder<Boolean> alwaysTrue() {
		return from(Functions.TRUE);
	}
	/** 
	 * @return a function builder which retuns always false. 
	 */
	public static Func0Builder<Boolean> alwaysFalse() {
		return from(Functions.FALSE);
	}
	/**
	 * Wraps the given reference object into a function call.
	 * <p>Note that the function may return null if the reference's object
	 * gets garbage collected.</p>
	 * @param <T> the referenced object type
	 * @param ref the reference to wrap
	 * @return the function builder
	 */
	public static <T> Func0Builder<T> from(@Nonnull final Reference<? extends T> ref) {
		return from(Functions.asFunc0(ref));
	}
	/**
	 * Returns a function which takes the logical not of the wrapped boolean returning function.
	 * <p><b>Note: this function will throw a ClassCastException if the current function return type
	 * is not Boolean.</b></p>
	 * @return the function builder.
	 */
	@SuppressWarnings("unchecked")
	public Func0Builder<Boolean> not() {
		return from(Functions.not((Func0<Boolean>)f));
	}
	/**
	 * Returns a function which produces the logical AND value of this and the other function.
	 * <p><b>Note: this function will throw a ClassCastException if the current function return type
	 * is not Boolean.</b></p>
	 * @param func the function to AND with
	 * @return the function builder
	 */
	@SuppressWarnings("unchecked")
	public Func0Builder<Boolean> and(Func0<Boolean> func) {
		return from(Functions.and((Func0<Boolean>)f, func));
	}
	/**
	 * Returns a function which produces the logical OR value of this and the other function.
	 * <p><b>Note: this function will throw a ClassCastException if the current function return type
	 * is not Boolean.</b></p>
	 * @param func the function to OR with
	 * @return the function builder
	 */
	@SuppressWarnings("unchecked")
	public Func0Builder<Boolean> or(Func0<Boolean> func) {
		return from(Functions.or((Func0<Boolean>)f, func));
	}
	/**
	 * Returns a function which produces the logical XOR value of this and the other function.
	 * <p><b>Note: this function will throw a ClassCastException if the current function return type
	 * is not Boolean.</b></p>
	 * @param func the function to XOR with
	 * @return the function builder
	 */
	@SuppressWarnings("unchecked")
	public Func0Builder<Boolean> xor(Func0<Boolean> func) {
		return from(Functions.xor((Func0<Boolean>)f, func));
	}
	/** @return convert this function into a callable instance. */
	public Callable<T> toCallable() {
		return Functions.asCallable(this.f);
	}
	/**
	 * @param <U> the function parameter (irrelevant) 
	 * @return convert this function by wrapping it into a 1 parameter function builder. 
	 */
	public <U> Func1Builder<U, T> toFunc1() {
		return Func1Builder.from(f);
	}
	/**
	 * @param <U> the function first parameter (irrelevant) 
	 * @param <V> the function second parameter (irrelevant) 
	 * @return convert this function by wrapping it into a 1 parameter function builder. 
	 */
	public <U, V> Func2Builder<U, V, T> toFunc2() {
		return Func2Builder.from(f);
	}
}
