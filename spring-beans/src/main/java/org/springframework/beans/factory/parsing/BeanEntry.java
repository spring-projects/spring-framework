/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.parsing;

/**
 * {@link ParseState} entry representing a bean definition.
 *
 * @author Rob Harrop
 * @since 2.0
 */
public class BeanEntry implements ParseState.Entry {

	private final String beanDefinitionName;


	/**
	 * Create a new {@code BeanEntry} instance.
	 * @param beanDefinitionName the name of the associated bean definition
	 */
	public BeanEntry(String beanDefinitionName) {
		this.beanDefinitionName = beanDefinitionName;
	}


	@Override
	public String toString() {
		return "Bean '" + this.beanDefinitionName + "'";
	}

}
