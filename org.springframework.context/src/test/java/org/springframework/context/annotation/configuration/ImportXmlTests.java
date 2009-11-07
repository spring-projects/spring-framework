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

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
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
    public void testImportXml() {
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

    @Ignore // TODO: SPR-6310
    @Test
    public void testImportXmlWithRelativePath() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ImportXmlWithRelativePathConfig.class);
        assertTrue("did not contain java-declared bean", ctx.containsBean("javaDeclaredBean"));
        assertTrue("did not contain xml-declared bean", ctx.containsBean("xmlDeclaredBean"));
    }

    @Configuration
    @ImportXml("ImportXmlConfig-context.xml")
    static class ImportXmlWithRelativePathConfig {
        public @Bean TestBean javaDeclaredBean() {
            return new TestBean("java.declared");
        }
    }
    
    @Ignore // TODO: SPR-6310
    @Test
    public void testImportXmlByConvention() {
    	ApplicationContext ctx = new AnnotationConfigApplicationContext(ImportXmlByConventionConfig.class);
    	assertTrue("context does not contain xml-declared bean", ctx.containsBean("xmlDeclaredBean"));
    }
    
    @Configuration
    //@ImportXml
    static class ImportXmlByConventionConfig {
    }
    
    @Test
    public void testImportXmlIsInheritedFromSuperclassDeclarations() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(FirstLevelSubConfig.class);
        assertTrue(ctx.containsBean("xmlDeclaredBean"));
    }
    
    @Test
    public void testImportXmlIsMergedFromSuperclassDeclarations() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(SecondLevelSubConfig.class);
        assertTrue("failed to pick up second-level-declared XML bean", ctx.containsBean("secondLevelXmlDeclaredBean"));
        assertTrue("failed to pick up parent-declared XML bean", ctx.containsBean("xmlDeclaredBean"));
    }
    
    @Configuration
    @ImportXml("classpath:org/springframework/context/annotation/configuration/ImportXmlConfig-context.xml")
    static class BaseConfig {
    }
    
    @Configuration
    static class FirstLevelSubConfig extends BaseConfig {
    }
    
    @Configuration
    @ImportXml("classpath:org/springframework/context/annotation/configuration/SecondLevelSubConfig-context.xml")
    static class SecondLevelSubConfig extends BaseConfig {
    }
    
    @Test
    public void testImportXmlWithNamespaceConfig() {
    	AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ImportXmlWithAopNamespaceConfig.class);
    	Object bean = ctx.getBean("proxiedXmlBean");
    	assertTrue(AopUtils.isAopProxy(bean));
    }
    
    @Configuration
    @ImportXml("classpath:org/springframework/context/annotation/configuration/ImportXmlWithAopNamespace-context.xml")
    static class ImportXmlWithAopNamespaceConfig {
    }
    
    @Aspect
    static class AnAspect {
    	@Before("execution(* test.beans.TestBean.*(..))")
    	public void advice() { }
    }
    
    @Test
    public void testImportXmlWithAutowiredConfig() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ImportXmlAutowiredConfig.class);
        String name = ctx.getBean("xmlBeanName", String.class);
        assertThat(name, equalTo("xml.declared"));
    }
    
    @Configuration
    @ImportXml(value="classpath:org/springframework/context/annotation/configuration/ImportXmlConfig-context.xml")
    static class ImportXmlAutowiredConfig {
        @Autowired TestBean xmlDeclaredBean;
        
        public @Bean String xmlBeanName() {
            return xmlDeclaredBean.getName();
        }
    }

}