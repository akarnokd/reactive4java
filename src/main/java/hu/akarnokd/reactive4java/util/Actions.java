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

package hu.akarnokd.reactive4java.util;

import hu.akarnokd.reactive4java.base.Action0;
import hu.akarnokd.reactive4java.base.Action0E;
import hu.akarnokd.reactive4java.base.Action1;
import hu.akarnokd.reactive4java.base.Action1E;
import hu.akarnokd.reactive4java.base.Action2;
import hu.akarnokd.reactive4java.base.Action2E;
import hu.akarnokd.reactive4java.base.Func0;
import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Func2;

import java.io.Closeable;
import java.io.IOException;

import javax.annotation.Nonnull;

/**
 * Helper class for Action interfaces.
 * @author akarnokd
 *
 */
public final class Actions {

    /** A helper action with one parameter which does nothing. */
    @Nonnull
    private static final Action1<Void> NO_ACTION_1 = new Action1<Void>() {
        @Override
        public void invoke(Void value) {
            
        }
    };
    /** A helper action without parameters which does nothing. */
    @Nonnull
    private static final Action0 NO_ACTION_0 = new Action0() {
        @Override
        public void invoke() {
            
        }
    };
    /** Empty action. */
    @Nonnull
    private static final Action2<Void, Void> NO_ACTION_2 = new Action2<Void, Void>() {
        @Override
        public void invoke(Void t, Void u) { }
    };
    /**
     * Wrap the supplied no-parameter function into an action.
     * The function's return value is ignored.
     * @param <T> the return type of the function, irrelevant
     * @param run the original runnable
     * @return the Action0 wrapping the runnable
     */
    @Nonnull 
    public static <T> Action0 asAction0(
            @Nonnull final Func0<T> run) {
        return new Action0() {
            @Override
            public void invoke() {
                run.invoke();
            }
        };
    }
    /**
     * Wrap the supplied runnable into an action.
     * @param run the original runnable
     * @return the Action0 wrapping the runnable
     */
    @Nonnull 
    public static Action0 asAction0(
            @Nonnull final Runnable run) {
        return new Action0() {
            @Override
            public void invoke() {
                run.run();
            }
        };
    }
    /**
     * Wrap the supplied one-parameter function into an action.
     * The function's return value is ignored.
     * @param <T> the parameter type
     * @param <U> the return type, irrelevant
     * @param run the original runnable
     * @return the Action0 wrapping the runnable
     */
    @Nonnull 
    public static <T, U> Action1<T> asAction1(
            @Nonnull final Func1<T, U> run) {
        return new Action1<T>() {
            @Override
            public void invoke(T param) {
                run.invoke(param);
            }
        };
    }
    /**
     * Wrap the parameterless action into an Action1.
     * The function's return value is ignored.
     * @param <T> the parameter type
     * @param run the original action
     * @return the Action0 wrapping the runnable
     */
    @Nonnull 
    public static <T> Action1<T> asAction1(
            @Nonnull final Action0 run) {
        return new Action1<T>() {
            @Override
            public void invoke(T param) {
                run.invoke();
            }
        };
    }
    /**
     * Wrap the parameterless action into an Action2.
     * The function's return value is ignored.
     * @param <T> the first parameter type
     * @param <U> the second parameter type
     * @param run the original action
     * @return the Action0 wrapping the runnable
     */
    @Nonnull 
    public static <T, U> Action2<T, U> asAction2(
            @Nonnull final Action0 run) {
        return new Action2<T, U>() {
            @Override
            public void invoke(T param1, U param2) {
                run.invoke();
            }
        };
    }
    /**
     * Wrap the supplied runnable into an action.
     * @param <T> the parameter type
     * @param run the original runnable
     * @return the Action0 wrapping the runnable
     */
    @Nonnull 
    public static <T> Action1<T> asAction1(
            @Nonnull final Runnable run) {
        return new Action1<T>() {
            @Override
            public void invoke(T param) {
                run.run();
            }
        };
    }
    /**
     * Wrap the given action into a runnable instance.
     * @param action the target action
     * @return the wrapper runnable
     */
    @Nonnull 
    public static Runnable asRunnable(
            @Nonnull final Action0 action) {
        return new Runnable() {
            @Override
            public void run() {
                action.invoke();
            }
        };
    }
    /** 
     * @return returns an empty action which does nothing. 
     */
    @Nonnull 
    public static Action0 noAction0() {
        return NO_ACTION_0;
    }
    /**
     * Returns an action which does nothing with its parameter.
     * @param <T> the type of the parameter (irrelevant)
     * @return the action
     * @since 0.96
     */
    @SuppressWarnings("unchecked")
    @Nonnull 
    public static <T> Action1<T> noAction1() {
        return (Action1<T>)NO_ACTION_1;
    }
    /**
     * Returns an action which does nothing with its parameter.
     * @param <T> the type of the first parameter (irrelevant)
     * @param <U> the type of the second parameter (irrelevant)
     * @return the action
     * @since 0.96
     */
    @SuppressWarnings("unchecked")
    @Nonnull 
    public static <T, U> Action2<T, U> noAction2() {
        return (Action2<T, U>)NO_ACTION_2;
    }
    /**
     * Returns a composite two parameter action from the supplied two actions
     * which will be invoked for each of the parameters.
     * @param first the first action reacting to the first parameter
     * @param second the second action reacting to the second parameter
     * @param <T> the first parameter type
     * @param <U> the second parameter type
     * @return the action composite
     * @since 0.96
     */
    @Nonnull
    public static <T, U> Action2<T, U> dualAction(
            @Nonnull final Action1<? super T> first, 
            @Nonnull final Action1<? super U> second) {
        return new Action2<T, U>() {
            @Override
            public void invoke(T t, U u) {
                first.invoke(t);
                second.invoke(u);
            }
        };
    }
    /**
     * Returns a composite two parameter action from the supplied two actions
     * which will be invoked for each of the parameters; and allows throwing an exception.
     * @param first the first action reacting to the first parameter
     * @param second the second action reacting to the second parameter
     * @param <T> the first parameter type
     * @param <U> the second parameter type
     * @param <E> the exception type
     * @return the action composite
     * @since 0.96
     */
    @Nonnull
    public static <T, U, E extends Exception> Action2E<T, U, E> dualAction(
            @Nonnull final Action1E<? super T, ? extends E> first, 
            @Nonnull final Action1E<? super U, ? extends E> second) {
        return new Action2E<T, U, E>() {
            @Override
            public void invoke(T t, U u) throws E {
                first.invoke(t);
                second.invoke(u);
            }
        };
    }
    /**
     * Wrap the given exception-less action.
     * @param action the action to wrap
     * @param <E> the exception type
     * @return the action with exception
     * @since 0.96
     */
    @Nonnull 
    public static <E extends Exception> Action0E<E> asAction0E(
            @Nonnull final Action0 action) {
        return new Action0E<E>() {
            @Override
            public void invoke() throws E {
                action.invoke();
            }
        };
    }
    /**
     * Wrap the given exception-less action.
     * @param action the action to wrap
     * @param <T> the parameter type
     * @param <E> the exception type
     * @return the action with exception
     * @since 0.96
     */
    @Nonnull 
    public static <T, E extends Exception> Action1E<T, E> asAction1E(
            @Nonnull final Action1<? super T> action) {
        return new Action1E<T, E>() {
            @Override
            public void invoke(T t) throws E {
                action.invoke(t);
            }
        };
    }
    /**
     * Wrap the given exception-less action.
     * @param action the action to wrap
     * @param <T> the first parameter type
     * @param <U> the second parameter type
     * @param <E> the exception type
     * @return the action with exception
     * @since 0.96
     */
    @Nonnull 
    public static <T, U, E extends Exception> Action2E<T, U, E> asAction2E(
            @Nonnull final Action2<? super T, ? super U> action) {
        return new Action2E<T, U, E>() {
            @Override
            public void invoke(T t, U u) throws E {
                action.invoke(t, u);
            }
        };
    }
    /**
     * Creates an action which will close the given closeable.
     * @param c the closeable
     * @return the action
     * @since 0.96
     */
    @Nonnull 
    public static Action0E<IOException> close(
            @Nonnull final Closeable c) {
        return new Action0E<IOException>() {
            @Override
            public void invoke() throws IOException {
                if (c != null) {
                    c.close();
                }
            }
        };
    }
    /**
     * Wraps the closeable into action and suppresses any close exception.
     * @param c the closeable
     * @return the action
     * @since 0.96
     */
    @Nonnull 
    public static Action0 closeSilently(
            @Nonnull final Closeable c) {
        return new Action0() {
            @Override
            public void invoke() {
                Closeables.closeSilently(c);
            }
        };
    }
    /**
     * Converts the given parameterless function into an action,
     * which when invoked, invokes the original function and ignores its result.
     * @param <T> the function's return type
     * @param func the function to wrap
     * @return the created action
     * @since 0.96
     */
    @Nonnull 
    public static <T> Action0 asAction(
            @Nonnull final Func0<T> func) {
        return new Action0() {
            @Override
            public void invoke() {
                func.invoke();
            }
        };
    }
    /**
     * Converts the given 1 parameter function into an action,
     * which when invoked, invokes the original function and ignores its result.
     * @param <T> the action/function parameter
     * @param <U> the function's return type
     * @param func the function to wrap
     * @return the created action
     * @since 0.96
     */
    @Nonnull 
    public static <T, U> Action1<T> asAction(
            @Nonnull final Func1<? super T, U> func) {
        return new Action1<T>() {
            @Override
            public void invoke(T value) {
                func.invoke(value);
            }
        };
    }
    /**
     * Converts the given 2 parameter function into an action,
     * which when invoked, invokes the original function and ignores its result.
     * @param <T> the first action/function parameter
     * @param <U> the second action/function parameter
     * @param <V> the function's return type
     * @param func the function to wrap
     * @return the created action
     * @since 0.96
     */
    @Nonnull 
    public static <T, U, V> Action2<T, U> asAction(
            @Nonnull final Func2<? super T, ? super U, V> func) {
        return new Action2<T, U>() {
            @Override
            public void invoke(T value1, U value2) {
                func.invoke(value1, value2);
            }
        };
    }
    /**
     * Wraps the given closeable instance into an Action0E.
     * @param closeable the closeable to call
     * @return the Action0E created
     */
    @Nonnull 
    public static Action0E<IOException> asAction0E(
            @Nonnull final Closeable closeable) {
        return new Action0E<IOException>() {
            @Override
            public void invoke() throws IOException {
                closeable.close();
            }
        };
    }
    /** Utility class. */
    private Actions() {
        
    }
}
