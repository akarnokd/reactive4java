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
 * The function interface which takes two parameter and returns something.
 * @author karnokd
 * @param <Return> the return type
 * @param <Param1> the first parameter
 * @param <Param2> the second parameter
 */
public interface Func2<Return, Param1, Param2> {
	/**
	 * The method that gets invoked with two parameters.
	 * @param param1 the first parameter value
	 * @param param2 the second parameter value
	 * @return the return object
	 */
	Return invoke(Param1 param1, Param2 param2);
}
