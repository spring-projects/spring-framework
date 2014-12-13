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

package org.springframework.test.context.junit4.annotation.meta;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfilesResolver;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;

/**
 * Integration tests for meta-annotation attribute override support, overriding
 * default attribute values defined in {@link ConfigClassesAndProfileResolverWithCustomDefaultsMetaConfig}.
 *
 * @author Sam Brannen
 * @since 4.0.3
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ConfigClassesAndProfileResolverWithCustomDefaultsMetaConfig(classes = LocalDevConfig.class, resolver = DevResolver.class)
public class ConfigClassesAndProfileResolverWithCustomDefaultsMetaConfigWithOverridesTests {

	@Autowired
	private String foo;


	@Test
	public void foo() {
		assertEquals("Local Dev Foo", foo);
	}
}

@Configuration
@Profile("dev")
class LocalDevConfig {

	@Bean
	public String foo() {
		return "Local Dev Foo";
	}
}

class DevResolver implements ActiveProfilesResolver {

	@Override
	public String[] resolve(Class<?> testClass) {
		// Checking that the "test class" name ends with "*Tests" ensures that an actual
		// test class is passed to this method as opposed to a "*Config" class which would
		// imply that we likely have been passed the 'declaringClass' instead of the
		// 'rootDeclaringClass'.
		return testClass.getName().endsWith("Tests") ? new String[] { "dev" } : new String[] {};
	}
}