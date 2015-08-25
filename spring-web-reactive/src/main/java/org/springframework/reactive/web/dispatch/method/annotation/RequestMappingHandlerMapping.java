/*
 * Copyright 2002-2015 the original author or authors.
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
package org.springframework.reactive.web.dispatch.method.annotation;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.reactive.web.dispatch.HandlerMapping;
import org.springframework.reactive.web.http.ServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.HandlerMethodSelector;


/**
 * @author Rossen Stoyanchev
 */
public class RequestMappingHandlerMapping implements HandlerMapping,
		ApplicationContextAware, InitializingBean {

	private static final Log logger = LogFactory.getLog(RequestMappingHandlerMapping.class);


	private final Map<String, HandlerMethod> methodMap = new LinkedHashMap<>();

	private ApplicationContext applicationContext;


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		for (Object bean : this.applicationContext.getBeansOfType(Object.class).values()) {
			detectHandlerMethods(bean);
		}
	}

	protected void detectHandlerMethods(final Object bean) {
		final Class<?> beanType = bean.getClass();
		if (AnnotationUtils.findAnnotation(beanType, Controller.class) != null) {
			HandlerMethodSelector.selectMethods(beanType, method -> {
				RequestMapping annotation = AnnotationUtils.findAnnotation(method, RequestMapping.class);
				if (annotation != null && annotation.value().length > 0) {
					String path = annotation.value()[0];
					HandlerMethod handlerMethod = new HandlerMethod(bean, method);
					if (logger.isInfoEnabled()) {
						logger.info("Mapped \"" + path + "\" onto " + handlerMethod);
					}
					methodMap.put(path, handlerMethod);
				}
				return false;
			});
		}
	}

	@Override
	public Object getHandler(ServerHttpRequest request) {
		String path = request.getURI().getPath();
		HandlerMethod handlerMethod = this.methodMap.get(path);
		if (logger.isDebugEnabled()) {
			logger.debug("Mapped " + path + " to [" + handlerMethod + "]");
		}
		return handlerMethod;
	}

}
