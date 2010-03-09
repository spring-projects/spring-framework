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

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/** @author Arjen Poutsma */
class ResourcePart extends AbstractPart {

	private static final byte[] FILE_NAME = new byte[]{';', ' ', 'f', 'i', 'l', 'e', 'n', 'a', 'm', 'e', '='};

	private final Resource resource;

	public ResourcePart(Resource resource) {
		super(new MediaType("application", "octet-stream"));
		Assert.notNull(resource, "'resource' must not be null");
		Assert.isTrue(resource.exists(), "'" + resource + "' does not exist");
		this.resource = resource;
	}

	@Override
	protected void writeContentDisposition(String name, OutputStream os) throws IOException {
		super.writeContentDisposition(name, os);
		String filename = resource.getFilename();
		if (StringUtils.hasLength(filename)) {
			os.write(FILE_NAME);
			os.write('"');
			os.write(getAsciiBytes(filename));
			os.write('"');
		}
	}

	@Override
	protected void writeData(OutputStream os) throws IOException {
		FileCopyUtils.copy(resource.getInputStream(), os);
	}
}
