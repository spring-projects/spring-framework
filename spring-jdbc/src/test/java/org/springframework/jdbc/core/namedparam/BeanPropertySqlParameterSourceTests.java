/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core.namedparam;

import java.sql.Types;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 */
class BeanPropertySqlParameterSourceTests {

	@Test
	void withNullBeanPassedToCtor() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new BeanPropertySqlParameterSource(null));
	}

	@Test
	void getValueWhereTheUnderlyingBeanHasNoSuchProperty() {
		BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(new TestBean());
		assertThatIllegalArgumentException().isThrownBy(() ->
				source.getValue("thisPropertyDoesNotExist"));
	}

	@Test
	void successfulPropertyAccess() {
		BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(new TestBean("tb", 99));
		assertThat(Arrays.asList(source.getReadablePropertyNames())).contains("name");
		assertThat(Arrays.asList(source.getReadablePropertyNames())).contains("age");
		assertThat(source.getValue("name")).isEqualTo("tb");
		assertThat(source.getValue("age")).isEqualTo(99);
		assertThat(source.getSqlType("name")).isEqualTo(Types.VARCHAR);
		assertThat(source.getSqlType("age")).isEqualTo(Types.INTEGER);
	}

	@Test
	void successfulPropertyAccessWithOverriddenSqlType() {
		BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(new TestBean("tb", 99));
		source.registerSqlType("age", Types.NUMERIC);
		assertThat(source.getValue("name")).isEqualTo("tb");
		assertThat(source.getValue("age")).isEqualTo(99);
		assertThat(source.getSqlType("name")).isEqualTo(Types.VARCHAR);
		assertThat(source.getSqlType("age")).isEqualTo(Types.NUMERIC);
	}

	@Test
	void hasValueWhereTheUnderlyingBeanHasNoSuchProperty() {
		BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(new TestBean());
		assertThat(source.hasValue("thisPropertyDoesNotExist")).isFalse();
	}

	@Test
	void getValueWhereTheUnderlyingBeanPropertyIsNotReadable() {
		BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(new NoReadableProperties());
		assertThatIllegalArgumentException().isThrownBy(() ->
				source.getValue("noOp"));
	}

	@Test
	void hasValueWhereTheUnderlyingBeanPropertyIsNotReadable() {
		BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(new NoReadableProperties());
		assertThat(source.hasValue("noOp")).isFalse();
	}

	@Test
	void toStringShowsParameterDetails() {
		BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(new TestBean("tb", 99));
		assertThat(source.toString())
			.startsWith("BeanPropertySqlParameterSource {")
			.contains("name=tb (type:VARCHAR)")
			.contains("age=99 (type:INTEGER)")
			.endsWith("}");
	}

	@Test
	void toStringShowsCustomSqlType() {
		BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(new TestBean("tb", 99));
		source.registerSqlType("name", Integer.MAX_VALUE);
		assertThat(source.toString())
				.startsWith("BeanPropertySqlParameterSource {")
				.contains("name=tb (type:" + Integer.MAX_VALUE + ")")
				.contains("age=99 (type:INTEGER)")
				.endsWith("}");
	}

	@Test
	void toStringDoesNotShowTypeUnknown() {
		BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(new TestBean("tb", 99));
		assertThat(source.toString())
				.startsWith("BeanPropertySqlParameterSource {")
				.contains("beanFactory=null")
				.doesNotContain("beanFactory=null (type:")
				.endsWith("}");
	}


	@SuppressWarnings("unused")
	private static final class NoReadableProperties {

		public void setNoOp(String noOp) {
		}
	}

}
