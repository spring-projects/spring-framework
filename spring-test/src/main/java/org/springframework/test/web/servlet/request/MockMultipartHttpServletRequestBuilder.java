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

package org.springframework.test.web.servlet.request;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.Part;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Default builder for {@link MockMultipartHttpServletRequest}.
 *
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @since 3.2
 */
public class MockMultipartHttpServletRequestBuilder extends MockHttpServletRequestBuilder {

	private final List<MockMultipartFile> files = new ArrayList<>();

	private final MultiValueMap<String, Part> parts = new LinkedMultiValueMap<>();


	/**
	 * Package-private constructor. Use static factory methods in
	 * {@link MockMvcRequestBuilders}.
	 * <p>For other ways to initialize a {@code MockMultipartHttpServletRequest},
	 * see {@link #with(RequestPostProcessor)} and the
	 * {@link RequestPostProcessor} extension point.
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVariables zero or more URI variables
	 */
	MockMultipartHttpServletRequestBuilder(String urlTemplate, Object... uriVariables) {
		super(HttpMethod.POST, urlTemplate, uriVariables);
		super.contentType(MediaType.MULTIPART_FORM_DATA);
	}

	/**
	 * Package-private constructor. Use static factory methods in
	 * {@link MockMvcRequestBuilders}.
	 * <p>For other ways to initialize a {@code MockMultipartHttpServletRequest},
	 * see {@link #with(RequestPostProcessor)} and the
	 * {@link RequestPostProcessor} extension point.
	 * @param uri the URL
	 * @since 4.0.3
	 */
	MockMultipartHttpServletRequestBuilder(URI uri) {
		super(HttpMethod.POST, uri);
		super.contentType(MediaType.MULTIPART_FORM_DATA);
	}


	/**
	 * Create a new MockMultipartFile with the given content.
	 * @param name the name of the file
	 * @param content the content of the file
	 */
	public MockMultipartHttpServletRequestBuilder file(String name, byte[] content) {
		this.files.add(new MockMultipartFile(name, content));
		return this;
	}

	/**
	 * Add the given MockMultipartFile.
	 * @param file the multipart file
	 */
	public MockMultipartHttpServletRequestBuilder file(MockMultipartFile file) {
		this.files.add(file);
		return this;
	}

	/**
	 * Add {@link Part} components to the request.
	 * @param parts one or more parts to add
	 * @since 5.0
	 */
	public MockMultipartHttpServletRequestBuilder part(Part... parts) {
		Assert.notEmpty(parts, "'parts' must not be empty");
		for (Part part : parts) {
			this.parts.add(part.getName(), part);
		}
		return this;
	}

	@Override
	public Object merge(@Nullable Object parent) {
		if (parent == null) {
			return this;
		}
		if (parent instanceof MockHttpServletRequestBuilder) {
			super.merge(parent);
			if (parent instanceof MockMultipartHttpServletRequestBuilder) {
				MockMultipartHttpServletRequestBuilder parentBuilder = (MockMultipartHttpServletRequestBuilder) parent;
				this.files.addAll(parentBuilder.files);
				parentBuilder.parts.keySet().stream().forEach(name ->
						this.parts.putIfAbsent(name, parentBuilder.parts.get(name)));
			}

		}
		else {
			throw new IllegalArgumentException("Cannot merge with [" + parent.getClass().getName() + "]");
		}
		return this;
	}

	/**
	 * Create a new {@link MockMultipartHttpServletRequest} based on the
	 * supplied {@code ServletContext} and the {@code MockMultipartFiles}
	 * added to this builder.
	 */
	@Override
	protected final MockHttpServletRequest createServletRequest(ServletContext servletContext) {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest(servletContext);
		this.files.forEach(request::addFile);
		this.parts.values().stream().flatMap(Collection::stream).forEach(request::addPart);
		return request;
	}

}
