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

package hu.akarnokd.reactive4java8.observables;

import java.util.concurrent.locks.Lock;

/**
 * A default observable with a group key.
 * @author karnok
 * @param <T> the observed value type
 * @param <K> the group key type
 */
public class DefaultGroupedObservable<T, K> extends DefaultObservable<T>
implements GroupedObservable<T, K> {
    /** The key. */
    private K key;
    /**
     * Constructor with key.
     * @param key
     */
    public DefaultGroupedObservable(K key) {
        super();
        this.key = key;
    }
    /**
     * Constructor with key and a shared lock.
     * @param key
     * @param sharedLock 
     */
    public DefaultGroupedObservable(K key, Lock sharedLock) {
        super(sharedLock);
        this.key = key;
    }

    @Override
    public K key() {
        return key;
    }
    
}
