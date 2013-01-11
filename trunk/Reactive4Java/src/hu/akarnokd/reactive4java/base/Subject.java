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
 * Represents an object that can observe values of T and produce values of U by
 * combining the Observer and Observable interfaces.
 * @author akarnokd, 2013.01.09.
 * @since 0.97
 * @param <T> the value type this object can observe
 * @param <U> the value type this object can produce
 */
public interface Subject<T, U> extends Observer<T>, Observable<U> {

}
