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

package org.springframework.beans.factory.parsing;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.beans.TestBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Rob Harrop
 * @since 2.0
 */
public class CustomProblemReporterTests extends TestCase {

	private CollatingProblemReporter problemReporter;

	private DefaultListableBeanFactory beanFactory;

	private XmlBeanDefinitionReader reader;


	protected void setUp() throws Exception {
		this.problemReporter = new CollatingProblemReporter();
		this.beanFactory = new DefaultListableBeanFactory();
		this.reader = new XmlBeanDefinitionReader(this.beanFactory);
		this.reader.setProblemReporter(this.problemReporter);
	}

	public void testErrorsAreCollated() throws Exception {
		this.reader.loadBeanDefinitions(new ClassPathResource("withErrors.xml", getClass()));
		assertEquals("Incorrect number of errors collated", 4, this.problemReporter.getErrors().length);

		TestBean bean = (TestBean) this.beanFactory.getBean("validBean");
		assertNotNull(bean);
	}


	private static class CollatingProblemReporter implements ProblemReporter {

		private List errors = new ArrayList();

		private List warnings = new ArrayList();


		public void fatal(Problem problem) {
			throw new BeanDefinitionParsingException(problem);
		}

		public void error(Problem problem) {
			System.out.println(problem);
			this.errors.add(problem);
		}

		public Problem[] getErrors() {
			return (Problem[]) this.errors.toArray(new Problem[this.errors.size()]);
		}

		public void warning(Problem problem) {
			System.out.println(problem);
			this.warnings.add(problem);
		}

		public Problem[] getWarnings() {
			return (Problem[]) this.warnings.toArray(new Problem[this.warnings.size()]);
		}
	}

}
