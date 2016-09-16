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

package org.springframework.web.reactive.support;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.server.reactive.ServletHttpHandlerAdapter;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.DispatcherHandler;

/**
 * Base class for {@link org.springframework.web.WebApplicationInitializer}
 * implementations that register a {@link DispatcherHandler} configured with annotated
 * {@link org.springframework.context.annotation.Configuration @Configuration} classes in the
 * servlet context, wrapping it in a {@link ServletHttpHandlerAdapter}.
 *
 * <p>Concrete implementations are required to implement {@link #getConfigClasses()}.
 * Further template and customization methods are provided by
 * {@link AbstractDispatcherHandlerInitializer}.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public abstract class AbstractAnnotationConfigDispatcherHandlerInitializer
		extends AbstractDispatcherHandlerInitializer {

	/**
	 * {@inheritDoc}
	 * <p>This implementation creates an {@link AnnotationConfigApplicationContext},
	 * providing it the annotated classes returned by {@link #getConfigClasses()}.
	 */
	@Override
	protected ApplicationContext createApplicationContext() {
		AnnotationConfigApplicationContext servletAppContext = new AnnotationConfigApplicationContext();
		Class<?>[] configClasses = getConfigClasses();
		if (!ObjectUtils.isEmpty(configClasses)) {
			servletAppContext.register(configClasses);
		}
		return servletAppContext;
	}

	/**
	 * Specify {@link org.springframework.context.annotation.Configuration @Configuration}
	 * and/or {@link org.springframework.stereotype.Component @Component} classes to be
	 * provided to the {@linkplain #createApplicationContext() application context}.
	 * @return the configuration classes for the dispatcher servlet application context
	 */
	protected abstract Class<?>[] getConfigClasses();

}
