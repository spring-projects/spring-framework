/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans.factory.aot;

/**
 * A {@link GenericFactoryBean} that has a bound for the target type.
 *
 * @author Stephane Nicoll
 */
public class NumberFactoryBean<T extends Number> extends GenericFactoryBean<T> {

	public NumberFactoryBean(Class<T> beanType) {
		super(beanType);
	}

}
