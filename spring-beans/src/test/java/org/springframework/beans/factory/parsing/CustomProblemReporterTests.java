/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.parsing;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.testfixture.io.ResourceTestUtils.qualifiedResource;

/**
 * @author Rob Harrop
 * @author Chris Beams
 * @since 2.0
 */
class CustomProblemReporterTests {

	private CollatingProblemReporter problemReporter;

	private DefaultListableBeanFactory beanFactory;

	private XmlBeanDefinitionReader reader;


	@BeforeEach
	void setup() {
		this.problemReporter = new CollatingProblemReporter();
		this.beanFactory = new DefaultListableBeanFactory();
		this.reader = new XmlBeanDefinitionReader(this.beanFactory);
		this.reader.setProblemReporter(this.problemReporter);
	}


	@Test
	void errorsAreCollated() {
		this.reader.loadBeanDefinitions(qualifiedResource(CustomProblemReporterTests.class, "context.xml"));
		assertThat(this.problemReporter.getErrors()).as("Incorrect number of errors collated").hasSize(4);

		TestBean bean = (TestBean) this.beanFactory.getBean("validBean");
		assertThat(bean).isNotNull();
	}


	private static class CollatingProblemReporter implements ProblemReporter {

		private final List<Problem> errors = new ArrayList<>();

		private final List<Problem> warnings = new ArrayList<>();

		@Override
		public void fatal(Problem problem) {
			throw new BeanDefinitionParsingException(problem);
		}

		@Override
		public void error(Problem problem) {
			this.errors.add(problem);
		}

		public Problem[] getErrors() {
			return this.errors.toArray(new Problem[this.errors.size()]);
		}

		@Override
		public void warning(Problem problem) {
			this.warnings.add(problem);
		}
	}

}
