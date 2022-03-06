/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.beans.factory.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * A {@link BeanFactoryContribution} that generates the bean definitions of a
 * bean factory, using {@link BeanRegistrationContributionProvider} to use
 * appropriate customizations if necessary.
 *
 * <p>{@link BeanRegistrationContributionProvider} can be ordered, with the default
 * implementation always coming last.
 *
 * @author Stephane Nicoll
 * @since 6.0
 * @see DefaultBeanRegistrationContributionProvider
 */
public class BeanDefinitionsContribution implements BeanFactoryContribution {

	private final DefaultListableBeanFactory beanFactory;

	private final List<BeanRegistrationContributionProvider> contributionProviders;

	private final Map<String, BeanFactoryContribution> contributions;

	BeanDefinitionsContribution(DefaultListableBeanFactory beanFactory,
			List<BeanRegistrationContributionProvider> contributionProviders) {
		this.beanFactory = beanFactory;
		this.contributionProviders = contributionProviders;
		this.contributions = new HashMap<>();
	}

	public BeanDefinitionsContribution(DefaultListableBeanFactory beanFactory) {
		this(beanFactory, initializeProviders(beanFactory));
	}

	private static List<BeanRegistrationContributionProvider> initializeProviders(DefaultListableBeanFactory beanFactory) {
		List<BeanRegistrationContributionProvider> providers = new ArrayList<>(SpringFactoriesLoader.loadFactories(
				BeanRegistrationContributionProvider.class, beanFactory.getBeanClassLoader()));
		providers.add(new DefaultBeanRegistrationContributionProvider(beanFactory));
		return providers;
	}

	@Override
	public void applyTo(BeanFactoryInitialization initialization) {
		writeBeanDefinitions(initialization);
	}

	@Override
	public BiPredicate<String, BeanDefinition> getBeanDefinitionExcludeFilter() {
		List<BiPredicate<String, BeanDefinition>> predicates = new ArrayList<>();
		for (String beanName : this.beanFactory.getBeanDefinitionNames()) {
			handleMergedBeanDefinition(beanName, beanDefinition -> predicates.add(
					getBeanRegistrationContribution(beanName, beanDefinition).getBeanDefinitionExcludeFilter()));
		}
		return predicates.stream().filter(Objects::nonNull).reduce((n, d) -> false, BiPredicate::or);
	}

	private void writeBeanDefinitions(BeanFactoryInitialization initialization) {
		for (String beanName : this.beanFactory.getBeanDefinitionNames()) {
			handleMergedBeanDefinition(beanName, beanDefinition -> {
				BeanFactoryContribution registrationContribution = getBeanRegistrationContribution(
						beanName, beanDefinition);
				registrationContribution.applyTo(initialization);
			});
		}
	}

	private BeanFactoryContribution getBeanRegistrationContribution(
			String beanName, RootBeanDefinition beanDefinition) {
		return this.contributions.computeIfAbsent(beanName, name -> {
			for (BeanRegistrationContributionProvider provider : this.contributionProviders) {
				BeanFactoryContribution contribution = provider.getContributionFor(
						beanName, beanDefinition);
				if (contribution != null) {
					return contribution;
				}
			}
			throw new BeanRegistrationContributionNotFoundException(beanName, beanDefinition);
		});
	}

	private void handleMergedBeanDefinition(String beanName, Consumer<RootBeanDefinition> consumer) {
		RootBeanDefinition beanDefinition = (RootBeanDefinition) this.beanFactory.getMergedBeanDefinition(beanName);
		try {
			consumer.accept(beanDefinition);
		}
		catch (BeanDefinitionGenerationException ex) {
			throw ex;
		}
		catch (Exception ex) {
			String msg = String.format("Failed to handle bean with name '%s' and type '%s'",
					beanName, beanDefinition.getResolvableType());
			throw new BeanDefinitionGenerationException(beanName, beanDefinition, msg, ex);
		}
	}

}
