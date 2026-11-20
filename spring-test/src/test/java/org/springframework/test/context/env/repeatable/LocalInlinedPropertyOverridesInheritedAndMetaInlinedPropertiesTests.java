/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.env.repeatable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.env.repeatable.LocalInlinedPropertyOverridesInheritedAndMetaInlinedPropertiesTests.Key1InlinedTestProperty;

/**
 * Integration tests for {@link TestPropertySource @TestPropertySource} as a
 * repeatable annotation.
 *
 * @author Anatoliy Korovin
 * @author Sam Brannen
 * @since 5.2
 */
@TestPropertySource(properties = "key1 = local")
@Key1InlinedTestProperty
class LocalInlinedPropertyOverridesInheritedAndMetaInlinedPropertiesTests extends AbstractClassWithTestProperty {

	@Test
	void test() {
		assertEnvironmentValue("key1", "local");
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@TestPropertySource(properties = "key1 = meta")
	@interface Key1InlinedTestProperty {
	}

}
