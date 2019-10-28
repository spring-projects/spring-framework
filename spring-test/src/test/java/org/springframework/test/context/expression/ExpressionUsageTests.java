/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.test.context.expression;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Andy Clement
 * @author Dave Syer
 */
@SpringJUnitConfig
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
	void testSpr5906() throws Exception {
		// verify the property values have been evaluated as expressions
		assertThat(props.getProperty("user.name")).isEqualTo("Dave");
		assertThat(props.getProperty("username")).isEqualTo("Andy");

		// verify the property keys have been evaluated as expressions
		assertThat(props.getProperty("Dave")).isEqualTo("exists");
		assertThat(props.getProperty("Andy")).isEqualTo("exists also");
	}

	@Test
	void testSpr5847() throws Exception {
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
