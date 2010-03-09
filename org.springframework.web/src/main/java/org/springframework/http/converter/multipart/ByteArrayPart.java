/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.http.converter.multipart;

import java.io.IOException;
import java.io.OutputStream;

import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

/**
 * @author Arjen Poutsma
 * @since 3.0.2
 */
class ByteArrayPart extends AbstractPart {

	private final byte[] value;

	public ByteArrayPart(byte[] value, MediaType contentType) {
		super(contentType);
		Assert.isTrue(value != null && value.length != 0, "'value' must not be null");
		this.value = value;
	}

	@Override
	protected void writeData(OutputStream os) throws IOException {
		FileCopyUtils.copy(value, os);
	}
}
