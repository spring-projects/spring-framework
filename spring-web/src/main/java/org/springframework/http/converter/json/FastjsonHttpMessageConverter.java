/*
 * Copyright 2002-2020 the original author or authors.
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

import java.io.BufferedReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter}.
 * that can read and write JSON using the
 * <a href="https://github.com/alibaba/fastjson2/">Alibaba fastjson2</a>。
 *
 * <p>This converter can be used to bind to typed beans or untyped {@code HashMap}s.
 * By default, it supports {@code application/json} and {@code application/*+json} with
 * {@code UTF-8} character set.
 *
 * <p>Tested against fastjson 2.0.40; compatible with fastjson 2.0 and higher.
 *
 * @author miao enkui
 * @since 6.1
 * @see com.alibaba.fastjson2.JSONObject
 * @see #setReadFeatures setWriteFeatures
 */
public class FastjsonHttpMessageConverter extends AbstractJsonHttpMessageConverter {
	private List<JSONReader.Feature> readFeatures;
	private List<JSONWriter.Feature> writeFeatures;

	/**
	 * Construct a new {@code FastjsonHttpMessageConverter} with default configuration.
	 */
	public FastjsonHttpMessageConverter() {
		this.readFeatures = new ArrayList<>();
		this.writeFeatures = new ArrayList<>();
	}

	/**
	 * Construct a new {@code FastjsonHttpMessageConverter} with the given JSONReader.Feature....
	 * @param readFeatures the JSONReader.Feature... to use
	 * @since 6.1
	 */
	public FastjsonHttpMessageConverter(JSONReader.Feature... readFeatures) {
		Assert.notNull(readFeatures, "JSONReader.Feature are required");
		this.readFeatures = List.of(readFeatures);
		this.writeFeatures = new ArrayList<>();
	}

	/**
	 * Construct a new {@code FastjsonHttpMessageConverter} with the given JSONWriter.Feature....
	 * @param writeFeatures the JSONWriter.Feature... to use
	 * @since 6.1
	 */
	public FastjsonHttpMessageConverter(JSONWriter.Feature... writeFeatures) {
		Assert.notNull(writeFeatures, "JSONWriter.Feature are required");
		this.writeFeatures = List.of(writeFeatures);
		this.readFeatures = new ArrayList<>();
	}

	/**
	 * Construct a new {@code FastjsonHttpMessageConverter} with the given JSONWriter.Feature... and JSONWriter.Feature....
	 * @param readFeatures the JSONReader.Feature... to use
	 * @param writeFeatures the JSONWriter.Feature... to use
	 * @since 6.1
	 */
	public FastjsonHttpMessageConverter(List<JSONReader.Feature> readFeatures, List<JSONWriter.Feature> writeFeatures) {
		Assert.notNull(readFeatures, "JSONReader.Feature are required");
		Assert.notNull(writeFeatures, "JSONWriter.Feature are required");
		this.readFeatures = readFeatures;
		this.writeFeatures = writeFeatures;
	}

	/**
	 * Set the {@code JSONReader.Feature...} to use.
	 * If not set, a default {@link JSONReader.Feature...} will be used.
	 * <p>Setting a custom-configured {@code JSONReader.Feature...} is one way to take further
	 * control of the JSON serialization process.
	 * @see #FastjsonHttpMessageConverter(JSONReader.Feature...)
	 * @since 6.1
	 */
	public void setReadFeatures(JSONReader.Feature... readFeatures) {
		Assert.notNull(readFeatures, "JSONReader.Feature are required");
		this.readFeatures = List.of(readFeatures);
	}

	/**
	 * Return the configured {@code List<JSONReader.Feature>} for this converter.
	 * @since 6.1
	 */
	public List<JSONReader.Feature> getReadFeatures() {
		return this.readFeatures;
	}

	/**
	 * Set a {@code JSONWriter.Feature...} to use.
	 * If not set, a default {@link JSONWriter.Feature...} will be used.
	 * <p>Setting a custom-configured {@code JSONWriter.Feature...} is one way to take further
	 * control of the JSON serialization process.
	 * @see #FastjsonHttpMessageConverter(JSONWriter.Feature...)
	 * @since 6.1
	 */
	public void setWriteFeatures(JSONWriter.Feature... writeFeatures) {
		Assert.notNull(writeFeatures, "JSONWriter.Feature are required");
		this.writeFeatures = List.of(writeFeatures);
	}

	/**
	 * Return the configured {@code List<JSONWriter.Feature>} for this converter.
	 * @since 6.1
	 */
	public List<JSONWriter.Feature> getWriteFeatures() {
		return this.writeFeatures;
	}

	@Override
	protected Object readInternal(Type resolvedType, Reader reader) throws Exception {
		BufferedReader bufferedReader = new BufferedReader(reader);
		Stream<String> lines = bufferedReader.lines();
		StringBuilder stringBuilder = new StringBuilder();
		lines.forEach(stringBuilder::append);
		return JSONObject.parseObject(stringBuilder.toString(), resolvedType, this.readFeatures.toArray(new JSONReader.Feature[0]));
	}

	@Override
	protected void writeInternal(Object object, @Nullable Type type, Writer writer) throws Exception {
		writer.write(JSONObject.toJSONString(object, this.writeFeatures.toArray(new JSONWriter.Feature[0])));
	}
}
