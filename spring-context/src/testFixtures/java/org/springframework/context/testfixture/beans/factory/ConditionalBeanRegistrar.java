/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.testfixture.beans.factory;

import org.springframework.beans.factory.BeanRegistrar;
import org.springframework.beans.factory.BeanRegistry;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;

public class ConditionalBeanRegistrar implements BeanRegistrar {

	@Override
	public void register(BeanRegistry registry, Environment env) {
		if (registry.containsBean("testBean") &&
				registry.containsBean(TestBean.class) &&
				registry.containsBean(new ParameterizedTypeReference<Comparable<Object>>() {
				})) {
			registry.registerBean("myTestBean", TestBean.class);
		}
	}
}
