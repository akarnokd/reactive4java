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

import java.util.function.Consumer;

public class Strange2 {
    interface V<T> {
        void next(T t);
    }
    static class V1 {
        static class Builder<T> {
            Builder<T> add(Consumer<? super T> func) {
                return this;
            }
            AutoCloseable create(O<? extends T> o) {
                return () -> { };
            }
        }
        static <T> Builder<T> builder() {
            return new Builder<>();
        }
    }
    interface O<T> {
        AutoCloseable register(V<? super T> listener);
//        default O<T> make() {
//            return (o) -> V1.builder().add(o::next).create(this);
//        }
        default O<T> make2() {
            return (o) -> V1.<T>builder().add(o::next).create(this);
        }
    }
}
