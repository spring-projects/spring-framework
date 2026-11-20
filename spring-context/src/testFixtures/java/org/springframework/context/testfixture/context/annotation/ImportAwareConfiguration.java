/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
