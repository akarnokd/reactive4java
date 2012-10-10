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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.annotation.Nonnull;

/**
 * The observable action listener which relays the actionPerformed() calls to next() calls.
 * This observable never signals finish() or error().
 * @author akarnokd, 2011.02.01.
 */
public class ObservableActionListener extends DefaultObservable<ActionEvent> implements ActionListener {
	@Override
	public void actionPerformed(ActionEvent e) {
		next(e);
	}
	/**
	 * Convenience method to create a new observable action listener and register it with the target component must have a public <code>addActionListener(ActionListener)</code> method.
	 * @param component the target component
	 * @return the new observable
	 */
	@Nonnull 
	public static ObservableActionListener register(@Nonnull Object component) {
		return new ObservableActionListener().registerWith(component);
	}
	/**
	 * Convenience method to register this observable with the target component which must have a public <code>addActionListener(ActionListener)</code> method. 
	 * @param component the target component
	 * @return this
	 */
	@Nonnull 
	public ObservableActionListener registerWith(@Nonnull Object component) {
		return SwingObservables.invoke(component, "add", ActionListener.class, this);
	}
	/**
	 * Convenience method to unregister this observable from the target component which must have a public <code>removeActionListener(ActionListener)</code> method. 
	 * @param component the target component
	 * @return this
	 */
	@Nonnull 
	public ObservableActionListener unregisterFrom(@Nonnull Object component) {
		return SwingObservables.invoke(component, "remove", ActionListener.class, this);
	}
}
