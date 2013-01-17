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
/*
 * Copyright (C) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.util.concurrent;

import java.util.Map;

/**
 * Minimal GWT emulation of a map providing atomic operations.
 * <p>Style fixes.</p>
 * @author Jesse Wilson
 * @author David Karnok
 * @param <K> the key type
 * @param <V> the value type
 */
public interface ConcurrentMap<K, V> extends Map<K, V> {
	/**
	 * Place the value in the map only if it is absent.
	 * @param key the key
	 * @param value the value
	 * @return the existing value if present
	 */
	V putIfAbsent(K key, V value);
	/**
	 * Remove a value from the map only if it is the
	 * expected value.
	 * @param key the key
	 * @param value the expected value
	 * @return true if the removal succeded
	 */
	boolean remove(Object key, Object value);
	/**
	 * Replace the given key with the given value.
	 * @param key the key
	 * @param value the value
	 * @return the old value
	 */
	V replace(K key, V value);
	/**
	 * Replace the value of the key only when it is
	 * the expected oldValue.
	 * @param key the key
	 * @param oldValue the value
	 * @param newValue the new value
	 * @return success indicator
	 */
	boolean replace(K key, V oldValue, V newValue);
}
