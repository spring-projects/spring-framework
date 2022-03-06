/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.context.generator;

import org.springframework.beans.factory.generator.BeanFactoryContribution;
import org.springframework.beans.factory.generator.BeanFactoryInitialization;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.ContextAnnotationAutowireCandidateResolver;

/**
 * A {@link BeanFactoryContribution} that configures the low-level
 * infrastructure necessary to process an AOT context.
 *
 * @author Stephane Nicoll
 */
class InfrastructureContribution implements BeanFactoryContribution {

	@Override
	public void applyTo(BeanFactoryInitialization initialization) {
		initialization.contribute(code -> {
			code.add("// infrastructure\n");
			code.addStatement("$T beanFactory = context.getDefaultListableBeanFactory()",
					DefaultListableBeanFactory.class);
			code.addStatement("beanFactory.setAutowireCandidateResolver(new $T())",
					ContextAnnotationAutowireCandidateResolver.class);
		});
	}

}
