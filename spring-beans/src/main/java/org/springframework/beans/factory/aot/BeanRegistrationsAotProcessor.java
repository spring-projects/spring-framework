/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.beans.factory.aot;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.aot.BeanRegistrationsAotContribution.Registration;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.lang.Nullable;

/**
 * {@link BeanFactoryInitializationAotProcessor} that contributes code to
 * register beans.
 *
 * @author Phillip Webb
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @since 6.0
 */
class BeanRegistrationsAotProcessor implements BeanFactoryInitializationAotProcessor {

	@Override
	@Nullable
	public BeanRegistrationsAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
		BeanDefinitionMethodGeneratorFactory beanDefinitionMethodGeneratorFactory =
				new BeanDefinitionMethodGeneratorFactory(beanFactory);
		List<Registration> registrations = new ArrayList<>();

		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			RegisteredBean registeredBean = RegisteredBean.of(beanFactory, beanName);
			BeanDefinitionMethodGenerator beanDefinitionMethodGenerator =
					beanDefinitionMethodGeneratorFactory.getBeanDefinitionMethodGenerator(registeredBean);
			if (beanDefinitionMethodGenerator != null) {
				registrations.add(new Registration(registeredBean, beanDefinitionMethodGenerator,
						beanFactory.getAliases(beanName)));
			}
		}

		if (registrations.isEmpty()) {
			return null;
		}
		return new BeanRegistrationsAotContribution(registrations);
	}

}
