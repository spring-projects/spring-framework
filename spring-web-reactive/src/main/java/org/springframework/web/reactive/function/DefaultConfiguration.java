/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.function;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.core.codec.ByteBufferDecoder;
import org.springframework.core.codec.ByteBufferEncoder;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.xml.Jaxb2XmlDecoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.util.ClassUtils;

/**
 * A default implementation of configuration.
 * @author Arjen Poutsma
 */
class DefaultConfiguration implements Router.Configuration {

	private static final boolean jackson2Present =
			ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper",
					DefaultConfiguration.class.getClassLoader()) &&
					ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator",
							DefaultConfiguration.class.getClassLoader());

	private static final boolean jaxb2Present =
			ClassUtils.isPresent("javax.xml.bind.Binder", DefaultConfiguration.class.getClassLoader());

	private final List<HttpMessageReader<?>> messageReaders = new ArrayList<>();

	private final List<HttpMessageWriter<?>> messageWriters = new ArrayList<>();

	public DefaultConfiguration() {
		this.messageReaders.add(new DecoderHttpMessageReader<>(new ByteBufferDecoder()));
		this.messageReaders.add(new DecoderHttpMessageReader<>(new StringDecoder()));
		this.messageWriters.add(new EncoderHttpMessageWriter<>(new ByteBufferEncoder()));
		this.messageWriters.add(new EncoderHttpMessageWriter<>(new CharSequenceEncoder()));
		if (jaxb2Present) {
			this.messageReaders.add(new DecoderHttpMessageReader<>(new Jaxb2XmlDecoder()));
			this.messageWriters.add(new EncoderHttpMessageWriter<>(new Jaxb2XmlEncoder()));
		}
		if (jackson2Present) {
			this.messageReaders.add(new DecoderHttpMessageReader<>(new Jackson2JsonDecoder()));
			this.messageWriters.add(new EncoderHttpMessageWriter<>(new Jackson2JsonEncoder()));
		}
	}

	@Override
	public Supplier<Stream<HttpMessageReader<?>>> messageReaders() {
		return this.messageReaders::stream;
	}

	@Override
	public Supplier<Stream<HttpMessageWriter<?>>> messageWriters() {
		return this.messageWriters::stream;
	}
}
