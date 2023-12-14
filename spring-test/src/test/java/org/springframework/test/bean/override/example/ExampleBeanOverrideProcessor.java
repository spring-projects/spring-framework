/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.bean.override.example;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.springframework.core.ResolvableType;
import org.springframework.test.bean.override.BeanOverrideProcessor;
import org.springframework.test.bean.override.OverrideMetadata;

public class ExampleBeanOverrideProcessor implements BeanOverrideProcessor {

	public ExampleBeanOverrideProcessor() {
	}

	private static final TestOverrideMetadata CONSTANT = new TestOverrideMetadata() {
		@Override
		public String toString() {
			return "{DUPLICATE_TRIGGER}";
		}
	};
	public static final String DUPLICATE_TRIGGER = "CONSTANT";

	@Override
	public OverrideMetadata createMetadata(Field field, Annotation overrideAnnotation, ResolvableType typeToOverride) {
		if (!(overrideAnnotation instanceof ExampleBeanOverrideAnnotation annotation)) {
			throw new IllegalStateException("unexpected annotation");
		}
		if (annotation.value().equals(DUPLICATE_TRIGGER)) {
			return CONSTANT;
		}
		return new TestOverrideMetadata(field, annotation, typeToOverride);
	}
}
