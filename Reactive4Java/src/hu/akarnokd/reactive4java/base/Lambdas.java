/*
 * Copyright 2011-2013 David Karnok
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
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.script.Bindings;
import javax.script.ScriptEngine;
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
 * for further details.<p>
 * @author akarnokd, 2011.02.19.
 */
public final class Lambdas {
	/**
	 * Returns a parameterless action which invokes the given script on the
	 * script engine.
	 * <p>The syntax of the expression follows the generic lambda format used throughout this utility class:</p>
	 * <code>=> expression1; expression;</code>
	 * <p>You may omit the => completely.
	 * @param expression the original lambda expression
	 * @param engine the script engine to use
	 * @param bindings the external resources the script might need
	 * @return the function
	 */
	@Nonnull
	public static Action0 action0(
			@Nonnull String expression,
			@Nonnull final ScriptEngine engine,
			@Nonnull final Map<String, Object> bindings) {
		expression = expression.trim();
		if (expression.startsWith("=>")) {
			expression = expression.substring(3).trim();
		}
		final String body = expression;
		return new Action0() {
			@Override
			public void invoke() {
				try {
					SimpleBindings b = new SimpleBindings(bindings);
					engine.eval(body, b);
				} catch (ScriptException ex) {
					throw new RuntimeException(ex);
				}
			}
		};
	}
	/**
	 * Creates a parameterless action which executes the given script with the
	 * given script engine. In addition, you may specify extra bindings in the varargs
	 * parameters as [String(name), Object(value)] type, e.g., <code>"ref1", 1, "ref2", 2.0</code>.
	 * <p>The syntax of the expression follows the generic lambda format used throughout this utility class:</p>
	 * <code>paramName => expression1; expression; return result;</code>
	 * @param expression the lambda expression
	 * @param engine the scripting engine to use
	 * @param nameThenObject the String, Object, String, Object, ... sequence of name and value pairs.
	 * @return the function
	 */
	@Nonnull
	public static Action0 action0(
			@Nonnull String expression,
			@Nonnull ScriptEngine engine,
			@Nonnull Object... nameThenObject) {
		Map<String, Object> bindings = new HashMap<String, Object>();
		for (int i = 0; i < nameThenObject.length; i += 2) {
			bindings.put((String)nameThenObject[i], nameThenObject[i + 1]);
		}
		return action0(expression, engine, bindings);
	}
	/**
	 * Creates a single parameter action which invokes the script engine with a given
	 * expression.
	 * <p>The syntax of the expression follows the generic lambda format used throughout this utility class:</p>
	 * <code>paramName => expression1; expression;</code>
	 * @param <T> the parameter type of the action
	 * @param expression the lambda expression
	 * @param engine the engine to run the expression
	 * @param bindings the external resources the script might need
	 * @return the function
	 */
	@Nonnull
	public static <T> Action1<T> action1(
			@Nonnull String expression,
			@Nonnull final ScriptEngine engine,
			@Nonnull final Map<String, Object> bindings) {
		int idx = expression.indexOf("=>");
		if (idx < 0) {
			throw new IllegalArgumentException("Expression missing the lambda indicator =>");
		}
		final String p1 = expression.substring(0, idx).trim();
		final String body = expression.substring(idx + 2);
		return new Action1<T>() {
			@Override
			public void invoke(T param1) {
				Bindings b = new SimpleBindings(bindings);
				b.put(p1, param1);
				try {
					engine.eval(body, b);
				} catch (ScriptException ex) {
					throw new RuntimeException(ex);
				}
			}
		};
	}
	/**
	 * Creates a single parameter action which executes the given script on
	 * the script engine. In addition, you may specify extra bindings in the varargs
	 * parameters as [String(name), Object(value)] type, e.g., <code>"ref1", 1, "ref2", 2.0</code>.
	 * <p>The syntax of the expression follows the generic lambda format used throughout this utility class:</p>
	 * <code>paramName => expression1; expression; return result;</code>
	 * @param <T> the parameter type of the action
	 * @param expression the lambda expression
	 * @param engine the script engine to use
	 * @param nameThenObject the String, Object, String, Object, ... sequence of name and value pairs.
	 * @return the function
	 */
	public static <T> Action1<T> action1(
			@Nonnull String expression,
			@Nonnull ScriptEngine engine,
			@Nonnull Object... nameThenObject) {
		Map<String, Object> bindings = new HashMap<String, Object>();
		for (int i = 0; i < nameThenObject.length; i += 2) {
			bindings.put((String)nameThenObject[i], nameThenObject[i + 1]);
		}
		return action1(expression, engine, bindings);
	}
	/**
	 * Creates a parameterless action which executes the given script as JavaScript.
	 * <p>The syntax of the expression follows the generic lambda format used throughout this utility class:</p>
	 * <code>paramName => expression1; expression; return result;</code>
	 * @param expression the lambda expression
	 * @param bindings the extra parameter bingings
	 * @return the function
	 */
	@Nonnull
	public static Action0 as0(
			@Nonnull String expression,
			@Nonnull Map<String, Object> bindings) {
		return action0(expression, getEngine("js"), bindings);
	}
	/**
	 * Creates a parameterless action which executes the given script as
	 * javascript. In addition, you may specify extra bindings in the varargs
	 * parameters as [String(name), Object(value)] type, e.g., <code>"ref1", 1, "ref2", 2.0</code>.
	 * <p>The syntax of the expression follows the generic lambda format used throughout this utility class:</p>
	 * <code>paramName => expression1; expression; return result;</code>
	 * @param expression the lambda expression
	 * @param nameThenObject the String, Object, String, Object, ... sequence of name and value pairs.
	 * @return the function
	 */
	@Nonnull
	public static Action0 as0(
			@Nonnull String expression,
			@Nonnull Object... nameThenObject) {
		return action0(expression, getEngine("js"), nameThenObject);
	}
	/**
	 * Creates a single parameter action which executes the given script as JavaScript.
	 * <p>The syntax of the expression follows the generic lambda format used throughout this utility class:</p>
	 * <code>paramName => expression1; expression; return result;</code>
	 * @param <T> the parameter type of the action
	 * @param expression the lambda expression
	 * @param bindings the extra parameter bindings
	 * @return the function
	 */
	public static <T> Action1<T> as1(
			@Nonnull String expression,
			@Nonnull Map<String, Object> bindings) {
		return action1(expression, getEngine("js"), bindings);
	}
	/**
	 * Creates a single parameter action which executes the given script as JavaScript.
	 * In addition, you may specify extra bindings in the varargs
	 * parameters as [String(name), Object(value)] type, e.g., <code>"ref1", 1, "ref2", 2.0</code>.
	 * <p>The syntax of the expression follows the generic lambda format used throughout this utility class:</p>
	 * <code>paramName => expression1; expression; return result;</code>
	 * @param <T> the parameter type of the action
	 * @param expression the lambda expression
	 * @param nameThenObject the String, Object, String, Object, ... sequence of name and value pairs.
	 * @return the function
	 */
	public static <T> Action1<T> as1(
			@Nonnull String expression,
			@Nonnull Object... nameThenObject) {
		return action1(expression, getEngine("js"), nameThenObject);
	}
	/**
	 * Retrieve a script engine for the given name.
	 * This method is equivalent to <code>new ScriptEngineManager().getEngineByName(name)</code> call.
	 * @param name the script engine name. For javascript, you may use <code>js</code>.
	 * @return the script engine
	 */
	@Nullable
	public static ScriptEngine getEngine(@Nonnull String name) {
		return new ScriptEngineManager().getEngineByName(name);
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
	@Nonnull
	public static <T> Func0<T> js0(@Nonnull String expression) {
		return script0(expression, getEngine("js"), new HashMap<String, Object>());
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
	@Nonnull
	public static <T> Func0<T> js0(
			@Nonnull String expression,
			@Nonnull Map<String, Object> bindings) {
		return script0(expression, getEngine("js"), bindings);
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
	@Nonnull
	public static <T> Func0<T> js0(
			@Nonnull String expression,
			@Nonnull Object... nameThenObject) {
		Map<String, Object> bindings = new HashMap<String, Object>();
		for (int i = 0; i < nameThenObject.length; i += 2) {
			bindings.put((String)nameThenObject[i], nameThenObject[i + 1]);
		}
		return js0(expression, bindings);
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
	@Nonnull
	public static <T, U> Func1<T, U> js1(
			@Nonnull String expression) {
		return script1(expression, getEngine("js"), new HashMap<String, Object>());
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
	@Nonnull
	public static <T, U> Func1<T, U> js1(
			@Nonnull String expression,
			@Nonnull Map<String, Object> bindings) {
		return script1(expression, getEngine("js"), bindings);
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
	@Nonnull
	public static <T, U> Func1<T, U> js1(
			@Nonnull String expression,
			@Nonnull Object... nameThenObject) {
		Map<String, Object> bindings = new HashMap<String, Object>();
		for (int i = 0; i < nameThenObject.length; i += 2) {
			bindings.put((String)nameThenObject[i], nameThenObject[i + 1]);
		}
		return js1(expression, bindings);
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
	@Nonnull
	public static <T, U, V> Func2<T, U, V> js2(
			@Nonnull String expression) {
		return script2(expression, getEngine("js"), new HashMap<String, Object>());
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
	@Nonnull
	public static <T, U, V> Func2<T, U, V> js2(
			@Nonnull String expression,
			@Nonnull Map<String, Object> bindings) {
		return script2(expression, getEngine("js"), bindings);
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
	@Nonnull
	public static <T, U, V> Func2<T, U, V> js2(
			@Nonnull String expression,
			@Nonnull Object... nameThenObject) {
		Map<String, Object> bindings = new HashMap<String, Object>();
		for (int i = 0; i < nameThenObject.length; i += 2) {
			bindings.put((String)nameThenObject[i], nameThenObject[i + 1]);
		}
		return js2(expression, bindings);
	}
//	/**
//	 * Test program for scripting capabilities.
//	 * @param args the arguments
//	 */
//	public static void main(String[] args) {
//		ScriptEngineManager mgr = new ScriptEngineManager();
//		List<ScriptEngineFactory> factories =
//			mgr.getEngineFactories();
//		for (ScriptEngineFactory factory : factories) {
//			System.out.println("ScriptEngineFactory Info");
//			String engName = factory.getEngineName();
//			String engVersion = factory.getEngineVersion();
//			String langName = factory.getLanguageName();
//			String langVersion = factory.getLanguageVersion();
//			System.out.printf("\tScript Engine: %s (%s)\n",
//					engName, engVersion);
//			List<String> engNames = factory.getNames();
//			for (String name : engNames) {
//				System.out.printf("\tEngine Alias: %s\n", name);
//			}
//			System.out.printf("\tLanguage: %s (%s)\n",
//					langName, langVersion);
//			System.out.printf("\tThreading: %s%n", factory.getParameter("THREADING"));
//		}
//	}
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
	@Nonnull
	public static <T> Func0<T> script0(
			@Nonnull String expression,
			@Nonnull final ScriptEngine engine,
			@Nonnull final Map<String, Object> bindings) {
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
	@Nonnull
	public static <T> Func0<T> script0(
			@Nonnull String expression,
			@Nonnull ScriptEngine engine,
			@Nonnull Object... nameThenObject) {
		Map<String, Object> bindings = new HashMap<String, Object>();
		for (int i = 0; i < nameThenObject.length; i += 2) {
			bindings.put((String)nameThenObject[i], nameThenObject[i + 1]);
		}
		return script0(expression, engine, bindings);
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
	@Nonnull
	public static <T, U> Func1<U, T> script1(
			@Nonnull String expression,
			@Nonnull final ScriptEngine engine,
			@Nonnull final Map<String, Object> bindings) {
		int idx = expression.indexOf("=>");
		if (idx < 0) {
			throw new IllegalArgumentException("Expression missing the lambda indicator =>");
		}
		final String p1 = expression.substring(0, idx).trim();
		final String body = expression.substring(idx + 2);
		return new Func1<U, T>() {
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
			}
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
	 * @param engine the script engine to use
	 * @param nameThenObject the String, Object, String, Object, ... sequence of name and value pairs.
	 * @return the function
	 */
	public static <T, U> Func1<T, U> script1(
			@Nonnull String expression,
			@Nonnull ScriptEngine engine,
			@Nonnull Object... nameThenObject) {
		Map<String, Object> bindings = new HashMap<String, Object>();
		for (int i = 0; i < nameThenObject.length; i += 2) {
			bindings.put((String)nameThenObject[i], nameThenObject[i + 1]);
		}
		return script1(expression, engine, bindings);
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
	@Nonnull
	public static <T, U, V> Func2<U, V, T> script2(
			@Nonnull String expression,
			@Nonnull final ScriptEngine engine,
			@Nonnull final Map<String, Object> bindings) {
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
		return new Func2<U, V, T>() {
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
			}
		};
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
	@Nonnull
	public static <T, U, V> Func2<T, U, V> script2(
			@Nonnull String expression,
			@Nonnull ScriptEngine engine,
			@Nonnull Object... nameThenObject) {
		Map<String, Object> bindings = new HashMap<String, Object>();
		for (int i = 0; i < nameThenObject.length; i += 2) {
			bindings.put((String)nameThenObject[i], nameThenObject[i + 1]);
		}
		return script2(expression, engine, bindings);
	}
	/**
	 * Utility class.
	 */
	private Lambdas() {
	}
}
