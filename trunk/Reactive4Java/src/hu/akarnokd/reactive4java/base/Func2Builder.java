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
	public static <T, U, V> Func2Builder<T, U, V> from(@Nonnull Func0<V> f) {
		return from(Functions.<T, U, V>asFunc2(f));
	}
	@Override
	public V invoke(T param1, U param2) {
		return f.invoke(param1, param2);
	}

}
