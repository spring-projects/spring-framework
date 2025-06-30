/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.beans.factory.support;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@code equals()} and {@code hashCode()} in bean definitions.
 *
 * @author Rob Harrop
 * @author Sam Brannen
 */
@SuppressWarnings("serial")
class DefinitionMetadataEqualsHashCodeTests {

	@Test
	void rootBeanDefinition() {
		RootBeanDefinition master = new RootBeanDefinition(TestBean.class);
		RootBeanDefinition equal = new RootBeanDefinition(TestBean.class);
		RootBeanDefinition notEqual = new RootBeanDefinition(String.class);
		RootBeanDefinition subclass = new RootBeanDefinition(TestBean.class) {
		};
		setBaseProperties(master);
		setBaseProperties(equal);
		setBaseProperties(notEqual);
		setBaseProperties(subclass);

		assertEqualsAndHashCodeContracts(master, equal, notEqual, subclass);
	}

	/**
	 * @since 3.2.8
	 * @see <a href="https://jira.spring.io/browse/SPR-11420">SPR-11420</a>
	 */
	@Test
	void rootBeanDefinitionAndMethodOverridesWithDifferentOverloadedValues() {
		RootBeanDefinition master = new RootBeanDefinition(TestBean.class);
		RootBeanDefinition equal = new RootBeanDefinition(TestBean.class);

		setBaseProperties(master);
		setBaseProperties(equal);

		// Simulate AbstractBeanDefinition.validate() which delegates to
		// AbstractBeanDefinition.prepareMethodOverrides():
		master.getMethodOverrides().getOverrides().iterator().next().setOverloaded(false);
		// But do not simulate validation of the 'equal' bean. As a consequence, a method
		// override in 'equal' will be marked as overloaded, but the corresponding
		// override in 'master' will not. But... the bean definitions should still be
		// considered equal.

		assertThat(equal).as("Should be equal").isEqualTo(master);
		assertThat(equal.hashCode()).as("Hash code for equal instances must match").isEqualTo(master.hashCode());
	}

	@Test
	void childBeanDefinition() {
		ChildBeanDefinition master = new ChildBeanDefinition("foo");
		ChildBeanDefinition equal = new ChildBeanDefinition("foo");
		ChildBeanDefinition notEqual = new ChildBeanDefinition("bar");
		ChildBeanDefinition subclass = new ChildBeanDefinition("foo") {
		};
		setBaseProperties(master);
		setBaseProperties(equal);
		setBaseProperties(notEqual);
		setBaseProperties(subclass);

		assertEqualsAndHashCodeContracts(master, equal, notEqual, subclass);
	}

	@Test
	void runtimeBeanReference() {
		RuntimeBeanReference master = new RuntimeBeanReference("name");
		RuntimeBeanReference equal = new RuntimeBeanReference("name");
		RuntimeBeanReference notEqual = new RuntimeBeanReference("someOtherName");
		RuntimeBeanReference subclass = new RuntimeBeanReference("name") {
		};
		assertEqualsAndHashCodeContracts(master, equal, notEqual, subclass);
	}

	private void setBaseProperties(AbstractBeanDefinition definition) {
		definition.setAbstract(true);
		definition.setAttribute("foo", "bar");
		definition.setAutowireCandidate(false);
		definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
		// definition.getConstructorArgumentValues().addGenericArgumentValue("foo");
		definition.setDependencyCheck(AbstractBeanDefinition.DEPENDENCY_CHECK_OBJECTS);
		definition.setDependsOn("foo", "bar");
		definition.setDestroyMethodName("destroy");
		definition.setEnforceDestroyMethod(false);
		definition.setEnforceInitMethod(true);
		definition.setFactoryBeanName("factoryBean");
		definition.setFactoryMethodName("factoryMethod");
		definition.setInitMethodName("init");
		definition.setLazyInit(true);
		definition.getMethodOverrides().addOverride(new LookupOverride("foo", "bar"));
		definition.getMethodOverrides().addOverride(new ReplaceOverride("foo", "bar"));
		definition.getPropertyValues().add("foo", "bar");
		definition.setResourceDescription("desc");
		definition.setRole(BeanDefinition.ROLE_APPLICATION);
		definition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		definition.setSource("foo");
	}

	private void assertEqualsAndHashCodeContracts(Object master, Object equal, Object notEqual, Object subclass) {
		assertThat(equal).as("Should be equal").isEqualTo(master);
		assertThat(equal.hashCode()).as("Hash code for equal instances should match").isEqualTo(master.hashCode());

		assertThat(notEqual).as("Should not be equal").isNotEqualTo(master);
		assertThat(notEqual.hashCode()).as("Hash code for non-equal instances should not match").isNotEqualTo(master.hashCode());

		assertThat(subclass).as("Subclass should be equal").isEqualTo(master);
		assertThat(subclass.hashCode()).as("Hash code for subclass should match").isEqualTo(master.hashCode());
	}

}
