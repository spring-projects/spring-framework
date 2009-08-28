/*
 * Copyright 2002-2008 the original author or authors.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.MultiValueMap;

/**
 * Default implementation of the
 * {@link org.springframework.web.multipart.MultipartHttpServletRequest}
 * interface. Provides management of pre-generated parameter values.
 *
 * @author Trevor D. Cook
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @since 29.09.2003
 * @see org.springframework.web.multipart.MultipartResolver
 */
public class DefaultMultipartHttpServletRequest extends AbstractMultipartHttpServletRequest {

	private Map<String, String[]> multipartParameters;


	/**
	 * Wrap the given HttpServletRequest in a MultipartHttpServletRequest.
	 * @param request the servlet request to wrap
	 * @param mpFiles a map of the multipart files
	 * @param mpParams a map of the parameters to expose,
	 * with Strings as keys and String arrays as values
	 */
	public DefaultMultipartHttpServletRequest(
			HttpServletRequest request, MultiValueMap<String, MultipartFile> mpFiles, Map<String, String[]> mpParams) {

		super(request);
		setMultipartFiles(mpFiles);
		setMultipartParameters(mpParams);
	}

	/**
	 * Wrap the given HttpServletRequest in a MultipartHttpServletRequest.
	 * @param request the servlet request to wrap
	 */
	public DefaultMultipartHttpServletRequest(HttpServletRequest request) {
		super(request);
	}


	@Override
	public Enumeration<String> getParameterNames() {
		Set<String> paramNames = new HashSet<String>();
		Enumeration paramEnum = super.getParameterNames();
		while (paramEnum.hasMoreElements()) {
			paramNames.add((String) paramEnum.nextElement());
		}
		paramNames.addAll(getMultipartParameters().keySet());
		return Collections.enumeration(paramNames);
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
	@SuppressWarnings("unchecked")
	public Map<String, String[]> getParameterMap() {
		Map<String, String[]> paramMap = new HashMap<String, String[]>();
		paramMap.putAll(super.getParameterMap());
		paramMap.putAll(getMultipartParameters());
		return paramMap;
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

}
