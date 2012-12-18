/*
 * Copyright 2002-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.type;

import junit.framework.TestCase;

import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.core.type.filter.AssignableTypeFilter;

/**
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 */
public class AssignableTypeFilterTests extends TestCase {

	public void testDirectMatch() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		String classUnderTest = "org.springframework.core.type.AssignableTypeFilterTests$TestNonInheritingClass";
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(classUnderTest);

		AssignableTypeFilter matchingFilter = new AssignableTypeFilter(TestNonInheritingClass.class);
		AssignableTypeFilter notMatchingFilter = new AssignableTypeFilter(TestInterface.class);
		assertFalse(notMatchingFilter.match(metadataReader, metadataReaderFactory));
		assertTrue(matchingFilter.match(metadataReader, metadataReaderFactory));
	}

	public void testInterfaceMatch() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		String classUnderTest = "org.springframework.core.type.AssignableTypeFilterTests$TestInterfaceImpl";
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(classUnderTest);

		AssignableTypeFilter filter = new AssignableTypeFilter(TestInterface.class);
		assertTrue(filter.match(metadataReader, metadataReaderFactory));
		ClassloadingAssertions.assertClassNotLoaded(classUnderTest);
	}

	public void testSuperClassMatch() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		String classUnderTest = "org.springframework.core.type.AssignableTypeFilterTests$SomeDaoLikeImpl";
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(classUnderTest);

		AssignableTypeFilter filter = new AssignableTypeFilter(SimpleJdbcDaoSupport.class);
		assertTrue(filter.match(metadataReader, metadataReaderFactory));
		ClassloadingAssertions.assertClassNotLoaded(classUnderTest);
	}

	public void testInterfaceThroughSuperClassMatch() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		String classUnderTest = "org.springframework.core.type.AssignableTypeFilterTests$SomeDaoLikeImpl";
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(classUnderTest);

		AssignableTypeFilter filter = new AssignableTypeFilter(JdbcDaoSupport.class);
		assertTrue(filter.match(metadataReader, metadataReaderFactory));
		ClassloadingAssertions.assertClassNotLoaded(classUnderTest);
	}


	// We must use a standalone set of types to ensure that no one else is loading them
	// and interfere with ClassloadingAssertions.assertClassNotLoaded()
	private static class TestNonInheritingClass {
	}


	private static interface TestInterface {
	}


	private static class TestInterfaceImpl implements TestInterface {
	}


	private static interface SomeDaoLikeInterface {
	}


	private static class SomeDaoLikeImpl extends SimpleJdbcDaoSupport implements SomeDaoLikeInterface {
	}

	private static interface JdbcDaoSupport {

	}

	private static class SimpleJdbcDaoSupport implements JdbcDaoSupport {

	}

}
