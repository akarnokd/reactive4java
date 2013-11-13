/*
 * Copyright 2013 karnok.
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

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 *
 * @author karnok
 */
public class Bug<T> {
    public void forEach(Consumer<? super T> consumer) {
        // ...
    }
    public void forEach(BiConsumer<? super Integer, ? super T> consumer) {
        forEach((Consumer<? super T>)(v -> consumer.accept(1, v)));
    }
    public void forEach(Predicate<? super T> stoppableConsumer) {
        // ...
    }
    public void forEach(BiPredicate<? super Integer, ? super T> stoppableConsumer) {
        forEach((Predicate<? super T>)(v -> { return stoppableConsumer.test(1, v); }));
    }
}
