/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.scripting.jruby;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyException;
import org.jruby.RubyNil;
import org.jruby.ast.ClassNode;
import org.jruby.ast.Colon2Node;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.Node;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;

import org.springframework.core.NestedRuntimeException;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Utility methods for handling JRuby-scripted objects.
 *
 * <p>As of Spring 3.0, this class requires JRuby 1.1 or higher.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rick Evans
 * @since 2.0
 */
public abstract class JRubyScriptUtils {

	/**
	 * Create a new JRuby-scripted object from the given script source,
	 * using the default {@link ClassLoader}.
	 * @param scriptSource the script source text
	 * @param interfaces the interfaces that the scripted Java object is to implement
	 * @return the scripted Java object
	 * @throws JumpException in case of JRuby parsing failure
	 * @see ClassUtils#getDefaultClassLoader()
	 */
	public static Object createJRubyObject(String scriptSource, Class[] interfaces) throws JumpException {
		return createJRubyObject(scriptSource, interfaces, ClassUtils.getDefaultClassLoader());
	}

	/**
	 * Create a new JRuby-scripted object from the given script source.
	 * @param scriptSource the script source text
	 * @param interfaces the interfaces that the scripted Java object is to implement
	 * @param classLoader the {@link ClassLoader} to create the script proxy with
	 * @return the scripted Java object
	 * @throws JumpException in case of JRuby parsing failure
	 */
	@SuppressWarnings("deprecation")
	public static Object createJRubyObject(String scriptSource, Class[] interfaces, ClassLoader classLoader) {
		Ruby ruby = initializeRuntime();

		Node scriptRootNode = ruby.parseEval(scriptSource, "", null, 0);
		// keep using the deprecated runNormally variant for JRuby 1.1/1.2 compatibility...
        IRubyObject rubyObject = ruby.runNormally(scriptRootNode, false);

		if (rubyObject instanceof RubyNil) {
			String className = findClassName(scriptRootNode);
			rubyObject = ruby.evalScriptlet("\n" + className + ".new");
		}
		// still null?
		if (rubyObject instanceof RubyNil) {
			throw new IllegalStateException("Compilation of JRuby script returned RubyNil: " + rubyObject);
		}

		return Proxy.newProxyInstance(classLoader, interfaces, new RubyObjectInvocationHandler(rubyObject, ruby));
	}

	/**
	 * Initializes an instance of the {@link org.jruby.Ruby} runtime.
	 */
	private static Ruby initializeRuntime() {
		return JavaEmbedUtils.initialize(Collections.EMPTY_LIST);
	}

	/**
	 * Given the root {@link Node} in a JRuby AST will locate the name of the
	 * class defined by that AST.
	 * @throws IllegalArgumentException if no class is defined by the supplied AST
	 */
	private static String findClassName(Node rootNode) {
		ClassNode classNode = findClassNode(rootNode);
		if (classNode == null) {
			throw new IllegalArgumentException("Unable to determine class name for root node '" + rootNode + "'");
		}
		Colon2Node node = (Colon2Node) classNode.getCPath();
		return node.getName();
	}

	/**
	 * Find the first {@link ClassNode} under the supplied {@link Node}.
	 * @return the found {@code ClassNode}, or {@code null}
	 * if no {@link ClassNode} is found
	 */
	private static ClassNode findClassNode(Node node) {
		if (node instanceof ClassNode) {
			return (ClassNode) node;
		}
		List<Node> children = node.childNodes();
		for (Node child : children) {
			if (child instanceof ClassNode) {
				return (ClassNode) child;
			}
			else if (child instanceof NewlineNode) {
				NewlineNode nn = (NewlineNode) child;
				Node found = findClassNode(nn.getNextNode());
				if (found instanceof ClassNode) {
					return (ClassNode) found;
				}
			}
		}
		for (Node child : children) {
			Node found = findClassNode(child);
			if (found instanceof ClassNode) {
				return (ClassNode) found;
			}
		}
		return null;
	}


