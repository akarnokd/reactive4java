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

package hu.akarnokd.reactive4java8.observables;

import hu.akarnokd.reactive4java8.Subject;
import hu.akarnokd.reactive4java8.Observer;
import hu.akarnokd.reactive4java8.Registration;
import hu.akarnokd.reactive4java8.util.LockSync;
import hu.akarnokd.reactive4java8.util.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Default implementation of the observable interface which keeps
 * track of the registered observers. 
 * <p>The default
 * observable is an observer by itself, so basically it can be used
 * to dispatch values to registered observers.</p>
 * <p>By default, the observer is not strict, i.e., allows any sequence
 * of events; use the DefaultObserver(boolean) constructor to change this
 * behavior.</p>
 * <p>The observer registrations are remembered individually, i.e., registering
 * the same physical observer twice will yield notifications twice.
 * The observable remembers the registration order of the observers.</p>
 * @author akarnokd, 2013.11.09.
 */
public class DefaultObservable<T> implements Subject<T, T> {
    /** Indicate the completedness of the observer part. */
    protected boolean done;
    /** The lock based synchronizer. */
    protected final LockSync ls;
    /** The registered observers. Access it through the LockSync object. */
    protected final Map<Registration, Observer<? super T>> observers = new LinkedHashMap<>();
    /**
     * Constructor, works in non-strict mode.
     */
    public DefaultObservable() {
        this(new ReentrantLock());
    }
    /**
     * Constructor, works in non-strict mode.
     * @param sharedLock A shared lock.
     */
    public DefaultObservable(Lock sharedLock) {
        ls = new LockSync(sharedLock);
    }
    @Override
    public Registration register(Observer<? super T> observer) {
        Objects.requireNonNull(observer);
        Registration token = new Registration() {

            @Override
            public void close() {
                unregister(this);
            }
            
        };
        
        boolean isDone = ls.sync(() -> {
            if (!done) {
                observers.put(token, observer);
            }
            return done;
        });
        
        if (!isDone) {
            return token;
        }
        return Registration.EMPTY;
    }
    /** Remove the given registration. */
    private void unregister(Registration token) {
        ls.sync(() -> observers.remove(token));
    }
    /**
     * Returns a list of all registered observers and their registration.
     * @return 
     */
    public List<Pair<Registration, Observer<? super T>>> observers() {
        return ls.sync(() -> {
           if (done) {
               return Collections.emptyList();
           }
           List<Pair<Registration, Observer<? super T>>> list = new ArrayList<>();
           observers.forEach((k, v) -> list.add(Pair.of(k, v)));
           return Collections.unmodifiableList(list);
        });
    }
    @Override
    public void next(T value) {
        List<Registration> toRemove = new LinkedList<>();
        observers().forEach((e) -> {
            try {
                e.second.next(value);
            } catch (Throwable t) {
                toRemove.add(e.first);
                e.second.error(t);
            }
        });
        if (!toRemove.isEmpty()) {
            ls.sync(() -> observers.keySet().removeAll(toRemove));
        }
    }
    @Override
    public void error(Throwable t) {
        observers().forEach((e) -> {
            e.second.error(t);
        });
        clear();
    }
    @Override
    public void finish() {
        observers().forEach((e) -> {
            e.second.finish();
        });
        clear();
    }
    /** Removes all registered observers. */
    public void clear() {
        ls.sync(() -> observers.clear());
    }
}
