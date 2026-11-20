/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.mvc.method.annotation;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.InitBinderDataBinderFactory;
import org.springframework.web.method.support.InvocableHandlerMethod;

/**
 * Creates a {@code ServletRequestDataBinder}.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ServletRequestDataBinderFactory extends InitBinderDataBinderFactory {

	/**
	 * Create a new instance.
	 * @param binderMethods one or more {@code @InitBinder} methods
	 * @param initializer provides global data binder initialization
	 */
	public ServletRequestDataBinderFactory(@Nullable List<InvocableHandlerMethod> binderMethods,
			@Nullable WebBindingInitializer initializer) {

		super(binderMethods, initializer);
	}

	/**
	 * Returns an instance of {@link org.springframework.web.servlet.support.ExtendedServletRequestDataBinder}.
	 */
	@Override
	protected ServletRequestDataBinder createBinderInstance(
			@Nullable Object target, String objectName, NativeWebRequest request) throws Exception {

		return new org.springframework.web.servlet.support.ExtendedServletRequestDataBinder(target, objectName);
	}

}
