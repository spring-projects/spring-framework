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

package org.springframework.http.converter.protobuf;

import com.google.protobuf.ExtensionRegistry;
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
 * @author Sebastien Deleuze
 * @since 5.0
 * @see JsonFormat#parser()
 * @see JsonFormat#printer()
 * @see #ProtobufJsonFormatHttpMessageConverter(com.google.protobuf.util.JsonFormat.Parser, com.google.protobuf.util.JsonFormat.Printer)
 */
public class ProtobufJsonFormatHttpMessageConverter extends ProtobufHttpMessageConverter {

	/**
	 * Constructor with default instances of
	 * {@link com.google.protobuf.util.JsonFormat.Parser JsonFormat.Parser},
	 * {@link com.google.protobuf.util.JsonFormat.Printer JsonFormat.Printer},
	 * and {@link ExtensionRegistry}.
	 */
	public ProtobufJsonFormatHttpMessageConverter() {
		this(null, null);
	}

	/**
	 * Constructor with given instances of
	 * {@link com.google.protobuf.util.JsonFormat.Parser JsonFormat.Parser},
	 * {@link com.google.protobuf.util.JsonFormat.Printer JsonFormat.Printer},
	 * and a default instance of {@link ExtensionRegistry}.
	 */
	public ProtobufJsonFormatHttpMessageConverter(
			@Nullable JsonFormat.Parser parser, @Nullable JsonFormat.Printer printer) {

		this(parser, printer, (ExtensionRegistry) null);
	}

	/**
	 * Constructor with given instances of
	 * {@link com.google.protobuf.util.JsonFormat.Parser JsonFormat.Parser},
	 * {@link com.google.protobuf.util.JsonFormat.Printer JsonFormat.Printer},
	 * and {@link ExtensionRegistry}.
	 */
	public ProtobufJsonFormatHttpMessageConverter(@Nullable JsonFormat.Parser parser,
			@Nullable JsonFormat.Printer printer, @Nullable ExtensionRegistry extensionRegistry) {

		super(new ProtobufJavaUtilSupport(parser, printer), extensionRegistry);
	}

	/**
	 * Construct a new {@code ProtobufJsonFormatHttpMessageConverter} with the given
	 * {@code JsonFormat.Parser} and {@code JsonFormat.Printer} configuration, also
	 * accepting an initializer that allows the registration of message extensions.
	 * @param parser the JSON parser configuration
	 * @param printer the JSON printer configuration
	 * @param registryInitializer an initializer for message extensions
	 * @deprecated as of 5.1, in favor of
	 * {@link #ProtobufJsonFormatHttpMessageConverter(com.google.protobuf.util.JsonFormat.Parser, com.google.protobuf.util.JsonFormat.Printer, ExtensionRegistry)}
	 */
	@Deprecated
	public ProtobufJsonFormatHttpMessageConverter(@Nullable JsonFormat.Parser parser,
			@Nullable JsonFormat.Printer printer, @Nullable ExtensionRegistryInitializer registryInitializer) {

		super(new ProtobufJavaUtilSupport(parser, printer), null);
		if (registryInitializer != null) {
			registryInitializer.initializeExtensionRegistry(this.extensionRegistry);
		}
	}

}
