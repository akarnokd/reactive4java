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

package hu.akarnokd.reactive4gwt.client;

import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.reactive.Observable;
import hu.akarnokd.reactive4java.reactive.Observer;
import hu.akarnokd.reactive4java.reactive.Reactive;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Reactive4GWT implements EntryPoint {
	/**
	 * This is the entry point method.
	 */
	@Override
	public void onModuleLoad() {
		
		final ListBox list = new ListBox(true);
		RootPanel.get().add(list);
		
		Reactive.selectMany(
				Reactive.range(0, 10),
				new Func1<Integer, Observable<Integer>>() {
					@Override
					public Observable<Integer> invoke(Integer param1) {
						return Reactive.range(0, param1 + 1);
					}
				}
		).register(new Observer<Integer>() {
			@Override
			public void next(Integer value) {
				list.addItem(value.toString());
			}

			@Override
			public void error(Throwable ex) {
				
			}

			@Override
			public void finish() {
				
			}
			
		});
		
		ScriptEngine js = new ScriptEngineManager().getEngineByName("js");
		
		try {
			Bindings b = new SimpleBindings();
			b.put("o", 1);
			list.addItem("" + js.eval("o", b));
			
			b.put("o", "Hello world");
			list.addItem("" + js.eval("o", b));
			
			b.put("o", 2);
			list.addItem("" + js.eval("o % 2 == 0", b));
		
		} catch (ScriptException ex) {
			Window.alert(ex.toString());
		}
	}
}
