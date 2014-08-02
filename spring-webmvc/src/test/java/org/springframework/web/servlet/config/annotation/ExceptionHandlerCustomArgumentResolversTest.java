/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.servlet.config.annotation;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.method.annotation.ModelAttributeMethodProcessor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.handler.HandlerExceptionResolverComposite;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests whether the default {@link ExceptionHandlerExceptionResolver}
 * within the {@link WebMvcConfigurationSupport} is configured with the
 * custom argument resolvers.
 *
 * @author Jakub Narloch
 */
public class ExceptionHandlerCustomArgumentResolversTest {

    private TestWebMvcConfigurationSupport config;

    @Before
    public void setUp() {
        StaticWebApplicationContext context = new StaticWebApplicationContext();
        context.setServletContext(new MockServletContext(new FileSystemResourceLoader()));

        config = new TestWebMvcConfigurationSupport();
        config.setApplicationContext(context);
        config.setServletContext(context.getServletContext());
    }

    @Test
    public void handlerExceptionResolverCustomArgumentResolvers() {

        HandlerExceptionResolver exceptionResolver = this.config.handlerExceptionResolver();
        List<HandlerExceptionResolver> expectedResolvers =
                ((HandlerExceptionResolverComposite)exceptionResolver).getExceptionResolvers();

        assertEquals(ExceptionHandlerExceptionResolver.class, expectedResolvers.get(0).getClass());

        ExceptionHandlerExceptionResolver eher = (ExceptionHandlerExceptionResolver) expectedResolvers.get(0);
        assertNotNull("Custom arguments resolvers hasn't been registered", eher.getCustomArgumentResolvers());
        assertEquals("Custom arguments resolvers hasn't been registered", 1, eher.getCustomArgumentResolvers().size());
        assertEquals("Invalid argument has been registered", ModelAttributeMethodProcessor.class,
                eher.getCustomArgumentResolvers().get(0).getClass());
    }

    /**
     * An simple implementation of {@link WebMvcConfigurationSupport} used
     * only for test purpose that overrides {@link #addArgumentResolvers(List)}
     * and register custom argument resolver.
     */
    private class TestWebMvcConfigurationSupport extends WebMvcConfigurationSupport {

        @Override
        protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
            argumentResolvers.add(new ModelAttributeMethodProcessor(true));
        }
    }
}
