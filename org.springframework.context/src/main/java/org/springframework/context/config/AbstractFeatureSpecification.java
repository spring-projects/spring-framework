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

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.parsing.SimpleProblemCollector;


/**
 * TODO SPR-7420: document
 *
 * @author Chris Beams
 * @since 3.1
 */
public abstract class AbstractFeatureSpecification implements SourceAwareSpecification {

	private static final Object DUMMY_SOURCE = new Object();
	private static final String DUMMY_SOURCE_NAME = "dummySource";

	protected Class<? extends FeatureSpecificationExecutor> executorType;

	private Object source = DUMMY_SOURCE;
	private String sourceName = DUMMY_SOURCE_NAME;

	protected AbstractFeatureSpecification(Class<? extends FeatureSpecificationExecutor> executorType) {
		this.executorType = executorType;
	}

	public final boolean validate(ProblemReporter problemReporter) {
		SimpleProblemCollector collector = new SimpleProblemCollector(this.source());
		this.doValidate(collector);
		collector.reportProblems(problemReporter);
		return collector.hasErrors() ? false : true;
	}

	protected abstract void doValidate(SimpleProblemCollector reporter);

	public AbstractFeatureSpecification source(Object source) {
		this.source = source;
		return this;
	}

	public Object source() {
		return this.source;
	}

	public AbstractFeatureSpecification sourceName(String sourceName) {
		this.sourceName = sourceName;
		return this;
	}

	public String sourceName() {
		return this.sourceName;
	}

	public void execute(ExecutorContext executorContext) {
		FeatureSpecificationExecutor executor =
			BeanUtils.instantiateClass(this.executorType, FeatureSpecificationExecutor.class);
		executor.execute(this, executorContext);
	}

}
