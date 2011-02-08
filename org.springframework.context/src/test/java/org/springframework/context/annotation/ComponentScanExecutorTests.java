/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.context.annotation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.parsing.FailFastProblemReporter;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.config.ExecutorContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mock.env.MockEnvironment;

/**
 * Unit tests for {@link ComponentScanExecutor}.
 * 
 * @author Chris Beams
 * @since 3.1
 */
public class ComponentScanExecutorTests {

	private ComponentScanExecutor executor;
	private ExecutorContext executorContext;
	private DefaultListableBeanFactory bf;

	@Before
	public void setUp() {
		this.bf = new DefaultListableBeanFactory();
		this.executor = new ComponentScanExecutor();
		this.executorContext = new ExecutorContext();
		this.executorContext.setRegistry(bf);
		this.executorContext.setResourceLoader(new DefaultResourceLoader());
		this.executorContext.setEnvironment(new MockEnvironment());
		this.executorContext.setRegistrar(new SimpleComponentRegistrar(bf));
		this.executorContext.setProblemReporter(new FailFastProblemReporter());
	}

	@Test
	public void validSpec() {
		this.executor.execute(new ComponentScanSpec("example.scannable"), this.executorContext);
		assertThat(bf.containsBean("fooServiceImpl"), is(true));
	}

	@Test(expected=BeanDefinitionParsingException.class)
	public void invalidSpec() {
		// ff problem reporter should throw due to no packages specified
		this.executor.execute(new ComponentScanSpec(), this.executorContext);
	}

}
