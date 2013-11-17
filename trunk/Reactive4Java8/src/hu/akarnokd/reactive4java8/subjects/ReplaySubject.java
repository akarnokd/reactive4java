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

package hu.akarnokd.reactive4java8.subjects;

import hu.akarnokd.reactive4java8.util.LockSync;
import hu.akarnokd.reactive4java8.Observer;
import hu.akarnokd.reactive4java8.Registration;
import hu.akarnokd.reactive4java8.registrations.SelfRegistration;
import hu.akarnokd.reactive4java8.Subject;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Remembers and replays the values to subscribed observers.
 * @author akarnokd, 2013.11.16.
 */
public class ReplaySubject<T> implements Subject<T, T> {
    private final LockSync rws;
    private boolean done;
    private Throwable exception;
    private final List<T> values = new LinkedList<>();
    private final Map<Registration, Observer<? super T>> observers = new LinkedHashMap<>();
    public ReplaySubject() {
        this(new ReentrantLock());
    }
    public ReplaySubject(Lock sharedLock) {
        rws = new LockSync(sharedLock);
    }
    @Override
    public Registration register(Observer<? super T> observer) {
        Registration reg = rws.sync(() -> {
            if (done) {
                return null;
            }
            Registration oreg = new SelfRegistration(this::unregister);
            if (replay(observer)) {
                observers.put(oreg, observer);
                return oreg;
            }
            return Registration.EMPTY;
        });
        // completion indicated via null;
        if (reg == null) {
            replay(observer);
            reg = Registration.EMPTY;
        }
        return reg;
    }
    /** Unregisters an observer by the given registration. */
    private void unregister(Registration reg) {
        rws.sync(() -> observers.remove(reg));
    }
    /** Replays the accumulated events to the observer. */
    private boolean replay(Observer<? super T> observer) {
        for (T t : values) {
            try {
                observer.next(t);
            } catch (Throwable e) {
                observer.error(e);
                return false;
            }
        }
        if (exception != null) {
            observer.error(exception);
            return false;
        } else 
        if (done) {
            observer.finish();
            return false;
        }
        return true;
    }
    @Override
    public void next(T value) {
        rws.sync(() -> {
            if (!done) {
                values.add(value);
                Iterator<Observer<? super T>> it = observers.values().iterator();
                while (it.hasNext()) {
                    Observer<? super T> e = it.next();
                    try {
                        e.next(value);
                    } catch (Throwable t) {
                        it.remove();
                        e.error(t);
                    }
                }
            }
        });
    }

    @Override
    public void error(Throwable t) {
        rws.sync(() -> {
            if (!done) {
                done = true;
                exception = t;
                observers.values().forEach(e -> e.error(t));
                observers.clear();
            }
        });
    }

    @Override
    public void finish() {
        rws.sync(() -> {
            if (!done) {
                done = true;
                observers.values().forEach(Observer::finish);
                observers.clear();
            }
        });
    }

}
