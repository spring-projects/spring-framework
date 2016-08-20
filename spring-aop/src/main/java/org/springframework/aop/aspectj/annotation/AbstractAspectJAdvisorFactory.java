/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.aop.aspectj.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.AjType;
import org.aspectj.lang.reflect.AjTypeSystem;
import org.aspectj.lang.reflect.PerClauseKind;

import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

/**
 * Abstract base class for factories that can create Spring AOP Advisors
 * given AspectJ classes from classes honoring the AspectJ 5 annotation syntax.
 *
 * <p>This class handles annotation parsing and validation functionality.
 * It does not actually generate Spring AOP Advisors, which is deferred to subclasses.
 *
 * @author Rod Johnson
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @since 2.0
 */
public abstract class AbstractAspectJAdvisorFactory implements AspectJAdvisorFactory {

	private static final String AJC_MAGIC = "ajc$";


	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	protected final ParameterNameDiscoverer parameterNameDiscoverer = new AspectJAnnotationParameterNameDiscoverer();


	/**
	 * We consider something to be an AspectJ aspect suitable for use by the Spring AOP system
	 * if it has the @Aspect annotation, and was not compiled by ajc. The reason for this latter test
	 * is that aspects written in the code-style (AspectJ language) also have the annotation present
	 * when compiled by ajc with the -1.5 flag, yet they cannot be consumed by Spring AOP.
	 */
	@Override
	public boolean isAspect(Class<?> clazz) {
		return (hasAspectAnnotation(clazz) && !compiledByAjc(clazz));
	}

	private boolean hasAspectAnnotation(Class<?> clazz) {
		return (AnnotationUtils.findAnnotation(clazz, Aspect.class) != null);
	}

