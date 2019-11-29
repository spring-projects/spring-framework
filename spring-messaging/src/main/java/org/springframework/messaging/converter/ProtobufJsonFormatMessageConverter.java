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
package org.springframework.messaging.converter;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.util.JsonFormat;

import org.springframework.lang.Nullable;

/**
 * Subclass of {@link ProtobufMessageConverter} for use with the official
 * {@code "com.google.protobuf:protobuf-java-util"} library for JSON support.
 *
 * <p>Most importantly, this class allows for custom JSON parser and printer
 * configurations through the {@link JsonFormat} utility. If no special parser
 * or printer configuration is given, default variants will be used instead.
 *
 * <p>Requires Protobuf 3.x and {@code "com.google.protobuf:protobuf-java-util"} 3.x,
 * with 3.3 or higher recommended.
 *
 * @author Rossen Stoyanchev
 * @since 5.2.2
 */
public class ProtobufJsonFormatMessageConverter extends ProtobufMessageConverter {

	/**
	 * Constructor with default instances of {@link JsonFormat.Parser},
	 * {@link JsonFormat.Printer}, and {@link ExtensionRegistry}.
	 */
	public ProtobufJsonFormatMessageConverter(@Nullable ExtensionRegistry extensionRegistry) {
		this(null, null);
	}

	/**
	 * Constructor with given instances of {@link JsonFormat.Parser},
	 * {@link JsonFormat.Printer}, and a default instance of {@link ExtensionRegistry}.
	 */
	public ProtobufJsonFormatMessageConverter(
			@Nullable JsonFormat.Parser parser, @Nullable JsonFormat.Printer printer) {

		this(parser, printer, null);
	}

	/**
	 * Constructor with given instances of {@link JsonFormat.Parser},
	 * {@link JsonFormat.Printer}, and {@link ExtensionRegistry}.
	 */
	public ProtobufJsonFormatMessageConverter(@Nullable JsonFormat.Parser parser,
			@Nullable JsonFormat.Printer printer, @Nullable ExtensionRegistry extensionRegistry) {

		super(new ProtobufJavaUtilSupport(parser, printer), extensionRegistry);
	}

}
