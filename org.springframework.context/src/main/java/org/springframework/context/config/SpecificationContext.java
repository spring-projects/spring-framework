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

package org.springframework.context.config;

import org.springframework.beans.factory.parsing.ComponentRegistrar;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

/**
 * TODO: rename to SpecificationContext?
 *
 * @author Chris Beams
 * @since 3.1
 */
public class ExecutorContext {

	private BeanDefinitionRegistry registry;
	private ComponentRegistrar registrar;
	private ResourceLoader resourceLoader;
	private Environment environment;
	private ProblemReporter problemReporter;

	public ExecutorContext() { }

	public void setRegistry(BeanDefinitionRegistry registry) {
		this.registry = registry;
	}

	public BeanDefinitionRegistry getRegistry() {
		return this.registry;
	}

	public void setRegistrar(ComponentRegistrar registrar) {
		this.registrar = registrar;
	}

	public ComponentRegistrar getRegistrar() {
		return this.registrar;
	}

	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	public ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}

	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	public Environment getEnvironment() {
		return this.environment;
	}

	public void setProblemReporter(ProblemReporter problemReporter) {
		this.problemReporter = problemReporter;
	}

	public ProblemReporter getProblemReporter() {
		return this.problemReporter;
	}

}
