/*
 * Copyright 2002-2011 the original author or authors.
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
package org.springframework.web.servlet.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.ComponentRegistrar;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.config.AbstractSpecificationExecutor;
import org.springframework.context.config.SpecificationContext;
import org.springframework.web.servlet.handler.MappedInterceptor;

/**
 * Executes {@link MvcInterceptors} specification, creating and registering
 * bean definitions as appropriate based on the configuration within.
 * 
 * @author Keith Donald
 * @author Rossen Stoyanchev
 * 
 * @since 3.1
 */
final class MvcInterceptorsExecutor extends AbstractSpecificationExecutor<MvcInterceptors> {

	@Override
	protected void doExecute(MvcInterceptors spec, SpecificationContext specContext) {
		ComponentRegistrar registrar = specContext.getRegistrar();
		Object source = spec.source();

		CompositeComponentDefinition compDefinition = new CompositeComponentDefinition(spec.sourceName(), source);

		for (Object interceptor : spec.interceptorMappings().keySet()) {
			RootBeanDefinition beanDef = new RootBeanDefinition(MappedInterceptor.class);
			beanDef.setSource(source);
			beanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			beanDef.getConstructorArgumentValues().addIndexedArgumentValue(0,
					spec.interceptorMappings().get(interceptor));
			beanDef.getConstructorArgumentValues().addIndexedArgumentValue(1, interceptor);

			String beanName = registrar.registerWithGeneratedName(beanDef);
			compDefinition.addNestedComponent(new BeanComponentDefinition(beanDef, beanName));
		}

		registrar.registerComponent(compDefinition);
	}

}
