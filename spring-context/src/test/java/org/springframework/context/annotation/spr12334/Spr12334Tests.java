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

package org.springframework.context.annotation.spr12334;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * @author Juergen Hoeller
 * @author Alex Pogrebnyak
 * @author Sam Brannen
 */
class Spr12334Tests {

	@Test
	void shouldNotScanTwice() {
		TestImport.scanned.set(0);

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.scan(TestImport.class.getPackage().getName());
		context.refresh();
		assertThat(TestImport.scanned).hasValue(1);
		assertThatNoException().isThrownBy(() -> context.getBean(TestConfiguration.class));
		context.close();
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Import(TestImport.class)
	@interface AnotherImport {
	}


	@Configuration
	@AnotherImport
	static class TestConfiguration {
	}


	static class TestImport implements ImportBeanDefinitionRegistrar {

		private static AtomicInteger scanned = new AtomicInteger();

		@Override
		public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry)  {
			if (scanned.get() > 0) {
				throw new IllegalStateException("Already scanned");
			}
			scanned.incrementAndGet();
		}
	}

}
