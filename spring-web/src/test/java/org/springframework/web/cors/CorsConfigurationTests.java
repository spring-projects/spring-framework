/*
 * Copyright 2002-2020 the original author or authors.
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

/**
 * Unit tests for {@link CorsConfiguration}.
 *
 * @author Sebastien Deleuze
 * @author Sam Brannen
 */
public class CorsConfigurationTests {

	@Test
	public void setNullValues() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(null);
		assertThat(config.getAllowedOrigins()).isNull();
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
	public void setValues() {
		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedOrigin("*");
		assertThat(config.getAllowedOrigins()).isEqualTo(Arrays.asList("*"));
		config.addAllowedHeader("*");
		assertThat(config.getAllowedHeaders()).isEqualTo(Arrays.asList("*"));
		config.addAllowedMethod("*");
		assertThat(config.getAllowedMethods()).isEqualTo(Arrays.asList("*"));
		config.addExposedHeader("*");
		config.setAllowCredentials(true);
		assertThat((boolean) config.getAllowCredentials()).isTrue();
		config.setMaxAge(123L);
		assertThat(config.getMaxAge()).isEqualTo(new Long(123));
	}

	@Test
	public void combineWithNull() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(Arrays.asList("*"));
		config.combine(null);
		assertThat(config.getAllowedOrigins()).isEqualTo(Arrays.asList("*"));
	}

	@Test
	public void combineWithNullProperties() {
		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedOrigin("*");
		config.addAllowedHeader("header1");
		config.addExposedHeader("header3");
		config.addAllowedMethod(HttpMethod.GET.name());
		config.setMaxAge(123L);
		config.setAllowCredentials(true);
		CorsConfiguration other = new CorsConfiguration();
		config = config.combine(other);
		assertThat(config.getAllowedOrigins()).isEqualTo(Arrays.asList("*"));
		assertThat(config.getAllowedHeaders()).isEqualTo(Arrays.asList("header1"));
		assertThat(config.getExposedHeaders()).isEqualTo(Arrays.asList("header3"));
		assertThat(config.getAllowedMethods()).isEqualTo(Arrays.asList(HttpMethod.GET.name()));
		assertThat(config.getMaxAge()).isEqualTo(new Long(123));
		assertThat((boolean) config.getAllowCredentials()).isTrue();
	}

	@Test  // SPR-15772
	public void combineWithDefaultPermitValues() {
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
	public void combineWithAsteriskWildCard() {
		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedOrigin("*");
		config.addAllowedHeader("*");
		config.addExposedHeader("*");
		config.addAllowedMethod("*");
		CorsConfiguration other = new CorsConfiguration();
		other.addAllowedOrigin("https://domain.com");
		other.addAllowedHeader("header1");
		other.addExposedHeader("header2");
		other.addAllowedHeader("anotherHeader1");
		other.addExposedHeader("anotherHeader2");
		other.addAllowedMethod(HttpMethod.PUT.name());
		CorsConfiguration combinedConfig = config.combine(other);
		assertThat(combinedConfig).isNotNull();
		assertThat(combinedConfig.getAllowedOrigins()).containsExactly("*");
		assertThat(combinedConfig.getAllowedHeaders()).containsExactly("*");
		assertThat(combinedConfig.getExposedHeaders()).containsExactly("*");
		assertThat(combinedConfig.getAllowedMethods()).containsExactly("*");

		combinedConfig = other.combine(config);
		assertThat(combinedConfig).isNotNull();
		assertThat(combinedConfig.getAllowedOrigins()).containsExactly("*");
		assertThat(combinedConfig.getAllowedHeaders()).containsExactly("*");
		assertThat(combinedConfig.getExposedHeaders()).containsExactly("*");
		assertThat(combinedConfig.getAllowedMethods()).containsExactly("*");
		assertThat(combinedConfig.getAllowedHeaders()).containsExactly("*");
	}

	@Test  // SPR-14792
	public void combineWithDuplicatedElements() {
		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedOrigin("https://domain1.com");
		config.addAllowedOrigin("https://domain2.com");
		config.addAllowedHeader("header1");
		config.addAllowedHeader("header2");
		config.addExposedHeader("header3");
		config.addExposedHeader("header4");
		config.addAllowedMethod(HttpMethod.GET.name());
		config.addAllowedMethod(HttpMethod.PUT.name());
		CorsConfiguration other = new CorsConfiguration();
		other.addAllowedOrigin("https://domain1.com");
		other.addAllowedHeader("header1");
		other.addExposedHeader("header3");
		other.addAllowedMethod(HttpMethod.GET.name());
		CorsConfiguration combinedConfig = config.combine(other);
		assertThat(combinedConfig.getAllowedOrigins()).isEqualTo(Arrays.asList("https://domain1.com", "https://domain2.com"));
		assertThat(combinedConfig.getAllowedHeaders()).isEqualTo(Arrays.asList("header1", "header2"));
		assertThat(combinedConfig.getExposedHeaders()).isEqualTo(Arrays.asList("header3", "header4"));
		assertThat(combinedConfig.getAllowedMethods()).isEqualTo(Arrays.asList(HttpMethod.GET.name(), HttpMethod.PUT.name()));
	}

	@Test
	public void combine() {
		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedOrigin("https://domain1.com");
		config.addAllowedHeader("header1");
		config.addExposedHeader("header3");
		config.addAllowedMethod(HttpMethod.GET.name());
		config.setMaxAge(123L);
		config.setAllowCredentials(true);
		CorsConfiguration other = new CorsConfiguration();
		other.addAllowedOrigin("https://domain2.com");
		other.addAllowedHeader("header2");
		other.addExposedHeader("header4");
		other.addAllowedMethod(HttpMethod.PUT.name());
		other.setMaxAge(456L);
		other.setAllowCredentials(false);
		config = config.combine(other);
		assertThat(config.getAllowedOrigins()).isEqualTo(Arrays.asList("https://domain1.com", "https://domain2.com"));
		assertThat(config.getAllowedHeaders()).isEqualTo(Arrays.asList("header1", "header2"));
		assertThat(config.getExposedHeaders()).isEqualTo(Arrays.asList("header3", "header4"));
		assertThat(config.getAllowedMethods()).isEqualTo(Arrays.asList(HttpMethod.GET.name(), HttpMethod.PUT.name()));
		assertThat(config.getMaxAge()).isEqualTo(new Long(456));
		assertThat((boolean) config.getAllowCredentials()).isFalse();
	}

	@Test
	public void checkOriginAllowed() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(Arrays.asList("*"));
		assertThat(config.checkOrigin("https://domain.com")).isEqualTo("*");
		config.setAllowCredentials(true);
		assertThat(config.checkOrigin("https://domain.com")).isEqualTo("https://domain.com");
		config.setAllowedOrigins(Arrays.asList("https://domain.com"));
		assertThat(config.checkOrigin("https://domain.com")).isEqualTo("https://domain.com");
		config.setAllowCredentials(false);
		assertThat(config.checkOrigin("https://domain.com")).isEqualTo("https://domain.com");
	}

	@Test
	public void checkOriginNotAllowed() {
		CorsConfiguration config = new CorsConfiguration();
		assertThat(config.checkOrigin(null)).isNull();
		assertThat(config.checkOrigin("https://domain.com")).isNull();
		config.addAllowedOrigin("*");
		assertThat(config.checkOrigin(null)).isNull();
		config.setAllowedOrigins(Arrays.asList("https://domain1.com"));
		assertThat(config.checkOrigin("https://domain2.com")).isNull();
		config.setAllowedOrigins(new ArrayList<>());
		assertThat(config.checkOrigin("https://domain.com")).isNull();
	}

	@Test
	public void checkMethodAllowed() {
		CorsConfiguration config = new CorsConfiguration();
		assertThat(config.checkHttpMethod(HttpMethod.GET)).isEqualTo(Arrays.asList(HttpMethod.GET, HttpMethod.HEAD));
		config.addAllowedMethod("GET");
		assertThat(config.checkHttpMethod(HttpMethod.GET)).isEqualTo(Arrays.asList(HttpMethod.GET));
		config.addAllowedMethod("POST");
		assertThat(config.checkHttpMethod(HttpMethod.GET)).isEqualTo(Arrays.asList(HttpMethod.GET, HttpMethod.POST));
		assertThat(config.checkHttpMethod(HttpMethod.POST)).isEqualTo(Arrays.asList(HttpMethod.GET, HttpMethod.POST));
	}

	@Test
	public void checkMethodNotAllowed() {
		CorsConfiguration config = new CorsConfiguration();
		assertThat(config.checkHttpMethod(null)).isNull();
		assertThat(config.checkHttpMethod(HttpMethod.DELETE)).isNull();
		config.setAllowedMethods(new ArrayList<>());
		assertThat(config.checkHttpMethod(HttpMethod.POST)).isNull();
	}

	@Test
	public void checkHeadersAllowed() {
		CorsConfiguration config = new CorsConfiguration();
		assertThat(config.checkHeaders(Collections.emptyList())).isEqualTo(Collections.emptyList());
		config.addAllowedHeader("header1");
		config.addAllowedHeader("header2");
		assertThat(config.checkHeaders(Arrays.asList("header1"))).isEqualTo(Arrays.asList("header1"));
		assertThat(config.checkHeaders(Arrays.asList("header1", "header2"))).isEqualTo(Arrays.asList("header1", "header2"));
		assertThat(config.checkHeaders(Arrays.asList("header1", "header2", "header3"))).isEqualTo(Arrays.asList("header1", "header2"));
	}

	@Test
	public void checkHeadersNotAllowed() {
		CorsConfiguration config = new CorsConfiguration();
		assertThat(config.checkHeaders(null)).isNull();
		assertThat(config.checkHeaders(Arrays.asList("header1"))).isNull();
		config.setAllowedHeaders(Collections.emptyList());
		assertThat(config.checkHeaders(Arrays.asList("header1"))).isNull();
		config.addAllowedHeader("header2");
		config.addAllowedHeader("header3");
		assertThat(config.checkHeaders(Arrays.asList("header1"))).isNull();
	}

	@Test  // SPR-15772
	public void changePermitDefaultValues() {
		CorsConfiguration config = new CorsConfiguration().applyPermitDefaultValues();
		config.addAllowedOrigin("https://domain.com");
		config.addAllowedHeader("header1");
		config.addAllowedMethod("PATCH");
		assertThat(config.getAllowedOrigins()).isEqualTo(Arrays.asList("*", "https://domain.com"));
		assertThat(config.getAllowedHeaders()).isEqualTo(Arrays.asList("*", "header1"));
		assertThat(config.getAllowedMethods()).isEqualTo(Arrays.asList("GET", "HEAD", "POST", "PATCH"));
	}

}
