/*
 * Copyright 2002-2014 the original author or authors.
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

import java.security.AccessControlException;
import java.security.Permission;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.StandardEnvironmentTests;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Tests integration between Environment and SecurityManagers. See SPR-9970.
 *
 * @author Chris Beams
 */
public class EnvironmentSecurityManagerIntegrationTests {

	private SecurityManager originalSecurityManager;

	private Map<String, String> env;


	@Before
	public void setUp() {
		originalSecurityManager = System.getSecurityManager();
		env = StandardEnvironmentTests.getModifiableSystemEnvironment();
		env.put(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, "p1");
	}

	@After
	public void tearDown() {
		env.remove(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME);
		System.setSecurityManager(originalSecurityManager);
	}


	@Test
	public void securityManagerDisallowsAccessToSystemEnvironmentButAllowsAccessToIndividualKeys() {
		SecurityManager securityManager = new SecurityManager() {
			@Override
			public void checkPermission(Permission perm) {
				// Disallowing access to System#getenv means that our
				// ReadOnlySystemAttributesMap will come into play.
				if ("getenv.*".equals(perm.getName())) {
					throw new AccessControlException("Accessing the system environment is disallowed");
				}
			}
		};
		System.setSecurityManager(securityManager);

		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(bf);
		reader.register(C1.class);
		assertThat(bf.containsBean("c1"), is(true));
	}

	@Test
	public void securityManagerDisallowsAccessToSystemEnvironmentAndDisallowsAccessToIndividualKey() {
		SecurityManager securityManager = new SecurityManager() {
			@Override
			public void checkPermission(Permission perm) {
				// Disallowing access to System#getenv means that our
				// ReadOnlySystemAttributesMap will come into play.
				if ("getenv.*".equals(perm.getName())) {
					throw new AccessControlException("Accessing the system environment is disallowed");
				}
				// Disallowing access to the spring.profiles.active property means that
				// the BeanDefinitionReader won't be able to determine which profiles are
				// active. We should see an INFO-level message in the console about this
				// and as a result, any components marked with a non-default profile will
				// be ignored.
				if (("getenv." + AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME).equals(perm.getName())) {
					throw new AccessControlException(
							format("Accessing system environment variable [%s] is disallowed",
									AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME));
				}
			}
		};
		System.setSecurityManager(securityManager);

		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(bf);
		reader.register(C1.class);
		assertThat(bf.containsBean("c1"), is(false));
	}


	@Component("c1")
	@Profile("p1")
	static class C1 {
	}

}
