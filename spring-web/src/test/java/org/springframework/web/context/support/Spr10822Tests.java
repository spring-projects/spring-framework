package org.springframework.web.context.support;

/*
 * Copyright 2002-2015 the original author or authors.
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

import org.junit.Test;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;

import static org.junit.Assert.*;

/**
 * Tests whether the WebApplicationContext's Environment contains property values
 * such as contextPath and realPath.
 *
 * @author Xiaolong Zuo
 * @since 4.2
 * @see ServletContextPropertySource
 */
public class Spr10822Tests {

    @Test
    public void testContextPathAndRealPath() {
        StaticWebApplicationContext wac = new StaticWebApplicationContext();
        ServletContext sc = new MockServletContext();
        String testContextPath = "/testContextPath";
        ((MockServletContext) sc).setContextPath(testContextPath);
        wac.setServletContext(sc);
        wac.refresh();
        sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
        assertEquals(sc.getContextPath(), wac.getEnvironment().resolvePlaceholders("${contextPath}"));
        assertEquals(sc.getRealPath(""), wac.getEnvironment().resolvePlaceholders("${realPath}"));
    }

}
