/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.multipart.support;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

/**
 * Default implementation of the
 * {@link org.springframework.web.multipart.MultipartHttpServletRequest}
 * interface. Provides management of pre-generated parameter values.
 *
 * <p>Used by {@link org.springframework.web.multipart.commons.CommonsMultipartResolver}.
 *
 * @author Trevor D. Cook
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @since 29.09.2003
 * @see org.springframework.web.multipart.MultipartResolver
 */
public class DefaultMultipartHttpServletRequest extends AbstractMultipartHttpServletRequest {

	private static final String CONTENT_TYPE = "Content-Type";

	private Map<String, String[]> multipartParameters;

	private Map<String, String> multipartParameterContentTypes;


	/**
	 * Wrap the given HttpServletRequest in a MultipartHttpServletRequest.
	 * @param request the servlet request to wrap
	 * @param mpFiles a map of the multipart files
	 * @param mpParams a map of the parameters to expose,
	 * with Strings as keys and String arrays as values
	 */
	public DefaultMultipartHttpServletRequest(HttpServletRequest request, MultiValueMap<String, MultipartFile> mpFiles,
			Map<String, String[]> mpParams, Map<String, String> mpParamContentTypes) {

		super(request);
		setMultipartFiles(mpFiles);
		setMultipartParameters(mpParams);
		setMultipartParameterContentTypes(mpParamContentTypes);
	}

	/**
	 * Wrap the given HttpServletRequest in a MultipartHttpServletRequest.
	 * @param request the servlet request to wrap
	 */
	public DefaultMultipartHttpServletRequest(HttpServletRequest request) {
		super(request);
	}


	@Override
	public String getParameter(String name) {
		String[] values = getMultipartParameters().get(name);
		if (values != null) {
			return (values.length > 0 ? values[0] : null);
		}
		return super.getParameter(name);
	}

	@Override
	public String[] getParameterValues(String name) {
		String[] values = getMultipartParameters().get(name);
		if (values != null) {
			return values;
		}
		return super.getParameterValues(name);
	}

	@Override
	public Enumeration<String> getParameterNames() {
		Map<String, String[]> multipartParameters = getMultipartParameters();
		if (multipartParameters.isEmpty()) {
			return super.getParameterNames();
		}

		Set<String> paramNames = new LinkedHashSet<>();
		Enumeration<String> paramEnum = super.getParameterNames();
		while (paramEnum.hasMoreElements()) {
			paramNames.add(paramEnum.nextElement());
		}
		paramNames.addAll(multipartParameters.keySet());
		return Collections.enumeration(paramNames);
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		Map<String, String[]> multipartParameters = getMultipartParameters();
		if (multipartParameters.isEmpty()) {
			return super.getParameterMap();
		}

		Map<String, String[]> paramMap = new LinkedHashMap<>();
		paramMap.putAll(super.getParameterMap());
		paramMap.putAll(multipartParameters);
		return paramMap;
	}

	@Override
	public String getMultipartContentType(String paramOrFileName) {
		MultipartFile file = getFile(paramOrFileName);
		if (file != null) {
			return file.getContentType();
		}
		else {
			return getMultipartParameterContentTypes().get(paramOrFileName);
		}
	}

	@Override
	public HttpHeaders getMultipartHeaders(String paramOrFileName) {
		String contentType = getMultipartContentType(paramOrFileName);
		if (contentType != null) {
			HttpHeaders headers = new HttpHeaders();
			headers.add(CONTENT_TYPE, contentType);
			return headers;
		}
		else {
			return null;
		}
	}


	/**
	 * Set a Map with parameter names as keys and String array objects as values.
	 * To be invoked by subclasses on initialization.
	 */
	protected final void setMultipartParameters(Map<String, String[]> multipartParameters) {
		this.multipartParameters = multipartParameters;
	}

	/**
	 * Obtain the multipart parameter Map for retrieval,
	 * lazily initializing it if necessary.
	 * @see #initializeMultipart()
	 */
	protected Map<String, String[]> getMultipartParameters() {
		if (this.multipartParameters == null) {
			initializeMultipart();
		}
		return this.multipartParameters;
	}

	/**
	 * Set a Map with parameter names as keys and content type Strings as values.
	 * To be invoked by subclasses on initialization.
	 */
	protected final void setMultipartParameterContentTypes(Map<String, String> multipartParameterContentTypes) {
		this.multipartParameterContentTypes = multipartParameterContentTypes;
	}

	/**
	 * Obtain the multipart parameter content type Map for retrieval,
	 * lazily initializing it if necessary.
	 * @see #initializeMultipart()
	 */
	protected Map<String, String> getMultipartParameterContentTypes() {
		if (this.multipartParameterContentTypes == null) {
			initializeMultipart();
		}
		return this.multipartParameterContentTypes;
	}

}
