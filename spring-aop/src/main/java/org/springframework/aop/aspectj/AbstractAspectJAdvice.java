/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.aop.aspectj;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.weaver.tools.JoinPointMatch;
import org.aspectj.weaver.tools.PointcutParameter;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.AopInvocationException;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.MethodMatchers;
import org.springframework.aop.support.StaticMethodMatcher;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.lang.Contract;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Base class for AOP Alliance {@link org.aopalliance.aop.Advice} classes
 * wrapping an AspectJ aspect or an AspectJ-annotated advice method.
 *
 * @author Rod Johnson
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @since 2.0
 */
@SuppressWarnings("serial")
public abstract class AbstractAspectJAdvice implements Advice, AspectJPrecedenceInformation, Serializable {

	/**
	 * Key used in ReflectiveMethodInvocation userAttributes map for the current joinpoint.
	 */
	protected static final String JOIN_POINT_KEY = JoinPoint.class.getName();


	/**
	 * Lazily instantiate joinpoint for the current invocation.
	 * Requires MethodInvocation to be bound with ExposeInvocationInterceptor.
	 * <p>Do not use if access is available to the current ReflectiveMethodInvocation
	 * (in an around advice).
	 * @return current AspectJ joinpoint, or through an exception if we're not in a
	 * Spring AOP invocation.
	 */
	public static JoinPoint currentJoinPoint() {
		MethodInvocation mi = ExposeInvocationInterceptor.currentInvocation();
		if (!(mi instanceof ProxyMethodInvocation pmi)) {
			throw new IllegalStateException("MethodInvocation is not a Spring ProxyMethodInvocation: " + mi);
		}
		JoinPoint jp = (JoinPoint) pmi.getUserAttribute(JOIN_POINT_KEY);
		if (jp == null) {
			jp = new MethodInvocationProceedingJoinPoint(pmi);
			pmi.setUserAttribute(JOIN_POINT_KEY, jp);
		}
		return jp;
	}


	private final Class<?> declaringClass;

	private final String methodName;

	private final Class<?>[] parameterTypes;

	protected transient Method aspectJAdviceMethod;

	private final AspectJExpressionPointcut pointcut;

	private final AspectInstanceFactory aspectInstanceFactory;

	/**
	 * The name of the aspect (ref bean) in which this advice was defined
	 * (used when determining advice precedence so that we can determine
	 * whether two pieces of advice come from the same aspect).
	 */
	private String aspectName = "";

	/**
	 * The order of declaration of this advice within the aspect.
	 */
	private int declarationOrder;

	/**
	 * This will be non-null if the creator of this advice object knows the argument names
	 * and sets them explicitly.
	 */
	private @Nullable String @Nullable [] argumentNames;

	/** Non-null if after throwing advice binds the thrown value. */
	private @Nullable String throwingName;

	/** Non-null if after returning advice binds the return value. */
	private @Nullable String returningName;

	private Class<?> discoveredReturningType = Object.class;

	private Class<?> discoveredThrowingType = Object.class;

	/**
	 * Index for thisJoinPoint argument (currently only
	 * supported at index 0 if present at all).
	 */
	private int joinPointArgumentIndex = -1;

	/**
	 * Index for thisJoinPointStaticPart argument (currently only
	 * supported at index 0 if present at all).
	 */
	private int joinPointStaticPartArgumentIndex = -1;

	private @Nullable Map<String, Integer> argumentBindings;

	private boolean argumentsIntrospected = false;

	private @Nullable Type discoveredReturningGenericType;
	// Note: Unlike return type, no such generic information is needed for the throwing type,
	// since Java doesn't allow exception types to be parameterized.


	/**
	 * Create a new AbstractAspectJAdvice for the given advice method.
	 * @param aspectJAdviceMethod the AspectJ-style advice method
	 * @param pointcut the AspectJ expression pointcut
	 * @param aspectInstanceFactory the factory for aspect instances
	 */
	public AbstractAspectJAdvice(
			Method aspectJAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aspectInstanceFactory) {

		Assert.notNull(aspectJAdviceMethod, "Advice method must not be null");
		this.declaringClass = aspectJAdviceMethod.getDeclaringClass();
		this.methodName = aspectJAdviceMethod.getName();
		this.parameterTypes = aspectJAdviceMethod.getParameterTypes();
		this.aspectJAdviceMethod = aspectJAdviceMethod;
		this.pointcut = pointcut;
		this.aspectInstanceFactory = aspectInstanceFactory;
	}


