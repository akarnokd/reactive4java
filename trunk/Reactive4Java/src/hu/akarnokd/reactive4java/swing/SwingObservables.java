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

import hu.akarnokd.reactive4java.base.Scheduler;
import hu.akarnokd.reactive4java.reactive.DefaultObservable;
import hu.akarnokd.reactive4java.reactive.Observable;
import hu.akarnokd.reactive4java.reactive.Observables;
import hu.akarnokd.reactive4java.reactive.Observer;
import hu.akarnokd.reactive4java.util.DefaultEdtScheduler;

import java.awt.Adjustable;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

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
	/** The common observable pool where the Observer methods get invoked by default. */
	static final AtomicReference<Scheduler> DEFAULT_EDT_SCHEDULER = new AtomicReference<Scheduler>(new DefaultEdtScheduler());
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
			return invoke(component, "add", ActionListener.class, this);
		}
		/**
		 * Convenience method to unregister this observable from the target component which must have a public <code>removeActionListener(ActionListener)</code> method. 
		 * @param component the target component
		 * @return this
		 */
		public ObservableActionListener unregisterFrom(Object component) {
			return invoke(component, "remove", ActionListener.class, this);
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
			return invoke(component, "add", ChangeListener.class, this);
		}
		/**
		 * Convenience method to unregister this observable from the target component which must have a public <code>removeChangeListener(ChangeListener)</code> method. 
		 * @param component the target component
		 * @return this
		 */
		public ObservableChangeListener unregisterFrom(Object component) {
			return invoke(component, "remove", ChangeListener.class, this);
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
			return invoke(component, "add", DocumentListener.class, this);
		}
		/**
		 * Convenience method to unregister this observable from the target component which must have a public <code>removeDocumentListener(DocumentListener)</code> method. 
		 * @param component the target component
		 * @return this
		 */
		public ObservableDocumentListener unregisterFrom(Object component) {
			return invoke(component, "remove", DocumentListener.class, this);
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
			return invoke(component, "add", ListDataListener.class, this);
		}
		/**
		 * Convenience method to unregister this observable from the target component which must have a public <code>removeListDataListener(ListDataListener)</code> method. 
		 * @param component the target component
		 * @return this
		 */
		public ObservableListDataListener unregisterFrom(Object component) {
			return invoke(component, "remove", ListDataListener.class, this);
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
			return invoke(component, "add", ListSelectionListener.class, this);
		}
		/**
		 * Convenience method to unregister this observable from the target component which must have a public <code>removeListSelectionListener(ListSelectionListener)</code> method. 
		 * @param component the target component
		 * @return this
		 */
		public ObservableListSelectionListener unregisterFrom(Object component) {
			return invoke(component, "remove", ListSelectionListener.class, this);
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
			if (component instanceof Adjustable) {
				((Adjustable)component).addAdjustmentListener(this);
				return this;
			}
			return invoke(component, "add", AdjustmentListener.class, this);
		}
		/**
		 * Convenience method to unregister this observable from the target component which must have a public <code>removeAdjustmentListener(AdjustmentListener)</code> method. 
		 * @param component the target component
		 * @return this
		 */
		public ObservableAdjustmentListener unregisterFrom(Object component) {
			if (component instanceof Adjustable) {
				((Adjustable)component).removeAdjustmentListener(this);
				return this;
			}
			return invoke(component, "remove", AdjustmentListener.class, this);
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
			return invoke(component, "add", KeyListener.class, this);
		}
		/**
		 * Convenience method to unregister this observable from the target component which must have a public <code>removeKeyListener(KeyListener)</code> method. 
		 * @param component the target component
		 * @return this
		 */
		public ObservableKeyListener unregisterFrom(Object component) {
			return invoke(component, "remove", KeyListener.class, this);
		}
	}
	/**
	 * Invoke the methodPrefix + paramType.getSimpleName method (i.e., "add" and ActionListener.class will produce addActionListener)
	 * on the object and wrap any exception into IllegalArgumentException.
	 * @param <T> the method parameter type
	 * @param <U> a type which extends the method parameter type T.
	 * @param o the target object
	 * @param methodPrefix the method name prefix
	 * @param paramType the required parameter type
	 * @param value the value to invoke with
	 * @return the value
	 */
	static <T, U extends T> U invoke(Object o, String methodPrefix, Class<T> paramType, U value) {
		if (o == null) {
			throw new IllegalArgumentException("o is null");
		}
		try {
			Method m = o.getClass().getMethod(methodPrefix + paramType.getSimpleName(), paramType);
			m.invoke(o, value);
		} catch (NoSuchMethodException ex) {
			throw new IllegalArgumentException(ex);
		} catch (IllegalAccessException ex) {
			throw new IllegalArgumentException(ex);
		} catch (InvocationTargetException ex) {
			throw new IllegalArgumentException(ex);
		}
		return value;
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
		public ObservableMouseListener registerWith(Component component, boolean normal, boolean motion, boolean wheel) {
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
	public static class ObservableWindowListener extends DefaultObservable<WindowEvent> implements WindowListener, WindowFocusListener, WindowStateListener {

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
		public static ObservableWindowListener register(Window component) {
			return new ObservableWindowListener().registerWith(component);
		}
		/**
		 * Convenience method to register this observer with the target component for all window event types.
		 * To deregister this observable, use the <code>unregisterFrom()</code>.
		 * @param component the target component
		 * @return this
		 */
		public ObservableWindowListener registerWith(Window component) {
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
		public ObservableWindowListener registerWith(Window component, boolean normal, boolean focus, boolean state) {
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
		public ObservableWindowListener unregisterFrom(Window component) {
			if (component == null) {
				throw new IllegalArgumentException("component is null");
			}
			component.removeWindowListener(this);
			component.removeWindowFocusListener(this);
			component.removeWindowStateListener(this);
			return this;
		}
	}
	/**
	 * The observable item listener which relays events from <code>ItemListener</code>.
	 * This observable never signals finish() or error().
	 * @author akarnokd, 2011.02.01.
	 */
	public static class ObservableItemListener extends DefaultObservable<ItemEvent> implements ItemListener {
		@Override
		public void itemStateChanged(ItemEvent e) {
			next(e);
		}

		/**
		 * Convenience method to register an observer on the component for <code>itemStateChanged(ItemEvent)</code> events.
		 * @param component the target component.
		 * @return the new observable
		 */
		public static ObservableItemListener register(Object component) {
			return new ObservableItemListener().registerWith(component);
		}
		/**
		 * Convenience method to register an observer on the component for <code>itemStateChanged(ItemEvent)</code> events.
		 * @param component the target component.
		 * @return the new observable
		 */
		public ObservableItemListener registerWith(Object component) {
			return invoke(component, "add", ItemListener.class, this);
		}
		/**
		 * Unregister this observer of list events from the given component.
		 * @param component the target component
		 * @return this
		 */
		public ObservableItemListener unregisterFrom(Object component) {
			return invoke(component, "remove", ItemListener.class, this);
		}
	}
	/**
	 * Create a dynamic observer for the given listener interface by
	 * proxying all method calls. None of the methods of the listener interface should require something meaningful to be returned, i.e., they all
	 * must be <code>void</code>, return <code>Void</code> (or the original call site should accept <code>null</code>s).
	 * Note that due this proxying effect, the handler invocation may be 100 times slower than a direct implementation
	 * @param <T> the listener interface type
	 * @param listener the list interface class
	 * @param bindTo the target observer, use the DefaultObservable
	 * @return the proxy instance
	 */
	public static <T> T create(Class<T> listener, final Observer<? super Dynamic> bindTo) {
		final InvocationHandler handler = new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args)
					throws Throwable {
				bindTo.next(new Dynamic(method.getName(), args));
				return null;
			}
		};
		
		return listener.cast(Proxy.newProxyInstance(listener.getClassLoader(), new Class<?>[] { listener }, handler));
	}
	/**
	 * Wrap the observable to the Event Dispatch Thread for listening to events.
	 * @param <T> the value type to observe
	 * @param observable the original observable
	 * @return the new observable
	 */
	public static <T> Observable<T> observeOnEdt(Observable<T> observable) {
		return Observables.observeOn(observable, DEFAULT_EDT_SCHEDULER.get());
	}
	/**
	 * @return the current default pool used by the Observables methods
	 */
	public static Scheduler getDefaultEdtScheduler() {
		return DEFAULT_EDT_SCHEDULER.get();
	}
	/**
	 * Replace the current default scheduler with the specified  new scheduler.
	 * This method is threadsafe
	 * @param newScheduler the new scheduler
	 * @return the current scheduler
	 */
	public static Scheduler replaceDefaultEdtScheduler(Scheduler newScheduler) {
		if (newScheduler == null) {
			throw new IllegalArgumentException("newScheduler is null");
		}
		return DEFAULT_EDT_SCHEDULER.getAndSet(newScheduler);
	}
	/**
	 * Restore the default scheduler back to the <code>DefaultScheduler</code>
	 * used when this class was initialized.
	 */
	public static void restoreDefaultEdtScheduler() {
		DEFAULT_EDT_SCHEDULER.set(new DefaultEdtScheduler());
	}
	/**
	 * Wrap the observable to the Event Dispatch Thread for subscribing to events.
	 * @param <T> the value type to observe
	 * @param observable the original observable
	 * @return the new observable
	 */
	public static <T> Observable<T> subscribeOnEdt(Observable<T> observable) {
		return Observables.subscribeOn(observable, DEFAULT_EDT_SCHEDULER.get());
	}
}
