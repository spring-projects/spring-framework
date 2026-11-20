/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.config;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.context.testfixture.cache.AbstractCacheAnnotationTests;

/**
 * @author Costin Leau
 * @author Chris Beams
 */
class AnnotationDrivenCacheConfigTests extends AbstractCacheAnnotationTests {

	@Override
	protected ConfigurableApplicationContext getApplicationContext() {
		return new GenericXmlApplicationContext(
				"/org/springframework/cache/config/annotationDrivenCacheConfig.xml");
	}

}
