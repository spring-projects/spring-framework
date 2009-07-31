/*
 * Copyright 2004-2009 the original author or authors.
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
package org.springframework.web.context.request;

import java.util.Iterator;

import org.springframework.util.Assert;
import org.springframework.util.CompositeIterator;
import org.springframework.util.StringKeyedMapAdapter;
import org.springframework.web.multipart.MultipartRequest;

/**
 * Map backed by a Web request parameter map for accessing request parameters.
 * Also provides support for multi-part requests, providing transparent access to the request "fileMap" as a request parameter entry.
 * @author Keith Donald
 * @since 3.0
 */ 
public class NativeWebRequestParameterMap extends StringKeyedMapAdapter<Object> {

	/**
	 * The wrapped native request.
	 */
	private NativeWebRequest request;

	/**
	 * Create a new map wrapping the parameters of given request.
	 */
	public NativeWebRequestParameterMap(NativeWebRequest request) {
		Assert.notNull(request, "The NativeWebRequest is required");
		this.request = request;
	}

	protected Object getAttribute(String key) {
		if (request instanceof MultipartRequest) {
			MultipartRequest multipartRequest = (MultipartRequest) request;
			Object data = multipartRequest.getFileMap().get(key);
			if (data != null) {
				return data;
			}
		}
		String[] parameters = request.getParameterValues(key);
		if (parameters == null) {
			return null;
		} else if (parameters.length == 1) {
			return parameters[0];
		} else {
			return parameters;
		}
	}

	protected void setAttribute(String key, Object value) {
		throw new UnsupportedOperationException("WebRequest parameter maps are immutable");
	}

	protected void removeAttribute(String key) {
		throw new UnsupportedOperationException("WebRequest parameter maps are immutable");
	}

	protected Iterator<String> getAttributeNames() {
		if (request instanceof MultipartRequest) {
			MultipartRequest multipartRequest = (MultipartRequest) request;
			CompositeIterator<String> iterator = new CompositeIterator<String>();
			iterator.add(multipartRequest.getFileMap().keySet().iterator());
			iterator.add(request.getParameterNames());
			return iterator;
		} else {
			return request.getParameterNames();			
		}
	}
}