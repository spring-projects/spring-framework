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

package org.springframework.http.codec.protobuf;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;

/**
 * Base class providing support methods for Protobuf encoding and decoding.
 *
 * @author Sebastien Deleuze
 * @since 5.1
 */
public abstract class ProtobufCodecSupport {

	static final List<MimeType> MIME_TYPES = Collections.unmodifiableList(
			Arrays.asList(
					new MimeType("application", "x-protobuf"),
					new MimeType("application", "octet-stream")));

	static final String DELIMITED_KEY = "delimited";

	static final String DELIMITED_VALUE = "true";


	protected boolean supportsMimeType(@Nullable MimeType mimeType) {
		return (mimeType == null || MIME_TYPES.stream().anyMatch(m -> m.isCompatibleWith(mimeType)));
	}

	protected List<MimeType> getMimeTypes() {
		return MIME_TYPES;
	}

}
