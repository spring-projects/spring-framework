/*
 * Copyright 2002-2024 the original author or authors.
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
	public void setup() {
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
	void testPrototypeAndSingletonBehaveDifferently() {
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