	/**
	 * Return the AspectJ-style advice method.
	 */
	public final Method getAspectJAdviceMethod() {
		return this.aspectJAdviceMethod;
	}

	/**
	 * Return the AspectJ expression pointcut.
	 */
	public final AspectJExpressionPointcut getPointcut() {
		calculateArgumentBindings();
		return this.pointcut;
	}

	/**
	 * Build a 'safe' pointcut that excludes the AspectJ advice method itself.
	 * @return a composable pointcut that builds on the original AspectJ expression pointcut
	 * @see #getPointcut()
	 */
	public final Pointcut buildSafePointcut() {
		Pointcut pc = getPointcut();
		MethodMatcher safeMethodMatcher = MethodMatchers.intersection(
				new AdviceExcludingMethodMatcher(this.aspectJAdviceMethod), pc.getMethodMatcher());
		return new ComposablePointcut(pc.getClassFilter(), safeMethodMatcher);
	}

	/**
	 * Return the factory for aspect instances.
	 */
	public final AspectInstanceFactory getAspectInstanceFactory() {
		return this.aspectInstanceFactory;
	}

	/**
	 * Return the ClassLoader for aspect instances.
	 */
	public final @Nullable ClassLoader getAspectClassLoader() {
		return this.aspectInstanceFactory.getAspectClassLoader();
	}

	@Override
	public int getOrder() {
		return this.aspectInstanceFactory.getOrder();
	}


	/**
	 * Set the name of the aspect (bean) in which the advice was declared.
	 */
	public void setAspectName(String name) {
		this.aspectName = name;
	}

	@Override
	public String getAspectName() {
		return this.aspectName;
	}

	/**
	 * Set the declaration order of this advice within the aspect.
	 */
	public void setDeclarationOrder(int order) {
		this.declarationOrder = order;
	}

	@Override
	public int getDeclarationOrder() {
		return this.declarationOrder;
	}

	/**
	 * Set by the creator of this advice object if the argument names are known.
	 * <p>This could be for example because they have been explicitly specified in XML
	 * or in an advice annotation.
	 * @param argumentNames comma delimited list of argument names
	 */
	public void setArgumentNames(String argumentNames) {
		String[] tokens = StringUtils.commaDelimitedListToStringArray(argumentNames);
		setArgumentNamesFromStringArray(tokens);
	}

	/**
	 * Set by the creator of this advice object if the argument names are known.
	 * <p>This could be for example because they have been explicitly specified in XML
	 * or in an advice annotation.
	 * @param argumentNames list of argument names
	 */
	public void setArgumentNamesFromStringArray(@Nullable String... argumentNames) {
		this.argumentNames = new String[argumentNames.length];
		for (int i = 0; i < argumentNames.length; i++) {
			String argumentName = argumentNames[i];
			this.argumentNames[i] = argumentName != null ? argumentName.strip() : null;
			if (!isVariableName(this.argumentNames[i])) {
				throw new IllegalArgumentException(
						"'argumentNames' property of AbstractAspectJAdvice contains an argument name '" +
						this.argumentNames[i] + "' that is not a valid Java identifier");
			}
		}
		if (this.aspectJAdviceMethod.getParameterCount() == this.argumentNames.length + 1) {
			// May need to add implicit join point arg name...
			for (int i = 0; i < this.aspectJAdviceMethod.getParameterCount(); i++) {
				Class<?> argType = this.aspectJAdviceMethod.getParameterTypes()[i];
				if (argType == JoinPoint.class ||
						argType == ProceedingJoinPoint.class ||
						argType == JoinPoint.StaticPart.class) {
					@Nullable String[] oldNames = this.argumentNames;
				this.argumentNames = new String[oldNames.length + 1];
				System.arraycopy(oldNames, 0, this.argumentNames, 0, i);
					this.argumentNames[i] = "THIS_JOIN_POINT";
					System.arraycopy(oldNames, i, this.argumentNames, i + 1, oldNames.length - i);
					break;
				}
			}
		}
	}

