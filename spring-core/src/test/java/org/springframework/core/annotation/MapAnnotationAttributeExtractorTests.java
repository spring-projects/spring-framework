/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.core.annotation.AnnotationUtilsTests.*;

/**
 * Unit tests for {@link MapAnnotationAttributeExtractor}.
 *
 * @author Sam Brannen
 * @since 4.2.1
 */
@SuppressWarnings("serial")
public class MapAnnotationAttributeExtractorTests extends AbstractAliasAwareAnnotationAttributeExtractorTestCase {

	@Override
	protected AnnotationAttributeExtractor<?> createExtractorFor(Class<?> clazz, String expected, Class<? extends Annotation> annotationType) {
		Map<String, Object> attributes = Collections.singletonMap(expected, expected);
		return new MapAnnotationAttributeExtractor(attributes, annotationType, clazz);
	}

	@Before
	public void clearCacheBeforeTests() {
		AnnotationUtils.clearCache();
	}


	@Test
	public void enrichAndValidateAttributesWithImplicitAliasesAndMinimalAttributes() throws Exception {
		Map<String, Object> attributes = new HashMap<>();
		Map<String, Object> expectedAttributes = new HashMap<String, Object>() {{
			put("groovyScript", "");
			put("xmlFile", "");
			put("value", "");
			put("location1", "");
			put("location2", "");
			put("location3", "");
			put("nonAliasedAttribute", "");
			put("configClass", Object.class);
		}};

		assertEnrichAndValidateAttributes(attributes, expectedAttributes);
	}

	@Test
	public void enrichAndValidateAttributesWithImplicitAliases() throws Exception {
		Map<String, Object> attributes = new HashMap<String, Object>() {{
			put("groovyScript", "groovy!");
		}};

		Map<String, Object> expectedAttributes = new HashMap<String, Object>() {{
			put("groovyScript", "groovy!");
			put("xmlFile", "groovy!");
			put("value", "groovy!");
			put("location1", "groovy!");
			put("location2", "groovy!");
			put("location3", "groovy!");
			put("nonAliasedAttribute", "");
			put("configClass", Object.class);
		}};

		assertEnrichAndValidateAttributes(attributes, expectedAttributes);
	}

	@Test
	public void enrichAndValidateAttributesWithSingleElementThatOverridesAnArray() {
		Map<String, Object> attributes = new HashMap<String, Object>() {{
			// Intentionally storing 'value' as a single String instead of an array.
			// put("value", asArray("/foo"));
			put("value", "/foo");
			put("name", "test");
		}};

		Map<String, Object> expected = new HashMap<String, Object>() {{
			put("value", asArray("/foo"));
			put("path", asArray("/foo"));
			put("name", "test");
			put("method", new RequestMethod[0]);
		}};

		MapAnnotationAttributeExtractor extractor = new MapAnnotationAttributeExtractor(attributes, WebMapping.class, null);
		Map<String, Object> enriched = extractor.getSource();

		assertEquals("attribute map size", expected.size(), enriched.size());
		expected.forEach((attr, expectedValue) -> assertThat("for attribute '" + attr + "'", enriched.get(attr), is(expectedValue)));
	}

	@SuppressWarnings("unchecked")
	private void assertEnrichAndValidateAttributes(Map<String, Object> sourceAttributes, Map<String, Object> expected) throws Exception {
		Class<? extends Annotation> annotationType = ImplicitAliasesContextConfig.class;

		// Since the ordering of attribute methods returned by the JVM is non-deterministic,
		// we have to rig the attributeAliasesCache in AnnotationUtils so that the tests
		// consistently fail in case enrichAndValidateAttributes() is buggy.
		// Otherwise, these tests would intermittently pass even for an invalid implementation.
		Field cacheField = AnnotationUtils.class.getDeclaredField("attributeAliasesCache");
		cacheField.setAccessible(true);
		Map<Class<? extends Annotation>, MultiValueMap<String, String>> attributeAliasesCache =
				(Map<Class<? extends Annotation>, MultiValueMap<String, String>>) cacheField.get(null);

		// Declare aliases in an order that will cause enrichAndValidateAttributes() to
		// fail unless it considers all aliases in the set of implicit aliases.
		MultiValueMap<String, String> aliases = new LinkedMultiValueMap<>();
		aliases.put("xmlFile", Arrays.asList("value", "groovyScript", "location1", "location2", "location3"));
		aliases.put("groovyScript", Arrays.asList("value", "xmlFile", "location1", "location2", "location3"));
		aliases.put("value", Arrays.asList("xmlFile", "groovyScript", "location1", "location2", "location3"));
		aliases.put("location1", Arrays.asList("xmlFile", "groovyScript", "value", "location2", "location3"));
		aliases.put("location2", Arrays.asList("xmlFile", "groovyScript", "value", "location1", "location3"));
		aliases.put("location3", Arrays.asList("xmlFile", "groovyScript", "value", "location1", "location2"));

		attributeAliasesCache.put(annotationType, aliases);

		MapAnnotationAttributeExtractor extractor = new MapAnnotationAttributeExtractor(sourceAttributes, annotationType, null);
		Map<String, Object> enriched = extractor.getSource();

		assertEquals("attribute map size", expected.size(), enriched.size());
		expected.forEach((attr, expectedValue) -> assertThat("for attribute '" + attr + "'", enriched.get(attr), is(expectedValue)));
	}

}
