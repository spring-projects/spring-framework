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

package org.springframework.context.index;

import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CandidateComponentsIndex}.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
class CandidateComponentsIndexTests {

	@Test
	void getCandidateTypes() {
		CandidateComponentsIndex index = new CandidateComponentsIndex(List.of(createSampleProperties()));
		Set<String> actual = index.getCandidateTypes("com.example.service", "service");
		assertThat(actual).contains("com.example.service.One",
				"com.example.service.sub.Two", "com.example.service.Three");
	}

	@Test
	void getCandidateTypesNoMatch() {
		CandidateComponentsIndex index = new CandidateComponentsIndex(List.of(createSampleProperties()));
		Set<String> actual = index.getCandidateTypes("com.example.service", "entity");
		assertThat(actual).isEmpty();
	}

	@Test
	void getCandidateTypesSubPackage() {
		CandidateComponentsIndex index = new CandidateComponentsIndex(List.of(createSampleProperties()));
		Set<String> actual = index.getCandidateTypes("com.example.service.sub", "service");
		assertThat(actual).contains("com.example.service.sub.Two");
	}

	@Test
	void getCandidateTypesSubPackageNoMatch() {
		CandidateComponentsIndex index = new CandidateComponentsIndex(List.of(createSampleProperties()));
		Set<String> actual = index.getCandidateTypes("com.example.service.none", "service");
		assertThat(actual).isEmpty();
	}

	@Test
	void mergeCandidateStereotypes() {
		CandidateComponentsIndex index = new CandidateComponentsIndex(List.of(
				createProperties("com.example.Foo", "service"),
				createProperties("com.example.Foo", "entity")));
		assertThat(index.getCandidateTypes("com.example", "service")).contains("com.example.Foo");
		assertThat(index.getCandidateTypes("com.example", "entity")).contains("com.example.Foo");
	}

	@ParameterizedTest  // gh-35601
	@ValueSource(strings = {
		"com.example.service",
		"com.example.service.sub",
		"com.example.service.subX",
		"com.example.domain",
		"com.example.domain.X"
	})
	void hasScannedPackage(String packageName) {
		CandidateComponentsIndex index = new CandidateComponentsIndex();
		createSampleProperties().keySet()
				.forEach(key -> index.registerScan(ClassUtils.getPackageName((String) key)));
		assertThat(index.hasScannedPackage(packageName)).isTrue();
	}

	@ParameterizedTest  // gh-35601
	@ValueSource(strings = {
		"com.example",
		"com.exampleX",
		"com.exampleX.service",
		"com.example.serviceX",
		"com.example.domainX"
	})
	void hasScannedPackageWithNoMatch(String packageName) {
		CandidateComponentsIndex index = new CandidateComponentsIndex();
		createSampleProperties().keySet()
				.forEach(key -> index.registerScan(ClassUtils.getPackageName((String) key)));
		assertThat(index.hasScannedPackage(packageName)).isFalse();
	}


	private static Properties createProperties(String key, String stereotypes) {
		Properties properties = new Properties();
		properties.put(key, String.join(",", stereotypes));
		return properties;
	}

	private static Properties createSampleProperties() {
		Properties properties = new Properties();
		properties.put("com.example.service.One", "service");
		properties.put("com.example.service.sub.Two", "service");
		properties.put("com.example.service.Three", "service");
		properties.put("com.example.domain.Four", "entity");
		return properties;
	}

}
