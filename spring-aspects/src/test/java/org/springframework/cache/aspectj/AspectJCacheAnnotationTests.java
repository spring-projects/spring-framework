/*
 * Copyright 2002-2019 the original author or authors.
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
public class AspectJCacheAnnotationTests extends AbstractCacheAnnotationTests {

	@Override
	protected ConfigurableApplicationContext getApplicationContext() {
		return new GenericXmlApplicationContext(
				"/org/springframework/cache/config/annotation-cache-aspectj.xml");
	}

	@Test
	public void testKeyStrategy() {
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
