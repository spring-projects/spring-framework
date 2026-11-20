/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.support;

import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.samples.SampleComponent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that verify that common caches are cleared at the end of a test
 * class. Regular callbacks cannot be used to validate this as they run
 * before the listener, so we need two test classes that are ordered to
 * validate the result.
 *
 * @author Stephane Nicoll
 */
@SpringJUnitConfig
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
class CommonCachesTestExecutionListenerIntegrationTests {

	@Autowired
	AbstractApplicationContext applicationContext;

	@Nested
	@Order(1)
	class FirstTests {

		@Test
		void lazyInitBeans() {
			assertThat(applicationContext.getBean(String.class)).isEqualTo("Dummy");
			assertThat(applicationContext.getResourceCache(MetadataReader.class)).isNotEmpty();
		}

	}

	@Nested
	@Order(2)
	class SecondTests {

		@Test
		void validateCommonCacheIsCleared() {
			assertThat(applicationContext.getResourceCache(MetadataReader.class)).isEmpty();
		}

	}


	@Configuration
	static class TestConfiguration {

		@Bean
		@Lazy
		String dummyBean(ResourceLoader resourceLoader) {
			ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
			scanner.setResourceLoader(resourceLoader);
			scanner.findCandidateComponents(SampleComponent.class.getPackageName());
			return "Dummy";
		}
	}

}
