/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.http.converter.json;

import java.util.Calendar;
import java.util.Date;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link GsonFactoryBean} tests.
 *
 * @author Roy Clarkson
 */
class GsonFactoryBeanTests {

	private static final String DATE_FORMAT = "yyyy-MM-dd";

	private final GsonFactoryBean factory = new GsonFactoryBean();


	@Test
	void serializeNulls() throws Exception {
		this.factory.setSerializeNulls(true);
		this.factory.afterPropertiesSet();
		Gson gson = this.factory.getObject();
		StringBean bean = new StringBean();

		assertThat(gson.toJson(bean)).isEqualTo("{\"name\":null}");
	}

	@Test
	void serializeNullsFalse() throws Exception {
		this.factory.setSerializeNulls(false);
		this.factory.afterPropertiesSet();
		Gson gson = this.factory.getObject();
		StringBean bean = new StringBean();

		assertThat(gson.toJson(bean)).isEqualTo("{}");
	}

	@Test
	void prettyPrinting() throws Exception {
		this.factory.setPrettyPrinting(true);
		this.factory.afterPropertiesSet();
		Gson gson = this.factory.getObject();
		StringBean bean = new StringBean();
		bean.setName("Jason");
		assertThat(gson.toJson(bean)).contains("  \"name\": \"Jason\"");
	}

	@Test
	void prettyPrintingFalse() throws Exception {
		this.factory.setPrettyPrinting(false);
		this.factory.afterPropertiesSet();
		Gson gson = this.factory.getObject();
		StringBean bean = new StringBean();
		bean.setName("Jason");

		assertThat(gson.toJson(bean)).isEqualTo("{\"name\":\"Jason\"}");
	}

	@Test
	void disableHtmlEscaping() throws Exception {
		this.factory.setDisableHtmlEscaping(true);
		this.factory.afterPropertiesSet();
		Gson gson = this.factory.getObject();
		StringBean bean = new StringBean();
		bean.setName("Bob=Bob");

		assertThat(gson.toJson(bean)).isEqualTo("{\"name\":\"Bob=Bob\"}");
	}

	@Test
	void disableHtmlEscapingFalse() throws Exception {
		this.factory.setDisableHtmlEscaping(false);
		this.factory.afterPropertiesSet();
		Gson gson = this.factory.getObject();
		StringBean bean = new StringBean();
		bean.setName("Bob=Bob");

		assertThat(gson.toJson(bean)).isEqualTo("{\"name\":\"Bob\\u003dBob\"}");
	}

	@Test
	void customizeDateFormatPattern() throws Exception {
		this.factory.setDateFormatPattern(DATE_FORMAT);
		this.factory.afterPropertiesSet();
		Gson gson = this.factory.getObject();
		DateBean bean = new DateBean();
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(Calendar.YEAR, 2014);
		cal.set(Calendar.MONTH, Calendar.JANUARY);
		cal.set(Calendar.DATE, 1);
		Date date = cal.getTime();
		bean.setDate(date);

		assertThat(gson.toJson(bean)).isEqualTo("{\"date\":\"2014-01-01\"}");
	}

	@Test
	void customizeDateFormatNone() throws Exception {
		this.factory.afterPropertiesSet();
		Gson gson = this.factory.getObject();
		DateBean bean = new DateBean();
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(Calendar.YEAR, 2014);
		cal.set(Calendar.MONTH, Calendar.JANUARY);
		cal.set(Calendar.DATE, 1);
		Date date = cal.getTime();
		bean.setDate(date);
		assertThat(gson.toJson(bean))
				.startsWith("{\"date\":\"Jan 1, 2014")
				.endsWith("12:00:00 AM\"}");
	}

	@Test
	void base64EncodeByteArrays() throws Exception {
		this.factory.setBase64EncodeByteArrays(true);
		this.factory.afterPropertiesSet();
		Gson gson = this.factory.getObject();
		ByteArrayBean bean = new ByteArrayBean();
		bean.setBytes(new byte[] {0x1, 0x2});

		assertThat(gson.toJson(bean)).isEqualTo("{\"bytes\":\"AQI\\u003d\"}");
	}

	@Test
	void base64EncodeByteArraysDisableHtmlEscaping() throws Exception {
		this.factory.setBase64EncodeByteArrays(true);
		this.factory.setDisableHtmlEscaping(true);
		this.factory.afterPropertiesSet();
		Gson gson = this.factory.getObject();
		ByteArrayBean bean = new ByteArrayBean();
		bean.setBytes(new byte[] {0x1, 0x2});

		assertThat(gson.toJson(bean)).isEqualTo("{\"bytes\":\"AQI=\"}");
	}

	@Test
	void base64EncodeByteArraysFalse() throws Exception {
		this.factory.setBase64EncodeByteArrays(false);
		this.factory.afterPropertiesSet();
		Gson gson = this.factory.getObject();
		ByteArrayBean bean = new ByteArrayBean();
		bean.setBytes(new byte[] {0x1, 0x2});

		assertThat(gson.toJson(bean)).isEqualTo("{\"bytes\":[1,2]}");
	}


	private static class StringBean {

		private String name;

		@SuppressWarnings("unused")
		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}


	private static class DateBean {

		private Date date;

		@SuppressWarnings("unused")
		public Date getDate() {
			return this.date;
		}

		public void setDate(Date date) {
			this.date = date;
		}
	}


	private static class ByteArrayBean {

		private byte[] bytes;

		@SuppressWarnings("unused")
		public byte[] getBytes() {
			return this.bytes;
		}

		public void setBytes(byte[] bytes) {
			this.bytes = bytes;
		}
	}

}
