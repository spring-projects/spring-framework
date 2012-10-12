/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.test.web.mock.servlet.request;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.util.ClassUtils;

/**
 * Default builder for {@link MockMultipartHttpServletRequest}.
 *
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @since 3.2
 */
public class MockMultipartHttpServletRequestBuilder extends MockHttpServletRequestBuilder {

	private final List<MockMultipartFile> files = new ArrayList<MockMultipartFile>();


	/**
	 * Package private constructor. Use static factory methods in
	 * {@link MockMvcRequestBuilders}.
	 *
	 * <p>For other ways to initialize a {@code MockMultipartHttpServletRequest},
	 * see {@link #with(RequestPostProcessor)} and the
	 * {@link RequestPostProcessor} extension point.
	 *
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param urlVariables zero or more URL variables
	 */
	MockMultipartHttpServletRequestBuilder(String urlTemplate, Object... urlVariables) {
		super(HttpMethod.POST, urlTemplate, urlVariables);
		super.contentType(MediaType.MULTIPART_FORM_DATA);
	}

	/**
	 * Create a new MockMultipartFile with the given content.
	 *
	 * @param name the name of the file
	 * @param content the content of the file
	 */
	public MockMultipartHttpServletRequestBuilder file(String name, byte[] content) {
		this.files.add(new MockMultipartFile(name, content));
		return this;
	}

	/**
	 * Add the given MockMultipartFile.
	 *
	 * @param file the multipart file
	 */
	public MockMultipartHttpServletRequestBuilder file(MockMultipartFile file) {
		this.files.add(file);
		return this;
	}

	@Override
	public Object merge(Object parent) {
		if (parent == null) {
			return this;
		}
		if (!(parent instanceof MockMultipartHttpServletRequestBuilder)) {
			throw new IllegalArgumentException("Cannot merge with [" + parent.getClass().getName() + "]");
		}

		super.merge(parent);

		MockMultipartHttpServletRequestBuilder parentBuilder = (MockMultipartHttpServletRequestBuilder) parent;
		this.files.addAll(parentBuilder.files);

		return this;
	}

	@Override
	protected final MockHttpServletRequest createServletRequest(ServletContext servletContext) {
		MockMultipartHttpServletRequest request = ClassUtils.hasMethod(ServletRequest.class, "startAsync") ?
				createServlet3Request() : new MockMultipartHttpServletRequest();

		for (MockMultipartFile file : this.files) {
			request.addFile(file);
		}
		return request;
	}

	private MockMultipartHttpServletRequest createServlet3Request() {
		try {
			String className = "org.springframework.test.web.mock.servlet.request.Servlet3MockMultipartHttpServletRequest";
			Class<?> clazz = ClassUtils.forName(className, MockMultipartHttpServletRequestBuilder.class.getClassLoader());
			Constructor<?> constructor = clazz.getDeclaredConstructor();
			return (MockMultipartHttpServletRequest) BeanUtils.instantiateClass(constructor);
		}
		catch (Throwable t) {
			throw new IllegalStateException("Failed to instantiate MockHttpServletRequest", t);
		}
	}

}
