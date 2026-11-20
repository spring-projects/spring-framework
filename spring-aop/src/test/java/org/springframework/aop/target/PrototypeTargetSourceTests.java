/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.target;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.testfixture.beans.SideEffectBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.testfixture.io.ResourceTestUtils.qualifiedResource;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
class PrototypeTargetSourceTests {

	/** Initial count value set in bean factory XML */
	private static final int INITIAL_COUNT = 10;

	private DefaultListableBeanFactory beanFactory;


	@BeforeEach
	void setup() {
		this.beanFactory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(this.beanFactory).loadBeanDefinitions(
				qualifiedResource(PrototypeTargetSourceTests.class, "context.xml"));
	}


	/**
	 * Test that multiple invocations of the prototype bean will result
	 * in no change to visible state, as a new instance is used.
	 * With the singleton, there will be change.
	 */
	@Test
	void prototypeAndSingletonBehaveDifferently() {
		SideEffectBean singleton = (SideEffectBean) beanFactory.getBean("singleton");
		assertThat(singleton.getCount()).isEqualTo(INITIAL_COUNT);
		singleton.doWork();
		assertThat(singleton.getCount()).isEqualTo((INITIAL_COUNT + 1));

		SideEffectBean prototype = (SideEffectBean) beanFactory.getBean("prototype");
		assertThat(prototype.getCount()).isEqualTo(INITIAL_COUNT);
		prototype.doWork();
		assertThat(prototype.getCount()).isEqualTo(INITIAL_COUNT);
	}

}
