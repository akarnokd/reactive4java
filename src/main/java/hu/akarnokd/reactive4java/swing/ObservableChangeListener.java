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
package hu.akarnokd.reactive4java.swing;

import hu.akarnokd.reactive4java.util.DefaultObservable;

import javax.annotation.Nonnull;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * The observable change listener which relays the stateChanged() calls to next() calls.
 * This observable never signals finish() or error().
 * @author akarnokd, 2011.02.01.
 */
public class ObservableChangeListener extends DefaultObservable<ChangeEvent> implements ChangeListener {
	@Override
	public void stateChanged(ChangeEvent e) {
		next(e);
	}
	/**
	 * Convenience method to create a new observable action listener and register it with the target component must have a public <code>addActionListener(ActionListener)</code> method.
	 * @param component the target component
	 * @return the new observable
	 */
	@Nonnull 
	public static ObservableChangeListener register(@Nonnull Object component) {
		return new ObservableChangeListener().registerWith(component);
	}
	/**
	 * Convenience method to register this observable with the target component which must have a public <code>addChangeListener(ChangeListener)</code> method. 
	 * @param component the target component
	 * @return this
	 */
	@Nonnull 
	public ObservableChangeListener registerWith(@Nonnull Object component) {
		return SwingObservables.invoke(component, "add", ChangeListener.class, this);
	}
	/**
	 * Convenience method to unregister this observable from the target component which must have a public <code>removeChangeListener(ChangeListener)</code> method. 
	 * @param component the target component
	 * @return this
	 */
	@Nonnull 
	public ObservableChangeListener unregisterFrom(@Nonnull Object component) {
		return SwingObservables.invoke(component, "remove", ChangeListener.class, this);
	}
}