	public void setReturningName(String name) {
		throw new UnsupportedOperationException("Only afterReturning advice can be used to bind a return value");
	}

	/**
	 * We need to hold the returning name at this level for argument binding calculations,
	 * this method allows the afterReturning advice subclass to set the name.
	 */
	protected void setReturningNameNoCheck(String name) {
		// name could be a variable or a type...
		if (isVariableName(name)) {
			this.returningName = name;
		}
		else {
			// assume a type
			try {
				this.discoveredReturningType = ClassUtils.forName(name, getAspectClassLoader());
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException("Returning name '" + name +
						"' is neither a valid argument name nor the fully-qualified " +
						"name of a Java type on the classpath. Root cause: " + ex);
			}
		}
	}

	protected Class<?> getDiscoveredReturningType() {
		return this.discoveredReturningType;
	}

	protected @Nullable Type getDiscoveredReturningGenericType() {
		return this.discoveredReturningGenericType;
	}

	public void setThrowingName(String name) {
		throw new UnsupportedOperationException("Only afterThrowing advice can be used to bind a thrown exception");
	}

	/**
	 * We need to hold the throwing name at this level for argument binding calculations,
	 * this method allows the afterThrowing advice subclass to set the name.
	 */
	protected void setThrowingNameNoCheck(String name) {
		// name could be a variable or a type...
		if (isVariableName(name)) {
			this.throwingName = name;
		}
		else {
			// assume a type
			try {
				this.discoveredThrowingType = ClassUtils.forName(name, getAspectClassLoader());
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException("Throwing name '" + name +
						"' is neither a valid argument name nor the fully-qualified " +
						"name of a Java type on the classpath. Root cause: " + ex);
			}
		}
	}

	protected Class<?> getDiscoveredThrowingType() {
		return this.discoveredThrowingType;
	}

	@Contract("null -> false")
	private static boolean isVariableName(@Nullable String name) {
		return AspectJProxyUtils.isVariableName(name);
	}


	/**
	 * Do as much work as we can as part of the set-up so that argument binding
	 * on subsequent advice invocations can be as fast as possible.
	 * <p>If the first argument is of type JoinPoint or ProceedingJoinPoint then we
	 * pass a JoinPoint in that position (ProceedingJoinPoint for around advice).
	 * <p>If the first argument is of type {@code JoinPoint.StaticPart}
	 * then we pass a {@code JoinPoint.StaticPart} in that position.
	 * <p>Remaining arguments have to be bound by pointcut evaluation at
	 * a given join point. We will get back a map from argument name to
	 * value. We need to calculate which advice parameter needs to be bound
	 * to which argument name. There are multiple strategies for determining
	 * this binding, which are arranged in a ChainOfResponsibility.
	 */
	public final void calculateArgumentBindings() {
		// The simple case... nothing to bind.
		if (this.argumentsIntrospected || this.parameterTypes.length == 0) {
			return;
		}

		int numUnboundArgs = this.parameterTypes.length;
		Class<?>[] parameterTypes = this.aspectJAdviceMethod.getParameterTypes();
		if (maybeBindJoinPoint(parameterTypes[0]) || maybeBindProceedingJoinPoint(parameterTypes[0]) ||
				maybeBindJoinPointStaticPart(parameterTypes[0])) {
			numUnboundArgs--;
		}

		if (numUnboundArgs > 0) {
			// need to bind arguments by name as returned from the pointcut match
			bindArgumentsByName(numUnboundArgs);
		}

		this.argumentsIntrospected = true;
	}

	private boolean maybeBindJoinPoint(Class<?> candidateParameterType) {
		if (JoinPoint.class == candidateParameterType) {
			this.joinPointArgumentIndex = 0;
			return true;
		}
		else {
			return false;
		}
	}

