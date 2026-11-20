/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.config;

import org.junit.jupiter.api.Test;

import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.context.testfixture.cache.AbstractCacheAnnotationTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Costin Leau
 * @author Chris Beams
 */
class CacheAdviceNamespaceTests extends AbstractCacheAnnotationTests {

	@Override
	protected ConfigurableApplicationContext getApplicationContext() {
		return new GenericXmlApplicationContext(
				"/org/springframework/cache/config/cache-advice.xml");
	}

	@Test
	void keyStrategy() {
		CacheInterceptor bean = this.ctx.getBean("cacheAdviceClass", CacheInterceptor.class);
		assertThat(bean.getKeyGenerator()).isSameAs(this.ctx.getBean("keyGenerator"));
	}

}
