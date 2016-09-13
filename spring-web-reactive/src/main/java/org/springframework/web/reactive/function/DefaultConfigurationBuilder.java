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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.context.ApplicationContext;
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
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.reactive.result.view.ViewResolver;

/**
 * @author Arjen Poutsma
 * @since 5.0
 */
class DefaultConfigurationBuilder implements Configuration.Builder {

	private static final boolean jackson2Present =
			ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper",
					DefaultConfigurationBuilder.class.getClassLoader()) &&
					ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator",
							DefaultConfigurationBuilder.class.getClassLoader());

	private static final boolean jaxb2Present =
			ClassUtils.isPresent("javax.xml.bind.Binder",
					DefaultConfigurationBuilder.class.getClassLoader());


	private final List<HttpMessageReader<?>> messageReaders = new ArrayList<>();

	private final List<HttpMessageWriter<?>> messageWriters = new ArrayList<>();

	private final List<ViewResolver> viewResolvers = new ArrayList<>();

	public void defaultConfiguration() {
		messageReader(new DecoderHttpMessageReader<>(new ByteBufferDecoder()));
		messageReader(new DecoderHttpMessageReader<>(new StringDecoder()));
		messageWriter(new EncoderHttpMessageWriter<>(new ByteBufferEncoder()));
		messageWriter(new EncoderHttpMessageWriter<>(new CharSequenceEncoder()));
		if (jaxb2Present) {
			messageReader(new DecoderHttpMessageReader<>(new Jaxb2XmlDecoder()));
			messageWriter(new EncoderHttpMessageWriter<>(new Jaxb2XmlEncoder()));
		}
		if (jackson2Present) {
			messageReader(new DecoderHttpMessageReader<>(new Jackson2JsonDecoder()));
			messageWriter(new EncoderHttpMessageWriter<>(new Jackson2JsonEncoder()));
		}
	}

	public void applicationContext(ApplicationContext applicationContext) {
		applicationContext.getBeansOfType(HttpMessageReader.class).values().forEach(this::messageReader);
		applicationContext.getBeansOfType(HttpMessageWriter.class).values().forEach(this::messageWriter);
		applicationContext.getBeansOfType(ViewResolver.class).values().forEach(this::viewResolver);
	}

	@Override
	public Configuration.Builder messageReader(HttpMessageReader<?> messageReader) {
		Assert.notNull(messageReader, "'messageReader' must not be null");
		this.messageReaders.add(messageReader);
		return this;
	}

	@Override
	public Configuration.Builder messageWriter(HttpMessageWriter<?> messageWriter) {
		Assert.notNull(messageWriter, "'messageWriter' must not be null");
		this.messageWriters.add(messageWriter);
		return this;
	}

	@Override
	public Configuration.Builder viewResolver(ViewResolver viewResolver) {
		Assert.notNull(viewResolver, "'viewResolver' must not be null");
		this.viewResolvers.add(viewResolver);
		return this;
	}

	@Override
	public Configuration build() {
		return new DefaultConfiguration(this.messageReaders, this.messageWriters,
				this.viewResolvers);
	}

	private static class DefaultConfiguration implements Configuration {

		private final List<HttpMessageReader<?>> messageReaders;

		private final List<HttpMessageWriter<?>> messageWriters;

		private final List<ViewResolver> viewResolvers;

		public DefaultConfiguration(
				List<HttpMessageReader<?>> messageReaders,
				List<HttpMessageWriter<?>> messageWriters,
				List<ViewResolver> viewResolvers) {
			this.messageReaders = unmodifiableCopy(messageReaders);
			this.messageWriters = unmodifiableCopy(messageWriters);
			this.viewResolvers = unmodifiableCopy(viewResolvers);
		}

		private static <T> List<T> unmodifiableCopy(List<? extends T> list) {
			return Collections.unmodifiableList(new ArrayList<>(list));
		}

		@Override
		public Supplier<Stream<HttpMessageReader<?>>> messageReaders() {
			return this.messageReaders::stream;
		}

		@Override
		public Supplier<Stream<HttpMessageWriter<?>>> messageWriters() {
			return this.messageWriters::stream;
		}

		@Override
		public Supplier<Stream<ViewResolver>> viewResolvers() {
			return this.viewResolvers::stream;
		}
	}

}
