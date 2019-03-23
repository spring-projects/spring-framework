/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.validation;

import org.junit.Test;

import org.springframework.tests.sample.beans.TestBean;
import org.springframework.validation.DefaultMessageCodesResolver.Format;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Tests for {@link DefaultMessageCodesResolver}.
 *
 * @author Phillip Webb
 */
public class DefaultMessageCodesResolverTests {

	private DefaultMessageCodesResolver resolver = new DefaultMessageCodesResolver();

	@Test
	public void shouldResolveMessageCode() throws Exception {
		String[] codes = resolver.resolveMessageCodes("errorCode", "objectName");
		assertThat(codes, is(equalTo(new String[] {
				"errorCode.objectName",
				"errorCode" })));
	}

	@Test
	public void shouldResolveFieldMessageCode() throws Exception {
		String[] codes = resolver.resolveMessageCodes("errorCode", "objectName", "field",
				TestBean.class);
		assertThat(codes, is(equalTo(new String[] {
				"errorCode.objectName.field",
				"errorCode.field",
				"errorCode.org.springframework.tests.sample.beans.TestBean",
				"errorCode" })));
	}

	@Test
	public void shouldResolveIndexedFieldMessageCode() throws Exception {
		String[] codes = resolver.resolveMessageCodes("errorCode", "objectName", "a.b[3].c[5].d",
				TestBean.class);
		assertThat(codes, is(equalTo(new String[] {
				"errorCode.objectName.a.b[3].c[5].d",
				"errorCode.objectName.a.b[3].c.d",
				"errorCode.objectName.a.b.c.d",
				"errorCode.a.b[3].c[5].d",
				"errorCode.a.b[3].c.d",
				"errorCode.a.b.c.d",
				"errorCode.d",
				"errorCode.org.springframework.tests.sample.beans.TestBean",
				"errorCode" })));
	}

	@Test
	public void shouldResolveMessageCodeWithPrefix() throws Exception {
		resolver.setPrefix("prefix.");
		String[] codes = resolver.resolveMessageCodes("errorCode", "objectName");
		assertThat(codes, is(equalTo(new String[] {
				"prefix.errorCode.objectName",
				"prefix.errorCode" })));
	}

	@Test
	public void shouldResolveFieldMessageCodeWithPrefix() throws Exception {
		resolver.setPrefix("prefix.");
		String[] codes = resolver.resolveMessageCodes("errorCode", "objectName", "field",
				TestBean.class);
		assertThat(codes, is(equalTo(new String[] {
				"prefix.errorCode.objectName.field",
				"prefix.errorCode.field",
				"prefix.errorCode.org.springframework.tests.sample.beans.TestBean",
				"prefix.errorCode" })));
	}

	@Test
	public void shouldSupportNullPrefix() throws Exception {
		resolver.setPrefix(null);
		String[] codes = resolver.resolveMessageCodes("errorCode", "objectName", "field",
				TestBean.class);
		assertThat(codes, is(equalTo(new String[] {
				"errorCode.objectName.field",
				"errorCode.field",
				"errorCode.org.springframework.tests.sample.beans.TestBean",
				"errorCode" })));
	}

	@Test
	public void shouldSupportMalformedIndexField() throws Exception {
		String[] codes = resolver.resolveMessageCodes("errorCode", "objectName", "field[",
				TestBean.class);
		assertThat(codes, is(equalTo(new String[] {
				"errorCode.objectName.field[",
				"errorCode.field[",
				"errorCode.org.springframework.tests.sample.beans.TestBean",
				"errorCode" })));
	}

	@Test
	public void shouldSupportNullFieldType() throws Exception {
		String[] codes = resolver.resolveMessageCodes("errorCode", "objectName", "field",
				null);
		assertThat(codes, is(equalTo(new String[] {
				"errorCode.objectName.field",
				"errorCode.field",
				"errorCode" })));
	}

	@Test
	public void shouldSupportPostfixFormat() throws Exception {
		resolver.setMessageCodeFormatter(Format.POSTFIX_ERROR_CODE);
		String[] codes = resolver.resolveMessageCodes("errorCode", "objectName");
		assertThat(codes, is(equalTo(new String[] {
				"objectName.errorCode",
				"errorCode" })));
	}

	@Test
	public void shouldSupportFieldPostfixFormat() throws Exception {
		resolver.setMessageCodeFormatter(Format.POSTFIX_ERROR_CODE);
		String[] codes = resolver.resolveMessageCodes("errorCode", "objectName", "field",
				TestBean.class);
		assertThat(codes, is(equalTo(new String[] {
				"objectName.field.errorCode",
				"field.errorCode",
				"org.springframework.tests.sample.beans.TestBean.errorCode",
				"errorCode" })));
	}

	@Test
	public void shouldSupportCustomFormat() throws Exception {
		resolver.setMessageCodeFormatter(new MessageCodeFormatter() {
			@Override
			public String format(String errorCode, String objectName, String field) {
				return DefaultMessageCodesResolver.Format.toDelimitedString(
						"CUSTOM-" + errorCode, objectName, field);
			}
		});
		String[] codes = resolver.resolveMessageCodes("errorCode", "objectName");
		assertThat(codes, is(equalTo(new String[] {
				"CUSTOM-errorCode.objectName",
				"CUSTOM-errorCode" })));
	}
}
