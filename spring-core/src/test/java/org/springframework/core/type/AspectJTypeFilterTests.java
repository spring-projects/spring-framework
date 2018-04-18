/*
 * Copyright 2002-2015 the original author or authors.
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

import org.junit.Test;

import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.core.type.filter.AspectJTypeFilter;
import org.springframework.stereotype.Component;

import static org.junit.Assert.*;

/**
 * @author Ramnivas Laddad
 */
public class AspectJTypeFilterTests {

	@Test
	public void namePatternMatches() throws Exception {
		assertMatch("org.springframework.core.type.AspectJTypeFilterTests$SomeClass",
				"org.springframework.core.type.AspectJTypeFilterTests.SomeClass");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTests$SomeClass",
				"*");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTests$SomeClass",
				"*..SomeClass");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTests$SomeClass",
				"org..SomeClass");
	}

	@Test
	public void namePatternNoMatches() throws Exception {
		assertNoMatch("org.springframework.core.type.AspectJTypeFilterTests$SomeClass",
				"org.springframework.core.type.AspectJTypeFilterTests.SomeClassX");
	}

	@Test
	public void subclassPatternMatches() throws Exception {
		assertMatch("org.springframework.core.type.AspectJTypeFilterTests$SomeClassExtendingSomeClass",
				"org.springframework.core.type.AspectJTypeFilterTests.SomeClass+");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTests$SomeClassExtendingSomeClass",
				"*+");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTests$SomeClassExtendingSomeClass",
				"java.lang.Object+");

		assertMatch("org.springframework.core.type.AspectJTypeFilterTests$SomeClassImplementingSomeInterface",
				"org.springframework.core.type.AspectJTypeFilterTests.SomeInterface+");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTests$SomeClassImplementingSomeInterface",
				"*+");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTests$SomeClassImplementingSomeInterface",
				"java.lang.Object+");

		assertMatch("org.springframework.core.type.AspectJTypeFilterTests$SomeClassExtendingSomeClassExtendingSomeClassAndImplemnentingSomeInterface",
				"org.springframework.core.type.AspectJTypeFilterTests.SomeInterface+");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTests$SomeClassExtendingSomeClassExtendingSomeClassAndImplemnentingSomeInterface",
				"org.springframework.core.type.AspectJTypeFilterTests.SomeClassExtendingSomeClass+");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTests$SomeClassExtendingSomeClassExtendingSomeClassAndImplemnentingSomeInterface",
				"org.springframework.core.type.AspectJTypeFilterTests.SomeClass+");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTests$SomeClassExtendingSomeClassExtendingSomeClassAndImplemnentingSomeInterface",
				"*+");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTests$SomeClassExtendingSomeClassExtendingSomeClassAndImplemnentingSomeInterface",
				"java.lang.Object+");
	}

	@Test
	public void subclassPatternNoMatches() throws Exception {
		assertNoMatch("org.springframework.core.type.AspectJTypeFilterTests$SomeClassExtendingSomeClass",
				"java.lang.String+");
	}

	@Test
	public void annotationPatternMatches() throws Exception {
		assertMatch("org.springframework.core.type.AspectJTypeFilterTests$SomeClassAnnotatedWithComponent",
				"@org.springframework.stereotype.Component *..*");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTests$SomeClassAnnotatedWithComponent",
				"@* *..*");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTests$SomeClassAnnotatedWithComponent",
				"@*..* *..*");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTests$SomeClassAnnotatedWithComponent",
				"@*..*Component *..*");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTests$SomeClassAnnotatedWithComponent",
				"@org.springframework.stereotype.Component *..*Component");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTests$SomeClassAnnotatedWithComponent",
				"@org.springframework.stereotype.Component *");
	}

	@Test
	public void annotationPatternNoMathces() throws Exception {
		assertNoMatch("org.springframework.core.type.AspectJTypeFilterTests$SomeClassAnnotatedWithComponent",
				"@org.springframework.stereotype.Repository *..*");
	}

	@Test
	public void compositionPatternMatches() throws Exception {
		assertMatch("org.springframework.core.type.AspectJTypeFilterTests$SomeClass",
				"!*..SomeOtherClass");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTests$SomeClassExtendingSomeClassExtendingSomeClassAndImplemnentingSomeInterface",
				"org.springframework.core.type.AspectJTypeFilterTests.SomeInterface+ " +
						"&& org.springframework.core.type.AspectJTypeFilterTests.SomeClass+ " +
						"&& org.springframework.core.type.AspectJTypeFilterTests.SomeClassExtendingSomeClass+");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTests$SomeClassExtendingSomeClassExtendingSomeClassAndImplemnentingSomeInterface",
				"org.springframework.core.type.AspectJTypeFilterTests.SomeInterface+ " +
						"|| org.springframework.core.type.AspectJTypeFilterTests.SomeClass+ " +
						"|| org.springframework.core.type.AspectJTypeFilterTests.SomeClassExtendingSomeClass+");
	}

	@Test
	public void compositionPatternNoMatches() throws Exception {
		assertNoMatch("org.springframework.core.type.AspectJTypeFilterTests$SomeClass",
				"*..Bogus && org.springframework.core.type.AspectJTypeFilterTests.SomeClass");
	}

	private void assertMatch(String type, String typePattern) throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(type);

		AspectJTypeFilter filter = new AspectJTypeFilter(typePattern, getClass().getClassLoader());
		assertTrue(filter.match(metadataReader, metadataReaderFactory));
		ClassloadingAssertions.assertClassNotLoaded(type);
	}

	private void assertNoMatch(String type, String typePattern) throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(type);

		AspectJTypeFilter filter = new AspectJTypeFilter(typePattern, getClass().getClassLoader());
		assertFalse(filter.match(metadataReader, metadataReaderFactory));
		ClassloadingAssertions.assertClassNotLoaded(type);
	}


	// We must use a standalone set of types to ensure that no one else is loading them
	// and interfering with ClassloadingAssertions.assertClassNotLoaded()
	interface SomeInterface {
	}

	static class SomeClass {
	}

	static class SomeClassExtendingSomeClass extends SomeClass {
	}

	static class SomeClassImplementingSomeInterface implements SomeInterface {
	}

	static class SomeClassExtendingSomeClassExtendingSomeClassAndImplemnentingSomeInterface
			extends SomeClassExtendingSomeClass implements SomeInterface {
	}

	@Component
	static class SomeClassAnnotatedWithComponent {
	}

}
