/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.beans.factory.parsing;

import static org.junit.Assert.*;
import static test.util.TestResourceUtils.qualifiedResource;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.Resource;

import test.beans.TestBean;

/**
 * @author Rob Harrop
 * @author Chris Beams
 * @since 2.0
 */
public final class CustomProblemReporterTests {

	private static final Resource CONTEXT = qualifiedResource(CustomProblemReporterTests.class, "context.xml");

	private CollatingProblemReporter problemReporter;

	private DefaultListableBeanFactory beanFactory;

	private XmlBeanDefinitionReader reader;


	@Before
	public void setUp() {
		this.problemReporter = new CollatingProblemReporter();
		this.beanFactory = new DefaultListableBeanFactory();
		this.reader = new XmlBeanDefinitionReader(this.beanFactory);
		this.reader.setProblemReporter(this.problemReporter);
	}

	@Test
	public void testErrorsAreCollated() {
		this.reader.loadBeanDefinitions(CONTEXT);
		assertEquals("Incorrect number of errors collated", 4, this.problemReporter.getErrors().length);

		TestBean bean = (TestBean) this.beanFactory.getBean("validBean");
		assertNotNull(bean);
	}


	private static class CollatingProblemReporter implements ProblemReporter {

		private List<Problem> errors = new ArrayList<Problem>();

		private List<Problem> warnings = new ArrayList<Problem>();


		@Override
		public void fatal(Problem problem) {
			throw new BeanDefinitionParsingException(problem);
		}

		@Override
		public void error(Problem problem) {
			System.out.println(problem);
			this.errors.add(problem);
		}

		public Problem[] getErrors() {
			return this.errors.toArray(new Problem[this.errors.size()]);
		}

		@Override
		public void warning(Problem problem) {
			System.out.println(problem);
			this.warnings.add(problem);
		}

		public Problem[] getWarnings() {
			return this.warnings.toArray(new Problem[this.warnings.size()]);
		}
	}

}
