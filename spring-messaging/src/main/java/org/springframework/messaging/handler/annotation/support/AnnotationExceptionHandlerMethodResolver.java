/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.handler.annotation.support;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.invocation.AbstractExceptionHandlerMethodResolver;

/**
 * A subclass of {@link AbstractExceptionHandlerMethodResolver} that looks for
 * {@link MessageExceptionHandler}-annotated methods in a given class. The actual
 * exception types handled are extracted either from the annotation, if present,
 * or from the method signature as a fallback option.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public class AnnotationExceptionHandlerMethodResolver extends AbstractExceptionHandlerMethodResolver {

	/**
	 * A constructor that finds {@link MessageExceptionHandler} methods in the given type.
	 * @param handlerType the type to introspect
	 */
	public AnnotationExceptionHandlerMethodResolver(Class<?> handlerType) {
		super(initExceptionMappings(handlerType));
	}

	private static Map<Class<? extends Throwable>, Method> initExceptionMappings(Class<?> handlerType) {
		Map<Method, MessageExceptionHandler> methods = MethodIntrospector.selectMethods(handlerType,
				(MethodIntrospector.MetadataLookup<MessageExceptionHandler>) method ->
						AnnotatedElementUtils.findMergedAnnotation(method, MessageExceptionHandler.class));

		Map<Class<? extends Throwable>, Method> result = new HashMap<>();
		for (Map.Entry<Method, MessageExceptionHandler> entry : methods.entrySet()) {
			Method method = entry.getKey();
			List<Class<? extends Throwable>> exceptionTypes = new ArrayList<>(Arrays.asList(entry.getValue().value()));
			if (exceptionTypes.isEmpty()) {
				exceptionTypes.addAll(getExceptionsFromMethodSignature(method));
			}
			for (Class<? extends Throwable> exceptionType : exceptionTypes) {
				Method oldMethod = result.put(exceptionType, method);
				if (oldMethod != null && !oldMethod.equals(method)) {
					throw new IllegalStateException("Ambiguous @ExceptionHandler method mapped for [" +
							exceptionType + "]: {" + oldMethod + ", " + method + "}");
				}
			}
		}
		return result;
	}

}
