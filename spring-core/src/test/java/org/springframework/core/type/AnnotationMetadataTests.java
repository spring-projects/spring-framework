/*
 * Copyright 2002-2009 the original author or authors.
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
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.stereotype.Component;

/**
 * @author Juergen Hoeller
 */
public class AnnotationMetadataTests extends TestCase {

	public void testStandardAnnotationMetadata() throws IOException {
		StandardAnnotationMetadata annInfo = new StandardAnnotationMetadata(AnnotatedComponent.class);
		doTestAnnotationInfo(annInfo);
		doTestMethodAnnotationInfo(annInfo);
	}

	public void testAsmAnnotationMetadata() throws IOException {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(AnnotatedComponent.class.getName());
		doTestAnnotationInfo(metadataReader.getAnnotationMetadata());
		doTestMethodAnnotationInfo(metadataReader.getAnnotationMetadata());
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
		assertTrue(metadata.hasAnnotation(SpecialAttr.class.getName()));
		assertEquals(3, metadata.getAnnotationTypes().size());
		assertTrue(metadata.getAnnotationTypes().contains(Component.class.getName()));
		assertTrue(metadata.getAnnotationTypes().contains(Scope.class.getName()));
		assertTrue(metadata.getAnnotationTypes().contains(SpecialAttr.class.getName()));

		Map<String, Object> compAttrs = metadata.getAnnotationAttributes(Component.class.getName());
		assertEquals(1, compAttrs.size());
		assertEquals("myName", compAttrs.get("value"));
		Map<String, Object> scopeAttrs = metadata.getAnnotationAttributes(Scope.class.getName());
		assertEquals(1, scopeAttrs.size());
		assertEquals("myScope", scopeAttrs.get("value"));
		Map<String, Object> specialAttrs = metadata.getAnnotationAttributes(SpecialAttr.class.getName());
		assertEquals(2, specialAttrs.size());
		assertEquals(String.class, specialAttrs.get("clazz"));
		assertEquals(Thread.State.NEW, specialAttrs.get("state"));
		Map<String, Object> specialAttrsString = metadata.getAnnotationAttributes(SpecialAttr.class.getName(), true);
		assertEquals(String.class.getName(), specialAttrsString .get("clazz"));
		assertEquals(Thread.State.NEW, specialAttrsString.get("state"));
	}

	private void doTestMethodAnnotationInfo(AnnotationMetadata classMetadata) {
		Set<MethodMetadata> methods = classMetadata.getAnnotatedMethods(Autowired.class.getName());
		assertEquals(1, methods.size());
		for (MethodMetadata methodMetadata : methods) {
			assertTrue(methodMetadata.isAnnotated(Autowired.class.getName()));
		}
		
	}


	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface SpecialAttr {

		Class clazz();

		Thread.State state();
	}


	@Component("myName")
	@Scope("myScope")
	@SpecialAttr(clazz = String.class, state = Thread.State.NEW)
	private static class AnnotatedComponent implements Serializable {
		
		@Autowired
		public void doWork(@Qualifier("myColor") java.awt.Color color) {
		}

		public void doSleep()  {
		}
	}

}
