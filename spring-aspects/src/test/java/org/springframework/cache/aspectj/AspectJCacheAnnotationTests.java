/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.aspectj;

import org.junit.jupiter.api.Test;

import org.springframework.cache.Cache;
import org.springframework.cache.config.CacheableService;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Costin Leau
 */
class AspectJCacheAnnotationTests extends AbstractCacheAnnotationTests {

	@Override
	protected ConfigurableApplicationContext getApplicationContext() {
		return new GenericXmlApplicationContext(
				"/org/springframework/cache/config/annotation-cache-aspectj.xml");
	}

	@Test
	void keyStrategy() {
		AnnotationCacheAspect aspect = ctx.getBean(
				"org.springframework.cache.config.internalCacheAspect", AnnotationCacheAspect.class);
		assertThat(aspect.getKeyGenerator()).isSameAs(ctx.getBean("keyGenerator"));
	}

	@Override
	protected void testMultiEvict(CacheableService<?> service) {
		Object o1 = new Object();

		Object r1 = service.multiCache(o1);
		Object r2 = service.multiCache(o1);

		Cache primary = cm.getCache("primary");
		Cache secondary = cm.getCache("secondary");

		assertThat(r2).isSameAs(r1);
		assertThat(primary.get(o1).get()).isSameAs(r1);
		assertThat(secondary.get(o1).get()).isSameAs(r1);

		service.multiEvict(o1);
		assertThat(primary.get(o1)).isNull();
		assertThat(secondary.get(o1)).isNull();

		Object r3 = service.multiCache(o1);
		Object r4 = service.multiCache(o1);
		assertThat(r3).isNotSameAs(r1);
		assertThat(r4).isSameAs(r3);

		assertThat(primary.get(o1).get()).isSameAs(r3);
		assertThat(secondary.get(o1).get()).isSameAs(r4);
	}

}
