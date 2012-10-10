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

/**
 * A parameterless function interface.
 * When <code>Func1</code> is used in a method parameter declaration, you
 * may consider using wildcards: 
 * <p><code>&lt;T> T someMethod(Func0&lt;? extends T> f);</code></p>
 * @author akarnokd
 * @param <Return> the return type
 */
public interface Func0<Return> {
	/**
	 * The function body to invoke.
	 * @return the return type
	 */
	Return invoke();
}
