/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.http.converter.json;

import java.util.Calendar;
import java.util.Date;

import com.google.gson.Gson;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * {@link GsonFactoryBean} tests.
 *
 * @author Roy Clarkson
 */
public class GsonFactoryBeanTests {

	private static final String DATE_FORMAT = "yyyy-MM-dd";

	private GsonFactoryBean factory = new GsonFactoryBean();


	@Test
	public void serializeNulls() throws Exception {
		this.factory.setSerializeNulls(true);
		this.factory.afterPropertiesSet();
		Gson gson = this.factory.getObject();
		StringBean bean = new StringBean();
		String result = gson.toJson(bean);
		assertEquals("{\"name\":null}", result);
	}

	@Test
	public void serializeNullsFalse() throws Exception {
		this.factory.setSerializeNulls(false);
		this.factory.afterPropertiesSet();
		Gson gson = this.factory.getObject();
		StringBean bean = new StringBean();
		String result = gson.toJson(bean);
		assertEquals("{}", result);
	}

	@Test
	public void prettyPrinting() throws Exception {
		this.factory.setPrettyPrinting(true);
		this.factory.afterPropertiesSet();
		Gson gson = this.factory.getObject();
		StringBean bean = new StringBean();
		bean.setName("Jason");
		String result = gson.toJson(bean);
		assertTrue(result.contains("  \"name\": \"Jason\""));
	}

	@Test
	public void prettyPrintingFalse() throws Exception {
		this.factory.setPrettyPrinting(false);
		this.factory.afterPropertiesSet();
		Gson gson = this.factory.getObject();
		StringBean bean = new StringBean();
		bean.setName("Jason");
		String result = gson.toJson(bean);
		assertEquals("{\"name\":\"Jason\"}", result);
	}

	@Test
	public void disableHtmlEscaping() throws Exception {
		this.factory.setDisableHtmlEscaping(true);
		this.factory.afterPropertiesSet();
		Gson gson = this.factory.getObject();
		StringBean bean = new StringBean();
		bean.setName("Bob=Bob");
		String result = gson.toJson(bean);
		assertEquals("{\"name\":\"Bob=Bob\"}", result);
	}

	@Test
	public void disableHtmlEscapingFalse() throws Exception {
		this.factory.setDisableHtmlEscaping(false);
		this.factory.afterPropertiesSet();
		Gson gson = this.factory.getObject();
		StringBean bean = new StringBean();
		bean.setName("Bob=Bob");
		String result = gson.toJson(bean);
		assertEquals("{\"name\":\"Bob\\u003dBob\"}", result);
	}

	@Test
	public void customizeDateFormatPattern() throws Exception {
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
		String result = gson.toJson(bean);
		assertEquals("{\"date\":\"2014-01-01\"}", result);
	}

	@Test
	public void customizeDateFormatNone() throws Exception {
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
		String result = gson.toJson(bean);
		assertTrue(result.startsWith("{\"date\":\"Jan 1, 2014"));
		assertTrue(result.endsWith("12:00:00 AM\"}"));
	}

	@Test
	public void base64EncodeByteArrays() throws Exception {
		this.factory.setBase64EncodeByteArrays(true);
		this.factory.afterPropertiesSet();
		Gson gson = this.factory.getObject();
		ByteArrayBean bean = new ByteArrayBean();
		bean.setBytes(new byte[] {0x1, 0x2});
		String result = gson.toJson(bean);
		assertEquals("{\"bytes\":\"AQI\\u003d\"}", result);
	}

	@Test
	public void base64EncodeByteArraysDisableHtmlEscaping() throws Exception {
		this.factory.setBase64EncodeByteArrays(true);
		this.factory.setDisableHtmlEscaping(true);
		this.factory.afterPropertiesSet();
		Gson gson = this.factory.getObject();
		ByteArrayBean bean = new ByteArrayBean();
		bean.setBytes(new byte[] {0x1, 0x2});
		String result = gson.toJson(bean);
		assertEquals("{\"bytes\":\"AQI=\"}", result);
	}

	@Test
	public void base64EncodeByteArraysFalse() throws Exception {
		this.factory.setBase64EncodeByteArrays(false);
		this.factory.afterPropertiesSet();
		Gson gson = this.factory.getObject();
		ByteArrayBean bean = new ByteArrayBean();
		bean.setBytes(new byte[] {0x1, 0x2});
		String result = gson.toJson(bean);
		assertEquals("{\"bytes\":[1,2]}", result);
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
