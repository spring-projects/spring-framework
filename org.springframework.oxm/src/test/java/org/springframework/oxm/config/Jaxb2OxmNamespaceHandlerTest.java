/*
 * Copyright ${YEAR} the original author or authors.
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

package org.springframework.oxm.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import junit.framework.TestCase;

public class Jaxb2OxmNamespaceHandlerTest extends TestCase {

    private ApplicationContext applicationContext;

    protected void setUp() throws Exception {
        applicationContext = new ClassPathXmlApplicationContext("jaxb2OxmNamespaceHandlerTest.xml", getClass());
    }

    public void testContextPathMarshaller() throws Exception {
        applicationContext.getBean("contextPathMarshaller", Jaxb2Marshaller.class);
    }

    public void testClassesToBeBoundMarshaller() throws Exception {
        applicationContext.getBean("classesMarshaller", Jaxb2Marshaller.class);
    }

}