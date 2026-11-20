/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.support;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

public class RequestDataValueProcessorWrapper implements RequestDataValueProcessor {

	private RequestDataValueProcessor processor;

	public void setRequestDataValueProcessor(RequestDataValueProcessor processor) {
		this.processor = processor;
	}

	@Override
	public String processUrl(HttpServletRequest request, String url) {
		return (this.processor != null) ? this.processor.processUrl(request, url) : url;
	}

	@Override
	public String processFormFieldValue(HttpServletRequest request, String name, String value, String type) {
		return (this.processor != null) ? this.processor.processFormFieldValue(request, name, value, type) : value;
	}

	@Override
	public String processAction(HttpServletRequest request, String action, String httpMethod) {
		return (this.processor != null) ? this.processor.processAction(request, action, httpMethod) : action;
	}

	@Override
	public Map<String, String> getExtraHiddenFields(HttpServletRequest request) {
		return (this.processor != null) ? this.processor.getExtraHiddenFields(request) : null;
	}

}
