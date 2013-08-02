/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.core;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link MethodParameters}.
 * 
 * @author Oliver Gierke
 */
public class MethodParametersUnitTests {

	@Test
	public void prefersAnnotatedParameterOverDiscovered() throws Exception {

		Method method = Sample.class.getMethod("method", String.class, String.class);
		MethodParameters parameters = new MethodParameters(method,
				new AnnotationAttribute(Qualifier.class));

		assertThat(parameters.getParameter("param"), is(notNullValue()));
		assertThat(parameters.getParameter("foo"), is(notNullValue()));
		assertThat(parameters.getParameter("another"), is(nullValue()));
	}

	@Retention(RetentionPolicy.RUNTIME)
	public static @interface Qualifier {

		String value() default "";
	}

	static class Sample {

		public void method(String param, @Qualifier("foo") String another) {
		}
	}
}
