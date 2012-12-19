/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.web.portlet.context;

import java.util.Locale;
import java.util.Map;

import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.PortletResponse;
import javax.portlet.filter.PortletRequestWrapper;
import javax.portlet.filter.PortletResponseWrapper;

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.mock.web.portlet.MockPortletRequest;
import org.springframework.mock.web.portlet.MockRenderRequest;
import org.springframework.mock.web.portlet.MockRenderResponse;
import org.springframework.web.multipart.MultipartRequest;

/**
 * @author Juergen Hoeller
 * @since 26.07.2006
 */
public class PortletWebRequestTests {

	@Test
	public void testParameters() {
		MockPortletRequest portletRequest = new MockPortletRequest();
		portletRequest.addParameter("param1", "value1");
		portletRequest.addParameter("param2", "value2");
		portletRequest.addParameter("param2", "value2a");

		PortletWebRequest request = new PortletWebRequest(portletRequest);
		assertEquals("value1", request.getParameter("param1"));
		assertEquals(1, request.getParameterValues("param1").length);
		assertEquals("value1", request.getParameterValues("param1")[0]);
		assertEquals("value2", request.getParameter("param2"));
		assertEquals(2, request.getParameterValues("param2").length);
		assertEquals("value2", request.getParameterValues("param2")[0]);
		assertEquals("value2a", request.getParameterValues("param2")[1]);

		Map paramMap = request.getParameterMap();
		assertEquals(2, paramMap.size());
		assertEquals(1, ((String[]) paramMap.get("param1")).length);
		assertEquals("value1", ((String[]) paramMap.get("param1"))[0]);
		assertEquals(2, ((String[]) paramMap.get("param2")).length);
		assertEquals("value2", ((String[]) paramMap.get("param2"))[0]);
		assertEquals("value2a", ((String[]) paramMap.get("param2"))[1]);
	}

	@Test
	public void testLocale() {
		MockPortletRequest portletRequest = new MockPortletRequest();
		portletRequest.addPreferredLocale(Locale.UK);

		PortletWebRequest request = new PortletWebRequest(portletRequest);
		assertEquals(Locale.UK, request.getLocale());
	}

	@Test
	public void testNativeRequest() {
		MockRenderRequest portletRequest = new MockRenderRequest();
		MockRenderResponse portletResponse = new MockRenderResponse();
		PortletWebRequest request = new PortletWebRequest(portletRequest, portletResponse);
		assertSame(portletRequest, request.getNativeRequest());
		assertSame(portletRequest, request.getNativeRequest(PortletRequest.class));
		assertSame(portletRequest, request.getNativeRequest(RenderRequest.class));
		assertSame(portletRequest, request.getNativeRequest(MockRenderRequest.class));
		assertNull(request.getNativeRequest(MultipartRequest.class));
		assertSame(portletResponse, request.getNativeResponse());
		assertSame(portletResponse, request.getNativeResponse(PortletResponse.class));
		assertSame(portletResponse, request.getNativeResponse(RenderResponse.class));
		assertSame(portletResponse, request.getNativeResponse(MockRenderResponse.class));
		assertNull(request.getNativeResponse(MultipartRequest.class));
	}

	@Test
	public void testDecoratedNativeRequest() {
		MockRenderRequest portletRequest = new MockRenderRequest();
		MockRenderResponse portletResponse = new MockRenderResponse();
		PortletRequest decoratedRequest = new PortletRequestWrapper(portletRequest);
		PortletResponse decoratedResponse = new PortletResponseWrapper(portletResponse);
		PortletWebRequest request = new PortletWebRequest(decoratedRequest, decoratedResponse);
		assertSame(decoratedRequest, request.getNativeRequest());
		assertSame(decoratedRequest, request.getNativeRequest(PortletRequest.class));
		assertSame(portletRequest, request.getNativeRequest(RenderRequest.class));
		assertSame(portletRequest, request.getNativeRequest(MockRenderRequest.class));
		assertNull(request.getNativeRequest(MultipartRequest.class));
		assertSame(decoratedResponse, request.getNativeResponse());
		assertSame(decoratedResponse, request.getNativeResponse(PortletResponse.class));
		assertSame(portletResponse, request.getNativeResponse(RenderResponse.class));
		assertSame(portletResponse, request.getNativeResponse(MockRenderResponse.class));
		assertNull(request.getNativeResponse(MultipartRequest.class));
	}

}
