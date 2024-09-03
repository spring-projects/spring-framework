/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.context.aot.samples.basic;

import org.junit.runner.RunWith;

import org.springframework.aot.AotDetector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.aot.AotTestAttributes;
import org.springframework.test.context.aot.samples.basic.BasicSpringVintageTests.CustomXmlBootstrapper;
import org.springframework.test.context.aot.samples.common.MessageService;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.support.DefaultTestContextBootstrapper;
import org.springframework.test.context.support.GenericXmlContextLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 6.0
 */
@BootstrapWith(CustomXmlBootstrapper.class)
@RunWith(SpringRunner.class)
// Override the default loader configured by the CustomXmlBootstrapper
@ContextConfiguration(classes = BasicTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
@TestPropertySource
public class BasicSpringVintageTests {

	@Autowired
	ApplicationContext context;

	@Autowired
	MessageService messageService;

	@Value("${test.engine}")
	String testEngine;

	@org.junit.Test
	public void test() {
		assertThat(messageService.generateMessage()).isEqualTo("Hello, AOT!");
		assertThat(testEngine).isEqualTo("vintage");
		assertThat(context.getEnvironment().getProperty("test.engine"))
			.as("@TestPropertySource").isEqualTo("vintage");
	}

	public static class CustomXmlBootstrapper extends DefaultTestContextBootstrapper {

		@Override
		protected Class<? extends ContextLoader> getDefaultContextLoaderClass(Class<?> testClass) {
			return GenericXmlContextLoader.class;
		}

		@Override
		protected MergedContextConfiguration processMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
			String stringKey = "@SpringBootConfiguration-" + mergedConfig.getTestClass().getName();
			String booleanKey1 = "@SpringBootConfiguration-" + mergedConfig.getTestClass().getName() + "-active1";
			String booleanKey2 = "@SpringBootConfiguration-" + mergedConfig.getTestClass().getName() + "-active2";
			AotTestAttributes aotAttributes = AotTestAttributes.getInstance();
			if (AotDetector.useGeneratedArtifacts()) {
				assertThat(aotAttributes.getString(stringKey))
					.as("AOT String attribute must already be present during AOT run-time execution")
					.isEqualTo("org.example.Main");
				assertThat(aotAttributes.getBoolean(booleanKey1))
					.as("AOT boolean attribute 1 must already be present during AOT run-time execution")
					.isTrue();
				assertThat(aotAttributes.getBoolean(booleanKey2))
					.as("AOT boolean attribute 2 must already be present during AOT run-time execution")
					.isTrue();
			}
			else {
				// Set AOT attributes during AOT build-time processing
				aotAttributes.setAttribute(stringKey, "org.example.Main");
				aotAttributes.setAttribute(booleanKey1, "TrUe");
				aotAttributes.setAttribute(booleanKey2, true);
			}
			return mergedConfig;
		}

	}

}
