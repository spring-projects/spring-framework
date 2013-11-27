/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cache;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.*;

/**
 * Tests to reproduce raised caching issues.
 *
 * @author Phillip Webb
 */
public class CacheReproTests {

	@Test
	public void spr11124() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				Spr11124Config.class);
		Spr11124Service bean = context.getBean(Spr11124Service.class);
		bean.single(2);
		bean.single(2);
		bean.multiple(2);
		bean.multiple(2);
		context.close();
	}

	@Configuration
	@EnableCaching
	public static class Spr11124Config {

		@Bean
		public CacheManager cacheManager() {
			return new ConcurrentMapCacheManager();
		}

		@Bean
		public Spr11124Service service() {
			return new Spr11124ServiceImpl();
		}

	}

	public interface Spr11124Service {

		public List<String> single(int id);

		public List<String> multiple(int id);

	}

	public static class Spr11124ServiceImpl implements Spr11124Service {

		private int multipleCount = 0;

		@Override
		@Cacheable(value = "smallCache")
		public List<String> single(int id) {
			if (this.multipleCount > 0) {
				fail("Called too many times");
			}
			this.multipleCount++;
			return Collections.emptyList();
		}

		@Override
		@Caching(cacheable = {
			@Cacheable(value = "bigCache", unless = "#result.size() < 4"),
			@Cacheable(value = "smallCache", unless = "#result.size() > 3") })
		public List<String> multiple(int id) {
			if (this.multipleCount > 0) {
				fail("Called too many times");
			}
			this.multipleCount++;
			return Collections.emptyList();
		}

	}

}
