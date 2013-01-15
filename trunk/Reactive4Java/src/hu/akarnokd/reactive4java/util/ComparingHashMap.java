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

import hu.akarnokd.reactive4java.base.Func0;
import hu.akarnokd.reactive4java.base.Func2;

import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.annotation.Nonnull;

/**
 * A hash map which uses custom equality comparer on its items.
 * <p>Note since the new string hashing behavior is hidden
 * under sun.misc, this class implements an older hashing
 * code which might be exploited with well crafted strings.</p>
 * <p>The implementation is not thread safe.</p>
 * <p>Note that this is not a complete replacement for java.util.HashMap</p>
 * @author akarnokd, 2013.01.15.
 * @param <T> the key type
 * @param <U> the value type
 * @since 0.97
 */
public class ComparingHashMap<T, U> {
	/** The set entry. */
	protected static class Entry {
		/** The key. */
		Object key;
		/** The hashcode. */
		int hash;
		/** The value. */
		Object value;
		/** The next entry. */
		Entry next;
	}
	/** The comparer function. */
	protected final Func2<? super T, ? super T, Boolean> comparer;
	/** The contained entries. */
	protected Entry[] entries;
	/** The current element count. */
	protected int count;
	/** The load factor before the table is resized. */
	protected double loadFactor;
	/** The load count before the table is resized. */
	protected int threshold;
	/**
	 * Default constructor with capacity 16, load factor of 0.75 and nullsafe equals() comparer.
	 */
	public ComparingHashMap() {
		this(16, 0.75, Functions.equals());
	}
	/**
	 * Constructor with the capacity, load factor of 0.75 and nullsafe equals() comparer.
	 * @param capacity the initial capacity.
	 */
	public ComparingHashMap(int capacity) {
		this(16, 0.75, Functions.equals());
	}
	/**
	 * Constructor with capacity 16, load factor of 0.75 and the given comparer.
	 * @param comparer the comparer function.
	 */
	public ComparingHashMap(@Nonnull Func2<? super T, ? super T, Boolean> comparer) {
		this(16, 0.75, comparer);
	}
	/**
	 * Constructor with the given capacity, load factor of 0.75 and the given comparer.
	 * @param capacity the initial capacity.
	 * @param comparer the comparer function.
	 */
	public ComparingHashMap(int capacity, @Nonnull Func2<? super T, ? super T, Boolean> comparer) {
		this(capacity, 0.75, comparer);
	}
	/**
	 * Initializes the set with the given capacity, load factor
	 * and comparer.
	 * @param capacity the initial capacity, will be rounded up to the next power of
	 * 2, minimum 16
	 * @param loadFactor the load factor where after the underlying table is resized
	 * @param comparer the content comparer
	 */
	public ComparingHashMap(int capacity, double loadFactor, @Nonnull Func2<? super T, ? super T, Boolean> comparer) {
		int cap = 16;
		while (cap < capacity) {
			cap *= 2;
		}
		this.loadFactor = loadFactor;
		this.threshold = (int)(cap * loadFactor);
		this.entries = new Entry[cap];
		this.comparer = comparer;
	}
	/**
	 * Tries to add the given item to this set.
	 * @param key the key
	 * @param newValue the new value
	 * @return the old value or null if was not present
	 */
	@SuppressWarnings("unchecked")
	public U put(Object key, U newValue) {
		int h = hash(key);
		int idx = indexFor(h, entries.length);
		for (Entry e = entries[idx]; e != null; e = e.next) {
			if (e.hash == h && comparer.invoke((T)e.key, (T)key)) {
				Object o = e.value;
				e.value = newValue;
				return (U)o;
			}
		}
		addEntry(h, key, idx, newValue);
		return null;
	}
	/**
	 * Put a new value into the map if the given key is not present.
	 * @param key the key
	 * @param newValue the new value if the key is not present
	 * @return the existing value or the new value if it was added
	 */
	public U putIfAbsent(Object key, final U newValue) {
		return computeIfAbsent(key, Functions.constant0(newValue));
	}
	/**
	 * Compute and put a new value into the map if the given key is not present.
	 * @param key the key
	 * @param supplier the new value supplier if the map doesn't contain the key
	 * @return the existing value or the new value if it was added
	 */
	@SuppressWarnings("unchecked")
	public U computeIfAbsent(Object key, Func0<? extends U> supplier) {
		int h = hash(key);
		int idx = indexFor(h, entries.length);
		for (Entry e = entries[idx]; e != null; e = e.next) {
			if (e.hash == h && comparer.invoke((T)e.key, (T)key)) {
				return (U)e.value;
			}
		}
		U newValue = supplier.invoke();
		addEntry(h, key, idx, newValue);
		return newValue;
	}
	/**
	 * Adds a new entry to the set, resizing
	 * the container as necessary.
	 * @param hash the hash of the item
	 * @param item the item
	 * @param idx the index
	 * @param value the value
	 */
	protected void addEntry(int hash, Object item, int idx, Object value) {
		if (count >= threshold && entries[idx] != null) {
			resize(entries.length * 2);
			hash = item != null ? hash(item) : 0;
			idx = indexFor(hash, entries.length);
		}
		Entry e = entries[idx];
		
		Entry e2 = new Entry();
		e2.key = item;
		e2.hash = hash;
		e2.value = value;
		e2.next = e;
		
		entries[idx] = e2;
		count++;
	}
	/**
	 * Resize the container and remap all its entries.
	 * @param newSize the new size
	 */
	protected void resize(int newSize) {
		Entry[] oldEntries = entries;
		entries = new Entry[newSize];
		
		for (Entry e : oldEntries) {
			while (e != null) {
				Entry n = e.next;

				int idx = indexFor(e.hash, newSize);
				e.next = entries[idx];
				entries[idx] = e;
				
				e = n;
			}
		}
		
		threshold = (int)(newSize * loadFactor);
	}
	
