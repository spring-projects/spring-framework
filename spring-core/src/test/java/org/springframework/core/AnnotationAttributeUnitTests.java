/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link AnnotationAttribute}.
 * 
 * @author Oliver Gierke
 */
public class AnnotationAttributeUnitTests {

	AnnotationAttribute valueAttribute = new AnnotationAttribute(MyAnnotation.class);

	AnnotationAttribute nonValueAttribute = new AnnotationAttribute(MyAnnotation.class,
			"nonValue");

	@Test
	public void readsAttributesFromType() {

		assertThat(valueAttribute.findValueOn(Sample.class), is((Object) "foo"));
		assertThat(nonValueAttribute.findValueOn(Sample.class), is((Object) "bar"));
	}

	@Test
	public void findsAttributesFromSubType() {
		assertThat(valueAttribute.findValueOn(SampleSub.class), is((Object) "foo"));
	}

	@Test
	public void doesNotGetValueFromSubTyp() {
		assertThat(valueAttribute.getValueFrom(SampleSub.class), is(nullValue()));
	}

	@Retention(RetentionPolicy.RUNTIME)
	public static @interface MyAnnotation {

		String value() default "";

		String nonValue() default "";
	}

	@MyAnnotation(value = "foo", nonValue = "bar")
	static class Sample {

	}

	static class SampleSub extends Sample {

	}
}
