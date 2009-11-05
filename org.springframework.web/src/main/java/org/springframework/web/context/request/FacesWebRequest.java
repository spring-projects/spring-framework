/*
 * Copyright 2002-2009 the original author or authors.
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

import java.security.Principal;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;

/**
 * {@link WebRequest} adapter for a JSF {@link javax.faces.context.FacesContext}.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 */
public class FacesWebRequest extends FacesRequestAttributes implements NativeWebRequest {

	private MultipartRequest multipartRequest;


	/**
	 * Create a new FacesWebRequest adapter for the given FacesContext.
	 * @param facesContext the current FacesContext
	 * @see javax.faces.context.FacesContext#getCurrentInstance()
	 */
	public FacesWebRequest(FacesContext facesContext) {
		super(facesContext);
		if (facesContext.getExternalContext().getRequest() instanceof MultipartRequest) {
			this.multipartRequest = (MultipartRequest) facesContext.getExternalContext().getRequest();
		}
	}


	public Object getNativeRequest() {
		return getExternalContext().getRequest();
	}

	public Object getNativeResponse() {
		return getExternalContext().getResponse();
	}


	public String getHeader(String headerName) {
		return getExternalContext().getRequestHeaderMap().get(headerName);
	}

	public String[] getHeaderValues(String headerName) {
		return getExternalContext().getRequestHeaderValuesMap().get(headerName);
	}

	public Iterator<String> getHeaderNames() {
		return getExternalContext().getRequestHeaderMap().keySet().iterator();
	}

	public String getParameter(String paramName) {
		return getExternalContext().getRequestParameterMap().get(paramName);
	}

	public Iterator<String> getParameterNames() {
		return getExternalContext().getRequestParameterNames();
	}
	
	public String[] getParameterValues(String paramName) {
		return getExternalContext().getRequestParameterValuesMap().get(paramName);
	}

	public Map<String, String[]> getParameterMap() {
		return getExternalContext().getRequestParameterValuesMap();
	}

	public Locale getLocale() {
		return getFacesContext().getExternalContext().getRequestLocale();
	}

	public String getContextPath() {
		return getFacesContext().getExternalContext().getRequestContextPath();
	}

	public String getRemoteUser() {
		return getFacesContext().getExternalContext().getRemoteUser();
	}

	public Principal getUserPrincipal() {
		return getFacesContext().getExternalContext().getUserPrincipal();
	}

	public boolean isUserInRole(String role) {
		return getFacesContext().getExternalContext().isUserInRole(role);
	}

	public boolean isSecure() {
		return false;
	}

	public boolean checkNotModified(long lastModifiedTimestamp) {
		return false;
	}

	public String getDescription(boolean includeClientInfo) {
		ExternalContext externalContext = getExternalContext();
		StringBuilder sb = new StringBuilder();
		sb.append("context=").append(externalContext.getRequestContextPath());
		if (includeClientInfo) {
			Object session = externalContext.getSession(false);
			if (session != null) {
				sb.append(";session=").append(getSessionId());
			}
			String user = externalContext.getRemoteUser();
			if (StringUtils.hasLength(user)) {
				sb.append(";user=").append(user);
			}
		}
		return sb.toString();
	}


	@SuppressWarnings("unchecked")
	public Iterator<String> getFileNames() {
		if (this.multipartRequest == null) {
			return (Iterator<String>) Collections.EMPTY_SET.iterator();
		}
		return this.multipartRequest.getFileNames();
	}

	public MultipartFile getFile(String name) {
		if (this.multipartRequest == null) {
			return null;
		}
		return this.multipartRequest.getFile(name);
	}

	public List<MultipartFile> getFiles(String name) {
		if (this.multipartRequest == null) {
			return null;
		}
		return this.multipartRequest.getFiles(name);
	}

	public Map<String, MultipartFile> getFileMap() {
		if (this.multipartRequest == null) {
			return Collections.emptyMap();
		}
		return this.multipartRequest.getFileMap();
	}

	public MultiValueMap<String, MultipartFile> getMultiFileMap() {
		if (this.multipartRequest == null) {
			return new LinkedMultiValueMap<String, MultipartFile>();
		}
		return this.multipartRequest.getMultiFileMap();
	}


	@Override
	public String toString() {
		return "FacesWebRequest: " + getDescription(true);
	}

}
