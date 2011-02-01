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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTextField;
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
		 * Convenience method to register an observable action listener with the given abstract button (JButton, JMenuItem, etc.).
		 * The registration is performed via <code>AbstractButton.addActionListener()</code>. To deregister,
		 * use the <code>AbstractButton.removeActionListener()</code> with the returned observable.
		 * @param button the target button
		 * @return the observable action listener
		 */
		public static ObservableActionListener register(AbstractButton button) {
			if (button == null) {
				throw new IllegalArgumentException("button is null");
			}
			ObservableActionListener o = new ObservableActionListener();
			button.addActionListener(o);
			return o;
		}
		/**
		 * Convenience method to register an observable action listener with the given abstract button (JTextField, JPasswordField, JFormattedTextField etc.).
		 * The registration is performed via <code>JTextField.addActionListener()</code>. To deregister,
		 * use the <code>JTextField.removeActionListener()</code> with the returned observable.
		 * @param field the target field
		 * @return the observable action listener
		 */
		public static ObservableActionListener register(JTextField field) {
			if (field == null) {
				throw new IllegalArgumentException("field is null");
			}
			ObservableActionListener o = new ObservableActionListener();
			field.addActionListener(o);
			return o;
		}
		/**
		 * Convenience method to register an observable action listener with the given JComboBox.
		 * The registration is performed via <code>JComboBox.addActionListener()</code>. To deregister,
		 * use the <code>JComboBox.removeActionListener()</code> with the returned observable.
		 * @param field the target field
		 * @return the observable action listener
		 */
		public static ObservableActionListener register(JComboBox field) {
			if (field == null) {
				throw new IllegalArgumentException("field is null");
			}
			ObservableActionListener o = new ObservableActionListener();
			field.addActionListener(o);
			return o;
		}
		/**
		 * Convenience method to register an action listener on an object which should have an <code>addActionListener(ActionListener)</code>
		 * public method. It uses reflection to detemine the method's existence. Throws IllegalArgumentException if the
		 * component is null or does not have the required method.
		 * @param component the target component.
		 * @return the observable action listener
		 */
		public static ObservableActionListener register(Object component) {
			if (component == null) {
				throw new IllegalArgumentException("component is null");
			}
			try {
				Method m = component.getClass().getMethod("addActionListener", ActionListener.class);
				ObservableActionListener o = new ObservableActionListener();
				m.invoke(component, o);
				return o;
			} catch (NoSuchMethodException ex) {
				throw new IllegalArgumentException(ex);
			} catch (IllegalAccessException ex) {
				throw new IllegalArgumentException(ex);
			} catch (InvocationTargetException ex) {
				throw new IllegalArgumentException(ex);
			}
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
		 * Convenience method to register an action listener on an object which should have an <code>addActionListener(ActionListener)</code>
		 * public method. It uses reflection to detemine the method's existence. Throws IllegalArgumentException if the
		 * component is null or does not have the required method.
		 * @param component the target component.
		 * @return the observable action listener
		 */
		public static ObservableChangeListener register(Object component) {
			if (component == null) {
				throw new IllegalArgumentException("component is null");
			}
			try {
				Method m = component.getClass().getMethod("addChangeListener", ChangeListener.class);
				ObservableChangeListener o = new ObservableChangeListener();
				m.invoke(component, o);
				return o;
			} catch (NoSuchMethodException ex) {
				throw new IllegalArgumentException(ex);
			} catch (IllegalAccessException ex) {
				throw new IllegalArgumentException(ex);
			} catch (InvocationTargetException ex) {
				throw new IllegalArgumentException(ex);
			}
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
		 * Convenience method to register an observable list selection listener with the given JList and return this observable.
		 * It calls the JList.addListSelectionListener(). To remove the observable, use the JList.removeListSelectionListener().
		 * @param list the target jlist
		 * @return creates a new observable for a ListSelectionListener.
		 */
		public static ObservableListSelectionListener register(@SuppressWarnings("rawtypes") JList list) {
			if (list == null) {
				throw new IllegalArgumentException("list is null");
			}
			ObservableListSelectionListener o = new ObservableListSelectionListener();
			list.addListSelectionListener(o);
			return o;
		}
		/**
		 * Convenience method to register an observable list selection listener with the given JTable instance.
		 * It uses the <code>table.getSelectionModel().addListSelectionListener()</code> for the registration. To remove the registration,
		 * use the <code>table.getSelectionModel().removeListSelectionListener()</code>. Note
		 * that the table table allows changing its selection model which might render this registration inoperable.
		 * @param table the target table
		 * @return creates a new observable for a ListDataListener.
		 */
		public static ObservableListSelectionListener register(JTable table) {
			if (table == null) {
				throw new IllegalArgumentException("table is null");
			}
			ObservableListSelectionListener o = new ObservableListSelectionListener();
			table.getSelectionModel().addListSelectionListener(o);
			return o;
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
		 * Convenience method to register with the given scrollbar by using the <code>Scrollbar.addAdjustmentListener()</code>.
		 * @param scrollbar the target scrollbar
		 * @return the observable
		 */
		public static ObservableAdjustmentListener register(Adjustable scrollbar) {
			ObservableAdjustmentListener o = new ObservableAdjustmentListener();
			scrollbar.addAdjustmentListener(o);
			return o;
		}
		/**
		 * Convenience method to register an action listener on an object which should have an <code>addAdjustmentListener()</code>
		 * public method. It uses reflection to detemine the method's existence. Throws IllegalArgumentException if the
		 * component is null or does not have the required method.
		 * @param component the target component.
		 * @return the observable action listener
		 */
		public static ObservableAdjustmentListener register(Object component) {
			if (component == null) {
				throw new IllegalArgumentException("component is null");
			}
			try {
				Method m = component.getClass().getMethod("addAdjustmentListener", AdjustmentListener.class);
				ObservableAdjustmentListener o = new ObservableAdjustmentListener();
				m.invoke(component, o);
				return o;
			} catch (NoSuchMethodException ex) {
				throw new IllegalArgumentException(ex);
			} catch (IllegalAccessException ex) {
				throw new IllegalArgumentException(ex);
			} catch (InvocationTargetException ex) {
				throw new IllegalArgumentException(ex);
			}
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
		 * Convenience method to register an action listener on an object which should have an <code>addAdjustmentListener()</code>
		 * public method. It uses reflection to detemine the method's existence. Throws IllegalArgumentException if the
		 * component is null or does not have the required method.
		 * @param component the target component.
		 * @return the observable action listener
		 */
		public static ObservableKeyListener register(Object component) {
			if (component == null) {
				throw new IllegalArgumentException("component is null");
			}
			try {
				Method m = component.getClass().getMethod("addKeyListener", KeyListener.class);
				ObservableKeyListener o = new ObservableKeyListener();
				m.invoke(component, o);
				return o;
			} catch (NoSuchMethodException ex) {
				throw new IllegalArgumentException(ex);
			} catch (IllegalAccessException ex) {
				throw new IllegalArgumentException(ex);
			} catch (InvocationTargetException ex) {
				throw new IllegalArgumentException(ex);
			}
		}
	}
	/**
	 * The observable key listener which relays all mouse related events (movement, clicks) to next().
	 * events and relays it to next(). The original type can be determined by using <code>MouseEvent.getID()</code>
	 * which can be one of the following value: 
	 * <ul>
	 * <li><code>MouseEvent.MOUSE_CLICKED</code></li>
	 * <li><code>MouseEvent.MOUSE_PRESSED</code></li>
	 * <li><code>MouseEvent.MOUSE_RELEASED</code></li>
	 * <li><code>MouseEvent.MOUSE_ENTERED</code></li>
	 * <li><code>MouseEvent.MOUSE_EXITED</code></li>
	 * </ul>
	 * This observable never signals finish() or error().
	 * @author akarnokd, 2011.02.01.
	 */
	public static class ObservableMouseListener extends DefaultObservable<MouseEvent> implements MouseListener {
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
	}
}
