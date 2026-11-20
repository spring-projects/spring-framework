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

/**
 * Composed annotation that declares a {@code meta} inlined property via
 * {@link TestPropertySource @TestPropertySource}.
 *
 * @author Anatoliy Korovin
 * @since 5.2
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@TestPropertySource(properties = "enigma = meta")
@interface MetaInlinedTestProperty {
}
