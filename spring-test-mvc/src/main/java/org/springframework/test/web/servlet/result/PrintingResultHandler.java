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

package org.springframework.test.web.servlet.result;

import java.util.Enumeration;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Result handler that prints {@link MvcResult} details to the "standard" output
 * stream. An instance of this class is typically accessed via
 * {@link MockMvcResultHandlers#print()}.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class PrintingResultHandler implements ResultHandler {

	private final ResultValuePrinter printer;


	/**
	 * Protected constructor.
	 * @param printer a {@link ResultValuePrinter} to do the actual writing
	 */
	protected PrintingResultHandler(ResultValuePrinter printer) {
		this.printer = printer;
	}

	/**
	 * @return the result value printer.
	 */
	protected ResultValuePrinter getPrinter() {
		return this.printer;
	}

	/**
	 * Print {@link MvcResult} details to the "standard" output stream.
	 */
	public final void handle(MvcResult result) throws Exception {

		this.printer.printHeading("MockHttpServletRequest");
		printRequest(result.getRequest());

		this.printer.printHeading("Handler");
		printHandler(result.getHandler(), result.getInterceptors());

		if (ClassUtils.hasMethod(ServletRequest.class, "startAsync")) {
			this.printer.printHeading("Async");
			printAsyncResult(result);
		}

		this.printer.printHeading("Resolved Exception");
		printResolvedException(result.getResolvedException());

		this.printer.printHeading("ModelAndView");
		printModelAndView(result.getModelAndView());

		this.printer.printHeading("FlashMap");
		printFlashMap(RequestContextUtils.getOutputFlashMap(result.getRequest()));

		this.printer.printHeading("MockHttpServletResponse");
		printResponse(result.getResponse());
	}

	/** Print the request */
	protected void printRequest(MockHttpServletRequest request) throws Exception {
		this.printer.printValue("HTTP Method", request.getMethod());
		this.printer.printValue("Request URI", request.getRequestURI());
		this.printer.printValue("Parameters", getParamsMultiValueMap(request));
		this.printer.printValue("Headers", getRequestHeaders(request));
	}

	protected final HttpHeaders getRequestHeaders(MockHttpServletRequest request) {
		HttpHeaders headers = new HttpHeaders();
		Enumeration<?> names = request.getHeaderNames();
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			Enumeration<String> values = request.getHeaders(name);
			while (values.hasMoreElements()) {
				headers.add(name, values.nextElement());
			}
		}
		return headers;
	}

	protected final MultiValueMap<String, String> getParamsMultiValueMap(MockHttpServletRequest request) {
		Map<String, String[]> params = request.getParameterMap();
		MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<String, String>();
		for (String name : params.keySet()) {
			if (params.get(name) != null) {
				for (String value : params.get(name)) {
					multiValueMap.add(name, value);
				}
			}
		}
		return multiValueMap;
	}

	protected void printAsyncResult(MvcResult result) throws Exception {
		HttpServletRequest request = result.getRequest();
		this.printer.printValue("Was async started", request.isAsyncStarted());
		this.printer.printValue("Async result", result.getAsyncResult(0));
	}

	/** Print the handler */
	protected void printHandler(Object handler, HandlerInterceptor[] interceptors) throws Exception {
		if (handler == null) {
			this.printer.printValue("Type", null);
		}
		else {
			if (handler instanceof HandlerMethod) {
				HandlerMethod handlerMethod = (HandlerMethod) handler;
				this.printer.printValue("Type", handlerMethod.getBeanType().getName());
				this.printer.printValue("Method", handlerMethod);
			}
			else {
				this.printer.printValue("Type", handler.getClass().getName());
			}
		}
	}

	/** Print exceptions resolved through a HandlerExceptionResolver */
	protected void printResolvedException(Exception resolvedException) throws Exception {
		if (resolvedException == null) {
			this.printer.printValue("Type", null);
		}
		else {
			this.printer.printValue("Type", resolvedException.getClass().getName());
		}
	}

	/** Print the ModelAndView */
	protected void printModelAndView(ModelAndView mav) throws Exception {
		this.printer.printValue("View name", (mav != null) ? mav.getViewName() : null);
		this.printer.printValue("View", (mav != null) ? mav.getView() : null);
		if (mav == null || mav.getModel().size() == 0) {
			this.printer.printValue("Model", null);
		}
		else {
			for (String name : mav.getModel().keySet()) {
				if (!name.startsWith(BindingResult.MODEL_KEY_PREFIX)) {
					Object value = mav.getModel().get(name);
					this.printer.printValue("Attribute", name);
					this.printer.printValue("value", value);
					Errors errors = (Errors) mav.getModel().get(BindingResult.MODEL_KEY_PREFIX + name);
					if (errors != null) {
						this.printer.printValue("errors", errors.getAllErrors());
					}
				}
			}
		}
	}

	/** Print "output" flash attributes */
	protected void printFlashMap(FlashMap flashMap) throws Exception {
		if (flashMap == null) {
			this.printer.printValue("Attributes", null);
		}
		else {
			for (String name : flashMap.keySet()) {
				this.printer.printValue("Attribute", name);
				this.printer.printValue("value", flashMap.get(name));
			}
		}
	}

	/** Print the response */
	protected void printResponse(MockHttpServletResponse response) throws Exception {
		this.printer.printValue("Status", response.getStatus());
		this.printer.printValue("Error message", response.getErrorMessage());
		this.printer.printValue("Headers", getResponseHeaders(response));
		this.printer.printValue("Content type", response.getContentType());
		this.printer.printValue("Body", response.getContentAsString());
		this.printer.printValue("Forwarded URL", response.getForwardedUrl());
		this.printer.printValue("Redirected URL", response.getRedirectedUrl());
		this.printer.printValue("Cookies", response.getCookies());
	}

	protected final HttpHeaders getResponseHeaders(MockHttpServletResponse response) {
		HttpHeaders headers = new HttpHeaders();
		for (String name : response.getHeaderNames()) {
			headers.put(name, response.getHeaders(name));
		}
		return headers;
	}


	/**
	 * A contract for how to actually write result information.
	 */
	protected interface ResultValuePrinter {

		void printHeading(String heading);

		void printValue(String label, Object value);
	}

}
