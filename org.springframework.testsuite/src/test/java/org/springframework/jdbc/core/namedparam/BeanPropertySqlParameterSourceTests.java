/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.jdbc.core.namedparam;

import java.sql.Types;
import java.util.Arrays;

import junit.framework.TestCase;

import org.springframework.beans.TestBean;
import org.springframework.test.AssertThrows;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 */
public class BeanPropertySqlParameterSourceTests extends TestCase {

	public void testWithNullBeanPassedToCtor() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				new BeanPropertySqlParameterSource(null);
			}
		}.runTest();
	}

	public void testGetValueWhereTheUnderlyingBeanHasNoSuchProperty() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(new TestBean());
				source.getValue("thisPropertyDoesNotExist");
			}
		}.runTest();
	}

	public void testSuccessfulPropertyAccess(){
		BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(new TestBean("tb", 99));
		assertTrue(Arrays.asList(source.getReadablePropertyNames()).contains("name"));
		assertTrue(Arrays.asList(source.getReadablePropertyNames()).contains("age"));
		assertEquals("tb", source.getValue("name"));
		assertEquals(new Integer(99), source.getValue("age"));
		assertEquals(Types.VARCHAR, source.getSqlType("name"));
		assertEquals(Types.INTEGER, source.getSqlType("age"));
	}

	public void testSuccessfulPropertyAccessWithOverriddenSqlType(){
		BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(new TestBean("tb", 99));
		source.registerSqlType("age", Types.NUMERIC);
		assertEquals("tb", source.getValue("name"));
		assertEquals(new Integer(99), source.getValue("age"));
		assertEquals(Types.VARCHAR, source.getSqlType("name"));
		assertEquals(Types.NUMERIC, source.getSqlType("age"));
	}

	public void testHasValueWhereTheUnderlyingBeanHasNoSuchProperty() throws Exception {
		BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(new TestBean());
		assertFalse(source.hasValue("thisPropertyDoesNotExist"));
	}

	public void testGetValueWhereTheUnderlyingBeanPropertyIsNotReadable() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(new NoReadableProperties());
				source.getValue("noOp");
			}
		}.runTest();
	}

	public void testHasValueWhereTheUnderlyingBeanPropertyIsNotReadable() throws Exception {
		BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(new NoReadableProperties());
		assertFalse(source.hasValue("noOp"));
	}


	private static final class NoReadableProperties {

		public void setNoOp(String noOp) {
		}
	}

}
