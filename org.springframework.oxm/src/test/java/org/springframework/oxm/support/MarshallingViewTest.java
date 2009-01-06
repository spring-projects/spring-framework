/*
 * Copyright 2007 the original author or authors.
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

package org.springframework.oxm.support;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.xml.transform.stream.StreamResult;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.oxm.Marshaller;

public class MarshallingViewTest extends TestCase {

    private MarshallingView view;

    private MockControl control;

    private Marshaller marshallerMock;

    protected void setUp() throws Exception {
        control = MockControl.createControl(Marshaller.class);
        marshallerMock = (Marshaller) control.getMock();
        view = new MarshallingView(marshallerMock);
    }

    public void testGetContentType() {
        Assert.assertEquals("Invalid content type", "application/xml", view.getContentType());
    }

    public void testRenderModelKey() throws Exception {
        Object toBeMarshalled = new Object();
        String modelKey = "key";
        view.setModelKey(modelKey);
        Map model = new HashMap();
        model.put(modelKey, toBeMarshalled);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        control.expectAndReturn(marshallerMock.supports(Object.class), true);
        marshallerMock.marshal(toBeMarshalled, new StreamResult(response.getOutputStream()));
        control.setMatcher(MockControl.ALWAYS_MATCHER);

        control.replay();
        view.render(model, request, response);
        Assert.assertEquals("Invalid content type", "application/xml", response.getContentType());
        Assert.assertEquals("Invalid content length", 0, response.getContentLength());
        control.verify();
    }

    public void testRenderModelKeyUnsupported() throws Exception {
        Object toBeMarshalled = new Object();
        String modelKey = "key";
        view.setModelKey(modelKey);
        Map model = new HashMap();
        model.put(modelKey, toBeMarshalled);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        control.expectAndReturn(marshallerMock.supports(Object.class), false);

        control.replay();
        try {
            view.render(model, request, response);
            fail("ServletException expected");
        }
        catch (ServletException ex) {
            // expected
        }
        control.verify();
    }

    public void testRenderNoModelKey() throws Exception {
        Object toBeMarshalled = new Object();
        String modelKey = "key";
        Map model = new HashMap();
        model.put(modelKey, toBeMarshalled);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        control.expectAndReturn(marshallerMock.supports(Object.class), true);
        marshallerMock.marshal(toBeMarshalled, new StreamResult(response.getOutputStream()));
        control.setMatcher(MockControl.ALWAYS_MATCHER);

        control.replay();
        view.render(model, request, response);
        Assert.assertEquals("Invalid content type", "application/xml", response.getContentType());
        Assert.assertEquals("Invalid content length", 0, response.getContentLength());
        control.verify();
    }

    public void testRenderUnsupportedModel() throws Exception {
        Object toBeMarshalled = new Object();
        String modelKey = "key";
        Map model = new HashMap();
        model.put(modelKey, toBeMarshalled);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        control.expectAndReturn(marshallerMock.supports(Object.class), false);

        control.replay();
        try {
            view.render(model, request, response);
            fail("ServletException expected");
        }
        catch (ServletException ex) {
            // expected
        }
        control.verify();
    }
}