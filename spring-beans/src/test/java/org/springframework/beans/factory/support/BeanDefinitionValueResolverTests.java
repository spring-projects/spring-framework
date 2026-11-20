/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BeanDefinitionValueResolver}.
 *
 * @author Stephane Nicoll
 */
class BeanDefinitionValueResolverTests {

	@Test
	void resolveInnerBean() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		RootBeanDefinition parentBd = new RootBeanDefinition();
		GenericBeanDefinition innerBd = new GenericBeanDefinition();
		innerBd.setAttribute("test", 42);
		BeanDefinitionValueResolver bdvr = new BeanDefinitionValueResolver(beanFactory, "test", parentBd);
		RootBeanDefinition resolvedInnerBd = bdvr.resolveInnerBean(null,innerBd, (name, mbd) -> {
			assertThat(name).isNotNull().startsWith("(inner bean");
			return mbd;
		});
		assertThat(resolvedInnerBd.getAttribute("test")).isEqualTo(42);
	}

}
