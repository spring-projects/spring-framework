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

package org.springframework.context.testfixture.context.annotation;

import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

@Configuration(proxyBeanMethods = false)
@SuppressWarnings("unused")
public class ImportAwareConfiguration implements ImportAware, EnvironmentAware {

	private AnnotationMetadata annotationMetadata;

	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		this.annotationMetadata = importMetadata;
	}

	@Override
	public void setEnvironment(Environment environment) {

	}

}