	private boolean maybeBindProceedingJoinPoint(Class<?> candidateParameterType) {
		if (ProceedingJoinPoint.class == candidateParameterType) {
			if (!supportsProceedingJoinPoint()) {
				throw new IllegalArgumentException("ProceedingJoinPoint is only supported for around advice");
			}
			this.joinPointArgumentIndex = 0;
			return true;
		}
		else {
			return false;
		}
	}

	protected boolean supportsProceedingJoinPoint() {
		return false;
	}

	private boolean maybeBindJoinPointStaticPart(Class<?> candidateParameterType) {
		if (JoinPoint.StaticPart.class == candidateParameterType) {
			this.joinPointStaticPartArgumentIndex = 0;
			return true;
		}
		else {
			return false;
		}
	}

	private void bindArgumentsByName(int numArgumentsExpectingToBind) {
		if (this.argumentNames == null) {
			this.argumentNames = createParameterNameDiscoverer().getParameterNames(this.aspectJAdviceMethod);
		}
		if (this.argumentNames != null) {
			// We have been able to determine the arg names.
			bindExplicitArguments(numArgumentsExpectingToBind);
		}
		else {
			throw new IllegalStateException("Advice method [" + this.aspectJAdviceMethod.getName() + "] " +
					"requires " + numArgumentsExpectingToBind + " arguments to be bound by name, but " +
					"the argument names were not specified and could not be discovered.");
		}
	}

	/**
	 * Create a ParameterNameDiscoverer to be used for argument binding.
	 * <p>The default implementation creates a {@link DefaultParameterNameDiscoverer}
	 * and adds a specifically configured {@link AspectJAdviceParameterNameDiscoverer}.
	 */
	protected ParameterNameDiscoverer createParameterNameDiscoverer() {
		// We need to discover them, or if that fails, guess,
		// and if we can't guess with 100% accuracy, fail.
		DefaultParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();
		AspectJAdviceParameterNameDiscoverer adviceParameterNameDiscoverer =
				new AspectJAdviceParameterNameDiscoverer(this.pointcut.getExpression());
		adviceParameterNameDiscoverer.setReturningName(this.returningName);
		adviceParameterNameDiscoverer.setThrowingName(this.throwingName);
		// Last in chain, so if we're called and we fail, that's bad...
		adviceParameterNameDiscoverer.setRaiseExceptions(true);
		discoverer.addDiscoverer(adviceParameterNameDiscoverer);
		return discoverer;
	}

	@SuppressWarnings("NullAway") // Dataflow analysis limitation
	private void bindExplicitArguments(int numArgumentsLeftToBind) {
		Assert.state(this.argumentNames != null, "No argument names available");
		this.argumentBindings = new HashMap<>();

		int numExpectedArgumentNames = this.aspectJAdviceMethod.getParameterCount();
		if (this.argumentNames.length != numExpectedArgumentNames) {
			throw new IllegalStateException("Expecting to find " + numExpectedArgumentNames +
					" arguments to bind by name in advice, but actually found " +
					this.argumentNames.length + " arguments.");
		}

		// So we match in number...
		int argumentIndexOffset = this.parameterTypes.length - numArgumentsLeftToBind;
		for (int i = argumentIndexOffset; i < this.argumentNames.length; i++) {
			this.argumentBindings.put(this.argumentNames[i], i);
		}

		// Check that returning and throwing were in the argument names list if
		// specified, and find the discovered argument types.
		if (this.returningName != null) {
			if (!this.argumentBindings.containsKey(this.returningName)) {
				throw new IllegalStateException("Returning argument name '" + this.returningName +
						"' was not bound in advice arguments");
			}
			else {
				Integer index = this.argumentBindings.get(this.returningName);
				this.discoveredReturningType = this.aspectJAdviceMethod.getParameterTypes()[index];
				this.discoveredReturningGenericType = this.aspectJAdviceMethod.getGenericParameterTypes()[index];
			}
		}
		if (this.throwingName != null) {
			if (!this.argumentBindings.containsKey(this.throwingName)) {
				throw new IllegalStateException("Throwing argument name '" + this.throwingName +
						"' was not bound in advice arguments");
			}
			else {
				Integer index = this.argumentBindings.get(this.throwingName);
				this.discoveredThrowingType = this.aspectJAdviceMethod.getParameterTypes()[index];
			}
		}

		// configure the pointcut expression accordingly.
		configurePointcutParameters(this.argumentNames, argumentIndexOffset);
	}

