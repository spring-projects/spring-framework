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

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import junit.framework.TestCase;

import org.springframework.context.annotation.Scope;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.stereotype.Component;

/**
 * @author Juergen Hoeller
 */
public class AnnotationMetadataTests extends TestCase {

	public void testStandardAnnotationMetadata() throws IOException {
		StandardAnnotationMetadata annInfo = new StandardAnnotationMetadata(AnnotatedComponent.class);
		doTestAnnotationInfo(annInfo);
	}

	public void testAsmAnnotationMetadata() throws IOException {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(AnnotatedComponent.class.getName());
		doTestAnnotationInfo(metadataReader.getAnnotationMetadata());
	}

	private void doTestAnnotationInfo(AnnotationMetadata metadata) {
		assertEquals(AnnotatedComponent.class.getName(), metadata.getClassName());
		assertFalse(metadata.isInterface());
		assertFalse(metadata.isAbstract());
		assertTrue(metadata.isConcrete());
		assertTrue(metadata.hasSuperClass());
		assertEquals(Object.class.getName(), metadata.getSuperClassName());
		assertEquals(1, metadata.getInterfaceNames().length);
		assertEquals(Serializable.class.getName(), metadata.getInterfaceNames()[0]);

		assertTrue(metadata.hasAnnotation(Component.class.getName()));
		assertTrue(metadata.hasAnnotation(Scope.class.getName()));
		assertEquals(2, metadata.getAnnotationTypes().size());
		assertTrue(metadata.getAnnotationTypes().contains(Component.class.getName()));
		assertTrue(metadata.getAnnotationTypes().contains(Scope.class.getName()));

		Map<String, Object> cattrs = metadata.getAnnotationAttributes(Component.class.getName());
		assertEquals(1, cattrs.size());
		assertEquals("myName", cattrs.get("value"));
		Map<String, Object> sattrs = metadata.getAnnotationAttributes(Scope.class.getName());
		assertEquals(1, sattrs.size());
		assertEquals("myScope", sattrs.get("value"));
	}


	@Component("myName")
	@Scope("myScope")
	private static class AnnotatedComponent implements Serializable {
	}

}
