/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.core.type.classreading;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.io.DefaultResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for primitive array attribute parsing by the {@code java.lang.classfile}
 * based {@link ClassFileAnnotationDelegate}. Mirrors the ASM-based
 * {@code MergedAnnotationMetadataVisitorTests} expectations.
 *
 * @author junhyeong9812
 */
class ClassFileAnnotationDelegatePrimitiveArrayTests {

	@Test
	void parsesNonEmptyPrimitiveArrayAttributes() throws Exception {
		MergedAnnotation<?> annotation = readAnnotation(WithArrays.class);
		assertThat(annotation.getValue("byteValue")).contains(new byte[] { 1 });
		assertThat(annotation.getValue("shortValue")).contains(new short[] { 2 });
		assertThat(annotation.getValue("intValue")).contains(new int[] { 3 });
		assertThat(annotation.getValue("longValue")).contains(new long[] { 4 });
		assertThat(annotation.getValue("booleanValue")).contains(new boolean[] { true });
		assertThat(annotation.getValue("charValue")).contains(new char[] { 'c' });
		assertThat(annotation.getValue("doubleValue")).contains(new double[] { 5.0 });
		assertThat(annotation.getValue("floatValue")).contains(new float[] { 6.0f });
	}

	@Test
	void parsesEmptyPrimitiveArrayAttributes() throws Exception {
		MergedAnnotation<?> annotation = readAnnotation(WithEmptyArrays.class);
		assertThat(annotation.getValue("byteValue")).contains(new byte[] {});
		assertThat(annotation.getValue("floatValue")).contains(new float[] {});
	}

	private static MergedAnnotation<?> readAnnotation(Class<?> type) throws Exception {
		ClassFileMetadataReaderFactory factory = new ClassFileMetadataReaderFactory(new DefaultResourceLoader());
		MetadataReader reader = factory.getMetadataReader(type.getName());
		return reader.getAnnotationMetadata().getAnnotations().get(ArrayTypesAnnotation.class);
	}


	@ArrayTypesAnnotation(byteValue = 1, shortValue = 2, intValue = 3, longValue = 4,
			booleanValue = true, charValue = 'c', doubleValue = 5.0, floatValue = 6.0f)
	static class WithArrays {
	}

	@ArrayTypesAnnotation(byteValue = {}, shortValue = {}, intValue = {}, longValue = {},
			booleanValue = {}, charValue = {}, doubleValue = {}, floatValue = {})
	static class WithEmptyArrays {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface ArrayTypesAnnotation {

		byte[] byteValue();

		short[] shortValue();

		int[] intValue();

		long[] longValue();

		boolean[] booleanValue();

		char[] charValue();

		double[] doubleValue();

		float[] floatValue();
	}

}