	/**
	 * All parameters from argumentIndexOffset onwards are candidates for
	 * pointcut parameters - but returning and throwing vars are handled differently
	 * and must be removed from the list if present.
	 */
	private void configurePointcutParameters(String[] argumentNames, int argumentIndexOffset) {
		int numParametersToRemove = argumentIndexOffset;
		if (this.returningName != null) {
			numParametersToRemove++;
		}
		if (this.throwingName != null) {
			numParametersToRemove++;
		}
		String[] pointcutParameterNames = new String[argumentNames.length - numParametersToRemove];
		Class<?>[] pointcutParameterTypes = new Class<?>[pointcutParameterNames.length];
		Class<?>[] methodParameterTypes = this.aspectJAdviceMethod.getParameterTypes();

		int index = 0;
		for (int i = 0; i < argumentNames.length; i++) {
			if (i < argumentIndexOffset) {
				continue;
			}
			if (argumentNames[i].equals(this.returningName) ||
				argumentNames[i].equals(this.throwingName)) {
				continue;
			}
			pointcutParameterNames[index] = argumentNames[i];
			pointcutParameterTypes[index] = methodParameterTypes[i];
			index++;
		}

		this.pointcut.setParameterNames(pointcutParameterNames);
		this.pointcut.setParameterTypes(pointcutParameterTypes);
	}

	/**
	 * Take the arguments at the method execution join point and output a set of arguments
	 * to the advice method.
	 * @param jp the current JoinPoint
	 * @param jpMatch the join point match that matched this execution join point
	 * @param returnValue the return value from the method execution (may be null)
	 * @param ex the exception thrown by the method execution (may be null)
	 * @return the empty array if there are no arguments
	 */
	protected @Nullable Object[] argBinding(JoinPoint jp, @Nullable JoinPointMatch jpMatch,
			@Nullable Object returnValue, @Nullable Throwable ex) {

		calculateArgumentBindings();

		// AMC start
		@Nullable Object[] adviceInvocationArgs = new Object[this.parameterTypes.length];
		int numBound = 0;

		if (this.joinPointArgumentIndex != -1) {
			adviceInvocationArgs[this.joinPointArgumentIndex] = jp;
			numBound++;
		}
		else if (this.joinPointStaticPartArgumentIndex != -1) {
			adviceInvocationArgs[this.joinPointStaticPartArgumentIndex] = jp.getStaticPart();
			numBound++;
		}

		if (!CollectionUtils.isEmpty(this.argumentBindings)) {
			// binding from pointcut match
			if (jpMatch != null) {
				PointcutParameter[] parameterBindings = jpMatch.getParameterBindings();
				for (PointcutParameter parameter : parameterBindings) {
					String name = parameter.getName();
					Integer index = this.argumentBindings.get(name);
					Assert.state(index != null, "Index must not be null");
					adviceInvocationArgs[index] = parameter.getBinding();
					numBound++;
				}
			}
			// binding from returning clause
			if (this.returningName != null) {
				Integer index = this.argumentBindings.get(this.returningName);
				Assert.state(index != null, "Index must not be null");
				adviceInvocationArgs[index] = returnValue;
				numBound++;
			}
			// binding from thrown exception
			if (this.throwingName != null) {
				Integer index = this.argumentBindings.get(this.throwingName);
				Assert.state(index != null, "Index must not be null");
				adviceInvocationArgs[index] = ex;
				numBound++;
			}
		}

		if (numBound != this.parameterTypes.length) {
			throw new IllegalStateException("Required to bind " + this.parameterTypes.length +
					" arguments, but only bound " + numBound + " (JoinPointMatch " +
					(jpMatch == null ? "was NOT" : "WAS") + " bound in invocation)");
		}

		return adviceInvocationArgs;
	}


