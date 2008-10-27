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

package org.springframework.web.multipart;

import java.util.Iterator;
import java.util.Map;

/**
 * This interface defines the multipart request access operations
 * that are exposed for actual multipart requests. It is extended
 * by {@link MultipartHttpServletRequest} and the Portlet
 * {@link org.springframework.web.portlet.multipart.MultipartActionRequest}.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 */
public interface MultipartRequest {

	/**
	 * Return an {@link java.util.Iterator} of String objects containing the
	 * parameter names of the multipart files contained in this request. These
	 * are the field names of the form (like with normal parameters), not the
	 * original file names.
	 * @return the names of the files
	 */
	Iterator getFileNames();

	/**
	 * Return the contents plus description of an uploaded file in this request,
	 * or <code>null</code> if it does not exist.
	 * @param name a String specifying the parameter name of the multipart file
	 * @return the uploaded content in the form of a {@link org.springframework.web.multipart.MultipartFile} object
	 */
	MultipartFile getFile(String name);

	/**
	 * Return a {@link java.util.Map} of the multipart files contained in this request.
	 * @return a map containing the parameter names as keys, and the
	 * {@link org.springframework.web.multipart.MultipartFile} objects as values
	 * @see MultipartFile
	 */
	Map getFileMap();

}
