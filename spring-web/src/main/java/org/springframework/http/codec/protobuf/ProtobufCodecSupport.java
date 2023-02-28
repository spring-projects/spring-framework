/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.http.codec.protobuf;

import java.util.Arrays;
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

	static final MimeType[] MIME_TYPES = new MimeType[]{
			new MimeType("application", "x-protobuf"),
			new MimeType("application", "octet-stream"),
			new MimeType("application", "vnd.google.protobuf")
	};

	static final String DELIMITED_KEY = "delimited";

	static final String DELIMITED_VALUE = "true";


	protected boolean supportsMimeType(@Nullable MimeType mimeType) {
		if (mimeType == null) {
			return true;
		}
		for (MimeType m : MIME_TYPES) {
			if (m.isCompatibleWith(mimeType)) {
				return true;
			}
		}
		return false;
	}

	protected List<MimeType> getMimeTypes() {
		return Arrays.asList(MIME_TYPES);
	}

}
