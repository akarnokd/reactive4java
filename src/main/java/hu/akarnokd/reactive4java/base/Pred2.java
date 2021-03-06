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
package hu.akarnokd.reactive4java.base;

/**
 * A convenience extension interface over the Func2 with
 * the return type fixed as boolean.
 * @author akarnokd, 2011.02.19.
 * @param <T> the first parameter type
 * @param <U> the second parameter type
 */
public interface Pred2<T, U> extends Func2<T, U, Boolean> {

}
