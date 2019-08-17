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
public class AspectJTypeFilterTests {

	@Test
	public void namePatternMatches() throws Exception {
		assertMatch("org.springframework.core.type.AspectJTypeFilterTestsTypes$SomeClass",
				"org.springframework.core.type.AspectJTypeFilterTestsTypes.SomeClass");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTestsTypes$SomeClass",
				"*");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTestsTypes$SomeClass",
				"*..SomeClass");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTestsTypes$SomeClass",
				"org..SomeClass");
	}

	@Test
	public void namePatternNoMatches() throws Exception {
		assertNoMatch("org.springframework.core.type.AspectJTypeFilterTestsTypes$SomeClass",
				"org.springframework.core.type.AspectJTypeFilterTestsTypes.SomeClassX");
	}

	@Test
	public void subclassPatternMatches() throws Exception {
		assertMatch("org.springframework.core.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClass",
				"org.springframework.core.type.AspectJTypeFilterTestsTypes.SomeClass+");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClass",
				"*+");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClass",
				"java.lang.Object+");

		assertMatch("org.springframework.core.type.AspectJTypeFilterTestsTypes$SomeClassImplementingSomeInterface",
				"org.springframework.core.type.AspectJTypeFilterTestsTypes.SomeInterface+");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTestsTypes$SomeClassImplementingSomeInterface",
				"*+");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTestsTypes$SomeClassImplementingSomeInterface",
				"java.lang.Object+");

		assertMatch("org.springframework.core.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface",
				"org.springframework.core.type.AspectJTypeFilterTestsTypes.SomeInterface+");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface",
				"org.springframework.core.type.AspectJTypeFilterTestsTypes.SomeClassExtendingSomeClass+");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface",
				"org.springframework.core.type.AspectJTypeFilterTestsTypes.SomeClass+");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface",
				"*+");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface",
				"java.lang.Object+");
	}

	@Test
	public void subclassPatternNoMatches() throws Exception {
		assertNoMatch("org.springframework.core.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClass",
				"java.lang.String+");
	}

	@Test
	public void annotationPatternMatches() throws Exception {
		assertMatch("org.springframework.core.type.AspectJTypeFilterTestsTypes$SomeClassAnnotatedWithComponent",
				"@org.springframework.stereotype.Component *..*");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTestsTypes$SomeClassAnnotatedWithComponent",
				"@* *..*");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTestsTypes$SomeClassAnnotatedWithComponent",
				"@*..* *..*");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTestsTypes$SomeClassAnnotatedWithComponent",
				"@*..*Component *..*");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTestsTypes$SomeClassAnnotatedWithComponent",
				"@org.springframework.stereotype.Component *..*Component");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTestsTypes$SomeClassAnnotatedWithComponent",
				"@org.springframework.stereotype.Component *");
	}

	@Test
	public void annotationPatternNoMatches() throws Exception {
		assertNoMatch("org.springframework.core.type.AspectJTypeFilterTestsTypes$SomeClassAnnotatedWithComponent",
				"@org.springframework.stereotype.Repository *..*");
	}

	@Test
	public void compositionPatternMatches() throws Exception {
		assertMatch("org.springframework.core.type.AspectJTypeFilterTestsTypes$SomeClass",
				"!*..SomeOtherClass");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface",
				"org.springframework.core.type.AspectJTypeFilterTestsTypes.SomeInterface+ " +
						"&& org.springframework.core.type.AspectJTypeFilterTestsTypes.SomeClass+ " +
						"&& org.springframework.core.type.AspectJTypeFilterTestsTypes.SomeClassExtendingSomeClass+");
		assertMatch("org.springframework.core.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface",
				"org.springframework.core.type.AspectJTypeFilterTestsTypes.SomeInterface+ " +
						"|| org.springframework.core.type.AspectJTypeFilterTestsTypes.SomeClass+ " +
						"|| org.springframework.core.type.AspectJTypeFilterTestsTypes.SomeClassExtendingSomeClass+");
	}

	@Test
	public void compositionPatternNoMatches() throws Exception {
		assertNoMatch("org.springframework.core.type.AspectJTypeFilterTestsTypes$SomeClass",
				"*..Bogus && org.springframework.core.type.AspectJTypeFilterTestsTypes.SomeClass");
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
