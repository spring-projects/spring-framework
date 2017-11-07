/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.http.codec.json;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;

import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * Decode a byte stream into Smile and convert to Object's with Jackson 2.9.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see Jackson2JsonEncoder
 */
public class Jackson2SmileDecoder extends AbstractJackson2Decoder {

	private static final MimeType SMILE_MIME_TYPE = new MediaType("application", "x-jackson-smile");


	public Jackson2SmileDecoder() {
		this(Jackson2ObjectMapperBuilder.smile().build(), SMILE_MIME_TYPE);
	}

	public Jackson2SmileDecoder(ObjectMapper mapper, MimeType... mimeTypes) {
		super(mapper, mimeTypes);
		Assert.isAssignable(SmileFactory.class, mapper.getFactory().getClass());
	}

	@Override
	public List<MimeType> getDecodableMimeTypes() {
		return Collections.singletonList(SMILE_MIME_TYPE);
	}

}
