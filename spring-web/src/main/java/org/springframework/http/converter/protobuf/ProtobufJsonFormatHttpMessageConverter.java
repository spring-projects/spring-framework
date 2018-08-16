/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.http.converter.protobuf;

import com.google.protobuf.util.JsonFormat;

import org.springframework.lang.Nullable;

/**
 * Subclass of {@link ProtobufHttpMessageConverter} which enforces the use of Protobuf 3 and
 * its official library {@code "com.google.protobuf:protobuf-java-util"} for JSON processing.
 *
 * <p>Most importantly, this class allows for custom JSON parser and printer configurations
 * through the {@link JsonFormat} utility. If no special parser or printer configuration is
 * given, default variants will be used instead.
 *
 * <p>Requires Protobuf 3.x and {@code "com.google.protobuf:protobuf-java-util"} 3.x,
 * with 3.3 or higher recommended.
 *
 * @author Juergen Hoeller
 * @since 5.0
 * @see JsonFormat#parser()
 * @see JsonFormat#printer()
 * @see #ProtobufJsonFormatHttpMessageConverter(JsonFormat.Parser, JsonFormat.Printer)
 */
public class ProtobufJsonFormatHttpMessageConverter extends ProtobufHttpMessageConverter {

	/**
	 * Construct a new {@code ProtobufJsonFormatHttpMessageConverter} with default
	 * {@code JsonFormat.Parser} and {@code JsonFormat.Printer} configuration.
	 */
	public ProtobufJsonFormatHttpMessageConverter() {
		this(null,  null, null);
	}

	/**
	 * Construct a new {@code ProtobufJsonFormatHttpMessageConverter} with the given
	 * {@code JsonFormat.Parser} and {@code JsonFormat.Printer} configuration.
	 * @param parser the JSON parser configuration
	 * @param printer the JSON printer configuration
	 */
	public ProtobufJsonFormatHttpMessageConverter(
			@Nullable JsonFormat.Parser parser, @Nullable JsonFormat.Printer printer) {

		this(parser, printer, null);
	}

	/**
	 * Construct a new {@code ProtobufJsonFormatHttpMessageConverter} with the given
	 * {@code JsonFormat.Parser} and {@code JsonFormat.Printer} configuration, also
	 * accepting an initializer that allows the registration of message extensions.
	 * @param parser the JSON parser configuration
	 * @param printer the JSON printer configuration
	 * @param registryInitializer an initializer for message extensions
	 */
	public ProtobufJsonFormatHttpMessageConverter(@Nullable JsonFormat.Parser parser,
			@Nullable JsonFormat.Printer printer, @Nullable ExtensionRegistryInitializer registryInitializer) {

		super(new ProtobufJavaUtilSupport(parser, printer), registryInitializer);
	}

}
