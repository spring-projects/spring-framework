/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.cache.annotation;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.BeanFactoryCacheOperationSourceAdvisor;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Repository;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Integration tests for the @EnableCaching annotation.
 *
 * @author Chris Beams
 * @since 3.1
 */
@SuppressWarnings("resource")
public class EnableCachingIntegrationTests {

	@Test
	public void repositoryIsClassBasedCacheProxy() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(Config.class, ProxyTargetClassCachingConfig.class);
		ctx.refresh();

		assertCacheProxying(ctx);
		assertThat(AopUtils.isCglibProxy(ctx.getBean(FooRepository.class)), is(true));
	}

	@Test
	public void repositoryUsesAspectJAdviceMode() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(Config.class, AspectJCacheConfig.class);
		try {
			ctx.refresh();
		}
		catch (Exception ex) {
			// this test is a bit fragile, but gets the job done, proving that an
			// attempt was made to look up the AJ aspect. It's due to classpath issues
			// in .integration-tests that it's not found.
			assertTrue(ex.getMessage().contains("AspectJCachingConfiguration"));
		}
	}

	private void assertCacheProxying(AnnotationConfigApplicationContext ctx) {
		FooRepository repo = ctx.getBean(FooRepository.class);

		boolean isCacheProxy = false;
		if (AopUtils.isAopProxy(repo)) {
			for (Advisor advisor : ((Advised)repo).getAdvisors()) {
				if (advisor instanceof BeanFactoryCacheOperationSourceAdvisor) {
					isCacheProxy = true;
					break;
				}
			}
		}
		assertTrue("FooRepository is not a cache proxy", isCacheProxy);
	}


	@Configuration
	@EnableCaching(proxyTargetClass=true)
	static class ProxyTargetClassCachingConfig {
		@Bean
		CacheManager mgr() {
			return new NoOpCacheManager();
		}
	}


	@Configuration
	static class Config {
		@Bean
		FooRepository fooRepository() {
			return new DummyFooRepository();
		}
	}


	@Configuration
	@EnableCaching(mode=AdviceMode.ASPECTJ)
	static class AspectJCacheConfig {
		@Bean
		CacheManager cacheManager() {
			return new NoOpCacheManager();
		}
	}


	interface FooRepository {
		List<Object> findAll();
	}


	@Repository
	static class DummyFooRepository implements FooRepository {

		@Override
		@Cacheable("primary")
		public List<Object> findAll() {
			return Collections.emptyList();
		}
	}
}
