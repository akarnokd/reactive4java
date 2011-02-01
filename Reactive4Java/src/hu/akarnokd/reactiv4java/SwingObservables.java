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
package hu.akarnokd.reactiv4java;

import java.awt.Adjustable;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Utility class to wrap typical Swing events into observables.
 * @author akarnokd, 2011.02.01.
 */
public final class SwingObservables {
	/** Utility class. */
	private SwingObservables() {
		// utility class
	}
	/**
	 * The observable action listener which relays the actionPerformed() calls to next() calls.
	 * This observable never signals finish() or error().
	 * @author akarnokd, 2011.02.01.
	 */
	public static class ObservableActionListener extends DefaultObservable<ActionEvent> implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			next(e);
		}
		/**
		 * Convenience method to create a new observable action listener and register it with the target component must have a public <code>addActionListener(ActionListener)</code> method.
		 * @param component the target component
		 * @return the new observable
		 */
		public static ObservableActionListener register(Object component) {
			return new ObservableActionListener().registerWith(component);
		}
		/**
		 * Convenience method to register this observable with the target component which must have a public <code>addActionListener(ActionListener)</code> method. 
		 * @param component the target component
		 * @return this
		 */
		public ObservableActionListener registerWith(Object component) {
			if (component == null) {
				throw new IllegalArgumentException("component is null");
			}
			try {
				Method m = component.getClass().getMethod("addActionListener", KeyListener.class);
				m.invoke(component, this);
			} catch (NoSuchMethodException ex) {
				throw new IllegalArgumentException(ex);
			} catch (IllegalAccessException ex) {
				throw new IllegalArgumentException(ex);
			} catch (InvocationTargetException ex) {
				throw new IllegalArgumentException(ex);
			}
			return this;
		}
		/**
		 * Convenience method to unregister this observable from the target component which must have a public <code>removeActionListener(ActionListener)</code> method. 
		 * @param component the target component
		 * @return this
		 */
		public ObservableActionListener unregisterFrom(Object component) {
			if (component == null) {
				throw new IllegalArgumentException("component is null");
			}
			try {
				Method m = component.getClass().getMethod("removeActionListener", KeyListener.class);
				m.invoke(component, this);
			} catch (NoSuchMethodException ex) {
				throw new IllegalArgumentException(ex);
			} catch (IllegalAccessException ex) {
				throw new IllegalArgumentException(ex);
			} catch (InvocationTargetException ex) {
				throw new IllegalArgumentException(ex);
			}
			return this;
		}
	}
	/**
	 * The observable change listener which relays the stateChanged() calls to next() calls.
	 * This observable never signals finish() or error().
	 * @author akarnokd, 2011.02.01.
	 */
	public static class ObservableChangeListener extends DefaultObservable<ChangeEvent> implements ChangeListener {
		@Override
		public void stateChanged(ChangeEvent e) {
			next(e);
		}
		/**
		 * Convenience method to create a new observable action listener and register it with the target component must have a public <code>addActionListener(ActionListener)</code> method.
		 * @param component the target component
		 * @return the new observable
		 */
		public static ObservableChangeListener register(Object component) {
			return new ObservableChangeListener().registerWith(component);
		}
		/**
		 * Convenience method to register this observable with the target component which must have a public <code>addChangeListener(ChangeListener)</code> method. 
		 * @param component the target component
		 * @return this
		 */
		public ObservableChangeListener registerWith(Object component) {
			if (component == null) {
				throw new IllegalArgumentException("component is null");
			}
			try {
				Method m = component.getClass().getMethod("addChangeListener", KeyListener.class);
				m.invoke(component, this);
			} catch (NoSuchMethodException ex) {
				throw new IllegalArgumentException(ex);
			} catch (IllegalAccessException ex) {
				throw new IllegalArgumentException(ex);
			} catch (InvocationTargetException ex) {
				throw new IllegalArgumentException(ex);
			}
			return this;
		}
		/**
		 * Convenience method to unregister this observable from the target component which must have a public <code>removeChangeListener(ChangeListener)</code> method. 
		 * @param component the target component
		 * @return this
		 */
		public ObservableChangeListener unregisterFrom(Object component) {
			if (component == null) {
				throw new IllegalArgumentException("component is null");
			}
			try {
				Method m = component.getClass().getMethod("removeChangeListener", KeyListener.class);
				m.invoke(component, this);
			} catch (NoSuchMethodException ex) {
				throw new IllegalArgumentException(ex);
			} catch (IllegalAccessException ex) {
				throw new IllegalArgumentException(ex);
			} catch (InvocationTargetException ex) {
				throw new IllegalArgumentException(ex);
			}
			return this;
		}
	}
	/**
	 * The observable document listener which relays the insertUpdate(), removeUpdate() and changedUpdate() events
	 * to next() calls. The original event type can be queried from the DocumentEvent.getType().
	 * This observable never signals finish() or error().
	 * @author akarnokd, 2011.02.01.
	 */
	public static class ObservableDocumentListener extends DefaultObservable<DocumentEvent> implements DocumentListener {

		@Override
		public void insertUpdate(DocumentEvent e) {
			next(e);
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			next(e);
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			next(e);
		}
		/**
		 * Convenience method to create a new observable action listener and register it with the target component must have a public <code>addDocumentListener(DocumentListener)</code> method.
		 * @param component the target component
		 * @return the new observable
		 */
		public static ObservableDocumentListener register(Object component) {
			return new ObservableDocumentListener().registerWith(component);
		}
		/**
		 * Convenience method to register this observable with the target component which must have a public <code>addDocumentListener(DocumentListener)</code> method. 
		 * @param component the target component
		 * @return this
		 */
		public ObservableDocumentListener registerWith(Object component) {
			if (component == null) {
				throw new IllegalArgumentException("component is null");
			}
			try {
				Method m = component.getClass().getMethod("addDocumentListener", DocumentListener.class);
				m.invoke(component, this);
			} catch (NoSuchMethodException ex) {
				throw new IllegalArgumentException(ex);
			} catch (IllegalAccessException ex) {
				throw new IllegalArgumentException(ex);
			} catch (InvocationTargetException ex) {
				throw new IllegalArgumentException(ex);
			}
			return this;
		}
		/**
		 * Convenience method to unregister this observable from the target component which must have a public <code>removeDocumentListener(DocumentListener)</code> method. 
		 * @param component the target component
		 * @return this
		 */
		public ObservableDocumentListener unregisterFrom(Object component) {
			if (component == null) {
				throw new IllegalArgumentException("component is null");
			}
			try {
				Method m = component.getClass().getMethod("removeDocumentListener", DocumentListener.class);
				m.invoke(component, this);
			} catch (NoSuchMethodException ex) {
				throw new IllegalArgumentException(ex);
			} catch (IllegalAccessException ex) {
				throw new IllegalArgumentException(ex);
			} catch (InvocationTargetException ex) {
				throw new IllegalArgumentException(ex);
			}
			return this;
		}
	}
	/**
	 * The observable list data listener which relays the intervalAdded(), intervalRemoved() and contentsChanged() events
	 * to next() calls. The original event type can be determined from the ListDataEvent.getType().
	 * This observable never signals finish() or error().
	 * @author akarnokd, 2011.02.01.
	 */
	public static class ObservableListDataListener extends DefaultObservable<ListDataEvent> implements ListDataListener {
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
		public static ObservableListDataListener register(Object component) {
			return new ObservableListDataListener().registerWith(component);
		}
		/**
		 * Convenience method to register this observable with the target component which must have a public <code>addListDataListener(ListDataListener)</code> method. 
		 * @param component the target component
		 * @return this
		 */
		public ObservableListDataListener registerWith(Object component) {
			if (component == null) {
				throw new IllegalArgumentException("component is null");
			}
			try {
				Method m = component.getClass().getMethod("addListDataListener", ListDataListener.class);
				m.invoke(component, this);
			} catch (NoSuchMethodException ex) {
				throw new IllegalArgumentException(ex);
			} catch (IllegalAccessException ex) {
				throw new IllegalArgumentException(ex);
			} catch (InvocationTargetException ex) {
				throw new IllegalArgumentException(ex);
			}
			return this;
		}
		/**
		 * Convenience method to unregister this observable from the target component which must have a public <code>removeListDataListener(ListDataListener)</code> method. 
		 * @param component the target component
		 * @return this
		 */
		public ObservableListDataListener unregisterFrom(Object component) {
			if (component == null) {
				throw new IllegalArgumentException("component is null");
			}
			try {
				Method m = component.getClass().getMethod("removeListDataListener", ListDataListener.class);
				m.invoke(component, this);
			} catch (NoSuchMethodException ex) {
				throw new IllegalArgumentException(ex);
			} catch (IllegalAccessException ex) {
				throw new IllegalArgumentException(ex);
			} catch (InvocationTargetException ex) {
				throw new IllegalArgumentException(ex);
			}
			return this;
		}
	}
	/**
	 * The observable list selection listener which relays the valueChanged() events to next().
	 * This observable never signals finish() or error().
	 * @author akarnokd, 2011.02.01.
	 */
	public static class ObservableListSelectionListener extends DefaultObservable<ListSelectionEvent> implements ListSelectionListener {

		@Override
		public void valueChanged(ListSelectionEvent e) {
			next(e);
		}
		
		/**
		 * Convenience method to create a new observable action listener and register it with the target component must have a public <code>addListSelectionListener(ListSelectionListener)</code> method.
		 * @param component the target component
		 * @return the new observable
		 */
		public static ObservableListSelectionListener register(Object component) {
			return new ObservableListSelectionListener().registerWith(component);
		}
		/**
		 * Convenience method to register this observable with the target component which must have a public <code>addListSelectionListener(ListSelectionListener)</code> method. 
		 * @param component the target component
		 * @return this
		 */
		public ObservableListSelectionListener registerWith(Object component) {
			if (component == null) {
				throw new IllegalArgumentException("component is null");
			}
			try {
				Method m = component.getClass().getMethod("addListSelectionListener", ListSelectionListener.class);
				m.invoke(component, this);
			} catch (NoSuchMethodException ex) {
				throw new IllegalArgumentException(ex);
			} catch (IllegalAccessException ex) {
				throw new IllegalArgumentException(ex);
			} catch (InvocationTargetException ex) {
				throw new IllegalArgumentException(ex);
			}
			return this;
		}
		/**
		 * Convenience method to unregister this observable from the target component which must have a public <code>removeListSelectionListener(ListSelectionListener)</code> method. 
		 * @param component the target component
		 * @return this
		 */
		public ObservableListSelectionListener unregisterFrom(Object component) {
			if (component == null) {
				throw new IllegalArgumentException("component is null");
			}
			try {
				Method m = component.getClass().getMethod("removeListSelectionListener", ListSelectionListener.class);
				m.invoke(component, this);
			} catch (NoSuchMethodException ex) {
				throw new IllegalArgumentException(ex);
			} catch (IllegalAccessException ex) {
				throw new IllegalArgumentException(ex);
			} catch (InvocationTargetException ex) {
				throw new IllegalArgumentException(ex);
			}
			return this;
		}
	}
	/**
	 * The observable adjustment listener which relays adjustmentValueChanged() events to next().
	 * This observable never signals finish() or error().
	 * @author akarnokd, 2011.02.01.
	 */
	public static class ObservableAdjustmentListener extends DefaultObservable<AdjustmentEvent> implements AdjustmentListener {
		@Override
		public void adjustmentValueChanged(AdjustmentEvent e) {
			next(e);
		}
		/**
		 * Convenience method to create a new observable action listener and register it with the target component must have a public <code>addAdjustmentListener(AdjustmentListener)</code> method.
		 * @param component the target component
		 * @return the new observable
		 */
		public static ObservableAdjustmentListener register(Object component) {
			return new ObservableAdjustmentListener().registerWith(component);
		}
		/**
		 * Convenience method to register this observable with the target component which must have a public <code>addAdjustmentListener(AdjustmentListener)</code> method. 
		 * @param component the target component
		 * @return this
		 */
		public ObservableAdjustmentListener registerWith(Object component) {
			if (component == null) {
				throw new IllegalArgumentException("component is null");
			}
			if (component instanceof Adjustable) {
				((Adjustable)component).addAdjustmentListener(this);
				return this;
			}
			try {
				Method m = component.getClass().getMethod("addAdjustmentListener", AdjustmentListener.class);
				m.invoke(component, this);
			} catch (NoSuchMethodException ex) {
				throw new IllegalArgumentException(ex);
			} catch (IllegalAccessException ex) {
				throw new IllegalArgumentException(ex);
			} catch (InvocationTargetException ex) {
				throw new IllegalArgumentException(ex);
			}
			return this;
		}
		/**
		 * Convenience method to unregister this observable from the target component which must have a public <code>removeAdjustmentListener(AdjustmentListener)</code> method. 
		 * @param component the target component
		 * @return this
		 */
		public ObservableAdjustmentListener unregisterFrom(Object component) {
			if (component == null) {
				throw new IllegalArgumentException("component is null");
			}
			if (component instanceof Adjustable) {
				((Adjustable)component).removeAdjustmentListener(this);
				return this;
			}
			try {
				Method m = component.getClass().getMethod("removeAdjustmentListener", AdjustmentListener.class);
				m.invoke(component, this);
			} catch (NoSuchMethodException ex) {
				throw new IllegalArgumentException(ex);
			} catch (IllegalAccessException ex) {
				throw new IllegalArgumentException(ex);
			} catch (InvocationTargetException ex) {
				throw new IllegalArgumentException(ex);
			}
			return this;
		}
	}
	/**
	 * The observable key listener which collects the <code>keyTyped()</code>, <code>keyPressed()</code>, <code>keyReleased()</code>
	 * events and relays it to next(). The original type can be determined by using <code>KeyEvent.getType()</code>.
	 * This observable never signals finish() or error().
	 * @author akarnokd, 2011.02.01.
	 */
	public static class ObservableKeyListener extends DefaultObservable<KeyEvent> implements KeyListener {

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
		public static ObservableKeyListener register(Object component) {
			return new ObservableKeyListener().registerWith(component);
		}
		/**
		 * Convenience method to register this observable with the target component which must have a public <code>addKeyListener(KeyListener)</code> method. 
		 * @param component the target component
		 * @return this
		 */
		public ObservableKeyListener registerWith(Object component) {
			if (component == null) {
				throw new IllegalArgumentException("component is null");
			}
			try {
				Method m = component.getClass().getMethod("addKeyListener", KeyListener.class);
				m.invoke(component, this);
			} catch (NoSuchMethodException ex) {
				throw new IllegalArgumentException(ex);
			} catch (IllegalAccessException ex) {
				throw new IllegalArgumentException(ex);
			} catch (InvocationTargetException ex) {
				throw new IllegalArgumentException(ex);
			}
			return this;
		}
		/**
		 * Convenience method to unregister this observable from the target component which must have a public <code>removeKeyListener(KeyListener)</code> method. 
		 * @param component the target component
		 * @return this
		 */
		public ObservableKeyListener unregisterFrom(Object component) {
			if (component == null) {
				throw new IllegalArgumentException("component is null");
			}
			try {
				Method m = component.getClass().getMethod("removeKeyListener", KeyListener.class);
				m.invoke(component, this);
			} catch (NoSuchMethodException ex) {
				throw new IllegalArgumentException(ex);
			} catch (IllegalAccessException ex) {
				throw new IllegalArgumentException(ex);
			} catch (InvocationTargetException ex) {
				throw new IllegalArgumentException(ex);
			}
			return this;
		}
	}
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
	public static class ObservableMouseListener extends DefaultObservable<MouseEvent> implements MouseListener, MouseMotionListener, MouseWheelListener {
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
		public static ObservableMouseListener register(Component component) {
			return new ObservableMouseListener().registerWith(component);
		}
		/**
		 * Convenience method to register this observer with the target component for all mouse event types.
		 * To deregister this observable, use the <code>unregisterFrom()</code>.
		 * @param component the target component
		 * @return this
		 */
		public ObservableMouseListener registerWith(Component component) {
			if (component == null) {
				throw new IllegalArgumentException("component is null");
			}
			component.addMouseListener(this);
			component.addMouseMotionListener(this);
			component.addMouseWheelListener(this);
			return this;
		}
		/**
		 * Unregister all mouse events from the given component.
		 * @param component the target component
		 * @return this
		 */
		public ObservableMouseListener unregisterFrom(Component component) {
			if (component == null) {
				throw new IllegalArgumentException("component is null");
			}
			component.removeMouseListener(this);
			component.removeMouseMotionListener(this);
			component.removeMouseWheelListener(this);
			return this;
		}
	}
}
