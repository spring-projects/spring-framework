/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.beans.factory.generator;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.support.CodeSnippet;
import org.springframework.javapoet.support.MultiStatement;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BeanFieldGenerator}.
 *
 * @author Stephane Nicoll
 */
class BeanFieldGeneratorTests {

	private final BeanFieldGenerator generator = new BeanFieldGenerator();

	@Test
	void generateSetFieldWithPublicField() {
		MultiStatement statement = this.generator.generateSetValue("bean",
				field(SampleBean.class, "one"), CodeBlock.of("$S", "test"));
		assertThat(CodeSnippet.process(statement.toCodeBlock())).isEqualTo("""
				bean.one = "test";
				""");
	}

	@Test
	void generateSetFieldWithPrivateField() {
		MultiStatement statement = this.generator.generateSetValue("example",
				field(SampleBean.class, "two"), CodeBlock.of("42"));
		CodeSnippet code = CodeSnippet.of(statement.toCodeBlock());
		assertThat(code.getSnippet()).isEqualTo("""
				Field twoField = ReflectionUtils.findField(BeanFieldGeneratorTests.SampleBean.class, "two");
				ReflectionUtils.makeAccessible(twoField);
				ReflectionUtils.setField(twoField, example, 42);
				""");
		assertThat(code.hasImport(ReflectionUtils.class)).isTrue();
		assertThat(code.hasImport(BeanFieldGeneratorTests.class)).isTrue();
	}


	private Field field(Class<?> type, String name) {
		Field field = ReflectionUtils.findField(type, name);
		assertThat(field).isNotNull();
		return field;
	}


	public static class SampleBean {

		public String one;

		private int two;

	}

}
