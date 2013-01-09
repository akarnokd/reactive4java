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

import hu.akarnokd.reactive4java.reactive.DefaultObservable;

import java.awt.Adjustable;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import javax.annotation.Nonnull;

/**
 * The observable adjustment listener which relays adjustmentValueChanged() events to next().
 * This observable never signals finish() or error().
 * @author akarnokd, 2011.02.01.
 */
public class ObservableAdjustmentListener extends DefaultObservable<AdjustmentEvent> implements AdjustmentListener {
	@Override
	public void adjustmentValueChanged(AdjustmentEvent e) {
		next(e);
	}
	/**
	 * Convenience method to create a new observable action listener and register it with the target component must have a public <code>addAdjustmentListener(AdjustmentListener)</code> method.
	 * @param component the target component
	 * @return the new observable
	 */
	@Nonnull 
	public static ObservableAdjustmentListener register(@Nonnull Object component) {
		return new ObservableAdjustmentListener().registerWith(component);
	}
	/**
	 * Convenience method to register this observable with the target component which must have a public <code>addAdjustmentListener(AdjustmentListener)</code> method. 
	 * @param component the target component
	 * @return this
	 */
	@Nonnull 
	public ObservableAdjustmentListener registerWith(@Nonnull Object component) {
		if (component instanceof Adjustable) {
			((Adjustable)component).addAdjustmentListener(this);
			return this;
		}
		return SwingObservables.invoke(component, "add", AdjustmentListener.class, this);
	}
	/**
	 * Convenience method to unregister this observable from the target component which must have a public <code>removeAdjustmentListener(AdjustmentListener)</code> method. 
	 * @param component the target component
	 * @return this
	 */
	@Nonnull 
	public ObservableAdjustmentListener unregisterFrom(@Nonnull Object component) {
		if (component instanceof Adjustable) {
			((Adjustable)component).removeAdjustmentListener(this);
			return this;
		}
		return SwingObservables.invoke(component, "remove", AdjustmentListener.class, this);
	}
}
