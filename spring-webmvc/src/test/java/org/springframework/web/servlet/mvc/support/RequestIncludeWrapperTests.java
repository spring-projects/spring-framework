package org.springframework.web.servlet.mvc.support;


import org.apache.commons.collections.map.HashedMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.web.servlet.support.RequestIncludeWrapper;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.Map;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

public class RequestIncludeWrapperTests
{
    private Map<String,String[]> customParams;
    MockHttpServletRequest request;


    @Before
    public void setup() {
        customParams = new HashedMap();

        request = new MockHttpServletRequest();
        request.addParameter("id","20");
        request.addParameter("size","40");

        customParams.put("name",new String[] {"Foo"});
        customParams.put("size",new String[] {"21","22"});


    }

    @Test
    public void customParamsNoFallback() {


        RequestIncludeWrapper wrappedRequest = new RequestIncludeWrapper(request, customParams, false);
        Map<String,String[]> parameterMap = wrappedRequest.getParameterMap();

        assertEquals(2,parameterMap.keySet().size());

        assertTrue(parameterMap.containsKey("name"));
        assertFalse(parameterMap.containsKey("id"));

        assertEquals("Foo",wrappedRequest.getParameter("name"));
        assertEquals("21",wrappedRequest.getParameter("size"));

        assertArrayEquals(new String[] {"21","22"},wrappedRequest.getParameterValues("size"));

    }

    @Test
    public void customParamsWithFallback() {
        RequestIncludeWrapper wrappedRequest = new RequestIncludeWrapper(request, customParams, true);
        Map<String,String[]> parameterMap = wrappedRequest.getParameterMap();

        assertEquals(3,parameterMap.keySet().size());

        assertTrue(parameterMap.containsKey("name"));
        assertTrue(parameterMap.containsKey("id"));

        assertEquals("Foo",wrappedRequest.getParameter("name"));
        assertEquals("21",wrappedRequest.getParameter("size"));

        assertEquals("20",wrappedRequest.getParameter("id"));

        assertArrayEquals(new String[] {"21","22"},wrappedRequest.getParameterValues("size"));
    }

    @Test
    public void noCustomParamsNoFallback() {
        RequestIncludeWrapper wrappedRequest = new RequestIncludeWrapper(request, false);
        Map<String,String[]> parameterMap = wrappedRequest.getParameterMap();

        assertEquals(0,parameterMap.keySet().size());



        assertNull(wrappedRequest.getParameter("id"));
        assertNull(wrappedRequest.getParameter("size"));
    }

    @Test
    public void noCustomParamsFallback() {
        RequestIncludeWrapper wrappedRequest = new RequestIncludeWrapper(request, true);
        Map<String,String[]> parameterMap = wrappedRequest.getParameterMap();

        assertEquals(request.getParameterMap().keySet().size(),parameterMap.keySet().size());
        Enumeration<String> paramNames = request.getParameterNames();
        while(paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String[] value = request.getParameterValues(paramName);
            assertArrayEquals(value, wrappedRequest.getParameterValues(paramName));
        }
    }
}
