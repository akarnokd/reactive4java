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

import hu.akarnokd.reactive4java8.registrations.CompositeRegistration;
import hu.akarnokd.reactive4java8.registrations.SingleRegistration;
import hu.akarnokd.reactive4java8.Registration;
import hu.akarnokd.reactive4java8.Observer;
import hu.akarnokd.reactive4java8.Observable;
import hu.akarnokd.reactive4java8.util.LockSync;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Observer implementation that supplies a management
 * callback to the lambda functions of the event methods.
 * @author akarnokd, 2013.11.16.
 */
public class SimpleObserver<T> implements Observer<T>, Registration {
    /** The next event handler. */
    private final BiConsumer<? super T, ? super Registration> onNext;
    /** The error event handler. */
    private final BiConsumer<? super Throwable, ? super Registration> onError;
    /** The finish event handler. */
    private final Consumer<? super Registration> onFinish;
    /** The uplink registration. */
    private final Registration reg;
    /** The synchronization object. */
    private final LockSync ls;
    /** The completion indicator. */
    private boolean done;
    /**
     * Constructor with uplink registration and the event handler functions.
     * @param reg
     * @param onNext
     * @param onError
     * @param onFinish 
     */
    public SimpleObserver(
        Registration reg,
        BiConsumer<? super T, ? super Registration> onNext,
        BiConsumer<? super Throwable, ? super Registration> onError,
        Consumer<? super Registration> onFinish
    ) {
        this(new ReentrantLock(), reg, onNext, onError, onFinish);
    }
    /**
     * Constructor with event handlers, no uplink registration
     * and non-fair reentrant lock.
     * @param onNext
     * @param onError
     * @param onFinish 
     */
    public SimpleObserver(
        BiConsumer<? super T, ? super Registration> onNext,
        BiConsumer<? super Throwable, ? super Registration> onError,
        Consumer<? super Registration> onFinish
    ) {
        this(new ReentrantLock(), Registration.EMPTY, onNext, onError, onFinish);
    }
    /**
     * Constructor with custom lock, uplink registration and event handlers.
     * @param sharedLock
     * @param reg
     * @param onNext
     * @param onError
     * @param onFinish 
     */
    public SimpleObserver(
        Lock sharedLock,
        Registration reg,
        BiConsumer<? super T, ? super Registration> onNext,
        BiConsumer<? super Throwable, ? super Registration> onError,
        Consumer<? super Registration> onFinish
    ) {
        this.reg = Objects.requireNonNull(reg);
        this.onNext = Objects.requireNonNull(onNext);
        this.onError = Objects.requireNonNull(onError);
        this.onFinish = Objects.requireNonNull(onFinish);
        this.ls = new LockSync(Objects.requireNonNull(sharedLock));
    }
    @Override
    public void next(T value) {
        boolean d = ls.sync(() -> {
            if (!done) {
                try {
                    onNext.accept(value, (Registration)this::setDone);
                } catch (Throwable t) {
                    done = true;
                    onError.accept(t, (Registration)this::setDone);
                }
                return done;
            }
            return false;
        });
        if (d) {
            reg.close();
        }
    }
    
    @Override
    public void error(Throwable t) {
        boolean d = ls.sync(() -> {
            if (!done) {
                onError.accept(t, (Registration)this::setDone);
                return done;
            }
            return false;
        });
        if (d) {
            reg.close();
        }
    }
    
    @Override
    public void finish() {
        boolean d = ls.sync(() -> {
            if (!done) {
                onFinish.accept((Registration)this::setDone);
                return done;
            }
            return false;
        });
        if (d) {
            reg.close();
        }
    }
    /** Sets the done flag, should be called while in {@code ls.sync()}.*/
    protected void setDone() {
        done = true;
    }
    
    @Override
    public void close() {
        ls.sync(() -> {
            done = true;
        });
        reg.close();
    }

    @Override
    public Observer<T> toThreadSafe() {
        return this;
    }
    
