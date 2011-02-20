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
package javax.script;

/**
 * The script engine interface for GWT.
 * @author karnok, 2011.02.20.
 */
public class ScriptEngineManager {

	/**
	 * Returns a script engine for the given name.
	 * If the engine is not supported, this returns null.
	 * @param name the engine name
	 * @return the script engine or null
	 */
	public ScriptEngine getEngineByName(String name) {
		if (name.equals("js") || name.equalsIgnoreCase("javascript")) {
			return new ScriptEngine() {
				@Override
				public Object eval(String script, Bindings bindings)
						throws ScriptException {
					try {
						return nativeEval(script);
					} catch (Throwable t) {
						throw new ScriptException(t);
					}
				}
			};
		}
		return null;
	}
	/**
	 * Evaluate the given script string.
	 * @param script the script
	 * @return the return value
	 */
	native Object nativeEval(String script) /*-{
		return $wnd.eval(script);
	}-*/;
}
