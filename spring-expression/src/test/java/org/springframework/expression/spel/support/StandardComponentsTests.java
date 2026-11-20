/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
