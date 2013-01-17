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
package java.util.concurrent;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * The GWT implementation of a linked blocking queue.
 * Basically a LinkedList backed queue.
 * @author akarnokd, 2011.02.19.
 * @param <T> the element type
 */
public class LinkedBlockingQueue<T> implements BlockingQueue<T> {
	/** The backing list. */
	final LinkedList<T> list = new LinkedList<T>();
	@Override
	public boolean add(T e) {
		return list.add(e);
	}

	@Override
	public boolean offer(T e) {
		return list.add(e);
	}

	@Override
	public T remove() {
		return list.removeFirst();
	}

	@Override
	public T poll() {
		if (list.size() > 0) {
			return list.removeFirst();
		}
		return null;
	}

	@Override
	public T element() {
		if (list.size() > 0) {
			return list.removeFirst();
		}
		throw new NoSuchElementException();
	}

	@Override
	public T peek() {
		if (list.size() > 0) {
			return list.getFirst();
		}
		return null;
	}

	@Override
	public int size() {
		return list.size();
	}

	@Override
	public boolean isEmpty() {
		return list.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return list.contains(o);
	}

	@Override
	public Iterator<T> iterator() {
		return list.iterator();
	}

	@Override
	public Object[] toArray() {
		return list.toArray();
	}

	@Override
	public <E> E[] toArray(E[] a) {
		return list.toArray(a);
	}

	@Override
	public boolean remove(Object o) {
		return list.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return list.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		return list.addAll(c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return list.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return list.retainAll(c);
	}

	@Override
	public void clear() {
		list.clear();
	}

	@Override
	public void drainTo(Collection<? super T> target) {
		target.addAll(list);
		list.clear();
	}

	@Override
	public T take() throws InterruptedException {
		return list.removeFirst();
	}

}