    /**
     * Builder to set various callback functions on the observer.
     * @param <T> 
     */
    public static class Builder<T> {
        private BiConsumer<? super T, ? super Registration> onNext = (t, r) -> { };
        private BiConsumer<? super Throwable, ? super Registration> onError = (e, r) -> { };
        private Consumer<? super Registration> onFinish = (r) -> { };
        private Registration reg = Registration.EMPTY;
        private Lock lock = new ReentrantLock();
        /**
         * Set the shared lock.
         * @param sharedLock 
         * @return this
         */
        public Builder<T> lock(Lock sharedLock) {
            this.lock = Objects.requireNonNull(sharedLock);
            return this;
        }
        /**
         * Set the uplink regisration
         * @param reg 
         * @return this
         */
        public Builder<T> registration(Registration reg) {
            this.reg = Objects.requireNonNull(reg);
            return this;
        }
        /**
         * Set the consumer with the value and registration to terminate
         * the observer.
         * @param nextFunc 
         * @return this
         */
        public Builder<T> next(BiConsumer<? super T, ? super Registration> nextFunc) {
            this.onNext = Objects.requireNonNull(nextFunc);
            return this;
        }
        /**
         * Set the value consumer with only the value.
         * @param nextFunc 
         * @return this
         */
        public Builder<T> next(Consumer<? super T> nextFunc) {
            Objects.requireNonNull(nextFunc);
            this.onNext = (t, r) -> nextFunc.accept(t);
            return this;
        }
        /**
         * Set a value consumer which ignores the value.
         * @param nextFunc 
         * @return this
         */
        public Builder<T> next(Runnable nextFunc) {
            Objects.requireNonNull(nextFunc);
            this.onNext = (t, r) -> nextFunc.run();
            return this;
        }
        /**
         * Set the error consumer with the registration to
         * terminate the observer.
         * @param errorFunc 
         * @return this
         */
        public Builder<T> error(BiConsumer<? super Throwable, ? super Registration> errorFunc) {
            this.onError = Objects.requireNonNull(errorFunc);
            return this;
        }
        /**
         * Sets the error function with only the exception value,
         * and the default behavior of terminating the observer.
         * @param errorFunc 
         * @return this
         */
        public Builder<T> error(Consumer<? super Throwable> errorFunc) {
            Objects.requireNonNull(errorFunc);
            this.onError = (e, r) -> { r.close(); errorFunc.accept(e); };
            return this;
        }
        /**
         * Sets the error function ignoring the exception and
         * the default behavior of terminating the observer.
         * @param errorFunc 
         * @return this
         */
        public Builder<T> error(Runnable errorFunc) {
            Objects.requireNonNull(errorFunc);
            this.onError = (e, r) -> { r.close(); errorFunc.run(); };
            return this;
        }
        /**
         * Sets the completion function with the registration to
         * terminate this observer.
         * @param finishFunc 
         * @return this
         */
        public Builder<T> finish(Consumer<? super Registration> finishFunc) {
            this.onFinish = Objects.requireNonNull(finishFunc);
            return this;
        }
        /**
         * Sets the finish function with the default 
         * behavior of terminating the observer.
         * @param finishFunc 
         * @return this
         */
        public Builder<T> finish(Runnable finishFunc) {
            Objects.requireNonNull(finishFunc);
            this.onFinish = (r) -> { r.close(); finishFunc.run(); };
            return this;
        }
        /**
         * Create a simple observer based on the settings so far.
         * @return the created simple observer
         */
        public SimpleObserver<T> create() {
            return new SimpleObserver<>(lock, reg, onNext, onError, onFinish);
        }
        /**
         * Creates and registers the built observer with the given target
         * observable.
         * @param obs
         * @return 
         */
        public Registration createAndRegister(Observable<? extends T> obs) {
            Objects.requireNonNull(obs);
            
            if (reg == Registration.EMPTY) {
                SingleRegistration sreg = new SingleRegistration();
                SimpleObserver<T> o = new SimpleObserver<>(lock, sreg, onNext, onError, onFinish);

                sreg.set(obs.registerSafe(o));

                return sreg;
            }
            CompositeRegistration creg = new CompositeRegistration();
            creg.add(reg);

            SimpleObserver<T> o = new SimpleObserver<>(lock, creg, onNext, onError, onFinish);
            
            creg.add(obs.registerSafe(o));
            
            return creg;
        }
    }
    /**
     * Constructs a builder for a simple observer.
     * @param <T>
     * @return 
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }
}
