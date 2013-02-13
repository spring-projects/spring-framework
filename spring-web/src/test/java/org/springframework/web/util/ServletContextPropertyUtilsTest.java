/*
 * Copyright 2002-2013 the original author or authors.
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
 */package org.springframework.web.util;

import org.junit.Test;
import org.springframework.mock.web.test.MockServletContext;

import static org.junit.Assert.assertEquals;

/**
 * Tests for the ServletContextPropertyUtil.
 *
 * @author Marten Deinum
 * @since 3.2.2
 */
public class ServletContextPropertyUtilsTest {

    @Test
    public void resolveAsServletContextInitParameter() {
        MockServletContext servletContext = new MockServletContext();
        servletContext.setInitParameter("test.prop", "bar");

        String resolved = ServletContextPropertyUtils.resolvePlaceholders("${test.prop:foo}", servletContext);
        assertEquals(resolved, "bar");

    }

    @Test
    public void fallbackToSystemProperties() {
        MockServletContext servletContext = new MockServletContext();
        System.setProperty("test.prop", "bar");
        try {
            String resolved = ServletContextPropertyUtils.resolvePlaceholders("${test.prop:foo}", servletContext);
            assertEquals(resolved, "bar");
        } finally {
            System.clearProperty("test.prop");
        }
    }


}
