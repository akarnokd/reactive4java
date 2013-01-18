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

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.annotation.Nonnull;

/**
 * The observable key listener which collects the <code>keyTyped()</code>, <code>keyPressed()</code>, <code>keyReleased()</code>
 * events and relays it to next(). The original type can be determined by using <code>KeyEvent.getType()</code>.
 * This observable never signals finish() or error().
 * @author akarnokd, 2011.02.01.
 */
public class ObservableKeyListener extends DefaultObservable<KeyEvent> implements KeyListener {

	@Override
	public void keyTyped(KeyEvent e) {
		next(e);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		next(e);
	}

	@Override
	public void keyReleased(KeyEvent e) {
		next(e);
	}
	/**
	 * Convenience method to register an action listener on an object which should have an <code>addKeyListener()</code>
	 * public method. It uses reflection to detemine the method's existence. Throws IllegalArgumentException if the
	 * component is null or does not have the required method.
	 * @param component the target component.
	 * @return the observable action listener
	 */
	@Nonnull 
	public static ObservableKeyListener register(@Nonnull Object component) {
		return new ObservableKeyListener().registerWith(component);
	}
	/**
	 * Convenience method to register this observable with the target component which must have a public <code>addKeyListener(KeyListener)</code> method. 
	 * @param component the target component
	 * @return this
	 */
	@Nonnull 
	public ObservableKeyListener registerWith(@Nonnull Object component) {
		return SwingObservables.invoke(component, "add", KeyListener.class, this);
	}
	/**
	 * Convenience method to unregister this observable from the target component which must have a public <code>removeKeyListener(KeyListener)</code> method. 
	 * @param component the target component
	 * @return this
	 */
	@Nonnull 
	public ObservableKeyListener unregisterFrom(@Nonnull Object component) {
		return SwingObservables.invoke(component, "remove", KeyListener.class, this);
	}
}
