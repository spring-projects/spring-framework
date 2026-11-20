/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.parsing;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanReference;

/**
 * ComponentDefinition based on a standard BeanDefinition, exposing the given bean
 * definition as well as inner bean definitions and bean references for the given bean.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class BeanComponentDefinition extends BeanDefinitionHolder implements ComponentDefinition {

	private static final BeanDefinition[] EMPTY_BEAN_DEFINITION_ARRAY = new BeanDefinition[0];

	private static final BeanReference[] EMPTY_BEAN_REFERENCE_ARRAY = new BeanReference[0];


	private final BeanDefinition[] innerBeanDefinitions;

	private final BeanReference[] beanReferences;


	/**
	 * Create a new BeanComponentDefinition for the given bean.
	 * @param beanDefinition the BeanDefinition
	 * @param beanName the name of the bean
	 */
	public BeanComponentDefinition(BeanDefinition beanDefinition, String beanName) {
		this(new BeanDefinitionHolder(beanDefinition, beanName));
	}

	/**
	 * Create a new BeanComponentDefinition for the given bean.
	 * @param beanDefinition the BeanDefinition
	 * @param beanName the name of the bean
	 * @param aliases alias names for the bean, or {@code null} if none
	 */
	public BeanComponentDefinition(BeanDefinition beanDefinition, String beanName, String @Nullable [] aliases) {
		this(new BeanDefinitionHolder(beanDefinition, beanName, aliases));
	}

	/**
	 * Create a new BeanComponentDefinition for the given bean.
	 * @param beanDefinitionHolder the BeanDefinitionHolder encapsulating
	 * the bean definition as well as the name of the bean
	 */
	public BeanComponentDefinition(BeanDefinitionHolder beanDefinitionHolder) {
		super(beanDefinitionHolder);

		List<BeanDefinition> innerBeans = new ArrayList<>();
		List<BeanReference> references = new ArrayList<>();
		PropertyValues propertyValues = beanDefinitionHolder.getBeanDefinition().getPropertyValues();
		for (PropertyValue propertyValue : propertyValues.getPropertyValues()) {
			Object value = propertyValue.getValue();
			if (value instanceof BeanDefinitionHolder beanDefHolder) {
				innerBeans.add(beanDefHolder.getBeanDefinition());
			}
			else if (value instanceof BeanDefinition beanDef) {
				innerBeans.add(beanDef);
			}
			else if (value instanceof BeanReference beanRef) {
				references.add(beanRef);
			}
		}
		this.innerBeanDefinitions = innerBeans.toArray(EMPTY_BEAN_DEFINITION_ARRAY);
		this.beanReferences = references.toArray(EMPTY_BEAN_REFERENCE_ARRAY);
	}


	@Override
	public String getName() {
		return getBeanName();
	}

	@Override
	public String getDescription() {
		return getShortDescription();
	}

	@Override
	public BeanDefinition[] getBeanDefinitions() {
		return new BeanDefinition[] {getBeanDefinition()};
	}

	@Override
	public BeanDefinition[] getInnerBeanDefinitions() {
		return this.innerBeanDefinitions;
	}

	@Override
	public BeanReference[] getBeanReferences() {
		return this.beanReferences;
	}


	/**
	 * This implementation returns this ComponentDefinition's description.
	 * @see #getDescription()
	 */
	@Override
	public String toString() {
		return getDescription();
	}

	/**
	 * This implementation expects the other object to be of type BeanComponentDefinition
	 * as well, in addition to the superclass's equality requirements.
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof BeanComponentDefinition && super.equals(other)));
	}

}