	/**
	 * Compute the hash of the supplied object,
	 * similar to HashMap.hash function.
	 * @param k the object
	 * @return the hash value
	 */
    final int hash(Object k) {
    	if (k == null) {
    		return 0;
    	}
        int h = 0;
        

        h ^= k.hashCode();

        h ^= (h >>> 20) ^ (h >>> 12);
        return h ^ (h >>> 7) ^ (h >>> 4);
    }

    /**
     * Returns index for hash code h.
     * @param h the hash
     * @param length the total container length
     * @return the index
     */
    static int indexFor(int h, int length) {
        return h & (length - 1);
    }
    /** @return is this set empty? */
    public boolean isEmpty() {
    	return count == 0;
    }
    /** @return the set size. */
    public int size() {
    	return count;
    }
    /**
     * Retrieves the value associated with the key or null
     * if not present.
     * @param key the key to check
     * @return the value or null if not present
     */
    @SuppressWarnings("unchecked")
    public U get(Object key) {
		int h = hash(key);
		int idx = indexFor(h, entries.length);
		for (Entry e = entries[idx]; e != null; e = e.next) {
			if (e.hash == h && comparer.invoke((T)e.key, (T)key)) {
				return (U)e.value;
			}
		}
		return null;
    }
    /**
     * @return a sequence of contained elements.
     */
    public Iterable<U> values() {
    	return new Iterable<U>() {
    		@Override
    		public Iterator<U> iterator() {
    			return new Iterator<U>() {
    				/** The current index. */
    				int index = -1;
    				/** The current (sub) element. */
    				Entry e;
					@Override
					public boolean hasNext() {
						if (e == null && index < entries.length) {
							index++;
							for (; index < entries.length; index++) {
								e = entries[index];
								if (e != null) {
									break;
								}
							}
						}
						return e != null;
					}

					@Override
					@SuppressWarnings("unchecked")
					public U next() {
						if (hasNext()) {
							U v = (U)e.value;
							e = e.next;
							return v;
						}
						throw new NoSuchElementException();
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
    			};
    		}
    	};
    }
}
