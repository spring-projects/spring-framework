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

package org.springframework.expression.spel.support;

import org.junit.jupiter.api.Test;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Operation;
import org.springframework.expression.OperatorOverloader;
import org.springframework.expression.TypeComparator;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypeLocator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class StandardComponentsTests {

	@Test
	void standardEvaluationContext() {
		StandardEvaluationContext context = new StandardEvaluationContext();
		assertThat(context.getTypeComparator()).isNotNull();

		TypeComparator tc = StandardTypeComparator.INSTANCE;
		context.setTypeComparator(tc);
		assertThat(context.getTypeComparator()).isEqualTo(tc);

		TypeLocator tl = new StandardTypeLocator();
		context.setTypeLocator(tl);
		assertThat(context.getTypeLocator()).isEqualTo(tl);
	}

	@Test
	void standardOperatorOverloader() {
		OperatorOverloader overloader = new StandardOperatorOverloader();
		assertThat(overloader.overridesOperation(Operation.ADD, null, null)).isFalse();
		assertThatExceptionOfType(EvaluationException.class)
				.isThrownBy(() -> overloader.operate(Operation.ADD, 2, 3));
	}

	@Test
	void standardTypeLocator() {
		StandardTypeLocator tl = new StandardTypeLocator();
		assertThat(tl.getImportPrefixes()).hasSize(1);
		tl.registerImport("java.util");
		assertThat(tl.getImportPrefixes()).hasSize(2);
		tl.removeImport("java.util");
		assertThat(tl.getImportPrefixes()).hasSize(1);
	}

	@Test
	void standardTypeConverter() {
		TypeConverter tc = new StandardTypeConverter();
		tc.convertValue(3, TypeDescriptor.forObject(3), TypeDescriptor.valueOf(Double.class));
	}

}
