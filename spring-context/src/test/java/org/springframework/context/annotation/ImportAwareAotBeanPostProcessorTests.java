/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.core.type.AnnotationMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ImportAwareAotBeanPostProcessor}.
 *
 * @author Stephane Nicoll
 */
class ImportAwareAotBeanPostProcessorTests {

	@Test
	void postProcessOnMatchingCandidate() {
		ImportAwareAotBeanPostProcessor postProcessor = new ImportAwareAotBeanPostProcessor(
				Map.of(TestImportAware.class.getName(), ImportAwareAotBeanPostProcessorTests.class.getName()));
		TestImportAware importAware = new TestImportAware();
		postProcessor.postProcessBeforeInitialization(importAware, "test");
		assertThat(importAware.importMetadata).isNotNull();
		assertThat(importAware.importMetadata.getClassName())
				.isEqualTo(ImportAwareAotBeanPostProcessorTests.class.getName());
	}

	@Test
	void postProcessOnMatchingCandidateWithNestedClass() {
		ImportAwareAotBeanPostProcessor postProcessor = new ImportAwareAotBeanPostProcessor(
				Map.of(TestImportAware.class.getName(), TestImporting.class.getName()));
		TestImportAware importAware = new TestImportAware();
		postProcessor.postProcessBeforeInitialization(importAware, "test");
		assertThat(importAware.importMetadata).isNotNull();
		assertThat(importAware.importMetadata.getClassName())
				.isEqualTo(TestImporting.class.getName());
	}

	@Test
	void postProcessOnNoCandidateDoesNotInvokeCallback() {
		ImportAwareAotBeanPostProcessor postProcessor = new ImportAwareAotBeanPostProcessor(
				Map.of(String.class.getName(), ImportAwareAotBeanPostProcessorTests.class.getName()));
		TestImportAware importAware = new TestImportAware();
		postProcessor.postProcessBeforeInitialization(importAware, "test");
		assertThat(importAware.importMetadata).isNull();
	}

	@Test
	void postProcessOnMatchingCandidateWithNoMetadata() {
		ImportAwareAotBeanPostProcessor postProcessor = new ImportAwareAotBeanPostProcessor(
				Map.of(TestImportAware.class.getName(), "com.example.invalid.DoesNotExist"));
		TestImportAware importAware = new TestImportAware();
		assertThatIllegalStateException().isThrownBy(() -> postProcessor.postProcessBeforeInitialization(importAware, "test"))
				.withMessageContaining("Failed to read metadata for 'com.example.invalid.DoesNotExist'");
	}


	static class TestImportAware implements ImportAware {

		private AnnotationMetadata importMetadata;

		@Override
		public void setImportMetadata(AnnotationMetadata importMetadata) {
			this.importMetadata = importMetadata;
		}
	}

	static class TestImporting {

	}

}
