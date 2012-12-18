/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.scheduling.config;

import org.w3c.dom.Element;

import org.springframework.aop.config.AopNamespaceUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.util.StringUtils;

/**
 * Parser for the 'annotation-driven' element of the 'task' namespace.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @author Chris Beams
 * @since 3.0
 */
public class AnnotationDrivenBeanDefinitionParser implements BeanDefinitionParser {

	/**
	 * The bean name of the internally managed async annotation processor (mode="proxy").
	 * @deprecated as of Spring 3.1 in favor of
	 * {@link AnnotationConfigUtils#ASYNC_ANNOTATION_PROCESSOR_BEAN_NAME}
	 */
	@Deprecated
	public static final String ASYNC_ANNOTATION_PROCESSOR_BEAN_NAME =
			AnnotationConfigUtils.ASYNC_ANNOTATION_PROCESSOR_BEAN_NAME;

	/**
	 * The bean name of the internally managed transaction aspect (mode="aspectj").
	 * @deprecated as of Spring 3.1 in favor of
	 * {@link AnnotationConfigUtils#ASYNC_EXECUTION_ASPECT_BEAN_NAME}
	 */
	@Deprecated
	public static final String ASYNC_EXECUTION_ASPECT_BEAN_NAME =
			AnnotationConfigUtils.ASYNC_EXECUTION_ASPECT_BEAN_NAME;

	/**
	 * The bean name of the internally managed scheduled annotation processor.
	 * @deprecated as of Spring 3.1 in favor of
	 * {@link AnnotationConfigUtils#SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME}
	 */
	@Deprecated
	public static final String SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME =
			AnnotationConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME;


	public BeanDefinition parse(Element element, ParserContext parserContext) {
		Object source = parserContext.extractSource(element);

		// Register component for the surrounding <task:annotation-driven> element.
		CompositeComponentDefinition compDefinition = new CompositeComponentDefinition(element.getTagName(), source);
		parserContext.pushContainingComponent(compDefinition);

		// Nest the concrete post-processor bean in the surrounding component.
		BeanDefinitionRegistry registry = parserContext.getRegistry();

		String mode = element.getAttribute("mode");
		if ("aspectj".equals(mode)) {
			// mode="aspectj"
			registerAsyncExecutionAspect(element, parserContext);
		}
		else {
			// mode="proxy"
			if (registry.containsBeanDefinition(AnnotationConfigUtils.ASYNC_ANNOTATION_PROCESSOR_BEAN_NAME)) {
				parserContext.getReaderContext().error(
						"Only one AsyncAnnotationBeanPostProcessor may exist within the context.", source);
			}
			else {
				BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
						"org.springframework.scheduling.annotation.AsyncAnnotationBeanPostProcessor");
				builder.getRawBeanDefinition().setSource(source);
				String executor = element.getAttribute("executor");
				if (StringUtils.hasText(executor)) {
					builder.addPropertyReference("executor", executor);
				}
				if (Boolean.valueOf(element.getAttribute(AopNamespaceUtils.PROXY_TARGET_CLASS_ATTRIBUTE))) {
					builder.addPropertyValue("proxyTargetClass", true);
				}
				registerPostProcessor(parserContext, builder, AnnotationConfigUtils.ASYNC_ANNOTATION_PROCESSOR_BEAN_NAME);
			}
		}

		if (registry.containsBeanDefinition(AnnotationConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME)) {
			parserContext.getReaderContext().error(
					"Only one ScheduledAnnotationBeanPostProcessor may exist within the context.", source);
		}
		else {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
					"org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor");
			builder.getRawBeanDefinition().setSource(source);
			String scheduler = element.getAttribute("scheduler");
			if (StringUtils.hasText(scheduler)) {
				builder.addPropertyReference("scheduler", scheduler);
			}
			registerPostProcessor(parserContext, builder, AnnotationConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME);
		}

		// Finally register the composite component.
		parserContext.popAndRegisterContainingComponent();

		return null;
	}

	private void registerAsyncExecutionAspect(Element element, ParserContext parserContext) {
		if (!parserContext.getRegistry().containsBeanDefinition(AnnotationConfigUtils.ASYNC_EXECUTION_ASPECT_BEAN_NAME)) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
					AnnotationConfigUtils.ASYNC_EXECUTION_ASPECT_CLASS_NAME);
			builder.setFactoryMethod("aspectOf");
			String executor = element.getAttribute("executor");
			if (StringUtils.hasText(executor)) {
				builder.addPropertyReference("executor", executor);
			}
			parserContext.registerBeanComponent(
					new BeanComponentDefinition(builder.getBeanDefinition(),
							AnnotationConfigUtils.ASYNC_EXECUTION_ASPECT_BEAN_NAME));
		}
	}

	private static void registerPostProcessor(
			ParserContext parserContext, BeanDefinitionBuilder builder, String beanName) {

		builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		parserContext.getRegistry().registerBeanDefinition(beanName, builder.getBeanDefinition());
		BeanDefinitionHolder holder = new BeanDefinitionHolder(builder.getBeanDefinition(), beanName);
		parserContext.registerComponent(new BeanComponentDefinition(holder));
	}

}
