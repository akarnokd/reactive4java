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
import java.util.function.Predicate;

/**
 * An indexed predicate that takes and index and an object value.
 * @author akarnokd, 2013.11.09.
 * @param <T> the value type
 */
@FunctionalInterface
public interface IndexedPredicate<T> {
    /**
     * Test the given index and object
     * @param index the index
     * @param value the value
     * @return true if the test succeded
     */
    boolean test(int index, T value);
    default IndexedPredicate<T> negate() {
        return (i, v) -> !test(i, v);
    }
    default IndexedPredicate<T> and(IndexedPredicate<? super T> other) {
        Objects.requireNonNull(other);
        return (i, v) -> test(i, v) && other.test(i, v);
    }
    default IndexedPredicate<T> and(Predicate<? super T> other) {
        Objects.requireNonNull(other);
        return (i, v) -> test(i, v) && other.test(v);
    }
    default IndexedPredicate<T> or(IndexedPredicate<? super T> other) {
        Objects.requireNonNull(other);
        return (i, v) -> test(i, v) || other.test(i, v);
    }
    default IndexedPredicate<T> or(Predicate<? super T> other) {
        Objects.requireNonNull(other);
        return (i, v) -> test(i, v) || other.test(v);
    }
}
