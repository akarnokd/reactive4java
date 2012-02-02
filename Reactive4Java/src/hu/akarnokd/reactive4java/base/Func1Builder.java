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
	public static <T, U> Func1Builder<T, U> from(@Nonnull Func0<U> f) {
		return from(Functions.<T, U>asFunc1(f));
	}
	@Override
	public U invoke(T param1) {
		return f.invoke(param1);
	}

}
