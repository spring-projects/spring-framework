/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.aop.aspectj;

import java.lang.reflect.Method;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.lang.reflect.SourceLocation;
import org.aspectj.runtime.internal.AroundClosure;

import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.util.Assert;

/**
 * Implementation of AspectJ ProceedingJoinPoint interface
 * wrapping an AOP Alliance MethodInvocation.
 *
 * <p><b>Note</b>: the <code>getThis()</code> method returns the current Spring AOP proxy.
 * The <code>getTarget()</code> method returns the current Spring AOP target (which may be
 * <code>null</code> if there is no target), and is a plain POJO without any advice.
 * <b>If you want to call the object and have the advice take effect, use
 * <code>getThis()</code>.</b> A common example is casting the object to an
 * introduced interface in the implementation of an introduction.
 *
 * <p>Of course there is no such distinction between target and proxy in AspectJ.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Adrian Colyer
 * @since 2.0
 */
public class MethodInvocationProceedingJoinPoint implements ProceedingJoinPoint, JoinPoint.StaticPart {
	
	private final ProxyMethodInvocation methodInvocation;

	private Object[] defensiveCopyOfArgs;

	/** Lazily initialized signature object */
	private Signature signature;

	/** Lazily initialized source location object */
	private SourceLocation sourceLocation;


	/**
	 * Create a new MethodInvocationProceedingJoinPoint, wrapping the given
	 * Spring ProxyMethodInvocation object.
	 * @param methodInvocation the Spring ProxyMethodInvocation object
	 */
	public MethodInvocationProceedingJoinPoint(ProxyMethodInvocation methodInvocation) {
		Assert.notNull(methodInvocation, "MethodInvocation must not be null");
		this.methodInvocation = methodInvocation;
	}

	public void set$AroundClosure(AroundClosure aroundClosure) {
		throw new UnsupportedOperationException();
	}

	public Object proceed() throws Throwable {
		return this.methodInvocation.invocableClone().proceed();
	}

	public Object proceed(Object[] arguments) throws Throwable {
		Assert.notNull(arguments, "Argument array passed to proceed cannot be null");
		if (arguments.length != this.methodInvocation.getArguments().length) {
			throw new IllegalArgumentException("Expecting " +
					this.methodInvocation.getArguments().length + " arguments to proceed, " +
					"but was passed " + arguments.length + " arguments");
		}
		this.methodInvocation.setArguments(arguments);
		return this.methodInvocation.invocableClone(arguments).proceed();
	}

	/**
	 * Returns the Spring AOP proxy. Cannot be <code>null</code>.
	 */
	public Object getThis() {
		return this.methodInvocation.getProxy();
	}

	/**
	 * Returns the Spring AOP target. May be <code>null</code> if there is no target.
	 */
	public Object getTarget() {
		return this.methodInvocation.getThis();
	}

	public Object[] getArgs() {
		if (this.defensiveCopyOfArgs == null) {
			Object[] argsSource = this.methodInvocation.getArguments();
			this.defensiveCopyOfArgs = new Object[argsSource.length];
			System.arraycopy(argsSource, 0, this.defensiveCopyOfArgs, 0, argsSource.length);
		}
		return this.defensiveCopyOfArgs;
	}

	public Signature getSignature() {
		if (this.signature == null) {
			this.signature = new MethodSignatureImpl();
		}
		return signature;
	}

	public SourceLocation getSourceLocation() {
		if (this.sourceLocation == null) {
			this.sourceLocation = new SourceLocationImpl();
		}
		return this.sourceLocation;
	}

	public String getKind() {
		return ProceedingJoinPoint.METHOD_EXECUTION;
	}

	public JoinPoint.StaticPart getStaticPart() {
		return this;
	}
	

	public String toShortString() {
		return "execution(" + this.methodInvocation.getMethod().getName() + ")";
	}

	public String toLongString() {
		return getClass().getName() + ": execution: [" + this.methodInvocation + "]";
	}

	public String toString() {
		return getClass().getName() + ": " + toShortString();
	}


	/**
	 * Lazily initialized MethodSignature.
	 */
	private class MethodSignatureImpl implements Signature, MethodSignature {

		public String toShortString() {
			return methodInvocation.getMethod().getName();
		}

		public String toLongString() {
			return methodInvocation.getMethod().toString();
		}

		public String getName() {
			return methodInvocation.getMethod().getName();
		}

		public int getModifiers() {
			return methodInvocation.getMethod().getModifiers();
		}

		public Class getDeclaringType() {
			return methodInvocation.getMethod().getDeclaringClass();
		}

		public String getDeclaringTypeName() {
			return methodInvocation.getMethod().getDeclaringClass().getName();
		}

		public Class getReturnType() {
			return methodInvocation.getMethod().getReturnType();
		}

		public Method getMethod() {
			return methodInvocation.getMethod();
		}

		public Class[] getParameterTypes() {
			return methodInvocation.getMethod().getParameterTypes();
		}

		public String[] getParameterNames() {
			// TODO consider allowing use of ParameterNameDiscoverer, or tying into
			// parameter names exposed for argument binding...
			throw new UnsupportedOperationException(
					"Parameter names cannot be determined unless compiled by AspectJ compiler");
		}

		public Class[] getExceptionTypes() {
			return methodInvocation.getMethod().getExceptionTypes();
		}
	}


	/**
	 * Lazily initialized SourceLocation.
	 */
	private class SourceLocationImpl implements SourceLocation {

		public Class getWithinType() {
			if (methodInvocation.getThis() == null) {
				throw new UnsupportedOperationException("No source location joinpoint available: target is null");
			}
			return methodInvocation.getThis().getClass();
		}

		public String getFileName() {
			throw new UnsupportedOperationException();
		}

		public int getLine() {
			throw new UnsupportedOperationException();
		}

		public int getColumn() {
			throw new UnsupportedOperationException();
		}
	}

}
