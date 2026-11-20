/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
		public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
			if (scanned.get() > 0) {
				throw new IllegalStateException("Already scanned");
			}
			scanned.incrementAndGet();
		}
	}

}
