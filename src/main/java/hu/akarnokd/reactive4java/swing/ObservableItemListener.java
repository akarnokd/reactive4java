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

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.annotation.Nonnull;

/**
 * The observable item listener which relays events from <code>ItemListener</code>.
 * This observable never signals finish() or error().
 * @author akarnokd, 2011.02.01.
 */
public class ObservableItemListener extends DefaultObservable<ItemEvent> implements ItemListener {
    @Override
    public void itemStateChanged(ItemEvent e) {
        next(e);
    }

    /**
     * Convenience method to register an observer on the component for <code>itemStateChanged(ItemEvent)</code> events.
     * @param component the target component.
     * @return the new observable
     */
    @Nonnull 
    public static ObservableItemListener register(@Nonnull Object component) {
        return new ObservableItemListener().registerWith(component);
    }
    /**
     * Convenience method to register an observer on the component for <code>itemStateChanged(ItemEvent)</code> events.
     * @param component the target component.
     * @return the new observable
     */
    @Nonnull 
    public ObservableItemListener registerWith(@Nonnull Object component) {
        return SwingObservables.invoke(component, "add", ItemListener.class, this);
    }
    /**
     * Unregister this observer of list events from the given component.
     * @param component the target component
     * @return this
     */
    @Nonnull 
    public ObservableItemListener unregisterFrom(@Nonnull Object component) {
        return SwingObservables.invoke(component, "remove", ItemListener.class, this);
    }
}
