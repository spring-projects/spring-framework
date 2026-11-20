/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans.factory.generator;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;

@FunctionalInterface
public interface BeanFactoryInitializer {

	void initializeBeanFactory(DefaultListableBeanFactory beanFactory);

}