	/**
	 * Invoke the advice method.
	 * @param jpMatch the JoinPointMatch that matched this execution join point
	 * @param returnValue the return value from the method execution (may be null)
	 * @param ex the exception thrown by the method execution (may be null)
	 * @return the invocation result
	 * @throws Throwable in case of invocation failure
	 */
	protected Object invokeAdviceMethod(
			@Nullable JoinPointMatch jpMatch, @Nullable Object returnValue, @Nullable Throwable ex)
			throws Throwable {

		return invokeAdviceMethodWithGivenArgs(argBinding(getJoinPoint(), jpMatch, returnValue, ex));
	}

	// As above, but in this case we are given the join point.
	protected Object invokeAdviceMethod(JoinPoint jp, @Nullable JoinPointMatch jpMatch,
			@Nullable Object returnValue, @Nullable Throwable t) throws Throwable {

		return invokeAdviceMethodWithGivenArgs(argBinding(jp, jpMatch, returnValue, t));
	}

	protected Object invokeAdviceMethodWithGivenArgs(@Nullable Object[] args) throws Throwable {
		@Nullable Object[] actualArgs = args;
		if (this.aspectJAdviceMethod.getParameterCount() == 0) {
			actualArgs = null;
		}
		try {
			ReflectionUtils.makeAccessible(this.aspectJAdviceMethod);
			return this.aspectJAdviceMethod.invoke(this.aspectInstanceFactory.getAspectInstance(), actualArgs);
		}
		catch (IllegalArgumentException ex) {
			throw new AopInvocationException("Mismatch on arguments to advice method [" +
					this.aspectJAdviceMethod + "]; pointcut expression [" +
					this.pointcut.getPointcutExpression() + "]", ex);
		}
		catch (InvocationTargetException ex) {
			throw ex.getTargetException();
		}
	}

	/**
	 * Overridden in around advice to return proceeding join point.
	 */
	protected JoinPoint getJoinPoint() {
		return currentJoinPoint();
	}

	/**
	 * Get the current join point match at the join point we are being dispatched on.
	 */
	protected @Nullable JoinPointMatch getJoinPointMatch() {
		MethodInvocation mi = ExposeInvocationInterceptor.currentInvocation();
		if (!(mi instanceof ProxyMethodInvocation pmi)) {
			throw new IllegalStateException("MethodInvocation is not a Spring ProxyMethodInvocation: " + mi);
		}
		return getJoinPointMatch(pmi);
	}

	// Note: We can't use JoinPointMatch.getClass().getName() as the key, since
	// Spring AOP does all the matching at a join point, and then all the invocations.
	// Under this scenario, if we just use JoinPointMatch as the key, then
	// 'last man wins' which is not what we want at all.
	// Using the expression is guaranteed to be safe, since 2 identical expressions
	// are guaranteed to bind in exactly the same way.
	protected @Nullable JoinPointMatch getJoinPointMatch(ProxyMethodInvocation pmi) {
		String expression = this.pointcut.getExpression();
		return (expression != null ? (JoinPointMatch) pmi.getUserAttribute(expression) : null);
	}


	@Override
	public String toString() {
		return getClass().getName() + ": advice method [" + this.aspectJAdviceMethod + "]; " +
				"aspect name '" + this.aspectName + "'";
	}

	private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
		inputStream.defaultReadObject();
		try {
			this.aspectJAdviceMethod = this.declaringClass.getMethod(this.methodName, this.parameterTypes);
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException("Failed to find advice method on deserialization", ex);
		}
	}


	/**
	 * MethodMatcher that excludes the specified advice method.
	 * @see AbstractAspectJAdvice#buildSafePointcut()
	 */
	private static class AdviceExcludingMethodMatcher extends StaticMethodMatcher {

		private final Method adviceMethod;

		public AdviceExcludingMethodMatcher(Method adviceMethod) {
			this.adviceMethod = adviceMethod;
		}

		@Override
		public boolean matches(Method method, Class<?> targetClass) {
			return !this.adviceMethod.equals(method);
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof AdviceExcludingMethodMatcher that &&
					this.adviceMethod.equals(that.adviceMethod)));
		}

		@Override
		public int hashCode() {
			return this.adviceMethod.hashCode();
		}

		@Override
		public String toString() {
			return getClass().getName() + ": " + this.adviceMethod;
		}
	}

}
