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

/**
 * Base interface for observing a stream of values.
 * <p>reactive4java note: the interface has default empty
 * implementation for the error() and finish methods
 * to allow simple lambda expressions to register with
 * the {@link Observable} implementations.</p>
 * @author akarnokd, 2013.11.08
 * @param <T> the value type to be observed
 */
@FunctionalInterface
public interface Observer<T> extends BaseObserver {
    /**
     * Receive a value.
     * <p>Exceptions thrown by the method is forwarded to
     * the error() method.</p>
     * @param value the value received
     */
    void next(T value);
    /**
     * Creates an observer which calls the given consumer
     * when it receives an event, but forwards the error and
     * finish events to the given wrapped (base)observer.
     * @param <T> the value type of the observer
     * @param o the base observer to wrap
     * @param consumer the consumer of events
     * @return the new observer
     */
    public static <T> Observer<T> wrap(BaseObserver o, Consumer<? super T> consumer) {
        return new Observer<T>() {
            @Override
            public void next(T value) {
                consumer.accept(value);
            }
            @Override
            public void error(Throwable t) {
                o.error(t);
            }
            @Override
            public void finish() {
                o.finish();
            }
        };
    }
    /**
     * Creates an observer by specifying its next and
     * error methods as the given consumer lambda functions.
     * @param <T>
     * @param next
     * @param error
     * @return 
     */
    public static <T> Observer<T> create(
            Consumer<? super T> next,
            Consumer<? super Throwable> error) {
        return new Observer<T>() {
            @Override
            public void next(T value) {
                next.accept(value);
            }
            @Override
            public void error(Throwable t) {
                error.accept(t);
            }
        };
    }
    /**
     * Creates an observer by specifying its next, error and
     * finish methods as the given consumer lambda functions.
     * @param <T>
     * @param next
     * @param error
     * @param finish
     * @return 
     */
    public static <T> Observer<T> create(
            Consumer<? super T> next,
            Consumer<? super Throwable> error,
            Runnable finish) {
        return new Observer<T>() {
            @Override
            public void next(T value) {
                next.accept(value);
            }
            @Override
            public void error(Throwable t) {
                error.accept(t);
            }
            @Override
            public void finish() {
                finish.run();
            }
        };
    }
    /**
     * Creates an observer by specifying its next and
     * finish methods as the given consumer lambda functions.
     * @param <T>
     * @param next
     * @param finish
     * @return 
     */
    public static <T> Observer<T> create(
            Consumer<? super T> next,
            Runnable finish) {
        return new Observer<T>() {
            @Override
            public void next(T value) {
                next.accept(value);
            }
            @Override
            public void finish() {
                finish.run();
            }
        };
        
    }
}
