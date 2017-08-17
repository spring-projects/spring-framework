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

package org.springframework.aop.support.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;

/**
 * Convenient class for annotation-matching method advices.
 *
 * @author Laszlo Csontos
 * @since 4.3
 * @see AnnotationMatchingPointcut
 * @see AnnotationMatchingMethodPointcutAdvisor
 */
@SuppressWarnings("unchecked")
public abstract class AnnotationMethodAdvice<A extends Annotation> implements MethodInterceptor {

	protected static final Annotation NULL_ANNOTATION = new NullAnnotation();
	protected static final Object NULL_RESULT = new Object();

	private static final ConcurrentMap<Method, Map<Class<? extends Annotation>, Annotation>> METHOD_ANNOTATION_BAG =
			new ConcurrentHashMap<Method, Map<Class<? extends Annotation>, Annotation>>();

	private final Class<A> annotationType;


	public AnnotationMethodAdvice() {
		annotationType = getAnnotationType();
	}


	public abstract Class<A> getAnnotationType();

	@Override
	public final Object invoke(MethodInvocation methodInvocation) throws Throwable {
		Method method = methodInvocation.getMethod();
		Object target = methodInvocation.getThis();

		Annotation annotation = findAnnotation(method, target.getClass());
		if (annotation == NULL_ANNOTATION) {
			return methodInvocation.proceed();
		}

		Object[] args = methodInvocation.getArguments();
		Object returnValue = before((A) annotation, method, args, target);

		if (returnValue != null) {
			if (returnValue == NULL_RESULT) {
				return null;
			} else {
				return returnValue;
			}
		}

		try {
			returnValue = methodInvocation.proceed();
			afterReturning((A) annotation, returnValue, method, args, target);
		} catch (Exception ex) {
			afterThrowing((A) annotation, method, args, target, ex);
			throw ex;
		} finally {
			duringFinally((A) annotation, method, args, target);
		}

		return returnValue;
	}

	protected void afterReturning(A annotation, Object returnValue, Method method, Object[] args, Object target) throws Exception {
	}

	protected void afterThrowing(A annotation, Method method, Object[] args, Object target, Exception ex) throws Exception {
	}

	protected Object before(A annotation, Method method, Object[] args, Object target) throws Exception {
		return null;
	}

	protected void duringFinally(A annotation, Method method, Object[] args, Object target) {
	}

	private Annotation findAnnotation(Method method, Class<?> targetClass) {
		Annotation annotation = getAnnotation(method);
		if (annotation != null) {
			return annotation;
		}

		Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);

		annotation = AnnotationUtils.findAnnotation(specificMethod, annotationType);
		if (annotation == null) {
			annotation = NULL_ANNOTATION;
		}

		putAnnotation(method, annotation);

		return annotation;
	}

	private Annotation getAnnotation(Method method) {
		Map<Class<? extends Annotation>, Annotation> annotationMap = METHOD_ANNOTATION_BAG.get(method);

		if (annotationMap == null) {
			return null;
		}

		return annotationMap.get(annotationType);
	}

	private void putAnnotation(Method method, Annotation annotation) {
		Map<Class<? extends Annotation>, Annotation> annotationMap = METHOD_ANNOTATION_BAG.get(method);
		if (annotationMap != null) {
			annotationMap.put(annotationType, annotation);
			return;
		}

		annotationMap = new ConcurrentHashMap<Class<? extends Annotation>, Annotation>();
		Map<Class<? extends Annotation>, Annotation> prevAnnotationMap =
				METHOD_ANNOTATION_BAG.putIfAbsent(method, annotationMap);

		if (prevAnnotationMap != null) {
			annotationMap.putAll(prevAnnotationMap);
		}

		annotationMap.put(annotationType, annotation);
	}


	private static class NullAnnotation implements Annotation {

		@Override
		public Class<? extends Annotation> annotationType() {
			return null;
		}

	}

}
