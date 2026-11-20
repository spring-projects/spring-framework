/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.config.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * Custom configuration annotation with meta-annotation attribute overrides for
 * {@link ContextConfiguration#classes} and {@link ActiveProfiles#profiles} and
 * <strong>no</strong> default configuration local to the composed annotation.
 *
 * @author Sam Brannen
 * @since 4.0.3
 */
@ContextConfiguration
@ActiveProfiles
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConfigClassesAndProfilesMetaConfig {

	@AliasFor(annotation = ContextConfiguration.class)
	Class<?>[] classes() default {};

	@AliasFor(annotation = ActiveProfiles.class)
	String[] profiles() default {};

}
