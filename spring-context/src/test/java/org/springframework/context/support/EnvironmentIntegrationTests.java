/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.support;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Tests covering the integration of the {@link Environment} into
 * {@link ApplicationContext} hierarchies.
 *
 * @author Chris Beams
 * @see org.springframework.core.env.EnvironmentSystemIntegrationTests
 */
class EnvironmentIntegrationTests {

	@Test
	void repro() {
		ConfigurableApplicationContext parent = new GenericApplicationContext();
		parent.refresh();

		AnnotationConfigApplicationContext child = new AnnotationConfigApplicationContext();
		child.setParent(parent);
		child.refresh();

		ConfigurableEnvironment env = child.getBean(ConfigurableEnvironment.class);
		assertThat(env).isSameAs(child.getEnvironment());

		child.close();
		parent.close();
	}

}
