/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import org.springframework.beans.factory.parsing.FailFastProblemReporter;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;

/**
 * Unit test proving that ASM-based {@link ConfigurationClassParser} correctly detects
 * circular use of the {@link Import @Import} annotation.
 *
 * @author Chris Beams
 */
class AsmCircularImportDetectionTests extends AbstractCircularImportDetectionTests {

	@Override
	protected ConfigurationClassParser newParser() {
		return new ConfigurationClassParser(
				new CachingMetadataReaderFactory(),
				new FailFastProblemReporter(),
				new StandardEnvironment(),
				new DefaultResourceLoader(),
				new AnnotationBeanNameGenerator(),
				new DefaultListableBeanFactory());
	}

	@Override
	protected String loadAsConfigurationSource(Class<?> clazz) {
		return clazz.getName();
	}

}
