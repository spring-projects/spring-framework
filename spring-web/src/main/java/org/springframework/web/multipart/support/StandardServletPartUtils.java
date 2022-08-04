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

package org.springframework.web.multipart.support;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartException;

/**
 * Utility methods for standard Servlet {@link Part} handling.
 *
 * @author Juergen Hoeller
 * @since 5.3
 * @see HttpServletRequest#getParts()
 * @see StandardServletMultipartResolver
 */
public abstract class StandardServletPartUtils {

	/**
	 * Retrieve all parts from the given servlet request.
	 * @param request the servlet request
	 * @return the parts in a MultiValueMap
	 * @throws MultipartException in case of failures
	 */
	public static MultiValueMap<String, Part> getParts(HttpServletRequest request) throws MultipartException {
		try {
			MultiValueMap<String, Part> parts = new LinkedMultiValueMap<>();
			for (Part part : request.getParts()) {
				parts.add(part.getName(), part);
			}
			return parts;
		}
		catch (Exception ex) {
			throw new MultipartException("Failed to get request parts", ex);
		}
	}

	/**
	 * Retrieve all parts with the given name from the given servlet request.
	 * @param request the servlet request
	 * @param name the name to look for
	 * @return the parts in a MultiValueMap
	 * @throws MultipartException in case of failures
	 */
	public static List<Part> getParts(HttpServletRequest request, String name) throws MultipartException {
		try {
			List<Part> parts = new ArrayList<>(1);
			for (Part part : request.getParts()) {
				if (part.getName().equals(name)) {
					parts.add(part);
				}
			}
			return parts;
		}
		catch (Exception ex) {
			throw new MultipartException("Failed to get request parts", ex);
		}
	}

	/**
	 * Bind all parts from the given servlet request.
	 * @param request the servlet request
	 * @param mpvs the property values to bind to
	 * @param bindEmpty whether to bind empty parts as well
	 * @throws MultipartException in case of failures
	 */
	public static void bindParts(HttpServletRequest request, MutablePropertyValues mpvs, boolean bindEmpty)
			throws MultipartException {

		getParts(request).forEach((key, values) -> {
			if (values.size() == 1) {
				Part part = values.get(0);
				if (bindEmpty || part.getSize() > 0) {
					mpvs.add(key, part);
				}
			}
			else {
				mpvs.add(key, values);
			}
		});
	}

}
