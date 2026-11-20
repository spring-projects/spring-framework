/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.type;

import org.junit.jupiter.api.Test;

import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.core.type.filter.AssignableTypeFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 */
class AssignableTypeFilterTests {

	@Test
	void directMatch() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		String classUnderTest = "example.type.AssignableTypeFilterTestsTypes$TestNonInheritingClass";
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(classUnderTest);

		AssignableTypeFilter matchingFilter = new AssignableTypeFilter(example.type.AssignableTypeFilterTestsTypes.TestNonInheritingClass.class);
		AssignableTypeFilter notMatchingFilter = new AssignableTypeFilter(example.type.AssignableTypeFilterTestsTypes.TestInterface.class);
		assertThat(notMatchingFilter.match(metadataReader, metadataReaderFactory)).isFalse();
		assertThat(matchingFilter.match(metadataReader, metadataReaderFactory)).isTrue();
	}

	@Test
	void interfaceMatch() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		String classUnderTest = "example.type.AssignableTypeFilterTestsTypes$TestInterfaceImpl";
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(classUnderTest);

		AssignableTypeFilter filter = new AssignableTypeFilter(example.type.AssignableTypeFilterTestsTypes.TestInterface.class);
		assertThat(filter.match(metadataReader, metadataReaderFactory)).isTrue();
		ClassloadingAssertions.assertClassNotLoaded(classUnderTest);
	}

	@Test
	void superClassMatch() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		String classUnderTest = "example.type.AssignableTypeFilterTestsTypes$SomeDaoLikeImpl";
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(classUnderTest);

		AssignableTypeFilter filter = new AssignableTypeFilter(example.type.AssignableTypeFilterTestsTypes.SimpleJdbcDaoSupport.class);
		assertThat(filter.match(metadataReader, metadataReaderFactory)).isTrue();
		ClassloadingAssertions.assertClassNotLoaded(classUnderTest);
	}

	@Test
	void interfaceThroughSuperClassMatch() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		String classUnderTest = "example.type.AssignableTypeFilterTestsTypes$SomeDaoLikeImpl";
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(classUnderTest);

		AssignableTypeFilter filter = new AssignableTypeFilter(example.type.AssignableTypeFilterTestsTypes.JdbcDaoSupport.class);
		assertThat(filter.match(metadataReader, metadataReaderFactory)).isTrue();
		ClassloadingAssertions.assertClassNotLoaded(classUnderTest);
	}

}
