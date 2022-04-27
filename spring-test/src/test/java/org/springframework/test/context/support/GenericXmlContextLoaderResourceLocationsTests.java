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

package org.springframework.test.context.support;

import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextLoader;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Unit test which verifies proper
 * {@link ContextLoader#processLocations(Class, String...) processing} of
 * {@code resource locations} by a {@link GenericXmlContextLoader}
 * configured via {@link ContextConfiguration @ContextConfiguration}.
 * Specifically, this test addresses the issues raised in <a
 * href="https://opensource.atlassian.com/projects/spring/browse/SPR-3949"
 * target="_blank">SPR-3949</a>:
 * <em>ContextConfiguration annotation should accept not only classpath resources</em>.
 *
 * @author Sam Brannen
 * @since 2.5
 */
class GenericXmlContextLoaderResourceLocationsTests {

	private static final Log logger = LogFactory.getLog(GenericXmlContextLoaderResourceLocationsTests.class);


	@ParameterizedTest(name = "[{index}] {0}")
	@MethodSource("contextConfigurationLocationsData")
	void assertContextConfigurationLocations(Class<?> testClass, String[] expectedLocations) throws Exception {
		ContextConfiguration contextConfig = testClass.getAnnotation(ContextConfiguration.class);
		ContextLoader contextLoader = new GenericXmlContextLoader();
		String[] configuredLocations = (String[]) AnnotationUtils.getValue(contextConfig);
		String[] processedLocations = contextLoader.processLocations(testClass, configuredLocations);

		if (logger.isDebugEnabled()) {
			logger.debug("----------------------------------------------------------------------");
			logger.debug("Configured locations: " + ObjectUtils.nullSafeToString(configuredLocations));
			logger.debug("Expected   locations: " + ObjectUtils.nullSafeToString(expectedLocations));
			logger.debug("Processed  locations: " + ObjectUtils.nullSafeToString(processedLocations));
		}

		assertThat(processedLocations).as("Verifying locations for test [" + testClass + "].").isEqualTo(expectedLocations);
	}

	static Stream<Arguments> contextConfigurationLocationsData() {
		return Stream.of(
			args(ClasspathNonExistentDefaultLocationsTestCase.class, array()),

			args(ClasspathExistentDefaultLocationsTestCase.class, array(
				"classpath:org/springframework/test/context/support/GenericXmlContextLoaderResourceLocationsTests$ClasspathExistentDefaultLocationsTestCase-context.xml")),

			args(ImplicitClasspathLocationsTestCase.class,
				array("classpath:/org/springframework/test/context/support/context1.xml",
					"classpath:/org/springframework/test/context/support/context2.xml")),

			args(ExplicitClasspathLocationsTestCase.class, array("classpath:context.xml")),

			args(ExplicitFileLocationsTestCase.class, array("file:/testing/directory/context.xml")),

			args(ExplicitUrlLocationsTestCase.class, array("https://example.com/context.xml")),

			args(ExplicitMixedPathTypesLocationsTestCase.class,
				array("classpath:/org/springframework/test/context/support/context1.xml", "classpath:context2.xml",
					"classpath:/context3.xml", "file:/testing/directory/context.xml",
					"https://example.com/context.xml"))
		);
	}

	private static Arguments args(Class<?> testClass, String[] expectedLocations) {
		return arguments(named(testClass.getSimpleName(), testClass), expectedLocations);
	}

	private static String[] array(String... elements) {
		return elements;
	}

	@ContextConfiguration
	class ClasspathNonExistentDefaultLocationsTestCase {
	}

	@ContextConfiguration
	class ClasspathExistentDefaultLocationsTestCase {
	}

	@ContextConfiguration({ "context1.xml", "context2.xml" })
	class ImplicitClasspathLocationsTestCase {
	}

	@ContextConfiguration("classpath:context.xml")
	class ExplicitClasspathLocationsTestCase {
	}

	@ContextConfiguration("file:/testing/directory/context.xml")
	class ExplicitFileLocationsTestCase {
	}

	@ContextConfiguration("https://example.com/context.xml")
	class ExplicitUrlLocationsTestCase {
	}

	@ContextConfiguration({ "context1.xml", "classpath:context2.xml", "/context3.xml",
		"file:/testing/directory/context.xml", "https://example.com/context.xml" })
	class ExplicitMixedPathTypesLocationsTestCase {
	}

}
