/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.test.context.support.AnnotationConfigContextLoaderUtils.detectDefaultConfigurationClasses;

/**
 * Tests for {@link AnnotationConfigContextLoaderUtils}.
 *
 * @author Sam Brannen
 * @since 4.1.5
 */
class AnnotationConfigContextLoaderUtilsTests {

	@Test
	void detectDefaultConfigurationClassesWithNullDeclaringClass() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				detectDefaultConfigurationClasses(null));
	}

	@Test
	void detectDefaultConfigurationClassesWithoutConfigurationClass() {
		Class<?>[] configClasses = detectDefaultConfigurationClasses(NoConfigTestCase.class);
		assertThat(configClasses).isNotNull();
		assertThat(configClasses).isEmpty();
	}

	@Test
	void detectDefaultConfigurationClassesWithExplicitConfigurationAnnotation() {
		Class<?>[] configClasses = detectDefaultConfigurationClasses(ExplicitConfigTestCase.class);
		assertThat(configClasses).isNotNull();
		assertThat(configClasses).isEqualTo(new Class<?>[] { ExplicitConfigTestCase.Config.class });
	}

	@Test
	void detectDefaultConfigurationClassesWithConfigurationMetaAnnotation() {
		Class<?>[] configClasses = detectDefaultConfigurationClasses(MetaAnnotatedConfigTestCase.class);
		assertThat(configClasses).isNotNull();
		assertThat(configClasses).isEqualTo(new Class<?>[] { MetaAnnotatedConfigTestCase.Config.class });
	}


	private static class NoConfigTestCase {

	}

	private static class ExplicitConfigTestCase {

		@Configuration
		static class Config {
		}
	}

	@Configuration
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	private @interface MetaConfig {
	}

	private static class MetaAnnotatedConfigTestCase {

		@MetaConfig
		static class Config {
		}
	}

}
