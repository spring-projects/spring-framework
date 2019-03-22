/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.junit.Test;

import org.springframework.core.annotation.AnnotationUtilsTests.GroovyImplicitAliasesContextConfigClass;
import org.springframework.core.annotation.AnnotationUtilsTests.ImplicitAliasesContextConfig;
import org.springframework.core.annotation.AnnotationUtilsTests.Location1ImplicitAliasesContextConfigClass;
import org.springframework.core.annotation.AnnotationUtilsTests.Location2ImplicitAliasesContextConfigClass;
import org.springframework.core.annotation.AnnotationUtilsTests.Location3ImplicitAliasesContextConfigClass;
import org.springframework.core.annotation.AnnotationUtilsTests.ValueImplicitAliasesContextConfigClass;
import org.springframework.core.annotation.AnnotationUtilsTests.XmlImplicitAliasesContextConfigClass;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Abstract base class for tests involving concrete implementations of
 * {@link AbstractAliasAwareAnnotationAttributeExtractor}.
 *
 * @author Sam Brannen
 * @since 4.2.1
 */
public abstract class AbstractAliasAwareAnnotationAttributeExtractorTestCase {

	@Test
	public void getAttributeValueForImplicitAliases() throws Exception {
		assertGetAttributeValueForImplicitAliases(GroovyImplicitAliasesContextConfigClass.class, "groovyScript");
		assertGetAttributeValueForImplicitAliases(XmlImplicitAliasesContextConfigClass.class, "xmlFile");
		assertGetAttributeValueForImplicitAliases(ValueImplicitAliasesContextConfigClass.class, "value");
		assertGetAttributeValueForImplicitAliases(Location1ImplicitAliasesContextConfigClass.class, "location1");
		assertGetAttributeValueForImplicitAliases(Location2ImplicitAliasesContextConfigClass.class, "location2");
		assertGetAttributeValueForImplicitAliases(Location3ImplicitAliasesContextConfigClass.class, "location3");
	}

	private void assertGetAttributeValueForImplicitAliases(Class<?> clazz, String expected) throws Exception {
		Method xmlFile = ImplicitAliasesContextConfig.class.getDeclaredMethod("xmlFile");
		Method groovyScript = ImplicitAliasesContextConfig.class.getDeclaredMethod("groovyScript");
		Method value = ImplicitAliasesContextConfig.class.getDeclaredMethod("value");

		AnnotationAttributeExtractor<?> extractor = createExtractorFor(clazz, expected, ImplicitAliasesContextConfig.class);

		assertThat(extractor.getAttributeValue(value), is(expected));
		assertThat(extractor.getAttributeValue(groovyScript), is(expected));
		assertThat(extractor.getAttributeValue(xmlFile), is(expected));
	}

	protected abstract AnnotationAttributeExtractor<?> createExtractorFor(Class<?> clazz, String expected, Class<? extends Annotation> annotationType);

}
