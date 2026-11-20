/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.env.repeatable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.env.repeatable.MetaComposedTestProperty.MetaMetaInlinedTestProperty;

/**
 * Composed annotation that declares a property via
 * {@link TestPropertySource @TestPropertySource} used as a meta-meta-annotation.
 *
 * @author Sam Brannen
 * @since 5.2
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@MetaMetaInlinedTestProperty
@interface MetaComposedTestProperty {

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@TestPropertySource(properties = "enigma = meta meta")
	@interface MetaMetaInlinedTestProperty {
	}

}