	/**
	 * InvocationHandler that invokes a JRuby script method.
	 */
	private static class RubyObjectInvocationHandler implements InvocationHandler {

		private final IRubyObject rubyObject;

		private final Ruby ruby;

		public RubyObjectInvocationHandler(IRubyObject rubyObject, Ruby ruby) {
			this.rubyObject = rubyObject;
			this.ruby = ruby;
		}

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (ReflectionUtils.isEqualsMethod(method)) {
				return (isProxyForSameRubyObject(args[0]));
			}
			else if (ReflectionUtils.isHashCodeMethod(method)) {
				return this.rubyObject.hashCode();
			}
			else if (ReflectionUtils.isToStringMethod(method)) {
				String toStringResult = this.rubyObject.toString();
				if (!StringUtils.hasText(toStringResult)) {
					toStringResult = ObjectUtils.identityToString(this.rubyObject);
				}
				return "JRuby object [" + toStringResult + "]";
			}
			try {
				IRubyObject[] rubyArgs = convertToRuby(args);
				IRubyObject rubyResult =
						this.rubyObject.callMethod(this.ruby.getCurrentContext(), method.getName(), rubyArgs);
				return convertFromRuby(rubyResult, method.getReturnType());
			}
			catch (RaiseException ex) {
				throw new JRubyExecutionException(ex);
			}
		}

		private boolean isProxyForSameRubyObject(Object other) {
			if (!Proxy.isProxyClass(other.getClass())) {
				return false;
			}
			InvocationHandler ih = Proxy.getInvocationHandler(other);
			return (ih instanceof RubyObjectInvocationHandler &&
					this.rubyObject.equals(((RubyObjectInvocationHandler) ih).rubyObject));
		}

		private IRubyObject[] convertToRuby(Object[] javaArgs) {
			if (javaArgs == null || javaArgs.length == 0) {
				return new IRubyObject[0];
			}
			IRubyObject[] rubyArgs = new IRubyObject[javaArgs.length];
			for (int i = 0; i < javaArgs.length; ++i) {
				rubyArgs[i] = JavaEmbedUtils.javaToRuby(this.ruby, javaArgs[i]);
			}
			return rubyArgs;
		}

		private Object convertFromRuby(IRubyObject rubyResult, Class returnType) {
			Object result = JavaEmbedUtils.rubyToJava(this.ruby, rubyResult, returnType);
			if (result instanceof RubyArray && returnType.isArray()) {
				result = convertFromRubyArray(((RubyArray) result).toJavaArray(), returnType);
			}
			return result;
		}

		private Object convertFromRubyArray(IRubyObject[] rubyArray, Class returnType) {
			Class targetType = returnType.getComponentType();
			Object javaArray = Array.newInstance(targetType, rubyArray.length);
			for (int i = 0; i < rubyArray.length; i++) {
				IRubyObject rubyObject = rubyArray[i];
				Array.set(javaArray, i, convertFromRuby(rubyObject, targetType));
			}
			return javaArray;
		}
	}


	/**
	 * Exception thrown in response to a JRuby {@link RaiseException}
	 * being thrown from a JRuby method invocation.
	 * <p>Introduced because the {@code RaiseException} class does not
	 * have useful {@link Object#toString()}, {@link Throwable#getMessage()},
	 * and {@link Throwable#printStackTrace} implementations.
	 */
	@SuppressWarnings("serial")
	public static class JRubyExecutionException extends NestedRuntimeException {

		/**
		 * Create a new {@code JRubyException},
		 * wrapping the given JRuby {@code RaiseException}.
		 * @param ex the cause (must not be {@code null})
		 */
		public JRubyExecutionException(RaiseException ex) {
			super(buildMessage(ex), ex);
		}

		private static String buildMessage(RaiseException ex) {
			RubyException rubyEx = ex.getException();
			return (rubyEx != null && rubyEx.message != null) ? rubyEx.message.toString() : "Unexpected JRuby error";
		}
	}

}
