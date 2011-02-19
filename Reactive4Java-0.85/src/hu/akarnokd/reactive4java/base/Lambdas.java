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
package hu.akarnokd.reactive4java.base;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

/**
 * Utility class to create functions which have
 * their body as Javascript (or other script) language.
 * <p>Note that scripting performance is typically 10-100 slower than the
 * same JIT compiled Java expression.</p>
 * <p>See the <a href='http://java.sun.com/developer/technicalArticles/J2SE/Desktop/scripting/'>scripting</a>
 * articles about how to script in java.</p> 
 * 
 * <p>Note that a script engines might not be thread safe, e.g., you might not run
 * multiple scripts without external synchronization.
 * See also <a href='http://download.oracle.com/javase/6/docs/api/javax/script/ScriptEngineFactory.html#getParameter(java.lang.String)'>THREADING</a>
 * for furhter details.<p>
 * @author akarnokd, 2011.02.19.
 */
public final class Lambdas {
	/**
	 * Utility class.
	 */
	private Lambdas() {
	}
	/**
	 * Returns a parameterless function which invokes the given script on the 
	 * script engine and retunrs its value.
	 * <p>The syntax of the expression follows the generic lambda format used throughout this utility class:</p>
	 * <code>param1, param2 => expression1; expression; return result</code>
	 * <p>For zero parameters, you may omit the => completely.
	 * @param <T> the return type of the function
	 * @param expression the original lambda expression
	 * @param engine the script engine to use
	 * @param bindings the external resources the script might need
	 * @return the function
	 */
	public static <T> Func0<T> js0(
			String expression, 
			final ScriptEngine engine, 
			final Map<String, Object> bindings) {
		expression = expression.trim();
		if (expression.startsWith("=>")) {
			expression = expression.substring(3).trim();
		}
		final String body = expression;
		return new Func0<T>() {
			@Override
			public T invoke() {
				try {
					SimpleBindings b = new SimpleBindings(bindings);
					@SuppressWarnings("unchecked")
					T result = (T)engine.eval(body, b);
					return result;
				} catch (ScriptException ex) {
					throw new RuntimeException(ex);
				}
			}
		};
	}
	/**
	 * Returns a parameterless function which invokes the given javascript
	 * and retunrs its value.
	 * <p>The syntax of the expression follows the generic lambda format used throughout this utility class:</p>
	 * <code> => expression1; expression; return result</code>
	 * <p>For zero parameters, you may omit the => completely.
	 * @param <T> the return type of the function
	 * @param expression the lambda expression
	 * @return the function
	 */
	public static <T> Func0<T> js0(String expression) {
		return js0(expression, getEngine("js"), new HashMap<String, Object>());
	}
	/**
	 * Returns a parameterless function which invokes the given javascript
	 * and retunrs its value.
	 * <p>The syntax of the expression follows the generic lambda format used throughout this utility class:</p>
	 * <code> => expression1; expression; return result</code>
	 * <p>For zero parameters, you may omit the => completely.
	 * @param <T> the return type of the function
	 * @param expression the lambda expression
	 * @param bindings the external resources the script might need
	 * @return the function
	 */
	public static <T> Func0<T> js0(String expression, Map<String, Object> bindings) {
		return js0(expression, getEngine("js"), bindings);
	}
	/**
	 * Creates a parameterless function which executes the given script as
	 * javascript. In addition, you may specify extra bindings in the varargs
	 * parameters as [String(name), Object(value)] type, e.g., <code>"ref1", 1, "ref2", 2.0</code>. 
	 * <p>The syntax of the expression follows the generic lambda format used throughout this utility class:</p>
	 * <code>paramName => expression1; expression; return result;</code>
	 * @param <T> the result type
	 * @param expression the lambda expression
	 * @param nameThenObject the String, Object, String, Object, ... sequence of name and value pairs.
	 * @return the function
	 */
	public static <T> Func0<T> js0(String expression, Object... nameThenObject) {
		Map<String, Object> bindings = new HashMap<String, Object>();
		for (int i = 0; i < nameThenObject.length; i += 2) {
			bindings.put((String)nameThenObject[i], nameThenObject[i + 1]);
		}
		return js0(expression, bindings);
	}
	/**
	 * Creates a parameterless function which executes the given script as
	 * javascript. In addition, you may specify extra bindings in the varargs
	 * parameters as [String(name), Object(value)] type, e.g., <code>"ref1", 1, "ref2", 2.0</code>. 
	 * <p>The syntax of the expression follows the generic lambda format used throughout this utility class:</p>
	 * <code>paramName => expression1; expression; return result;</code>
	 * @param <T> the result type
	 * @param expression the lambda expression
	 * @param engine the scripting engine to use
	 * @param nameThenObject the String, Object, String, Object, ... sequence of name and value pairs.
	 * @return the function
	 */
	public static <T> Func0<T> js0(String expression, ScriptEngine engine, Object... nameThenObject) {
		Map<String, Object> bindings = new HashMap<String, Object>();
		for (int i = 0; i < nameThenObject.length; i += 2) {
			bindings.put((String)nameThenObject[i], nameThenObject[i + 1]);
		}
		return js0(expression, engine, bindings);
	}
	/**
	 * Creates a single parameter function which invokes the script engine with a given
	 * expression and returns its result.
	 * <p>The syntax of the expression follows the generic lambda format used throughout this utility class:</p>
	 * <code>paramName => expression1; expression; return result;</code>
	 * @param <T> the result type of the function
	 * @param <U> the parameter type
	 * @param expression the lambda expression
	 * @param engine the engine to run the expression
	 * @param bindings the external resources the script might need
	 * @return the function
	 */
	public static <T, U> Func1<T, U> js1(
			String expression, 
			final ScriptEngine engine,
			final Map<String, Object> bindings) {
		int idx = expression.indexOf("=>");
		if (idx < 0) {
			throw new IllegalArgumentException("Expression missing the lambda indicator =>");
		}
		final String p1 = expression.substring(0, idx).trim();
		final String body = expression.substring(idx + 2);
		return new Func1<T, U>() {
			@Override
			public T invoke(U param1) {
				Bindings b = new SimpleBindings(bindings);
				b.put(p1, param1);
				try {
					@SuppressWarnings("unchecked")
					T result = (T)engine.eval(body, b);
					return result;
				} catch (ScriptException ex) {
					throw new RuntimeException(ex);
				}
			};
		};
	}
	/**
	 * Creates a single parameter function which executes the given script as
	 * javascript. In addition, you may specify extra bindings in the varargs
	 * parameters as [String(name), Object(value)] type, e.g., <code>"ref1", 1, "ref2", 2.0</code>. 
	 * <p>The syntax of the expression follows the generic lambda format used throughout this utility class:</p>
	 * <code>paramName => expression1; expression; return result;</code>
	 * @param <T> the result type
	 * @param <U> the parameter type of the function
	 * @param expression the lambda expression
	 * @param nameThenObject the String, Object, String, Object, ... sequence of name and value pairs.
	 * @return the function
	 */
	public static <T, U> Func1<T, U> js1(String expression, Object... nameThenObject) {
		Map<String, Object> bindings = new HashMap<String, Object>();
		for (int i = 0; i < nameThenObject.length; i += 2) {
			bindings.put((String)nameThenObject[i], nameThenObject[i + 1]);
		}
		return js1(expression, bindings);
	}
	/**
	 * Creates a single parameter function which executes the given script as
	 * javascript. In addition, you may specify extra bindings in the varargs
	 * parameters as [String(name), Object(value)] type, e.g., <code>"ref1", 1, "ref2", 2.0</code>. 
	 * <p>The syntax of the expression follows the generic lambda format used throughout this utility class:</p>
	 * <code>paramName => expression1; expression; return result;</code>
	 * @param <T> the result type
	 * @param <U> the parameter type of the function
	 * @param expression the lambda expression
	 * @param engine the script engine to use
	 * @param nameThenObject the String, Object, String, Object, ... sequence of name and value pairs.
	 * @return the function
	 */
	public static <T, U> Func1<T, U> js1(String expression, ScriptEngine engine, Object... nameThenObject) {
		Map<String, Object> bindings = new HashMap<String, Object>();
		for (int i = 0; i < nameThenObject.length; i += 2) {
			bindings.put((String)nameThenObject[i], nameThenObject[i + 1]);
		}
		return js1(expression, engine, bindings);
	}
	/**
	 * Creates a single parameter function which invokes the script engine with a given
	 * expression and returns its result.
	 * <p>The syntax of the expression follows the generic lambda format used throughout this utility class:</p>
	 * <code>paramName => expression1; expression; return result;</code>
	 * @param <T> the result type of the function
	 * @param <U> the parameter type
	 * @param expression the lambda expression
	 * @return the function
	 */
	public static <T, U> Func1<T, U> js1(String expression) {
		return js1(expression, getEngine("js"), new HashMap<String, Object>());
	}
	/**
	 * Creates a single parameter function which invokes the script engine with a given
	 * expression and returns its result.
	 * <p>The syntax of the expression follows the generic lambda format used throughout this utility class:</p>
	 * <code>paramName => expression1; expression; return result;</code>
	 * @param <T> the result type of the function
	 * @param <U> the parameter type
	 * @param expression the lambda expression
	 * @param bindings the external resources the script might need
	 * @return the function
	 */
	public static <T, U> Func1<T, U> js1(String expression, Map<String, Object> bindings) {
		return js1(expression, getEngine("js"), bindings);
	}
	/**
	 * Creates a two parameter function which invokes the script engine with a given
	 * expression and returns its result.
	 * <p>The syntax of the expression follows the generic lambda format used throughout this utility class:</p>
	 * <code>paramName1, paramName2 => expression1; expression; return result;</code>
	 * @param <T> the result type of the function
	 * @param <U> the first parameter type
	 * @param <V> the second parameter type
	 * @param expression the lambda expression
	 * @param engine the engine to run the expression
	 * @param bindings the external resources the script might need
	 * @return the function
	 */
	public static <T, U, V> Func2<T, U, V> js2(
			String expression, 
			final ScriptEngine engine,
			final Map<String, Object> bindings) {
		int idx = expression.indexOf("=>");
		if (idx < 0) {
			throw new IllegalArgumentException("Expression missing the lambda indicator =>");
		}
		String params = expression.substring(0, idx);
		final String[] paramList = params.split("\\s*,\\s*");
		if (paramList.length != 2) {
			throw new IllegalArgumentException("Specify exactly two parameter names in the expression.");
		}
		final String body = expression.substring(idx + 2);
		return new Func2<T, U, V>() {
			@Override
			public T invoke(U param1, V param2) {
				Bindings b = new SimpleBindings(bindings);
				b.put(paramList[0], param1);
				b.put(paramList[1], param2);
				try {
					@SuppressWarnings("unchecked")
					T result = (T)engine.eval(body, b); 
					return result;
				} catch (ScriptException ex) {
					throw new RuntimeException(ex);
				}
			};
		};
	}
	/**
	 * Creates a two parameter function which invokes the script engine with a given
	 * expression and returns its result.
	 * <p>The syntax of the expression follows the generic lambda format used throughout this utility class:</p>
	 * <code>paramName1, paramName2 => expression1; expression; return result;</code>
	 * @param <T> the result type of the function
	 * @param <U> the first parameter type
	 * @param <V> the second parameter type
	 * @param expression the lambda expression
	 * @return the function
	 */
	public static <T, U, V> Func2<T, U, V> js2(
			String expression) {
		return js2(expression, getEngine("js"), new HashMap<String, Object>());
	}
	/**
	 * Creates a two parameter function which invokes the script engine with a given
	 * expression and returns its result.
	 * <p>The syntax of the expression follows the generic lambda format used throughout this utility class:</p>
	 * <code>paramName1, paramName2 => expression1; expression; return result;</code>
	 * @param <T> the result type of the function
	 * @param <U> the first parameter type
	 * @param <V> the second parameter type
	 * @param expression the lambda expression
	 * @param bindings the external resources the script might need
	 * @return the function
	 */
	public static <T, U, V> Func2<T, U, V> js2(
			String expression, 
			Map<String, Object> bindings) {
		return js2(expression, getEngine("js"), bindings);
	}
	/**
	 * Creates a two parameter function which executes the given script as
	 * javascript. In addition, you may specify extra bindings in the varargs
	 * parameters as [String(name), Object(value)] type, e.g., <code>"ref1", 1, "ref2", 2.0</code>. 
	 * <p>The syntax of the expression follows the generic lambda format used throughout this utility class:</p>
	 * <code>paramName => expression1; expression; return result;</code>
	 * @param <T> the result type
	 * @param <U> the first parameter type
	 * @param <V> the second parameter type
	 * @param expression the lambda expression
	 * @param nameThenObject the String, Object, String, Object, ... sequence of name and value pairs.
	 * @return the function
	 */
	public static <T, U, V> Func2<T, U, V> js2(String expression, Object... nameThenObject) {
		Map<String, Object> bindings = new HashMap<String, Object>();
		for (int i = 0; i < nameThenObject.length; i += 2) {
			bindings.put((String)nameThenObject[i], nameThenObject[i + 1]);
		}
		return js2(expression, bindings);
	}
	/**
	 * Creates a two parameter function which executes the given script as
	 * javascript. In addition, you may specify extra bindings in the varargs
	 * parameters as [String(name), Object(value)] type, e.g., <code>"ref1", 1, "ref2", 2.0</code>. 
	 * <p>The syntax of the expression follows the generic lambda format used throughout this utility class:</p>
	 * <code>paramName => expression1; expression; return result;</code>
	 * @param <T> the result type
	 * @param <U> the first parameter type
	 * @param <V> the second parameter type
	 * @param expression the lambda expression
	 * @param engine the script engine to use
	 * @param nameThenObject the String, Object, String, Object, ... sequence of name and value pairs.
	 * @return the function
	 */
	public static <T, U, V> Func2<T, U, V> js2(String expression, 
			ScriptEngine engine,
			Object... nameThenObject) {
		Map<String, Object> bindings = new HashMap<String, Object>();
		for (int i = 0; i < nameThenObject.length; i += 2) {
			bindings.put((String)nameThenObject[i], nameThenObject[i + 1]);
		}
		return js2(expression, engine, bindings);
	}
	/**
	 * Retrieve a script engine for the given name.
	 * This method is equivalent to <code>new ScriptEngineManager().getEngineByName(name)</code> call.
	 * @param name the script engine name. For javascript, you may use <code>js</code>.
	 * @return the script engine
	 */
	public static ScriptEngine getEngine(String name) {
		return new ScriptEngineManager().getEngineByName(name);
	}
	/**
	 * Test program for scripting capabilities.
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		ScriptEngineManager mgr = new ScriptEngineManager();
		List<ScriptEngineFactory> factories = 
			mgr.getEngineFactories();
		for (ScriptEngineFactory factory : factories) {
			System.out.println("ScriptEngineFactory Info");
			String engName = factory.getEngineName();
			String engVersion = factory.getEngineVersion();
			String langName = factory.getLanguageName();
			String langVersion = factory.getLanguageVersion();
			System.out.printf("\tScript Engine: %s (%s)\n", 
					engName, engVersion);
			List<String> engNames = factory.getNames();
			for (String name : engNames) {
				System.out.printf("\tEngine Alias: %s\n", name);
			}
			System.out.printf("\tLanguage: %s (%s)\n", 
					langName, langVersion);
			System.out.printf("\tThreading: %s%n", factory.getParameter("THREADING"));
		} 
	}
}
