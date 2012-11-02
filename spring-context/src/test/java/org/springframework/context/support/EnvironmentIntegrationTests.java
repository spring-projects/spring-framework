/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.support;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Tests covering the integration of {@link Environment} into {@link ApplicationContext} hierarchies.
 *
 * @author Chris Beams
 */
public class EnvironmentIntegrationTests {

	@Test
	public void repro() {
		ConfigurableApplicationContext parent = new GenericApplicationContext();
		parent.refresh();

		AnnotationConfigApplicationContext child = new AnnotationConfigApplicationContext();
		child.setParent(parent);
		child.refresh();

		ConfigurableEnvironment env = child.getBean(ConfigurableEnvironment.class);
		assertThat("unknown env", env, anyOf(
				sameInstance(parent.getEnvironment()),
				sameInstance(child.getEnvironment())));
		assertThat("expected child ctx env", env, sameInstance(child.getEnvironment()));
	}

}
