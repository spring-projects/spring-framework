/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.index;

import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CandidateComponentsIndex}.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
class CandidateComponentsIndexTests {

	@Nested
	class ComponentIndexFilesTests {

		@Test
		void getCandidateTypes() {
			var index = new CandidateComponentsIndex(List.of(createSampleProperties()));
			var candidateTypes = index.getCandidateTypes("com.example.service", "service");
			assertThat(candidateTypes).contains("com.example.service.One",
				"com.example.service.sub.Two", "com.example.service.Three");
		}

		@Test
		void getCandidateTypesNoMatch() {
			var index = new CandidateComponentsIndex(List.of(createSampleProperties()));
			var candidateTypes = index.getCandidateTypes("com.example.service", "entity");
			assertThat(candidateTypes).isEmpty();
		}

		@Test
		void getCandidateTypesSubPackage() {
			var index = new CandidateComponentsIndex(List.of(createSampleProperties()));
			var candidateTypes = index.getCandidateTypes("com.example.service.sub", "service");
			assertThat(candidateTypes).contains("com.example.service.sub.Two");
		}

		@Test
		void getCandidateTypesSubPackageNoMatch() {
			var index = new CandidateComponentsIndex(List.of(createSampleProperties()));
			var candidateTypes = index.getCandidateTypes("com.example.service.none", "service");
			assertThat(candidateTypes).isEmpty();
		}

		@Test
		void parsesMultipleCandidateStereotypes() {
			var index = new CandidateComponentsIndex(List.of(
				createProperties("com.example.Foo", "service", "entity")));
			assertThat(index.getCandidateTypes("com.example", "service")).contains("com.example.Foo");
			assertThat(index.getCandidateTypes("com.example", "entity")).contains("com.example.Foo");
		}

		@Test
		void mergesCandidateStereotypes() {
			var index = new CandidateComponentsIndex(List.of(
				createProperties("com.example.Foo", "service"),
				createProperties("com.example.Foo", "entity")));
			assertThat(index.getCandidateTypes("com.example", "service")).contains("com.example.Foo");
			assertThat(index.getCandidateTypes("com.example", "entity")).contains("com.example.Foo");
		}

		private static Properties createSampleProperties() {
			var properties = new Properties();
			properties.put("com.example.service.One", "service");
			properties.put("com.example.service.sub.Two", "service");
			properties.put("com.example.service.Three", "service");
			properties.put("com.example.domain.Four", "entity");
			return properties;
		}

		private static Properties createProperties(String key, String... stereotypes) {
			var properties = new Properties();
			properties.put(key, String.join(",", stereotypes));
			return properties;
		}

	}

	@Nested
	class ProgrammaticIndexTests {

		@ParameterizedTest  // gh-35601
		@ValueSource(strings = {
			"com.example.service",
			"com.example.service.sub",
			"com.example.service.subX",
			"com.example.domain",
			"com.example.domain.X"
		})
		void hasScannedPackage(String packageName) {
			var index = new CandidateComponentsIndex();
			index.registerScan("com.example.service", "com.example.service.sub", "com.example.domain");
			assertThat(index.hasScannedPackage(packageName)).isTrue();
		}

		@ParameterizedTest  // gh-35601
		@ValueSource(strings = {
			"com.example.service",
			"com.example.domain",
			"com.example.web",
		})
		void hasScannedPackageForStarPattern(String packageName) {
			var index = new CandidateComponentsIndex();
			index.registerScan("com.example.*");
			assertThat(index.hasScannedPackage(packageName)).isTrue();
		}

		@ParameterizedTest  // gh-35601
		@ValueSource(strings = {
			"com.example",
			"com.example.service",
			"com.example.service.sub",
		})
		void hasScannedPackageForStarStarPattern(String packageName) {
			var index = new CandidateComponentsIndex();
			index.registerScan("com.example.**");
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
			var index = new CandidateComponentsIndex();
			index.registerScan("com.example.service", "com.example.domain");
			assertThat(index.hasScannedPackage(packageName)).isFalse();
		}

		@ParameterizedTest  // gh-35601
		@ValueSource(strings = {
			"com.example",
			"com.exampleX",
			"com.exampleX.service"
		})
		void hasScannedPackageForStarPatternWithNoMatch(String packageName) {
			var index = new CandidateComponentsIndex();
			index.registerScan("com.example.*");
			assertThat(index.hasScannedPackage(packageName)).isFalse();
		}

		@ParameterizedTest  // gh-35601
		@ValueSource(strings = {
			"com.exampleX",
			"com.exampleX.service"
		})
		void hasScannedPackageForStarStarPatternWithNoMatch(String packageName) {
			var index = new CandidateComponentsIndex();
			index.registerScan("com.example.**");
			assertThat(index.hasScannedPackage(packageName)).isFalse();
		}

	}

}
