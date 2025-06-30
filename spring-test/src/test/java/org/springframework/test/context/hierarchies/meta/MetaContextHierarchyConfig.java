/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
