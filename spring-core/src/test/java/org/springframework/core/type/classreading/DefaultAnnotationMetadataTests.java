/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.type.classreading;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.Test;

import org.springframework.core.OverridingClassLoader;
import org.springframework.core.type.AbstractAnnotationMetadataTests;
import org.springframework.core.type.AnnotationMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link SimpleAnnotationMetadata} and
 * {@link SimpleAnnotationMetadataReadingVisitor} on Java < 24,
 * and for the ClassFile API variant on Java >= 24.
 *
 * @author Phillip Webb
 * @author Brian Clozel
 */
class DefaultAnnotationMetadataTests extends AbstractAnnotationMetadataTests {

	@Override
	protected AnnotationMetadata get(Class<?> source) {
		try {
			return MetadataReaderFactory.create(source.getClassLoader())
					.getMetadataReader(source.getName()).getAnnotationMetadata();
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Test
	void getClassAttributeWhenUnknownClass() throws IOException {
		var classLoader = new FilteringClassLoader(getClass().getClassLoader());
		var mergedAnnotation = MetadataReaderFactory.create(classLoader)
				.getMetadataReader(WithClassMissingFromClasspath.class.getName())
				.getAnnotationMetadata()
				.getAnnotations()
				.get(ClassAttributes.class);
		assertThat(mergedAnnotation.getStringArray("types")).contains("javax.annotation.meta.When");
		assertThatIllegalArgumentException().isThrownBy(() -> mergedAnnotation.getClassArray("types"));
	}


	private static class FilteringClassLoader extends OverridingClassLoader {

		FilteringClassLoader(ClassLoader parent) {
			super(parent);
		}

		@Override
		protected boolean isEligibleForOverriding(String className) {
			return className.startsWith("javax.annotation.");
		}

		@Override
		protected Class<?> loadClassForOverriding(String name) throws ClassNotFoundException {
			throw new ClassNotFoundException(name);
		}
	}


	@ClassAttributes(types = {javax.annotation.meta.When.class})
	@javax.annotation.Nonnull(when = javax.annotation.meta.When.MAYBE)
	static class WithClassMissingFromClasspath {
	}


	@Retention(RetentionPolicy.RUNTIME)
	@interface ClassAttributes {

		Class<?>[] types();
	}

}
