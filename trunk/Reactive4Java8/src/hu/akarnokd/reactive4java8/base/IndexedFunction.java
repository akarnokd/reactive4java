/*
 * Copyright 2013 akarnokd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hu.akarnokd.reactive4java8.base;

import java.util.Objects;
import java.util.function.Function;

/**
 * An indexed function which takes an index and a value
 * and returns another value.
 * @author akarnokd, 2013.11.09.
 * @param <T> the value type
 * @param <R> the return type
 */
@FunctionalInterface
public interface IndexedFunction<T, R> {
    /**
     * Apply to the given index and value.
     * @param index the index
     * @param value the value
     * @return the return value
     */
    R apply(int index, T value);
    default <V> IndexedFunction<V, R> compose(IndexedFunction<? super V, ? extends T> before) {
        Objects.requireNonNull(before);
        return (i, v) -> apply(i, before.apply(i, v));
    }
    default <V> IndexedFunction<V, R> compose(Function<? super V, ? extends T> before) {
        Objects.requireNonNull(before);
        return (i, v) -> apply(i, before.apply(v));
    }
    default <V> IndexedFunction<T, V> andThen(IndexedFunction<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (i, v) -> after.apply(i, apply(i, v));
    }
    default <V> IndexedFunction<T, V> andThen(Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (i, v) -> after.apply(apply(i, v));
    }
}
