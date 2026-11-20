/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.hierarchies.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;

/**
 * Custom context hierarchy configuration annotation.
 *
 * @author Sam Brannen
 * @since 4.0.3
 */
@ContextHierarchy(@ContextConfiguration(classes = { DevConfig.class, ProductionConfig.class }))
@ActiveProfiles("dev")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MetaContextHierarchyConfig {
}

@Configuration
@DevProfile
class DevConfig {

	@Bean
	public String foo() {
		return "Dev Foo";
	}
}

@Configuration
@ProdProfile
class ProductionConfig {

	@Bean
	public String foo() {
		return "Production Foo";
	}
}

@Profile("dev")
@Retention(RetentionPolicy.RUNTIME)
@interface DevProfile {
}

@Profile("prod")
@Retention(RetentionPolicy.RUNTIME)
@interface ProdProfile {
}
