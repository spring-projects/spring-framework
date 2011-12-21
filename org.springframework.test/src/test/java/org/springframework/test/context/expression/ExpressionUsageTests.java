/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.test.context.expression;

import static junit.framework.Assert.assertEquals;

import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Andy Clement
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ExpressionUsageTests {

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
	public void testSpr5906() throws Exception {
		// verify the property values have been evaluated as expressions
		assertEquals("Dave", props.getProperty("user.name"));
		assertEquals("Andy", props.getProperty("username"));

		// verify the property keys have been evaluated as expressions
		assertEquals("exists", props.getProperty("Dave"));
		assertEquals("exists also", props.getProperty("Andy"));
	}

	@Test
	public void testSpr5847() throws Exception {
		assertEquals("Andy", andy2.getName());
		assertEquals("Andy", andy.getName());
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
