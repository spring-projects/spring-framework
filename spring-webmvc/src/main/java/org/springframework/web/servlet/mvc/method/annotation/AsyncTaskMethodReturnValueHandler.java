/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.mvc.method.annotation;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.WebAsyncTask;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Handles return values of type {@link WebAsyncTask}.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class AsyncTaskMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	private final @Nullable BeanFactory beanFactory;


	public AsyncTaskMethodReturnValueHandler(@Nullable BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return WebAsyncTask.class.isAssignableFrom(returnType.getParameterType());
	}

	@Override
	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		if (returnValue == null) {
			mavContainer.setRequestHandled(true);
			return;
		}

		WebAsyncTask<?> webAsyncTask = (WebAsyncTask<?>) returnValue;
		if (this.beanFactory != null) {
			webAsyncTask.setBeanFactory(this.beanFactory);
		}
		WebAsyncUtils.getAsyncManager(webRequest).startCallableProcessing(webAsyncTask, mavContainer);
	}

}
