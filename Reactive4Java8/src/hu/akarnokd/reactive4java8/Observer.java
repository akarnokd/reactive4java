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

package hu.akarnokd.reactive4java8;

import hu.akarnokd.reactive4java8.observers.BaseObserver;
import hu.akarnokd.reactive4java8.observers.BasicObserver;
import hu.akarnokd.reactive4java8.observers.SimpleObserver;
import hu.akarnokd.reactive4java8.observers.ThreadSafeObserver;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Base interface for observing a stream of events.
 * @author akarnokd, 2013.11.08
 * @param <T> the value type to be observed
 */
public interface Observer<T> extends BaseObserver {
    /**
     * Receive a value.
     * <p>Exceptions thrown by the method is forwarded to
     * the error() method.</p>
     * @param value the value received
     */
    void next(T value);
    /**
     * Constructs an observer with the specified event handler methods.
     * @param <T>
     * @param onNext
     * @return 
     */
    static <T> Observer<T> create(Consumer<? super T> onNext) {
        return SimpleObserver.<T>builder()
            .next(onNext)
            .create();
    }
    /**
     * Constructs an observer with the specified event handler methods.
     * @param <T>
     * @param onNext
     * @param onError
     * @return 
     */
    static <T> Observer<T> create(
            Consumer<? super T> onNext,
            Consumer<? super Throwable> onError
    ) {
        return SimpleObserver.<T>builder()
            .next(onNext)
            .error(onError)
            .create();
    }
    /**
     * Constructs an observer with the specified event handler methods.
     * @param <T>
     * @param onNext
     * @param onError
     * @param onFinish
     * @return 
     */
    static <T> Observer<T> create(
            Consumer<? super T> onNext,
            Consumer<? super Throwable> onError,
            Runnable onFinish
    ) {
        return SimpleObserver.<T>builder()
            .next(onNext)
            .error(onError)
            .finish(onFinish)
            .create();
    }
    /**
     * Constructs an observer with the specified event handler methods.
     * @param <T>
     * @param reg
     * @param onNext
     * @param onError
     * @param onFinish
     * @return 
     */
    static <T> Observer<T> create(
            Registration reg,
            Consumer<? super T> onNext,
            Consumer<? super Throwable> onError,
            Runnable onFinish
    ) {
        return SimpleObserver.<T>builder()
            .registration(reg)
            .next(onNext)
            .error(onError)
            .finish(onFinish)
            .create();
    }
    /**
     * Constructs an observer with the specified event handler methods.
     * @param <T>
     * @param sharedLock
     * @param reg
     * @param onNext
     * @param onError
     * @param onFinish
     * @return 
     */
    static <T> Observer<T> create(
            Lock sharedLock,
            Registration reg,
            Consumer<? super T> onNext,
            Consumer<? super Throwable> onError,
            Runnable onFinish
    ) {
        return SimpleObserver.<T>builder()
            .lock(sharedLock)
            .registration(reg)
            .next(onNext)
            .error(onError)
            .finish(onFinish)
            .create();
    }
    /**
     * Constructs an observer with the specified event handler methods.
     * @param <T>
     * @param onNext
     * @param onError
     * @param onFinish
     * @return 
     */
    static <T> Observer<T> create(
            BiConsumer<? super T, ? super Registration> onNext,
            BiConsumer<? super Throwable, ? super Registration> onError,
            Consumer<? super Registration> onFinish
    ) {
        return SimpleObserver.<T>builder()
            .next(onNext)
            .error(onError)
            .finish(onFinish)
            .create();
    }
    /**
     * Constructs an observer with the specified event handler methods.
     * @param <T>
     * @param reg
     * @param onNext
     * @param onError
     * @param onFinish
     * @return 
     */
    static <T> Observer<T> create(
            Registration reg,
            BiConsumer<? super T, ? super Registration> onNext,
            BiConsumer<? super Throwable, ? super Registration> onError,
            Consumer<? super Registration> onFinish
    ) {
        return SimpleObserver.<T>builder()
            .registration(reg)
            .next(onNext)
            .error(onError)
            .finish(onFinish)
            .create();
    }
    /**
     * Constructs an observer with the specified event handler methods.
     * @param <T>
     * @param sharedLock
     * @param reg
     * @param onNext
     * @param onError
     * @param onFinish
     * @return 
     */
    static <T> Observer<T> create(
            Lock sharedLock,
            Registration reg,
            BiConsumer<? super T, ? super Registration> onNext,
            BiConsumer<? super Throwable, ? super Registration> onError,
            Consumer<? super Registration> onFinish
    ) {
        return SimpleObserver.<T>builder()
            .lock(sharedLock)
            .registration(reg)
            .next(onNext)
            .error(onError)
            .finish(onFinish)
            .create();
    }
    default <U> Observer<U> compose(Function<? super U, ? extends T> function) {
        return new BasicObserver<>(
            this,
            (v) -> next(function.apply(v))
        );
    }
    /**
     * Wraps the given observer and overrides its next() behavior
     * with the lambda.
     * @param <T>
     * @param observer
     * @param onNext
     * @return 
     */
    static <T> Observer<T> wrap(BaseObserver observer, Consumer<? super T> onNext) {
        return new BasicObserver<>(observer, onNext);
    }
    default Observer<T> toThreadSafe() {
        return new ThreadSafeObserver(this);
    }
    default Observer<T> toThreadSafe(Lock lock) {
        return new ThreadSafeObserver(lock, this);
    }
}
