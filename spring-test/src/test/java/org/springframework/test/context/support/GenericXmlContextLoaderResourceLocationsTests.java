/*
 * Copyright 2002-2015 the original author or authors.
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
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

import static org.junit.Assert.*;

/**
 * JUnit 4 based unit test which verifies proper
 * {@link ContextLoader#processLocations(Class, String...) processing} of
 * {@code resource locations} by a {@link GenericXmlContextLoader}
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


	@Parameters(name = "{0}")
	public static Collection<Object[]> contextConfigurationLocationsData() {
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

		@ContextConfiguration("http://example.com/context.xml")
		class ExplicitUrlLocationsTestCase {
		}

		@ContextConfiguration({ "context1.xml", "classpath:context2.xml", "/context3.xml",
			"file:/testing/directory/context.xml", "http://example.com/context.xml" })
		class ExplicitMixedPathTypesLocationsTestCase {
		}

		return Arrays.asList(new Object[][] {

			{ ClasspathNonExistentDefaultLocationsTestCase.class.getSimpleName(), new String[] {} },

			{
				ClasspathExistentDefaultLocationsTestCase.class.getSimpleName(),
				new String[] { "classpath:org/springframework/test/context/support/GenericXmlContextLoaderResourceLocationsTests$1ClasspathExistentDefaultLocationsTestCase-context.xml" } },

			{
				ImplicitClasspathLocationsTestCase.class.getSimpleName(),
				new String[] { "classpath:/org/springframework/test/context/support/context1.xml",
					"classpath:/org/springframework/test/context/support/context2.xml" } },

			{ ExplicitClasspathLocationsTestCase.class.getSimpleName(), new String[] { "classpath:context.xml" } },

			{ ExplicitFileLocationsTestCase.class.getSimpleName(), new String[] { "file:/testing/directory/context.xml" } },

			{ ExplicitUrlLocationsTestCase.class.getSimpleName(), new String[] { "http://example.com/context.xml" } },

			{
				ExplicitMixedPathTypesLocationsTestCase.class.getSimpleName(),
				new String[] { "classpath:/org/springframework/test/context/support/context1.xml",
					"classpath:context2.xml", "classpath:/context3.xml", "file:/testing/directory/context.xml",
					"http://example.com/context.xml" } }

		});
	}

	public GenericXmlContextLoaderResourceLocationsTests(final String testClassName, final String[] expectedLocations) throws Exception {
		this.testClass = ClassUtils.forName(getClass().getName() + "$1" + testClassName, getClass().getClassLoader());
		this.expectedLocations = expectedLocations;
	}

	@Test
	public void assertContextConfigurationLocations() throws Exception {

		final ContextConfiguration contextConfig = this.testClass.getAnnotation(ContextConfiguration.class);
		final ContextLoader contextLoader = new GenericXmlContextLoader();
		final String[] configuredLocations = (String[]) AnnotationUtils.getValue(contextConfig);
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

}
