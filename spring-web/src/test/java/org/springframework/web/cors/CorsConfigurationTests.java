/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.cors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link CorsConfiguration}.
 *
 * @author Sebastien Deleuze
 * @author Sam Brannen
 */
class CorsConfigurationTests {

	@Test
	void setNullValues() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(null);
		assertThat(config.getAllowedOrigins()).isNull();
		config.setAllowedOriginPatterns(null);
		assertThat(config.getAllowedOriginPatterns()).isNull();
		config.setAllowedHeaders(null);
		assertThat(config.getAllowedHeaders()).isNull();
		config.setAllowedMethods(null);
		assertThat(config.getAllowedMethods()).isNull();
		config.setExposedHeaders(null);
		assertThat(config.getExposedHeaders()).isNull();
		config.setAllowCredentials(null);
		assertThat(config.getAllowCredentials()).isNull();
		config.setMaxAge((Long) null);
		assertThat(config.getMaxAge()).isNull();
	}

	@Test
	void setValues() {
		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedOrigin("*");
		config.addAllowedOriginPattern("http://*.example.com");
		config.addAllowedHeader("*");
		config.addAllowedMethod("*");
		config.addExposedHeader("*");
		config.setAllowCredentials(true);
		config.setMaxAge(123L);

		assertThat(config.getAllowedOrigins()).containsExactly("*");
		assertThat(config.getAllowedOriginPatterns()).containsExactly("http://*.example.com");
		assertThat(config.getAllowedHeaders()).containsExactly("*");
		assertThat(config.getAllowedMethods()).containsExactly("*");
		assertThat(config.getExposedHeaders()).containsExactly("*");
		assertThat(config.getAllowCredentials()).isTrue();
		assertThat(config.getMaxAge()).isEqualTo(Long.valueOf(123));
	}

	@Test
	void combineWithNull() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(Collections.singletonList("*"));
		config.combine(null);
		assertThat(config.getAllowedOrigins()).containsExactly("*");
		assertThat(config.getAllowedOriginPatterns()).isNull();
	}

	@Test
	void combineWithNullProperties() {
		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedOrigin("*");
		config.setAllowedOriginPatterns(Collections.singletonList("http://*.example.com"));
		config.addAllowedHeader("header1");
		config.addExposedHeader("header3");
		config.addAllowedMethod(HttpMethod.GET.name());
		config.setMaxAge(123L);
		config.setAllowCredentials(true);

		CorsConfiguration other = new CorsConfiguration();
		config = config.combine(other);

		assertThat(config).isNotNull();
		assertThat(config.getAllowedOrigins()).containsExactly("*");
		assertThat(config.getAllowedOriginPatterns()).containsExactly("http://*.example.com");
		assertThat(config.getAllowedHeaders()).containsExactly("header1");
		assertThat(config.getExposedHeaders()).containsExactly("header3");
		assertThat(config.getAllowedMethods()).containsExactly(HttpMethod.GET.name());
		assertThat(config.getMaxAge()).isEqualTo(Long.valueOf(123));
		assertThat(config.getAllowCredentials()).isTrue();
	}

	@Test  // SPR-15772
	void combineWithDefaultPermitValues() {
		CorsConfiguration config = new CorsConfiguration().applyPermitDefaultValues();
		CorsConfiguration other = new CorsConfiguration();
		other.addAllowedOrigin("https://domain.com");
		other.addAllowedHeader("header1");
		other.addAllowedMethod(HttpMethod.PUT.name());

		CorsConfiguration combinedConfig = config.combine(other);
		assertThat(combinedConfig).isNotNull();
		assertThat(combinedConfig.getAllowedOrigins()).containsExactly("https://domain.com");
		assertThat(combinedConfig.getAllowedHeaders()).containsExactly("header1");
		assertThat(combinedConfig.getAllowedMethods()).containsExactly(HttpMethod.PUT.name());
		assertThat(combinedConfig.getExposedHeaders()).isEmpty();

		combinedConfig = other.combine(config);
		assertThat(combinedConfig).isNotNull();
		assertThat(combinedConfig.getAllowedOrigins()).containsExactly("https://domain.com");
		assertThat(combinedConfig.getAllowedHeaders()).containsExactly("header1");
		assertThat(combinedConfig.getAllowedMethods()).containsExactly(HttpMethod.PUT.name());
		assertThat(combinedConfig.getExposedHeaders()).isEmpty();

		combinedConfig = config.combine(new CorsConfiguration());
		assertThat(config.getAllowedOrigins()).containsExactly("*");
		assertThat(config.getAllowedHeaders()).containsExactly("*");
		assertThat(combinedConfig).isNotNull();
		assertThat(combinedConfig.getAllowedMethods())
				.containsExactly(HttpMethod.GET.name(), HttpMethod.HEAD.name(), HttpMethod.POST.name());
		assertThat(combinedConfig.getExposedHeaders()).isEmpty();

		combinedConfig = new CorsConfiguration().combine(config);
		assertThat(config.getAllowedOrigins()).containsExactly("*");
		assertThat(config.getAllowedHeaders()).containsExactly("*");
		assertThat(combinedConfig).isNotNull();
		assertThat(combinedConfig.getAllowedMethods())
				.containsExactly(HttpMethod.GET.name(), HttpMethod.HEAD.name(), HttpMethod.POST.name());
		assertThat(combinedConfig.getExposedHeaders()).isEmpty();
	}

	@Test
	void combinePatternWithDefaultPermitValues() {
		CorsConfiguration config = new CorsConfiguration().applyPermitDefaultValues();
		CorsConfiguration other = new CorsConfiguration();
		other.addAllowedOriginPattern("http://*.com");

		CorsConfiguration combinedConfig = other.combine(config);
		assertThat(combinedConfig).isNotNull();
		assertThat(combinedConfig.getAllowedOrigins()).isNull();
		assertThat(combinedConfig.getAllowedOriginPatterns()).containsExactly("http://*.com");

		combinedConfig = config.combine(other);
		assertThat(combinedConfig).isNotNull();
		assertThat(combinedConfig.getAllowedOrigins()).isNull();
		assertThat(combinedConfig.getAllowedOriginPatterns()).containsExactly("http://*.com");
	}

	@Test
	void combinePatternWithDefaultPermitValuesAndCustomOrigin() {
		CorsConfiguration config = new CorsConfiguration().applyPermitDefaultValues();
		config.setAllowedOrigins(Collections.singletonList("https://domain.com"));

		CorsConfiguration other = new CorsConfiguration();
		other.addAllowedOriginPattern("http://*.com");

		CorsConfiguration combinedConfig = other.combine(config);
		assertThat(combinedConfig).isNotNull();
		assertThat(combinedConfig.getAllowedOrigins()).containsExactly("https://domain.com");
		assertThat(combinedConfig.getAllowedOriginPatterns()).containsExactly("http://*.com");

		combinedConfig = config.combine(other);
		assertThat(combinedConfig).isNotNull();
		assertThat(combinedConfig.getAllowedOrigins()).containsExactly("https://domain.com");
		assertThat(combinedConfig.getAllowedOriginPatterns()).containsExactly("http://*.com");
	}

	@Test
	void combineWithAsteriskWildCard() {
		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedOrigin("*");
		config.addAllowedHeader("*");
		config.addExposedHeader("*");
		config.addAllowedMethod("*");
		config.addAllowedOriginPattern("*");

		CorsConfiguration other = new CorsConfiguration();
		other.addAllowedOrigin("https://domain.com");
		other.addAllowedOriginPattern("http://*.company.com");
		other.addAllowedHeader("header1");
		other.addExposedHeader("header2");
		other.addAllowedHeader("anotherHeader1");
		other.addExposedHeader("anotherHeader2");
		other.addAllowedMethod(HttpMethod.PUT.name());

		CorsConfiguration combinedConfig = config.combine(other);
		assertThat(combinedConfig).isNotNull();
		assertThat(combinedConfig.getAllowedOrigins()).containsExactly("*");
		assertThat(combinedConfig.getAllowedOriginPatterns()).containsExactly("*");
		assertThat(combinedConfig.getAllowedHeaders()).containsExactly("*");
		assertThat(combinedConfig.getExposedHeaders()).containsExactly("*");
		assertThat(combinedConfig.getAllowedMethods()).containsExactly("*");

		combinedConfig = other.combine(config);
		assertThat(combinedConfig).isNotNull();
		assertThat(combinedConfig.getAllowedOrigins()).containsExactly("*");
		assertThat(combinedConfig.getAllowedOriginPatterns()).containsExactly("*");
		assertThat(combinedConfig.getAllowedHeaders()).containsExactly("*");
		assertThat(combinedConfig.getExposedHeaders()).containsExactly("*");
		assertThat(combinedConfig.getAllowedMethods()).containsExactly("*");
		assertThat(combinedConfig.getAllowedHeaders()).containsExactly("*");
	}

	@Test  // SPR-14792
	void combineWithDuplicatedElements() {
		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedOrigin("https://domain1.com");
		config.addAllowedOrigin("https://domain2.com");
		config.addAllowedHeader("header1");
		config.addAllowedHeader("header2");
		config.addExposedHeader("header3");
		config.addExposedHeader("header4");
		config.addAllowedMethod(HttpMethod.GET.name());
		config.addAllowedMethod(HttpMethod.PUT.name());
		config.addAllowedOriginPattern("http://*.domain1.com");
		config.addAllowedOriginPattern("http://*.domain2.com");

		CorsConfiguration other = new CorsConfiguration();
		other.addAllowedOrigin("https://domain1.com");
		other.addAllowedOriginPattern("http://*.domain1.com");
		other.addAllowedHeader("header1");
		other.addExposedHeader("header3");
		other.addAllowedMethod(HttpMethod.GET.name());

		CorsConfiguration combinedConfig = config.combine(other);
		assertThat(combinedConfig).isNotNull();
		assertThat(combinedConfig.getAllowedOrigins()).containsExactly("https://domain1.com", "https://domain2.com");
		assertThat(combinedConfig.getAllowedHeaders()).containsExactly("header1", "header2");
		assertThat(combinedConfig.getExposedHeaders()).containsExactly("header3", "header4");
		assertThat(combinedConfig.getAllowedMethods()).containsExactly(HttpMethod.GET.name(), HttpMethod.PUT.name());
		assertThat(combinedConfig.getAllowedOriginPatterns()).containsExactly("http://*.domain1.com", "http://*.domain2.com");
	}

	@Test
	void combine() {
		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedOrigin("https://domain1.com");
		config.addAllowedOriginPattern("http://*.domain1.com");
		config.addAllowedHeader("header1");
		config.addExposedHeader("header3");
		config.addAllowedMethod(HttpMethod.GET.name());
		config.setMaxAge(123L);
		config.setAllowCredentials(true);

		CorsConfiguration other = new CorsConfiguration();
		other.addAllowedOrigin("https://domain2.com");
		other.addAllowedOriginPattern("http://*.domain2.com");
		other.addAllowedHeader("header2");
		other.addExposedHeader("header4");
		other.addAllowedMethod(HttpMethod.PUT.name());
		other.setMaxAge(456L);
		other.setAllowCredentials(false);

		config = config.combine(other);
		assertThat(config).isNotNull();
		assertThat(config.getAllowedOrigins()).containsExactly("https://domain1.com", "https://domain2.com");
		assertThat(config.getAllowedHeaders()).containsExactly("header1", "header2");
		assertThat(config.getExposedHeaders()).containsExactly("header3", "header4");
		assertThat(config.getAllowedMethods()).containsExactly(HttpMethod.GET.name(), HttpMethod.PUT.name());
		assertThat(config.getMaxAge()).isEqualTo(Long.valueOf(456));
		assertThat(config).isNotNull();
		assertThat(config.getAllowCredentials()).isFalse();
		assertThat(config.getAllowedOriginPatterns()).containsExactly("http://*.domain1.com", "http://*.domain2.com");
	}

	@Test
	void checkOriginAllowed() {
		// "*" matches
		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedOrigin("*");
		assertThat(config.checkOrigin("https://domain.com")).isEqualTo("*");

		// "*" does not match together with allowCredentials
		config.setAllowCredentials(true);
		assertThatIllegalArgumentException().isThrownBy(() -> config.checkOrigin("https://domain.com"));

		// comma-delimited origins list
		config.setAllowedOrigins(Collections.singletonList("https://a1.com,https://a2.com"));
		assertThat(config.checkOrigin("https://a1.com")).isEqualTo("https://a1.com");
		assertThat(config.checkOrigin("https://a2.com/")).isEqualTo("https://a2.com/");

		// specific origin matches Origin header with or without trailing "/"
		config.setAllowedOrigins(Collections.singletonList("https://domain.com"));
		assertThat(config.checkOrigin("https://domain.com")).isEqualTo("https://domain.com");
		assertThat(config.checkOrigin("https://domain.com/")).isEqualTo("https://domain.com/");

		// specific origin with trailing "/" matches Origin header with or without trailing "/"
		config.setAllowedOrigins(Collections.singletonList("https://domain.com/"));
		assertThat(config.checkOrigin("https://domain.com")).isEqualTo("https://domain.com");
		assertThat(config.checkOrigin("https://domain.com/")).isEqualTo("https://domain.com/");

		config.setAllowCredentials(false);
		assertThat(config.checkOrigin("https://domain.com")).isEqualTo("https://domain.com");
	}

	@Test
	void checkOriginNotAllowed() {
		CorsConfiguration config = new CorsConfiguration();
		assertThat(config.checkOrigin(null)).isNull();
		assertThat(config.checkOrigin("https://domain.com")).isNull();

		config.addAllowedOrigin("*");
		assertThat(config.checkOrigin(null)).isNull();

		config.setAllowedOrigins(Collections.singletonList("https://domain1.com"));
		assertThat(config.checkOrigin("https://domain2.com")).isNull();

		config.setAllowedOrigins(new ArrayList<>());
		assertThat(config.checkOrigin("https://domain.com")).isNull();
	}

	@Test
	void checkOriginPatternAllowed() {
		CorsConfiguration config = new CorsConfiguration();
		assertThat(config.checkOrigin("https://domain.com")).isNull();

		config.applyPermitDefaultValues();
		assertThat(config.checkOrigin("https://domain.com")).isEqualTo("*");

		config.setAllowCredentials(true);
		assertThatIllegalArgumentException().isThrownBy(() -> config.checkOrigin("https://domain.com"));

		config.addAllowedOriginPattern("https://*.domain.com");
		assertThat(config.checkOrigin("https://example.domain.com")).isEqualTo("https://example.domain.com");

		config.addAllowedOriginPattern("https://*.port.domain.com:[*]");
		assertThat(config.checkOrigin("https://example.port.domain.com")).isEqualTo("https://example.port.domain.com");
		assertThat(config.checkOrigin("https://example.port.domain.com:1234")).isEqualTo("https://example.port.domain.com:1234");

		config.addAllowedOriginPattern("https://*.specific.port.com:[8080,8081]");
		assertThat(config.checkOrigin("https://example.specific.port.com:8080")).isEqualTo("https://example.specific.port.com:8080");
		assertThat(config.checkOrigin("https://example.specific.port.com:8081")).isEqualTo("https://example.specific.port.com:8081");
		assertThat(config.checkOrigin("https://example.specific.port.com:1234")).isNull();

		config.addAllowedOriginPattern("https://*.a1.com:[8080,8081],https://*.a2.com");
		assertThat(config.checkOrigin("https://example.a1.com:8080")).isEqualTo("https://example.a1.com:8080");
		assertThat(config.checkOrigin("https://example.a1.com:8081")).isEqualTo("https://example.a1.com:8081");
		assertThat(config.checkOrigin("https://example.a1.com:8082")).isNull();
		assertThat(config.checkOrigin("https://example.a2.com")).isEqualTo("https://example.a2.com");

		config.setAllowCredentials(false);
		assertThat(config.checkOrigin("https://example.domain.com")).isEqualTo("https://example.domain.com");
	}

	@Test
	void checkOriginPatternNotAllowed() {
		CorsConfiguration config = new CorsConfiguration();
		assertThat(config.checkOrigin(null)).isNull();
		assertThat(config.checkOrigin("https://domain.com")).isNull();
		config.addAllowedOriginPattern("*");
		assertThat(config.checkOrigin(null)).isNull();

		config.setAllowedOriginPatterns(Collections.singletonList("http://*.domain1.com"));
		assertThat(config.checkOrigin("https://domain2.com")).isNull();

		config.setAllowedOriginPatterns(new ArrayList<>());
		assertThat(config.checkOrigin("https://domain.com")).isNull();

		config.setAllowedOriginPatterns(Collections.singletonList("https://*.specific.port.com:[8080,8081]"));
		assertThat(config.checkOrigin("https://example.specific.port.com:1234")).isNull();
	}

	@Test
	void checkMethodAllowed() {
		CorsConfiguration config = new CorsConfiguration();
		assertThat(config.checkHttpMethod(HttpMethod.GET)).containsExactly(HttpMethod.GET, HttpMethod.HEAD);

		config.addAllowedMethod("GET");
		assertThat(config.checkHttpMethod(HttpMethod.GET)).containsExactly(HttpMethod.GET);

		config.addAllowedMethod("POST");
		assertThat(config.checkHttpMethod(HttpMethod.GET)).containsExactly(HttpMethod.GET, HttpMethod.POST);
		assertThat(config.checkHttpMethod(HttpMethod.POST)).containsExactly(HttpMethod.GET, HttpMethod.POST);
	}

	@Test
	void checkMethodNotAllowed() {
		CorsConfiguration config = new CorsConfiguration();
		assertThat(config.checkHttpMethod(null)).isNull();
		assertThat(config.checkHttpMethod(HttpMethod.DELETE)).isNull();

		config.setAllowedMethods(new ArrayList<>());
		assertThat(config.checkHttpMethod(HttpMethod.POST)).isNull();
	}

	@Test
	void checkHeadersAllowed() {
		CorsConfiguration config = new CorsConfiguration();
		assertThat(config.checkHeaders(Collections.emptyList())).isEqualTo(Collections.emptyList());

		config.addAllowedHeader("header1");
		config.addAllowedHeader("header2");

		assertThat(config.checkHeaders(Collections.singletonList("header1"))).containsExactly("header1");
		assertThat(config.checkHeaders(Arrays.asList("header1", "header2"))).containsExactly("header1", "header2");
		assertThat(config.checkHeaders(Arrays.asList("header1", "header2", "header3"))).containsExactly("header1", "header2");
	}

	@Test
	void checkHeadersNotAllowed() {
		CorsConfiguration config = new CorsConfiguration();
		assertThat(config.checkHeaders(null)).isNull();
		assertThat(config.checkHeaders(Collections.singletonList("header1"))).isNull();

		config.setAllowedHeaders(Collections.emptyList());
		assertThat(config.checkHeaders(Collections.singletonList("header1"))).isNull();

		config.addAllowedHeader("header2");
		config.addAllowedHeader("header3");
		assertThat(config.checkHeaders(Collections.singletonList("header1"))).isNull();
	}

	@Test  // SPR-15772
	void changePermitDefaultValues() {
		CorsConfiguration config = new CorsConfiguration().applyPermitDefaultValues();
		config.addAllowedOrigin("https://domain.com");
		config.addAllowedHeader("header1");
		config.addAllowedMethod("PATCH");

		assertThat(config.getAllowedOrigins()).containsExactly("*", "https://domain.com");
		assertThat(config.getAllowedHeaders()).containsExactly("*", "header1");
		assertThat(config.getAllowedMethods()).containsExactly("GET", "HEAD", "POST", "PATCH");
	}

	@Test
	void permitDefaultDoesntSetOriginWhenPatternPresent() {
		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedOriginPattern("http://*.com");
		config = config.applyPermitDefaultValues();

		assertThat(config.getAllowedOrigins()).isNull();
		assertThat(config.getAllowedOriginPatterns()).containsExactly("http://*.com");
	}
}
