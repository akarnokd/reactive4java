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

package hu.akarnokd.reactive4java.swing;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * Defines a composite class holding a dynamic method invocation arguments, i.e., [method, arguments...].
 * @author akarnokd, 2011.02.01.
 */
public final class Dynamic {
	/** The original method name. */
	@Nonnull 
	public final String method;
	/** The original method arguments. */
	@Nonnull 
	public final List<Object> arguments;
	/**
	 * Construct a new dynamic object with the given parameters.
	 * @param method the method name
	 * @param arguments the argument list
	 */
	public Dynamic(@Nonnull String method, @Nonnull Object... arguments) {
		if (method == null) {
			throw new IllegalArgumentException("method is null");
		}
		if (arguments == null) {
			throw new IllegalArgumentException("arguments is null");
		}
		this.method = method;
		this.arguments = Collections.unmodifiableList(Arrays.asList(arguments));
	}
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Dynamic) {
			Dynamic d = (Dynamic)obj;
			return method.equals(d.method) && arguments.equals(d.arguments);
		}
		return false;
	}
	@Override
	public int hashCode() {
		return (17 + method.hashCode()) * 31 + arguments.hashCode();
	}
	@Override
	public String toString() {
		return method + " " + arguments;
	}
}
