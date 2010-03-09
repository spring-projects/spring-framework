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

import java.io.File;
import java.nio.charset.Charset;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;

/**
 * @author Arjen Poutsma
 * @since 3.0.2
 */
public class MultipartMap extends LinkedMultiValueMap<String, Part> {

	public void addTextPart(String name, String value) {
		Assert.hasText(name, "'name' must not be empty");
		add(name, new StringPart(value));
	}

	public void addTextPart(String name, String value, Charset charset) {
		Assert.hasText(name, "'name' must not be empty");
		add(name, new StringPart(value, charset));
	}

	public void addBinaryPart(String name, Resource resource) {
		Assert.hasText(name, "'name' must not be empty");
		add(name, new ResourcePart(resource));
	}

	public void addBinaryPart(Resource resource) {
		Assert.notNull(resource, "'resource' must not be null");
		addBinaryPart(resource.getFilename(), resource);
	}

	public void addBinaryPart(String name, File file) {
		addBinaryPart(name, new FileSystemResource(file));
	}

	public void addBinaryPart(File file) {
		addBinaryPart(new FileSystemResource(file));
	}

	public void addPart(String name, byte[] value, MediaType contentType) {
		Assert.hasText(name, "'name' must not be empty");
		add(name, new ByteArrayPart(value, contentType));
	}

}
