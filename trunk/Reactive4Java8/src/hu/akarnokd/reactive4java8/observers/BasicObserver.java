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

package hu.akarnokd.reactive4java8.observers;

import hu.akarnokd.reactive4java8.Observer;
import java.util.Objects;
import java.util.function.Consumer;

/** Basic, unsynchronized observer executing lambdas. */
public class BasicObserver<T> implements Observer<T> {
    private final Consumer<? super T> onNext;
    private final Consumer<? super Throwable> onError;
    private final Runnable onFinish;

    public BasicObserver(Consumer<? super T> onNext, Consumer<? super Throwable> onError, Runnable onFinish) {
        this.onNext = Objects.requireNonNull(onNext);
        this.onError = Objects.requireNonNull(onError);
        this.onFinish = Objects.requireNonNull(onFinish);
    }

    /**
     * Constructor which overrides the next() event and
     * forwards the other events to the onNext lambda.
     * @param observer
     * @param onNext
     */
    public BasicObserver(BaseObserver observer, Consumer<? super T> onNext) {
        this.onNext = Objects.requireNonNull(onNext);
        this.onError = observer::error;
        this.onFinish = observer::finish;
    }

    @Override
    public void next(T value) {
        onNext.accept(value);
    }

    @Override
    public void error(Throwable t) {
        onError.accept(t);
    }

    @Override
    public void finish() {
        onFinish.run();
    }

}
