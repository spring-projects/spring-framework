/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.aot;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.aot.BeanRegistrationsAotContribution.Registration;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;

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
	public @Nullable BeanRegistrationsAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
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
