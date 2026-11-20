/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
			reader.loadBeanDefinitions(new ClassPathResource("DuplicateBeanIdTests-sameLevel-context.xml", getClass())));
	}

	@Test
	void duplicateBeanIdsAcrossNestingLevels() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.setAllowBeanDefinitionOverriding(true);
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(bf);
		reader.loadBeanDefinitions(new ClassPathResource("DuplicateBeanIdTests-multiLevel-context.xml", getClass()));
		TestBean testBean = bf.getBean(TestBean.class); // there should be only one
		assertThat(testBean.getName()).isEqualTo("nested");
	}

}
