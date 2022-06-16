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

package org.springframework.context.generator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import javax.lang.model.element.Modifier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.generator.GeneratedType;
import org.springframework.aot.generator.GeneratedTypeContext;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.generator.AotContributingBeanFactoryPostProcessor;
import org.springframework.beans.factory.generator.AotContributingBeanPostProcessor;
import org.springframework.beans.factory.generator.BeanDefinitionsContribution;
import org.springframework.beans.factory.generator.BeanFactoryContribution;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.OrderComparator;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;

/**
 * Process an {@link ApplicationContext} and its {@link BeanFactory} to generate
 * code that represents the state of the bean factory, as well as the necessary
 * hints that can be used at runtime in a constrained environment.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public class ApplicationContextAotGenerator {

	private static final Log logger = LogFactory.getLog(ApplicationContextAotGenerator.class);

	/**
	 * Refresh the specified {@link GenericApplicationContext} and generate the
	 * necessary code to restore the state of its {@link BeanFactory}, using the
	 * specified {@link GeneratedTypeContext}.
	 * @param applicationContext the application context to handle
	 * @param generationContext the generation context to use
	 */
	public void generateApplicationContext(GenericApplicationContext applicationContext,
			GeneratedTypeContext generationContext) {
		applicationContext.refreshForAotProcessing();

		DefaultListableBeanFactory beanFactory = applicationContext.getDefaultListableBeanFactory();
		List<BeanFactoryContribution> contributions = resolveBeanFactoryContributions(beanFactory);

		filterBeanFactory(contributions, beanFactory);
		ApplicationContextInitialization applicationContextInitialization = new ApplicationContextInitialization(generationContext);
		applyContributions(contributions, applicationContextInitialization);

		GeneratedType mainGeneratedType = generationContext.getMainGeneratedType();
		mainGeneratedType.customizeType(type -> type.addSuperinterface(ParameterizedTypeName.get(
				ApplicationContextInitializer.class, GenericApplicationContext.class)));
		mainGeneratedType.addMethod(initializeMethod(applicationContextInitialization.toCodeBlock()));
	}

	private MethodSpec.Builder initializeMethod(CodeBlock methodBody) {
		MethodSpec.Builder method = MethodSpec.methodBuilder("initialize").addModifiers(Modifier.PUBLIC)
				.addParameter(GenericApplicationContext.class, "context").addAnnotation(Override.class);
		method.addCode(methodBody);
		return method;
	}

	private void filterBeanFactory(List<BeanFactoryContribution> contributions, DefaultListableBeanFactory beanFactory) {
		BiPredicate<String, BeanDefinition> filter = Stream.concat(Stream.of(aotContributingExcludeFilter()),
						contributions.stream().map(BeanFactoryContribution::getBeanDefinitionExcludeFilter))
				.filter(Objects::nonNull).reduce((n, d) -> false, BiPredicate::or);
		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition bd = beanFactory.getMergedBeanDefinition(beanName);
			if (filter.test(beanName, bd)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Filtering out bean with name" + beanName + ": " + bd);
				}
				beanFactory.removeBeanDefinition(beanName);
			}
		}
	}

	// TODO: is this right?
	private BiPredicate<String, BeanDefinition> aotContributingExcludeFilter() {
		return (beanName, beanDefinition) -> {
			Class<?> type = beanDefinition.getResolvableType().toClass();
			return AotContributingBeanFactoryPostProcessor.class.isAssignableFrom(type) ||
					AotContributingBeanPostProcessor.class.isAssignableFrom(type);
		};
	}


	private void applyContributions(List<BeanFactoryContribution> contributions,
			ApplicationContextInitialization initialization) {
		for (BeanFactoryContribution contribution : contributions) {
			contribution.applyTo(initialization);
		}
	}

	/**
	 * Resolve the {@link BeanFactoryContribution} available in the specified
	 * bean factory. Infrastructure is contributed first, and bean definitions
	 * registration last.
	 * @param beanFactory the bean factory to process
	 * @return the contribution to apply
	 * @see InfrastructureContribution
	 * @see BeanDefinitionsContribution
	 */
	private List<BeanFactoryContribution> resolveBeanFactoryContributions(DefaultListableBeanFactory beanFactory) {
		List<BeanFactoryContribution> contributions = new ArrayList<>();
		contributions.add(new InfrastructureContribution());
		List<AotContributingBeanFactoryPostProcessor> postProcessors = getAotContributingBeanFactoryPostProcessors(beanFactory);
		for (AotContributingBeanFactoryPostProcessor postProcessor : postProcessors) {
			BeanFactoryContribution contribution = postProcessor.contribute(beanFactory);
			if (contribution != null) {
				contributions.add(contribution);
			}
		}
		contributions.add(new BeanDefinitionsContribution(beanFactory));
		return contributions;
	}

	private static List<AotContributingBeanFactoryPostProcessor> getAotContributingBeanFactoryPostProcessors(DefaultListableBeanFactory beanFactory) {
		String[] postProcessorNames = beanFactory.getBeanNamesForType(AotContributingBeanFactoryPostProcessor.class, true, false);
		List<AotContributingBeanFactoryPostProcessor> postProcessors = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			postProcessors.add(beanFactory.getBean(ppName, AotContributingBeanFactoryPostProcessor.class));
		}
		postProcessors.addAll(SpringFactoriesLoader.loadFactories(AotContributingBeanFactoryPostProcessor.class,
				beanFactory.getBeanClassLoader()));
		sortPostProcessors(postProcessors, beanFactory);
		return postProcessors;
	}

	private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
		// Nothing to sort?
		if (postProcessors.size() <= 1) {
			return;
		}
		Comparator<Object> comparatorToUse = null;
		if (beanFactory instanceof DefaultListableBeanFactory) {
			comparatorToUse = ((DefaultListableBeanFactory) beanFactory).getDependencyComparator();
		}
		if (comparatorToUse == null) {
			comparatorToUse = OrderComparator.INSTANCE;
		}
		postProcessors.sort(comparatorToUse);
	}

}
