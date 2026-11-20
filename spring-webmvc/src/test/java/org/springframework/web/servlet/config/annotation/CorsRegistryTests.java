/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.config.annotation;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture with a {@link CorsRegistry}.
 *
 * @author Sebastien Deleuze
 */
class CorsRegistryTests {

	private CorsRegistry registry;

	@BeforeEach
	void setUp() {
		this.registry = new CorsRegistry();
	}

	@Test
	void noMapping() {
		assertThat(this.registry.getCorsConfigurations()).isEmpty();
	}

	@Test
	void multipleMappings() {
		this.registry.addMapping("/foo");
		this.registry.addMapping("/bar");
		assertThat(this.registry.getCorsConfigurations()).hasSize(2);
	}

	@Test
	void customizedMapping() {
		this.registry.addMapping("/foo").allowedOrigins("https://domain2.com", "https://domain2.com")
				.allowedMethods("DELETE").allowCredentials(true).allowPrivateNetwork(true)
				.allowedHeaders("header1", "header2").exposedHeaders("header3", "header4").maxAge(3600);
		Map<String, CorsConfiguration> configs = this.registry.getCorsConfigurations();
		assertThat(configs).hasSize(1);
		CorsConfiguration config = configs.get("/foo");
		assertThat(config.getAllowedOrigins()).isEqualTo(Arrays.asList("https://domain2.com", "https://domain2.com"));
		assertThat(config.getAllowedMethods()).isEqualTo(Collections.singletonList("DELETE"));
		assertThat(config.getAllowedHeaders()).isEqualTo(Arrays.asList("header1", "header2"));
		assertThat(config.getExposedHeaders()).isEqualTo(Arrays.asList("header3", "header4"));
		assertThat(config.getAllowCredentials()).isTrue();
		assertThat(config.getAllowPrivateNetwork()).isTrue();
		assertThat(config.getMaxAge()).isEqualTo(Long.valueOf(3600));
	}

	@Test
	void allowCredentials() {
		this.registry.addMapping("/foo").allowCredentials(true);
		CorsConfiguration config = this.registry.getCorsConfigurations().get("/foo");
		assertThat(config.getAllowedOrigins())
				.as("Globally origins=\"*\" and allowCredentials=true should be possible")
				.containsExactly("*");
	}

	@Test
	void combine() {
		CorsConfiguration otherConfig = new CorsConfiguration();
		otherConfig.addAllowedOrigin("http://localhost:3000");
		otherConfig.addAllowedMethod("*");
		otherConfig.applyPermitDefaultValues();

		this.registry.addMapping("/api/**").combine(otherConfig);

		Map<String, CorsConfiguration> configs = this.registry.getCorsConfigurations();
		assertThat(configs).hasSize(1);
		CorsConfiguration config = configs.get("/api/**");
		assertThat(config.getAllowedOrigins()).isEqualTo(Collections.singletonList("http://localhost:3000"));
		assertThat(config.getAllowedMethods()).isEqualTo(Collections.singletonList("*"));
		assertThat(config.getAllowedHeaders()).isEqualTo(Collections.singletonList("*"));
		assertThat(config.getExposedHeaders()).isEmpty();
		assertThat(config.getAllowCredentials()).isNull();
		assertThat(config.getAllowPrivateNetwork()).isNull();
		assertThat(config.getMaxAge()).isEqualTo(Long.valueOf(1800));
	}
}
