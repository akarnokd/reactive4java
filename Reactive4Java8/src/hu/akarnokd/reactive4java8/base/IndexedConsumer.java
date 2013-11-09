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
import java.util.function.Consumer;

/**
 * A consumer which takes an index and an object value.
 * @author akarnokd, 2013.11.08.
 */
@FunctionalInterface
public interface IndexedConsumer<T> {
    /**
     * The function to accept the indexed value.
     * @param index the index
     * @param value the value
     */
    void accept(int index, T value);
    /**
     * Creates an indexed consumer which calls the given other
     * indexed consumer once the current consumer accepted the incoming value.
     * @param after the indexed consumer to call after
     * @return the new composite indexed consumer
     */
    default IndexedConsumer<T> andThen(IndexedConsumer<? super T> after) {
        Objects.requireNonNull(after);
        return (i, v) -> { accept(i, v); after.accept(i, v); };
    }
    /**
     * Creates an indexed consumer which calls the given other
     * consumer once the current consumer accepted the incoming value.
     * @param after the indexed consumer to call after
     * @return the new composite indexed consumer
     */
    default IndexedConsumer<T> andThen(Consumer<? super T> after) {
        Objects.requireNonNull(after);
        return (i, v) -> { accept(i, v); after.accept(v); };
    }
}
