/*
 * Copyright 2011 David Karnok
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

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.annotation.Nonnull;

/**
 * The observable mouse listener which relays all mouse related events (movement, clicks, wheel) to next().
 * events and relays it to next(). The original type can be determined by using <code>MouseEvent.getID()</code>
 * which can be one of the following value: 
 * <ul>
 * <li>MouseListener events
 * <ul>
 * <li><code>MouseEvent.MOUSE_CLICKED</code></li>
 * <li><code>MouseEvent.MOUSE_PRESSED</code></li>
 * <li><code>MouseEvent.MOUSE_RELEASED</code></li>
 * <li><code>MouseEvent.MOUSE_ENTERED</code></li>
 * <li><code>MouseEvent.MOUSE_EXITED</code></li>
 * </ul>
 * </li>
 * <li>MouseMotionListener events
 * <ul>
 * <li><code>MouseEvent.MOUSE_MOVED</code></li>
 * <li><code>MouseEvent.MOUSE_DRAGGED</code></li>
 * </ul>
 * </li>
 * <li>MouseWheelListener events
 * <ul>
 * <li><code>MouseEvent.MOUSE_WHEEL</code></li>
 * </ul>
 * </li>
 * </ul>
 * This observable never signals finish() or error().
 * @author akarnokd, 2011.02.01.
 */
public class ObservableMouseListener extends DefaultObservable<MouseEvent> implements MouseListener, MouseMotionListener, MouseWheelListener {
	@Override
	public void mouseClicked(MouseEvent e) {
		next(e);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		next(e);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		next(e);
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		next(e);
	}

	@Override
	public void mouseExited(MouseEvent e) {
		next(e);
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		next(e);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		next(e);
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		next(e);
	}
	/**
	 * Convenience method to register an observer on the component for all mouse event types.
	 * To unregister, use the <code>Component.removeMouseListener()</code>, <code>Component.removeMouseMotionListener()</code> and
	 * <code>Component.removeMouseWheelListener()</code> methods. 
	 * @param component the target component.
	 * @return the new observable
	 */
	@Nonnull 
	public static ObservableMouseListener register(@Nonnull Component component) {
		return new ObservableMouseListener().registerWith(component);
	}
	/**
	 * Convenience method to register this observer with the target component for all mouse event types.
	 * To deregister this observable, use the <code>unregisterFrom()</code>.
	 * @param component the target component
	 * @return this
	 */
	@Nonnull 
	public ObservableMouseListener registerWith(@Nonnull Component component) {
		return registerWith(component, true, true, true);
	}
	/**
	 * Convenience method to register this observer with the target component for any mouse event types.
	 * To deregister this observable, use the <code>unregisterFrom()</code>.
	 * @param component the target component
	 * @param normal register for normal events?
	 * @param motion register for motion events?
	 * @param wheel register for wheel events?
	 * @return this
	 */
	@Nonnull 
	public ObservableMouseListener registerWith(
			@Nonnull Component component, 
			boolean normal, 
			boolean motion, 
			boolean wheel) {
		if (component == null) {
			throw new IllegalArgumentException("component is null");
		}
		if (normal) {
			component.addMouseListener(this);
		}
		if (motion) {
			component.addMouseMotionListener(this);
		}
		if (wheel) {
			component.addMouseWheelListener(this);
		}
		return this;
	}
	/**
	 * Unregister all mouse events from the given component.
	 * @param component the target component
	 * @return this
	 */
	@Nonnull 
	public ObservableMouseListener unregisterFrom(@Nonnull Component component) {
		if (component == null) {
			throw new IllegalArgumentException("component is null");
		}
		component.removeMouseListener(this);
		component.removeMouseMotionListener(this);
		component.removeMouseWheelListener(this);
		return this;
	}
}
