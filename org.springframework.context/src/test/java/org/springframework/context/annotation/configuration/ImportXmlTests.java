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

package org.springframework.context.annotation.configuration;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportXml;

import test.beans.TestBean;

/**
 * Integration tests for {@link ImportXml} support.
 *
 * @author Chris Beams
 */
public class ImportXmlTests {
    @Test
    public void testImportXmlWorks() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ImportXmlConfig.class);
        assertTrue("did not contain java-declared bean", ctx.containsBean("javaDeclaredBean"));
        assertTrue("did not contain xml-declared bean", ctx.containsBean("xmlDeclaredBean"));
    }

    @Configuration
    @ImportXml("classpath:org/springframework/context/annotation/configuration/ImportXmlConfig-context.xml")
    static class ImportXmlConfig {
        public @Bean TestBean javaDeclaredBean() {
            return new TestBean("java.declared");
        }
    }

    // -------------------------------------------------------------------------

    @Ignore
    @Test
    public void testImportXmlWorksWithRelativePathing() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ImportsXmlWithRelativeTo.class);
        assertTrue("did not contain java-declared bean", ctx.containsBean("javaDeclaredBean"));
        assertTrue("did not contain xml-declared bean", ctx.containsBean("xmlDeclaredBean"));
    }

    @Configuration
    @ImportXml(value="beans.xml", relativeTo=ImportXmlTests.class)
    static class ImportsXmlWithRelativeTo {
        public @Bean TestBean javaDeclaredBean() {
            return new TestBean("java.declared");
        }
    }
    
    @Ignore
    @Test
    public void testImportXmlWorksWithAutowired() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(AutowiredImportXml.class);
        String name = ctx.getBean("xmlBeanName", String.class);
        assertThat(name, equalTo("xmlBean"));
    }
    
    @Configuration
    @ImportXml(value="beans.xml", relativeTo=AutowiredImportXml.class)
    static class AutowiredImportXml {
        @Autowired TestBean xmlDeclaredBean;
        
        public @Bean String xmlBeanName() {
            return xmlDeclaredBean.getName();
        }
    }

}