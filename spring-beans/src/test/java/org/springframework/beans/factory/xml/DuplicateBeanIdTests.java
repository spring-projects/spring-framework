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

package org.springframework.beans.factory.xml;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;

/**
 * Bean id attributes (and all other id attributes across the core schemas) are
 * not typed as xsd:id, but as xsd:string.  This allows for using the same bean
 * id within nested &lt;beans&gt; elements.
 *
 * <p>Duplicate IDs *within the same level of nesting* will still be treated as an
 * error through the ProblemReporter, as this could never be an intended/valid
 * situation.
 *
 * @author Chris Beams
 * @since 3.1
 * @see org.springframework.beans.factory.xml.XmlBeanFactoryTests#withDuplicateName
 * @see org.springframework.beans.factory.xml.XmlBeanFactoryTests#withDuplicateNameInAlias
 */
class DuplicateBeanIdTests {

	@Test
	void duplicateBeanIdsWithinSameNestingLevelRaisesError() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(bf);
		assertThatException().as("duplicate ids in same nesting level").isThrownBy(() ->
			reader.loadBeanDefinitions(new ClassPathResource("DuplicateBeanIdTests-sameLevel-context.xml", this.getClass())));
	}

	@Test
	void duplicateBeanIdsAcrossNestingLevels() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.setAllowBeanDefinitionOverriding(true);
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(bf);
		reader.loadBeanDefinitions(new ClassPathResource("DuplicateBeanIdTests-multiLevel-context.xml", this.getClass()));
		TestBean testBean = bf.getBean(TestBean.class); // there should be only one
		assertThat(testBean.getName()).isEqualTo("nested");
	}

}
