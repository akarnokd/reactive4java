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
 * A minimalist interface to run JavaScript eval() from within GWT.
 * @author karnok, 2011.02.20.
 */
public interface ScriptEngine {
	/**
	 * Evaluate the given script by using the given
	 * bindings.
	 * @param script the script string
	 * @param bindings the bindings for extra parameters
	 * @return the result of the evaluation
	 * @throws ScriptException if there was trouble with the script
	 */
	Object eval(String script, Bindings bindings) throws ScriptException;
}
