/*
 * Copyright 2002-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.support;

import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextLoader;
import org.springframework.util.ObjectUtils;

/**
 * JUnit 4 based unit test which verifies proper
 * {@link ContextLoader#processLocations(Class,String...) processing} of
 * <code>resource locations</code> by a {@link GenericXmlContextLoader}
 * configured via {@link ContextConfiguration @ContextConfiguration}.
 * Specifically, this test addresses the issues raised in <a
 * href="http://opensource.atlassian.com/projects/spring/browse/SPR-3949"
 * target="_blank">SPR-3949</a>:
 * <em>ContextConfiguration annotation should accept not only classpath resources</em>.
 *
 * @author Sam Brannen
 * @since 2.5
 */
@RunWith(Parameterized.class)
public class GenericXmlContextLoaderResourceLocationsTests {

	private static final Log logger = LogFactory.getLog(GenericXmlContextLoaderResourceLocationsTests.class);

	protected final Class<?> testClass;
	protected final String[] expectedLocations;


	public GenericXmlContextLoaderResourceLocationsTests(final Class<?> testClass, final String[] expectedLocations) {
		this.testClass = testClass;
		this.expectedLocations = expectedLocations;
	}

	@Parameters
	public static Collection<Object[]> contextConfigurationLocationsData() {
		return Arrays.asList(new Object[][] {

			{
				ClasspathDefaultLocationsTest.class,
				new String[] { "classpath:/org/springframework/test/context/support/GenericXmlContextLoaderResourceLocationsTests$ClasspathDefaultLocationsTest-context.xml" } },

			{
				ImplicitClasspathLocationsTest.class,
				new String[] { "classpath:/org/springframework/test/context/support/context1.xml",
					"classpath:/org/springframework/test/context/support/context2.xml" } },

			{ ExplicitClasspathLocationsTest.class, new String[] { "classpath:context.xml" } },

			{ ExplicitFileLocationsTest.class, new String[] { "file:/testing/directory/context.xml" } },

			{ ExplicitUrlLocationsTest.class, new String[] { "http://example.com/context.xml" } },

			{
				ExplicitMixedPathTypesLocationsTest.class,
				new String[] { "classpath:/org/springframework/test/context/support/context1.xml",
					"classpath:context2.xml", "classpath:/context3.xml", "file:/testing/directory/context.xml",
					"http://example.com/context.xml" } }

		});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void assertContextConfigurationLocations() throws Exception {

		final ContextConfiguration contextConfig = this.testClass.getAnnotation(ContextConfiguration.class);
		final ContextLoader contextLoader = new GenericXmlContextLoader();
		final String[] configuredLocations = (String[]) AnnotationUtils.getValue(contextConfig, "locations");
		final String[] processedLocations = contextLoader.processLocations(this.testClass, configuredLocations);

		if (logger.isDebugEnabled()) {
			logger.debug("----------------------------------------------------------------------");
			logger.debug("Configured locations: " + ObjectUtils.nullSafeToString(configuredLocations));
			logger.debug("Expected   locations: " + ObjectUtils.nullSafeToString(this.expectedLocations));
			logger.debug("Processed  locations: " + ObjectUtils.nullSafeToString(processedLocations));
		}

		assertArrayEquals("Verifying locations for test [" + this.testClass + "].", this.expectedLocations,
				processedLocations);
	}


	@ContextConfiguration
	private static class ClasspathDefaultLocationsTest {
	}

	@ContextConfiguration(locations = { "context1.xml", "context2.xml" })
	private static class ImplicitClasspathLocationsTest {
	}

	@ContextConfiguration(locations = { "classpath:context.xml" })
	private static class ExplicitClasspathLocationsTest {
	}

	@ContextConfiguration(locations = { "file:/testing/directory/context.xml" })
	private static class ExplicitFileLocationsTest {
	}

	@ContextConfiguration(locations = { "http://example.com/context.xml" })
	private static class ExplicitUrlLocationsTest {
	}

	@ContextConfiguration(locations = { "context1.xml", "classpath:context2.xml", "/context3.xml",
		"file:/testing/directory/context.xml", "http://example.com/context.xml" })
	private static class ExplicitMixedPathTypesLocationsTest {
	}

}
