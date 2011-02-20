/*
 * Copyright 2011 David Karnok
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
package hu.akarnokd.reactiv4java;

/**
 * An observable which provides a key as its group identity.
 * @author akarnokd, 2011.01.29.
 * @param <Key> the key type
 * @param <Value> the value type
 */
public interface GroupedObservable<Key, Value> extends Observable<Value> {
	/** @return the key of this group. */
	Key key();
}