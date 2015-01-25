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
package org.springframework.test.context.annotation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ChildOf;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import javax.annotation.Resource;

import static org.junit.Assert.assertEquals;

/**
 * Test class for SPR-6343 asserting inheritance of XmlDeclaredBeanDefinition to
 * AnnotatedBean.
 * <p>
 * To additional tests line out different ways of inheriting
 * (value-)properties-definitions:<br />
 * <ol>
 * <li>classic inheritance for annotated beans and</li>
 * <li>classic java-config shared method configuration and</li>
 * <li>classic parent-bean-inheritance from xml-config and</li>
 * <li>new inheritance from xml-configured parent to annotated child bean.</li>
 * </ol>
 *
 * @author Jan Esser
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class })
@ContextConfiguration("parent-bean-test.xml")
public class ParentBeanTests {

    @Resource(name = "annotatedParent")
    private ParentBean parentBean;

    @Resource(name = "annotatedChild")
    private ChildBean childBean;

    @Resource(name = "configuredChild")
    private ChildBean configuredChild;

    @Resource(name = "declaredChild")
    private ChildBean declaredChild;

    /**
     * Annotation inheritance here.
     */
    @Test
    public void testParentClassInheritance() {
        assertEquals("test", parentBean.getAnnotatedAttribute());
        assertEquals("test", childBean.getAnnotatedAttribute());
    }

    /**
     * Common code in configuration class.
     */
    @Test
    public void testParentConfigured() {
        assertEquals("test", configuredChild.getConfiguredAttribute());
    }

    /**
     * Classic xml-parent.
     */
    @Test
    public void testParentDeclared() {
        assertEquals("test", declaredChild.getDeclaredAttribute());
    }

    @Test
    public void testChildOf() {
        assertEquals("test", childBean.getDeclaredAttribute());
    }

    @Configuration
    public static class TestConfig {

        protected ParentBean configuredParentBean(/* inout */ParentBean pb) {
            pb.setConfiguredAttribute("test");
            return pb;
        }

        @Bean(name = { "configuredParent" })
        public ParentBean createConfiguredParent() {
            return configuredParentBean(new ParentBean());
        }

        @Bean(name = "configuredChild")
        public ChildBean createConfiguredChild() {
            ChildBean cb = new ChildBean();
            configuredParentBean(cb);
            return cb;
        }
    }

    @Component("annotatedParent")
    public static class ParentBean {
        @Value("test")
        private String annotatedAttribute;

        private String configuredAttribute;

        private String declaredAttribute;

        public String getAnnotatedAttribute() {
            return annotatedAttribute;
        }

        public void setAnnotatedAttribute(String annotatedAttribute) {
            this.annotatedAttribute = annotatedAttribute;
        }

        public String getConfiguredAttribute() {
            return configuredAttribute;
        }

        public void setConfiguredAttribute(String configuredAttribute) {
            this.configuredAttribute = configuredAttribute;
        }

        public String getDeclaredAttribute() {
            return declaredAttribute;
        }

        public void setDeclaredAttribute(String declaredAttribute) {
            this.declaredAttribute = declaredAttribute;
        }
    }

    @ChildOf(parent = "declaredParent")
    @Component("annotatedChild")
    public static class ChildBean extends ParentBean {
        //
    }
}

