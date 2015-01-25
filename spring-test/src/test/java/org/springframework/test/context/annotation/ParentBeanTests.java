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

@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class})
@ContextConfiguration("parent-bean-test.xml")
public class ParentBeanTests {

    @Configuration
    public static class TestConfig {

        protected ParentBean configuredParentBean(/*inout*/ParentBean pb) {
            pb.setConfiguredAttribute("test");
            return pb;
        }

        @Bean(name = {"configuredParent"})
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
    }

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
}