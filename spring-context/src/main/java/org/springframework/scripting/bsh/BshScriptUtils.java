/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.scripting.bsh;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.Primitive;
import bsh.XThis;

import org.springframework.core.NestedRuntimeException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Utility methods for handling BeanShell-scripted objects.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public abstract class BshScriptUtils {

	/**
	 * Create a new BeanShell-scripted object from the given script source.
	 * <p>With this {@code createBshObject} variant, the script needs to
	 * declare a full class or return an actual instance of the scripted object.
	 * @param scriptSource the script source text
	 * @return the scripted Java object
	 * @throws EvalError in case of BeanShell parsing failure
	 */
	public static Object createBshObject(String scriptSource) throws EvalError {
		return createBshObject(scriptSource, null, null);
	}

	/**
	 * Create a new BeanShell-scripted object from the given script source,
	 * using the default ClassLoader.
	 * <p>The script may either be a simple script that needs a corresponding proxy
	 * generated (implementing the specified interfaces), or declare a full class
	 * or return an actual instance of the scripted object (in which case the
	 * specified interfaces, if any, need to be implemented by that class/instance).
	 * @param scriptSource the script source text
	 * @param scriptInterfaces the interfaces that the scripted Java object is
	 * supposed to implement (may be {@code null} or empty if the script itself
	 * declares a full class or returns an actual instance of the scripted object)
	 * @return the scripted Java object
	 * @throws EvalError in case of BeanShell parsing failure
	 * @see #createBshObject(String, Class[], ClassLoader)
	 */
	public static Object createBshObject(String scriptSource, @Nullable Class<?>... scriptInterfaces) throws EvalError {
		return createBshObject(scriptSource, scriptInterfaces, ClassUtils.getDefaultClassLoader());
	}

	/**
	 * Create a new BeanShell-scripted object from the given script source.
	 * <p>The script may either be a simple script that needs a corresponding proxy
	 * generated (implementing the specified interfaces), or declare a full class
	 * or return an actual instance of the scripted object (in which case the
	 * specified interfaces, if any, need to be implemented by that class/instance).
	 * @param scriptSource the script source text
	 * @param scriptInterfaces the interfaces that the scripted Java object is
	 * supposed to implement (may be {@code null} or empty if the script itself
	 * declares a full class or returns an actual instance of the scripted object)
	 * @param classLoader the ClassLoader to use for evaluating the script
	 * @return the scripted Java object
	 * @throws EvalError in case of BeanShell parsing failure
	 */
	public static Object createBshObject(String scriptSource, @Nullable Class<?>[] scriptInterfaces, @Nullable ClassLoader classLoader)
			throws EvalError {

		Object result = evaluateBshScript(scriptSource, scriptInterfaces, classLoader);
		if (result instanceof Class) {
			Class<?> clazz = (Class<?>) result;
			try {
				return ReflectionUtils.accessibleConstructor(clazz).newInstance();
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Could not instantiate script class: " + clazz.getName(), ex);
			}
		}
		else {
			return result;
		}
	}

	/**
	 * Evaluate the specified BeanShell script based on the given script source,
	 * returning the Class defined by the script.
	 * <p>The script may either declare a full class or return an actual instance of
	 * the scripted object (in which case the Class of the object will be returned).
	 * In any other case, the returned Class will be {@code null}.
	 * @param scriptSource the script source text
	 * @param classLoader the ClassLoader to use for evaluating the script
	 * @return the scripted Java class, or {@code null} if none could be determined
	 * @throws EvalError in case of BeanShell parsing failure
	 */
	@Nullable
	static Class<?> determineBshObjectType(String scriptSource, @Nullable ClassLoader classLoader) throws EvalError {
		Assert.hasText(scriptSource, "Script source must not be empty");
		Interpreter interpreter = new Interpreter();
		if (classLoader != null) {
			interpreter.setClassLoader(classLoader);
		}
		Object result = interpreter.eval(scriptSource);
		if (result instanceof Class) {
			return (Class<?>) result;
		}
		else if (result != null) {
			return result.getClass();
		}
		else {
			return null;
		}
	}

	/**
	 * Evaluate the specified BeanShell script based on the given script source,
	 * keeping a returned script Class or script Object as-is.
	 * <p>The script may either be a simple script that needs a corresponding proxy
	 * generated (implementing the specified interfaces), or declare a full class
	 * or return an actual instance of the scripted object (in which case the
	 * specified interfaces, if any, need to be implemented by that class/instance).
	 * @param scriptSource the script source text
	 * @param scriptInterfaces the interfaces that the scripted Java object is
	 * supposed to implement (may be {@code null} or empty if the script itself
	 * declares a full class or returns an actual instance of the scripted object)
	 * @param classLoader the ClassLoader to use for evaluating the script
	 * @return the scripted Java class or Java object
	 * @throws EvalError in case of BeanShell parsing failure
	 */
	static Object evaluateBshScript(
			String scriptSource, @Nullable Class<?>[] scriptInterfaces, @Nullable ClassLoader classLoader)
			throws EvalError {

		Assert.hasText(scriptSource, "Script source must not be empty");
		Interpreter interpreter = new Interpreter();
		interpreter.setClassLoader(classLoader);
		Object result = interpreter.eval(scriptSource);
		if (result != null) {
			return result;
		}
		else {
			// Simple BeanShell script: Let's create a proxy for it, implementing the given interfaces.
			if (ObjectUtils.isEmpty(scriptInterfaces)) {
				throw new IllegalArgumentException("Given script requires a script proxy: " +
						"At least one script interface is required.\nScript: " + scriptSource);
			}
			XThis xt = (XThis) interpreter.eval("return this");
			return Proxy.newProxyInstance(classLoader, scriptInterfaces, new BshObjectInvocationHandler(xt));
		}
	}


	/**
	 * InvocationHandler that invokes a BeanShell script method.
	 */
	private static class BshObjectInvocationHandler implements InvocationHandler {

		private final XThis xt;

		public BshObjectInvocationHandler(XThis xt) {
			this.xt = xt;
		}

		@Override
		@Nullable
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (ReflectionUtils.isEqualsMethod(method)) {
				return (isProxyForSameBshObject(args[0]));
			}
			else if (ReflectionUtils.isHashCodeMethod(method)) {
				return this.xt.hashCode();
			}
			else if (ReflectionUtils.isToStringMethod(method)) {
				return "BeanShell object [" + this.xt + "]";
			}
			try {
				Object result = this.xt.invokeMethod(method.getName(), args);
				if (result == Primitive.NULL || result == Primitive.VOID) {
					return null;
				}
				if (result instanceof Primitive) {
					return ((Primitive) result).getValue();
				}
				return result;
			}
			catch (EvalError ex) {
				throw new BshExecutionException(ex);
			}
		}

		private boolean isProxyForSameBshObject(Object other) {
			if (!Proxy.isProxyClass(other.getClass())) {
				return false;
			}
			InvocationHandler ih = Proxy.getInvocationHandler(other);
			return (ih instanceof BshObjectInvocationHandler &&
					this.xt.equals(((BshObjectInvocationHandler) ih).xt));
		}
	}


	/**
	 * Exception to be thrown on script execution failure.
	 */
	@SuppressWarnings("serial")
	public static final class BshExecutionException extends NestedRuntimeException {

		private BshExecutionException(EvalError ex) {
			super("BeanShell script execution failed", ex);
		}
	}

}
