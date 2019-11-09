/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core.type;

import example.type.AspectJTypeFilterTestsTypes;
import org.junit.jupiter.api.Test;

import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.core.type.filter.AspectJTypeFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ramnivas Laddad
 * @author Sam Brannen
 * @see AspectJTypeFilterTestsTypes
 */
class AspectJTypeFilterTests {

	@Test
	void namePatternMatches() throws Exception {
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClass",
				"example.type.AspectJTypeFilterTestsTypes.SomeClass");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClass",
				"*");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClass",
				"*..SomeClass");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClass",
				"example..SomeClass");
	}

	@Test
	void namePatternNoMatches() throws Exception {
		assertNoMatch("example.type.AspectJTypeFilterTestsTypes$SomeClass",
				"example.type.AspectJTypeFilterTestsTypes.SomeClassX");
	}

	@Test
	void subclassPatternMatches() throws Exception {
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClass",
				"example.type.AspectJTypeFilterTestsTypes.SomeClass+");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClass",
				"*+");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClass",
				"java.lang.Object+");

		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassImplementingSomeInterface",
				"example.type.AspectJTypeFilterTestsTypes.SomeInterface+");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassImplementingSomeInterface",
				"*+");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassImplementingSomeInterface",
				"java.lang.Object+");

		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface",
				"example.type.AspectJTypeFilterTestsTypes.SomeInterface+");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface",
				"example.type.AspectJTypeFilterTestsTypes.SomeClassExtendingSomeClass+");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface",
				"example.type.AspectJTypeFilterTestsTypes.SomeClass+");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface",
				"*+");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface",
				"java.lang.Object+");
	}

	@Test
	void subclassPatternNoMatches() throws Exception {
		assertNoMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClass",
				"java.lang.String+");
	}

	@Test
	void annotationPatternMatches() throws Exception {
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassAnnotatedWithComponent",
				"@org.springframework.stereotype.Component *..*");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassAnnotatedWithComponent",
				"@* *..*");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassAnnotatedWithComponent",
				"@*..* *..*");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassAnnotatedWithComponent",
				"@*..*Component *..*");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassAnnotatedWithComponent",
				"@org.springframework.stereotype.Component *..*Component");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassAnnotatedWithComponent",
				"@org.springframework.stereotype.Component *");
	}

	@Test
	void annotationPatternNoMatches() throws Exception {
		assertNoMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassAnnotatedWithComponent",
				"@org.springframework.stereotype.Repository *..*");
	}

	@Test
	void compositionPatternMatches() throws Exception {
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClass",
				"!*..SomeOtherClass");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface",
				"example.type.AspectJTypeFilterTestsTypes.SomeInterface+ " +
						"&& example.type.AspectJTypeFilterTestsTypes.SomeClass+ " +
						"&& example.type.AspectJTypeFilterTestsTypes.SomeClassExtendingSomeClass+");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface",
				"example.type.AspectJTypeFilterTestsTypes.SomeInterface+ " +
						"|| example.type.AspectJTypeFilterTestsTypes.SomeClass+ " +
						"|| example.type.AspectJTypeFilterTestsTypes.SomeClassExtendingSomeClass+");
	}

	@Test
	void compositionPatternNoMatches() throws Exception {
		assertNoMatch("example.type.AspectJTypeFilterTestsTypes$SomeClass",
				"*..Bogus && example.type.AspectJTypeFilterTestsTypes.SomeClass");
	}

	private void assertMatch(String type, String typePattern) throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(type);

		AspectJTypeFilter filter = new AspectJTypeFilter(typePattern, getClass().getClassLoader());
		assertThat(filter.match(metadataReader, metadataReaderFactory)).isTrue();
		ClassloadingAssertions.assertClassNotLoaded(type);
	}

	private void assertNoMatch(String type, String typePattern) throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(type);

		AspectJTypeFilter filter = new AspectJTypeFilter(typePattern, getClass().getClassLoader());
		assertThat(filter.match(metadataReader, metadataReaderFactory)).isFalse();
		ClassloadingAssertions.assertClassNotLoaded(type);
	}

}
