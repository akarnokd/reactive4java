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

import java.util.Map;

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
						return invoke(script);
					} catch (Throwable t) {
						throw new ScriptException(t);
					}
				}
			};
		}
		return null;
	}
	/** The current bind sequence. */
	int bindSequence;
	/** 
	 * Prepare the bindings for the given sequence.
	 * @param index the sequence number 
	 */
    native void prepareOnWindow(int index) /*-{
    	$wnd["reactive4java_bindings_" + index] = new Array();
	}-*/;
    /**
     * Set a binding variable name and value on the given sequence.
     * @param index the sequence number
     * @param name the variable name
     * @param value the value
     */
	native void setOnWindow(int index, String name, Object value) /*-{
	    $wnd["reactive4java_bindings_" + index][name] = value;
	}-*/;
	/**
	 * Clear the binding variables on the given sequence number.
	 * @param index the sequence number
	 */
	native void clearOnWindow(int index) /*-{
	    $wnd["reactive4java_bindings_" + index] = null;
	}-*/;
	/**
	 * Invoke a Javascript script.
	 * <p>The method auto-boxes {@code boolean} into {@code java.lang.Boolean}
	 *  and {@code number} into {@code java.lang.Double} objects. 
	 * @param script the script to execute
	 * @return the return value
	 */
	native Object invoke(String script) /*-{
	    var result = $wnd.eval(script);
	    if (typeof(result) == "boolean") {
	        return result ? @java.lang.Boolean::TRUE : @java.lang.Boolean::FALSE;
	    } else
	    if (typeof(result) == "number") {
	        return @java.lang.Double::valueOf(D)(result);
	    }
	    return result;
	}-*/;
	/**
	 * Invoke the script with the given mappings of variables.
	 * @param script the script to invoke
	 * @param bindings the variable bindings
	 * @return the returned value
	 */
	public Object invoke(String script, Bindings bindings) {
	    int seq = bindSequence++;
	    try {
	        StringBuilder script2 = new StringBuilder();
	        prepareOnWindow(seq);
	        for (Map.Entry<String, Object> e : bindings.entrySet()) {
	            setOnWindow(seq, e.getKey(), e.getValue());
	            script2.append("var ").append(e.getKey()).append(" = ")
	            .append("window[\"reactive4java_bindings_\" + ").append(seq)
	            .append("][\"").append(e.getKey()).append("\"];\r\n");
	        }
	        script2.append("\r\n").append(script);
	        return invoke(script);
	    } finally {
	        clearOnWindow(seq);
	    } 
	}
}
