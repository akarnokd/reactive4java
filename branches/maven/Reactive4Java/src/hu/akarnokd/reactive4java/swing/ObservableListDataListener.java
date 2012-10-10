/*
 * Copyright 2011-2012 David Karnok
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
package hu.akarnokd.reactive4java.swing;

import hu.akarnokd.reactive4java.reactive.DefaultObservable;

import javax.annotation.Nonnull;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * The observable list data listener which relays the intervalAdded(), intervalRemoved() and contentsChanged() events
 * to next() calls. The original event type can be determined from the ListDataEvent.getType().
 * This observable never signals finish() or error().
 * @author akarnokd, 2011.02.01.
 */
public class ObservableListDataListener extends DefaultObservable<ListDataEvent> implements ListDataListener {
	@Override
	public void intervalAdded(ListDataEvent e) {
		next(e);
	}

	@Override
	public void intervalRemoved(ListDataEvent e) {
		next(e);
	}

	@Override
	public void contentsChanged(ListDataEvent e) {
		next(e);
	}
	/**
	 * Convenience method to create a new observable action listener and register it with the target component must have a public <code>addListDataListener(ListDataListener)</code> method.
	 * @param component the target component
	 * @return the new observable
	 */
	@Nonnull 
	public static ObservableListDataListener register(@Nonnull Object component) {
		return new ObservableListDataListener().registerWith(component);
	}
	/**
	 * Convenience method to register this observable with the target component which must have a public <code>addListDataListener(ListDataListener)</code> method. 
	 * @param component the target component
	 * @return this
	 */
	@Nonnull 
	public ObservableListDataListener registerWith(@Nonnull Object component) {
		return SwingObservables.invoke(component, "add", ListDataListener.class, this);
	}
	/**
	 * Convenience method to unregister this observable from the target component which must have a public <code>removeListDataListener(ListDataListener)</code> method. 
	 * @param component the target component
	 * @return this
	 */
	@Nonnull 
	public ObservableListDataListener unregisterFrom(@Nonnull Object component) {
		return SwingObservables.invoke(component, "remove", ListDataListener.class, this);
	}
}
