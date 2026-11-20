/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.type;

import example.type.AnnotationTypeFilterTestsTypes;
import example.type.InheritedAnnotation;
import example.type.NonInheritedAnnotation;
import org.junit.jupiter.api.Test;

import org.springframework.core.testfixture.stereotype.Component;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author Oliver Gierke
 * @author Sam Brannen
 * @see AnnotationTypeFilterTestsTypes
 */
class AnnotationTypeFilterTests {

	@Test
	void directAnnotationMatch() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		String classUnderTest = "example.type.AnnotationTypeFilterTestsTypes$SomeComponent";
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(classUnderTest);

		AnnotationTypeFilter filter = new AnnotationTypeFilter(InheritedAnnotation.class);
		assertThat(filter.match(metadataReader, metadataReaderFactory)).isTrue();
		ClassloadingAssertions.assertClassNotLoaded(classUnderTest);
	}

	@Test
	void inheritedAnnotationFromInterfaceDoesNotMatch() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		String classUnderTest = "example.type.AnnotationTypeFilterTestsTypes$SomeClassWithSomeComponentInterface";
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(classUnderTest);

		AnnotationTypeFilter filter = new AnnotationTypeFilter(InheritedAnnotation.class);
		// Must fail as annotation on interfaces should not be considered a match
		assertThat(filter.match(metadataReader, metadataReaderFactory)).isFalse();
		ClassloadingAssertions.assertClassNotLoaded(classUnderTest);
	}

	@Test
	void inheritedAnnotationFromBaseClassDoesMatch() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		String classUnderTest = "example.type.AnnotationTypeFilterTestsTypes$SomeSubclassOfSomeComponent";
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(classUnderTest);

		AnnotationTypeFilter filter = new AnnotationTypeFilter(InheritedAnnotation.class);
		assertThat(filter.match(metadataReader, metadataReaderFactory)).isTrue();
		ClassloadingAssertions.assertClassNotLoaded(classUnderTest);
	}

	@Test
	void nonInheritedAnnotationDoesNotMatch() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		String classUnderTest = "example.type.AnnotationTypeFilterTestsTypes$SomeSubclassOfSomeClassMarkedWithNonInheritedAnnotation";
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(classUnderTest);

		AnnotationTypeFilter filter = new AnnotationTypeFilter(NonInheritedAnnotation.class);
		// Must fail as annotation isn't inherited
		assertThat(filter.match(metadataReader, metadataReaderFactory)).isFalse();
		ClassloadingAssertions.assertClassNotLoaded(classUnderTest);
	}

	@Test
	void nonAnnotatedClassDoesntMatch() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		String classUnderTest = "example.type.AnnotationTypeFilterTestsTypes$SomeNonCandidateClass";
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(classUnderTest);

		AnnotationTypeFilter filter = new AnnotationTypeFilter(Component.class);
		assertThat(filter.match(metadataReader, metadataReaderFactory)).isFalse();
		ClassloadingAssertions.assertClassNotLoaded(classUnderTest);
	}

	@Test
	void matchesInterfacesIfConfigured() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		String classUnderTest = "example.type.AnnotationTypeFilterTestsTypes$SomeClassWithSomeComponentInterface";
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(classUnderTest);

		AnnotationTypeFilter filter = new AnnotationTypeFilter(InheritedAnnotation.class, false, true);
		assertThat(filter.match(metadataReader, metadataReaderFactory)).isTrue();
		ClassloadingAssertions.assertClassNotLoaded(classUnderTest);
	}

}
