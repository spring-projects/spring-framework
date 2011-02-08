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

package org.springframework.context.annotation.configuration;

import org.springframework.beans.factory.parsing.SimpleProblemCollector;
import org.springframework.context.config.AbstractFeatureSpecification;
import org.springframework.context.config.ExecutorContext;
import org.springframework.context.config.FeatureSpecification;
import org.springframework.context.config.FeatureSpecificationExecutor;

public class StubSpecification extends AbstractFeatureSpecification {

	public StubSpecification() {
		this(StubSpecificationExecutor.class);
	}

	public StubSpecification(Class<? extends FeatureSpecificationExecutor> excecutorType) {
		super(excecutorType);
	}

	@Override
	protected void doValidate(SimpleProblemCollector reporter) {
	}

}

class StubSpecificationExecutor implements FeatureSpecificationExecutor {

	public void execute(FeatureSpecification spec, ExecutorContext executorContext) {
	}

}
