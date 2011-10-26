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
package hu.akarnokd.reactive4java.test;

import hu.akarnokd.reactive4java.base.Action1;
import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.reactive.Observable;
import hu.akarnokd.reactive4java.reactive.Reactive;
import hu.akarnokd.reactive4java.swing.ObservableDocumentListener;
import hu.akarnokd.reactive4java.swing.SwingObservables;

import java.awt.Container;
import java.util.concurrent.TimeUnit;

import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;

/**
 * A simple demonstration application how to start processing once the user stopped or paused typing for a specific time.
 * @author akarnokd, 2011.02.01.
 */
public class ThrottleDemo extends JFrame {
	/** */
	private static final long serialVersionUID = -6282175324077200803L;
	/**
	 * Construct the GUI.
	 */
	public ThrottleDemo() {
		super("Throttle demo");
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		Container c = getContentPane();

		GroupLayout gl = new GroupLayout(c);
		c.setLayout(gl);
		gl.setAutoCreateContainerGaps(true);
		gl.setAutoCreateGaps(true);
		
		JLabel typeHereLabel = new JLabel("Type here:");
		JLabel resultsHereLabel = new JLabel("Results will show up here:");
		
		final JTextField textField = new JTextField();
		final JList<String> resultsHere = new JList<String>();
		JScrollPane sp = new JScrollPane(resultsHere);
		
		gl.setHorizontalGroup(
			gl.createParallelGroup()
			.addGroup(
				gl.createSequentialGroup()
				.addComponent(typeHereLabel)
				.addComponent(textField)
			)
			.addComponent(resultsHereLabel)
			.addComponent(sp)
		);
		
		gl.setVerticalGroup(
			gl.createSequentialGroup()
			.addGroup(
				gl.createParallelGroup(Alignment.BASELINE)
				.addComponent(typeHereLabel)
				.addComponent(textField, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
			)
			.addComponent(resultsHereLabel)
			.addComponent(sp)
		);
		
		ObservableDocumentListener dl = ObservableDocumentListener.register(textField.getDocument()); 
//		dl.register(Observables.println("DL: "));
		
		Observable<String> extract = Reactive.select(dl, 
				new Func1<DocumentEvent, String>() {
			@Override
			public String invoke(DocumentEvent param1) {
				return textField.getText();
			}
		});
		extract.register(Reactive.println("EXTRACT: "));

		Observable<String> distinct = Reactive.distinct(extract);
		distinct.register(Reactive.println("DISTINCT: "));
		
		Observable<String> throttle = Reactive.throttle(distinct, 500, TimeUnit.MILLISECONDS);
		throttle.register(Reactive.println("THROTTLE: "));
		
//		Observable<String> takeuntil = Observables.takeUntil(throttle, dl);
//		takeuntil.register(Observables.println("TAKEUNTIL: "));
		
		Observable<String> result = SwingObservables.observeOnEdt(throttle);
		result.register(Reactive.println("RESULT: "));
			
		result.register(Reactive.toObserver(new Action1<String>() {
			@Override
			public void invoke(String value) {
				DefaultListModel<String> model = new DefaultListModel<String>();
				for (int i = 0; i < 20; i++) {
					model.addElement(value + " " + i);
				}
				resultsHere.setModel(model);
			}
		}
		));
		pack();
	}
	/**
	 * Main program.
	 * @param args no arguments
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				ThrottleDemo demo = new ThrottleDemo();
				demo.setLocationRelativeTo(null);
				demo.setVisible(true);
			}
		});
	}

}
