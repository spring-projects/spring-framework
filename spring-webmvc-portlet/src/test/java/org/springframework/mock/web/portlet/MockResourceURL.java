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

package org.springframework.mock.web.portlet;

import java.util.Map;
import javax.portlet.ResourceURL;

/**
 * Mock implementation of the {@link javax.portlet.ResourceURL} interface.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public class MockResourceURL extends MockBaseURL implements ResourceURL {

	private String resourceID;

	private String cacheability;


	//---------------------------------------------------------------------
	// ResourceURL methods
	//---------------------------------------------------------------------

	@Override
	public void setResourceID(String resourceID) {
		this.resourceID = resourceID;
	}

	public String getResourceID() {
		return this.resourceID;
	}

	@Override
	public void setCacheability(String cacheLevel) {
		this.cacheability = cacheLevel;
	}

	@Override
	public String getCacheability() {
		return this.cacheability;
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(encodeParameter("resourceID", this.resourceID));
		if (this.cacheability != null) {
			sb.append(";").append(encodeParameter("cacheability", this.cacheability));
		}
		for (Map.Entry<String, String[]> entry : this.parameters.entrySet()) {
			sb.append(";").append(encodeParameter("param_" + entry.getKey(), entry.getValue()));
		}
		return (isSecure() ? "https:" : "http:") +
				"//localhost/mockportlet?" + sb.toString();
	}

}
