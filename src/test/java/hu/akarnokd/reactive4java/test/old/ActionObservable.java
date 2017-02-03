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

package hu.akarnokd.reactive4java.test.old;

import hu.akarnokd.reactive4java.base.Action0;
import hu.akarnokd.reactive4java.base.Action1;
import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Func2;
import hu.akarnokd.reactive4java.base.Observable;
import hu.akarnokd.reactive4java.base.Observer;
import hu.akarnokd.reactive4java.base.Option;
import hu.akarnokd.reactive4java.base.Ref;
import hu.akarnokd.reactive4java.reactive.Reactive;
import hu.akarnokd.reactive4java.util.Actions;
import hu.akarnokd.reactive4java.util.Functions;
import hu.akarnokd.reactive4java.util.Observables;

import javax.annotation.Nonnull;


/**
 * The functional observable helper class with the anamorphism, bind and catamorphism basic operators.
 * Inspired by http://blogs.bartdesmet.net/blogs/bart/archive/2010/01/01/the-essence-of-linq-minlinq.aspx
 * @author akarnokd
 */
public final class ActionObservable {
    /** Helper class. */
    private ActionObservable() {
        // helper class.
    }
    /**
     * Returns an action to an action to receive an option of T.
     * @param <T> the type of the value (irrelevant)
     * @return the action to action to option
     */
    public static <T> Action1<Action1<Option<T>>> empty() {
        return new Action1<Action1<Option<T>>>() {
            @Override
            public void invoke(Action1<Option<T>> value) {
                value.invoke(Option.<T>none());
            }
        };
    }
    /**
     * Returns an action which takes an action which takes an option of T.
     * @param <T> the type of the value
     * @param value the value to return
     * @return the action to action to option of T
     */
    public static <T> Action1<Action1<Option<T>>> single(final T value) {
        return new Action1<Action1<Option<T>>>() {
            @Override
            public void invoke(Action1<Option<T>> action) {
                action.invoke(Option.some(value));
                action.invoke(Option.<T>none());
            }
        };
    }
    /**
     * The binding option to convert a source stream of Ts into streaming Rs.
     * @param <T> the source type
     * @param <R> the converted type
     * @param source the source of Ts
     * @param selector the selector which converts Ts to Rs.
     * @return the action to action that receives option of R
     */
    public static <T, R> Action1<Action1<Option<R>>> bind(
            final Action1<Action1<Option<T>>> source,
            final Func1<T, Action1<Action1<Option<R>>>> selector) {
        return new Action1<Action1<Option<R>>>() {
            @Override
            public void invoke(final Action1<Option<R>> o) {
                source.invoke(new Action1<Option<T>>() {
                    @Override
                    public void invoke(Option<T> x) {
                        if (x == Option.none()) {
                            o.invoke(Option.<R>none());
                        } else {
                            selector.invoke(x.value()).invoke(new Action1<Option<R>>() {
                                @Override
                                public void invoke(Option<R> y) {
                                    if (y != Option.none()) {
                                        o.invoke(y);
                                    }
                                }
                            });
                        }
                    }
                });
            }
        };
    }
    /**
     * Anamorphism that creates Ts starting from the seed value until the condition holds.
     * Its equivalent is a for loop: for (int i = 0; i &lt; 10; i++)
     * @param <T> the type of the values to generate
     * @param seed the initial value
     * @param condition the condition until the Ts should be produced
     * @param next the way of compute the next T
     * @return the function of function of option of T
     */
    public static <T> Action1<Action1<Option<T>>> ana(
            final T seed,
            final Func1<T, Boolean> condition,
            final Func1<T, T> next) {
        return new Action1<Action1<Option<T>>>() {
            @Override
            public void invoke(Action1<Option<T>> o) {
                for (T value = seed; condition.invoke(value); value = next.invoke(value)) {
                    o.invoke(Option.some(value));
                }
                o.invoke(Option.<T>none());
            }
        };
    }
    /**
     * The action of action of option of none provider implemented by using anamorphism.
     * @param <T> the type of the value (irrelevant)
     * @return the action to action to option
     */
    public static <T> Action1<Action1<Option<T>>> emptyAna() {
        Func1<T, Boolean> condition = Functions.alwaysFalse1();
        return ana(null, condition, null);
    }
    /**
     * The action of action of option of only a single element provider implemented by using anamorphism.
     * @param <T> the value type
     * @param value the value to return only once
     * @return the action to action to option of T
     */
    public static <T> Action1<Action1<Option<T>>> singleAna(T value) {
        Func1<T, T> identity = Functions.identity();
        return ana(value, new Func1<T, Boolean>() {
            boolean once;
            @Override
            public Boolean invoke(T param1) {
                if (once) {
                    return false;
                }
                once = true;
                return true;
            }
        }, identity);
    }
    /**
     * A catamorphism which creates a single R out of the sequence of Ts by using an aggregator.
     * The method is a greedy operation: it must wait all source values to arrive, therefore, do not use it on infinite sources.
     * @param <T> the type of the sequence values
     * @param <R> the intermediate and output types
     * @param source the source of the sequence values
     * @param seed the initial value of the aggregation (e.g., start from zero)
     * @param aggregator the aggregator function which takes a sequence value and the previous output value and produces a new output value
     * @return the aggregation result
     */
    public static <T, R> R cata(
            Action1<Action1<Option<T>>> source,
            R seed,
            final Func2<R, T, R> aggregator) {
        final Ref<R> result = Ref.of(seed);
        source.invoke(new Action1<Option<T>>() {
            /** Indicate the aggregation end. */
            boolean end;
            @Override
            public void invoke(Option<T> value) {
                if (value != Option.none() && !end) {
                    result.set(aggregator.invoke(result.get(), value.value()));
                } else {
                    end = true;
                }
            }
        });
        return result.get();
    }
    /**
     * Convert the Observable instance into a functional-observable object.
     * @param <T> the type of the elements
     * @param source the source observable
     * @return the action to action to option of T
     */
    @Nonnull
    public static <T> Action1<Action1<Option<T>>> asFObservable(
            @Nonnull final Observable<? extends T> source) {
        return new Action1<Action1<Option<T>>>() {
            @Override
            public void invoke(final Action1<Option<T>> o) {
                source.register(asObserver(o));
            }
        };
    }
    /**
     * Convert the functional observable into a normal observable object.
     * @param <T> the type of the elements to observe.
     * @param source the source of the functional-observable elements
     * @return the observable object
     */
    @Nonnull
    public static <T> Observable<T> asObservable(
            @Nonnull final Action1<Action1<Option<T>>> source) {
        return Observables.createWithAction(new Func1<Observer<? super T>, Action0>() {
            @Override
            public Action0 invoke(final Observer<? super T> o) {
                source.invoke(asAction(o));
                return Actions.noAction0();
            }
        });
    }
    /**
     * Transform the given action to an observer.
     * The wrapper observer converts its next() messages to Option.some(),
     * the finish() to Option.none() and error() to Option.error().
     * @param <T> the element type to observe
     * @param action the action to wrap
     * @return the observer
     */
    @Nonnull
    public static <T> Observer<T> asObserver(
            @Nonnull final Action1<? super Option<T>> action) {
        return new Observer<T>() {
            @Override
            public void error(@Nonnull Throwable ex) {
                action.invoke(Option.<T>error(ex));
            }

            @Override
            public void finish() {
                action.invoke(Option.<T>none());
            }

            @Override
            public void next(T value) {
                action.invoke(Option.some(value));
            }

        };
    }
    /**
     * Wraps the given observer into an action object which then dispatches
     * various incoming Option values to next(), finish() and error().
     * @param <T> the element type
     * @param observer the observer to wrap
     * @return the wrapper action
     */
    @Nonnull
    public static <T> Action1<Option<T>> asAction(
            @Nonnull final Observer<? super T> observer) {
        return new Action1<Option<T>>() {
            @Override
            public void invoke(Option<T> value) {
                Reactive.dispatch(observer, value);
            }
        };
    }
}

