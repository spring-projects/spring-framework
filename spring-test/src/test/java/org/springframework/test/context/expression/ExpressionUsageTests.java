/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.expression;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Andy Clement
 * @author Dave Syer
 */
@SpringJUnitConfig
@DisabledInAotMode("SpEL is not supported in AOT")
class ExpressionUsageTests {

	@Autowired
	@Qualifier("derived")
	private Properties props;

	@Autowired
	@Qualifier("andy2")
	private Foo andy2;

	@Autowired
	@Qualifier("andy")
	private Foo andy;


	@Test
	void spr5906() {
		// verify the property values have been evaluated as expressions
		assertThat(props.getProperty("user.name")).isEqualTo("Dave");
		assertThat(props.getProperty("username")).isEqualTo("Andy");

		// verify the property keys have been evaluated as expressions
		assertThat(props.getProperty("Dave")).isEqualTo("exists");
		assertThat(props.getProperty("Andy")).isEqualTo("exists also");
	}

	@Test
	void spr5847() {
		assertThat(andy2.getName()).isEqualTo("Andy");
		assertThat(andy.getName()).isEqualTo("Andy");
	}


	public static class Foo {

		private String name;


		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
