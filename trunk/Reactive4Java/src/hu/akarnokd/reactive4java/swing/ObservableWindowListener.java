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

import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;

import javax.annotation.Nonnull;

/**
 * The observable window listener which relays events from <code>WindowListener</code>, <code>WindowFocusListener</code> and <code>WindowStateListener</code>.
 * The <code>WindowEvent.getID()</code> contains the original event type:
 * <ul>
 * <li>WindowListener events:
 * <ul>
 * <li>WindowEvent.WINDOW_OPENED</li>
 * <li>WindowEvent.WINDOW_CLOSING</li>
 * <li>WindowEvent.WINDOW_CLOSED</li>
 * <li>WindowEvent.WINDOW_ICONIFIED</li>
 * <li>WindowEvent.WINDOW_DEICONIFIED</li>
 * <li>WindowEvent.WINDOW_ACTIVATED</li>
 * <li>WindowEvent.WINDOW_DEACTIVATEd</li>
 * </ul>
 * </li>
 * <li>WindowFocusListener events:
 * <ul>
 * <li>WindowEvent.WINDOW_GAINED_FOCUS</li>
 * <li>WindowEvent.WINDOW_LOST_FOCUS</li>
 * </ul>
 * </li>
 * <li>WindowStateListener events:
 * <ul>
 * <li>WindowEvent.WINDOW_STATE_CHANGED</li>
 * </ul>
 * </li>
 * </ul>
 * This observable never signals finish() or error().
 * @author akarnokd, 2011.02.01.
 */
public class ObservableWindowListener extends DefaultObservable<WindowEvent> implements WindowListener, WindowFocusListener, WindowStateListener {

	@Override
	public void windowStateChanged(WindowEvent e) {
		next(e);
	}

	@Override
	public void windowGainedFocus(WindowEvent e) {
		next(e);
	}

	@Override
	public void windowLostFocus(WindowEvent e) {
		next(e);
	}

	@Override
	public void windowOpened(WindowEvent e) {
		next(e);
	}

	@Override
	public void windowClosing(WindowEvent e) {
		next(e);
	}

	@Override
	public void windowClosed(WindowEvent e) {
		next(e);
	}

	@Override
	public void windowIconified(WindowEvent e) {
		next(e);
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		next(e);
	}

	@Override
	public void windowActivated(WindowEvent e) {
		next(e);
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		next(e);
	}
	/**
	 * Convenience method to register an observer on the component for all window event types.
	 * To unregister, use the <code>Component.removeMouseListener()</code>, <code>Component.removeMouseMotionListener()</code> and
	 * <code>Component.removeMouseWheelListener()</code> methods. 
	 * @param component the target component.
	 * @return the new observable
	 */
	@Nonnull 
	public static ObservableWindowListener register(@Nonnull Window component) {
		return new ObservableWindowListener().registerWith(component);
	}
	/**
	 * Convenience method to register this observer with the target component for all window event types.
	 * To deregister this observable, use the <code>unregisterFrom()</code>.
	 * @param component the target component
	 * @return this
	 */
	@Nonnull 
	public ObservableWindowListener registerWith(@Nonnull Window component) {
		return registerWith(component, true, true, true);
	}
	/**
	 * Convenience method to register this observer with the target component for any window event types.
	 * To deregister this observable, use the <code>unregisterFrom()</code>.
	 * @param component the target component
	 * @param normal register for normal events?
	 * @param focus register for focus events?
	 * @param state register for state events?
	 * @return this
	 */
	@Nonnull 
	public ObservableWindowListener registerWith(
			@Nonnull Window component, 
			boolean normal, 
			boolean focus, 
			boolean state) {
		if (component == null) {
			throw new IllegalArgumentException("component is null");
		}
		if (normal) {
			component.addWindowListener(this);
		}
		if (focus) {
			component.addWindowFocusListener(this);
		}
		if (state) {
			component.addWindowStateListener(this);
		}
		return this;
	}
	/**
	 * Unregister all window events from the given component.
	 * @param component the target component
	 * @return this
	 */
	@Nonnull 
	public ObservableWindowListener unregisterFrom(@Nonnull Window component) {
		if (component == null) {
			throw new IllegalArgumentException("component is null");
		}
		component.removeWindowListener(this);
		component.removeWindowFocusListener(this);
		component.removeWindowStateListener(this);
		return this;
	}
}
