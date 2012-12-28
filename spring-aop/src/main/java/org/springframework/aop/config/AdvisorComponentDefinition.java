/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.aop.config;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.parsing.AbstractComponentDefinition;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.beans.factory.parsing.ComponentDefinition}
 * that bridges the gap between the advisor bean definition configured
 * by the {@code &lt;aop:advisor&gt;} tag and the component definition
 * infrastructure.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class AdvisorComponentDefinition extends AbstractComponentDefinition {

	private final String advisorBeanName;

	private final BeanDefinition advisorDefinition;

	private String description;

	private BeanReference[] beanReferences;

	private BeanDefinition[] beanDefinitions;


	public AdvisorComponentDefinition(String advisorBeanName, BeanDefinition advisorDefinition) {
		 this(advisorBeanName, advisorDefinition, null);
	}

	public AdvisorComponentDefinition(
			String advisorBeanName, BeanDefinition advisorDefinition, BeanDefinition pointcutDefinition) {

		Assert.notNull(advisorBeanName, "'advisorBeanName' must not be null");
		Assert.notNull(advisorDefinition, "'advisorDefinition' must not be null");
		this.advisorBeanName = advisorBeanName;
		this.advisorDefinition = advisorDefinition;
		unwrapDefinitions(advisorDefinition, pointcutDefinition);
	}


	private void unwrapDefinitions(BeanDefinition advisorDefinition, BeanDefinition pointcutDefinition) {
		MutablePropertyValues pvs = advisorDefinition.getPropertyValues();
		BeanReference adviceReference = (BeanReference) pvs.getPropertyValue("adviceBeanName").getValue();

		if (pointcutDefinition != null) {
			this.beanReferences = new BeanReference[] {adviceReference};
			this.beanDefinitions = new BeanDefinition[] {advisorDefinition, pointcutDefinition};
			this.description = buildDescription(adviceReference, pointcutDefinition);
		}
		else {
			BeanReference pointcutReference = (BeanReference) pvs.getPropertyValue("pointcut").getValue();
			this.beanReferences = new BeanReference[] {adviceReference, pointcutReference};
			this.beanDefinitions = new BeanDefinition[] {advisorDefinition};
			this.description = buildDescription(adviceReference, pointcutReference);
		}
	}

	private String buildDescription(BeanReference adviceReference, BeanDefinition pointcutDefinition) {
		return new StringBuilder("Advisor <advice(ref)='").
				append(adviceReference.getBeanName()).append("', pointcut(expression)=[").
				append(pointcutDefinition.getPropertyValues().getPropertyValue("expression").getValue()).
				append("]>").toString();
	}

	private String buildDescription(BeanReference adviceReference, BeanReference pointcutReference) {
		return new StringBuilder("Advisor <advice(ref)='").
				append(adviceReference.getBeanName()).append("', pointcut(ref)='").
				append(pointcutReference.getBeanName()).append("'>").toString();
	}


	@Override
	public String getName() {
		return this.advisorBeanName;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public BeanDefinition[] getBeanDefinitions() {
		return this.beanDefinitions;
	}

	@Override
	public BeanReference[] getBeanReferences() {
		return this.beanReferences;
	}

	@Override
	public Object getSource() {
		return this.advisorDefinition.getSource();
	}

}