	/**
	 * We need to detect this as "code-style" AspectJ aspects should not be
	 * interpreted by Spring AOP.
	 */
	private boolean compiledByAjc(Class<?> clazz) {
		// The AJTypeSystem goes to great lengths to provide a uniform appearance between code-style and
		// annotation-style aspects. Therefore there is no 'clean' way to tell them apart. Here we rely on
		// an implementation detail of the AspectJ compiler.
		for (Field field : clazz.getDeclaredFields()) {
			if (field.getName().startsWith(AJC_MAGIC)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void validate(Class<?> aspectClass) throws AopConfigException {
		// If the parent has the annotation and isn't abstract it's an error
		if (aspectClass.getSuperclass().getAnnotation(Aspect.class) != null &&
				!Modifier.isAbstract(aspectClass.getSuperclass().getModifiers())) {
			throw new AopConfigException("[" + aspectClass.getName() + "] cannot extend concrete aspect [" +
					aspectClass.getSuperclass().getName() + "]");
		}

		AjType<?> ajType = AjTypeSystem.getAjType(aspectClass);
		if (!ajType.isAspect()) {
			throw new NotAnAtAspectException(aspectClass);
		}
		if (ajType.getPerClause().getKind() == PerClauseKind.PERCFLOW) {
			throw new AopConfigException(aspectClass.getName() + " uses percflow instantiation model: " +
					"This is not supported in Spring AOP.");
		}
		if (ajType.getPerClause().getKind() == PerClauseKind.PERCFLOWBELOW) {
			throw new AopConfigException(aspectClass.getName() + " uses percflowbelow instantiation model: " +
					"This is not supported in Spring AOP.");
		}
	}

	/**
	 * The pointcut and advice annotations both have an "argNames" member which contains a
	 * comma-separated list of the argument names. We use this (if non-empty) to build the
	 * formal parameters for the pointcut.
	 */
	protected AspectJExpressionPointcut createPointcutExpression(
			Method annotatedMethod, Class<?> declarationScope, String[] pointcutParameterNames) {

		Class<?> [] pointcutParameterTypes = new Class<?>[0];
		if (pointcutParameterNames != null) {
			pointcutParameterTypes = extractPointcutParameterTypes(pointcutParameterNames,annotatedMethod);
		}

		AspectJExpressionPointcut ajexp =
				new AspectJExpressionPointcut(declarationScope,pointcutParameterNames,pointcutParameterTypes);
		ajexp.setLocation(annotatedMethod.toString());
		return ajexp;
	}

	/**
	 * Create the pointcut parameters needed by aspectj based on the given argument names
	 * and the argument types that are available from the adviceMethod. Needs to take into
	 * account (ignore) any JoinPoint based arguments as these are not pointcut context but
	 * rather part of the advice execution context (thisJoinPoint, thisJoinPointStaticPart)
	 */
	private Class<?>[] extractPointcutParameterTypes(String[] argNames, Method adviceMethod) {
		Class<?>[] ret = new Class<?>[argNames.length];
		Class<?>[] paramTypes = adviceMethod.getParameterTypes();
		if (argNames.length > paramTypes.length) {
			throw new IllegalStateException("Expecting at least " + argNames.length +
					" arguments in the advice declaration, but only found " + paramTypes.length);
		}

		// Make the simplifying assumption for now that all of the JoinPoint based arguments
		// come first in the advice declaration.
		int typeOffset = paramTypes.length - argNames.length;
		for (int i = 0; i < ret.length; i++) {
			ret[i] = paramTypes[i + typeOffset];
		}
		return ret;
	}


	/**
	 * Find and return the first AspectJ annotation on the given method
	 * (there <i>should</i> only be one anyway...)
	 */
	@SuppressWarnings("unchecked")
	protected static AspectJAnnotation<?> findAspectJAnnotationOnMethod(Method method) {
		Class<?>[] classesToLookFor = new Class<?>[] {
				Before.class, Around.class, After.class, AfterReturning.class, AfterThrowing.class, Pointcut.class};
		for (Class<?> c : classesToLookFor) {
			AspectJAnnotation<?> foundAnnotation = findAnnotation(method, (Class<Annotation>) c);
			if (foundAnnotation != null) {
				return foundAnnotation;
			}
		}
		return null;
	}

	private static <A extends Annotation> AspectJAnnotation<A> findAnnotation(Method method, Class<A> toLookFor) {
		A result = AnnotationUtils.findAnnotation(method, toLookFor);
		if (result != null) {
			return new AspectJAnnotation<>(result);
		}
		else {
			return null;
		}
	}


	protected enum AspectJAnnotationType {

		AtPointcut,
		AtBefore,
		AtAfter,
		AtAfterReturning,
		AtAfterThrowing,
		AtAround
	}


	/**
	 * Class modelling an AspectJ annotation, exposing its type enumeration and
	 * pointcut String.
	 */
	protected static class AspectJAnnotation<A extends Annotation> {

		private static final String[] EXPRESSION_PROPERTIES = new String[] {"value", "pointcut"};

		private static Map<Class<?>, AspectJAnnotationType> annotationTypes =
				new HashMap<>();

		static {
			annotationTypes.put(Pointcut.class,AspectJAnnotationType.AtPointcut);
			annotationTypes.put(After.class,AspectJAnnotationType.AtAfter);
			annotationTypes.put(AfterReturning.class,AspectJAnnotationType.AtAfterReturning);
			annotationTypes.put(AfterThrowing.class,AspectJAnnotationType.AtAfterThrowing);
			annotationTypes.put(Around.class,AspectJAnnotationType.AtAround);
			annotationTypes.put(Before.class,AspectJAnnotationType.AtBefore);
		}

		private final A annotation;

		private final AspectJAnnotationType annotationType;

		private final String pointcutExpression;

		private final String argumentNames;

		public AspectJAnnotation(A annotation) {
			this.annotation = annotation;
			this.annotationType = determineAnnotationType(annotation);
			// We know these methods exist with the same name on each object,
			// but need to invoke them reflectively as there isn't a common interface.
			try {
				this.pointcutExpression = resolveExpression(annotation);
				this.argumentNames = (String) annotation.getClass().getMethod("argNames").invoke(annotation);
			}
			catch (Exception ex) {
				throw new IllegalArgumentException(annotation + " cannot be an AspectJ annotation", ex);
			}
		}

		private AspectJAnnotationType determineAnnotationType(A annotation) {
			for (Class<?> type : annotationTypes.keySet()) {
				if (type.isInstance(annotation)) {
					return annotationTypes.get(type);
				}
			}
			throw new IllegalStateException("Unknown annotation type: " + annotation.toString());
		}

		private String resolveExpression(A annotation) throws Exception {
			String expression = null;
			for (String methodName : EXPRESSION_PROPERTIES) {
				Method method;
				try {
					method = annotation.getClass().getDeclaredMethod(methodName);
				}
				catch (NoSuchMethodException ex) {
					method = null;
				}
				if (method != null) {
					String candidate = (String) method.invoke(annotation);
					if (StringUtils.hasText(candidate)) {
						expression = candidate;
					}
				}
			}
			return expression;
		}

		public AspectJAnnotationType getAnnotationType() {
			return this.annotationType;
		}

		public A getAnnotation() {
			return this.annotation;
		}

		public String getPointcutExpression() {
			return this.pointcutExpression;
		}

		public String getArgumentNames() {
			return this.argumentNames;
		}

		@Override
		public String toString() {
			return this.annotation.toString();
		}
	}


	/**
	 * ParameterNameDiscoverer implementation that analyzes the arg names
	 * specified at the AspectJ annotation level.
	 */
	private static class AspectJAnnotationParameterNameDiscoverer implements ParameterNameDiscoverer {

		@Override
		public String[] getParameterNames(Method method) {
			if (method.getParameterCount() == 0) {
				return new String[0];
			}
			AspectJAnnotation<?> annotation = findAspectJAnnotationOnMethod(method);
			if (annotation == null) {
				return null;
			}
			StringTokenizer strTok = new StringTokenizer(annotation.getArgumentNames(), ",");
			if (strTok.countTokens() > 0) {
				String[] names = new String[strTok.countTokens()];
				for (int i = 0; i < names.length; i++) {
					names[i] = strTok.nextToken();
				}
				return names;
			}
			else {
				return null;
			}
		}

		@Override
		public String[] getParameterNames(Constructor<?> ctor) {
			throw new UnsupportedOperationException("Spring AOP cannot handle constructor advice");
		}
	}

}
