/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.multipart;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;

/**
 * Provides additional methods for dealing with multipart content within a
 * servlet request, allowing to access uploaded files.
 * Implementations also need to override the standard
 * {@link javax.servlet.ServletRequest} methods for parameter access, making
 * multipart parameters available.
 *
 * <p>A concrete implementation is
 * {@link org.springframework.web.multipart.support.DefaultMultipartHttpServletRequest}.
 * As an intermediate step,
 * {@link org.springframework.web.multipart.support.AbstractMultipartHttpServletRequest}
 * can be subclassed.
 *
 * @author Juergen Hoeller
 * @author Trevor D. Cook
 * @since 29.09.2003
 * @see MultipartResolver
 * @see MultipartFile
 * @see javax.servlet.http.HttpServletRequest#getParameter
 * @see javax.servlet.http.HttpServletRequest#getParameterNames
 * @see javax.servlet.http.HttpServletRequest#getParameterMap
 * @see org.springframework.web.multipart.support.DefaultMultipartHttpServletRequest
 * @see org.springframework.web.multipart.support.AbstractMultipartHttpServletRequest
 */
public interface MultipartHttpServletRequest extends HttpServletRequest, MultipartRequest {

	/**
	 * Return this request's method as a convenient HttpMethod instance.
	 */
	@Nullable
	HttpMethod getRequestMethod();

	/**
	 * Return this request's headers as a convenient HttpHeaders instance.
	 */
	HttpHeaders getRequestHeaders();

	/**
	 * Return the headers for the specified part of the multipart request.
	 * <p>If the underlying implementation supports access to part headers,
	 * then all headers are returned. Otherwise, e.g. for a file upload, the
	 * returned headers may expose a 'Content-Type' if available.
	 */
	@Nullable
	HttpHeaders getMultipartHeaders(String paramOrFileName);

}
