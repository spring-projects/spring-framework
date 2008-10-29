/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.web.servlet.handler.metadata;

import java.util.HashMap;

import junit.framework.TestCase;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.HandlerExecutionChain;

/**
 * @author Rod Johnson
 */
public class PathMapHandlerMappingTests extends TestCase {
	
	public void testSatisfiedConstructorDependency() throws Exception {
		String path = "/Constructor.htm";
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.registerSingleton("test", TestBean.class, new MutablePropertyValues());

		HashUrlMapHandlerMapping hm = new HashUrlMapHandlerMapping();
		hm.register(ConstructorController.class, new PathMap(path));
		hm.setApplicationContext(wac);

		ConstructorController cc = (ConstructorController) wac.getBean(ConstructorController.class.getName());
		assertSame(wac.getBean("test"), cc.testBean);
		HandlerExecutionChain chain = hm.getHandler(new MockHttpServletRequest("GET", path));
		assertNotNull(chain);
		assertEquals("Path is mapped correctly based on attribute", cc, chain.getHandler());
		chain = hm.getHandler(new MockHttpServletRequest("GET", "completeRubbish.html"));
		assertNull("Don't know anything about this path", chain);
	}

	public void testUnsatisfiedConstructorDependency() throws Exception {
		String path = "/Constructor.htm";
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		// No registration of a TestBean
		//wac.registerSingleton("test", TestBean.class, new MutablePropertyValues());

		HashUrlMapHandlerMapping hm = new HashUrlMapHandlerMapping();
		hm.register(ConstructorController.class, new PathMap(path));
		try {
			hm.setApplicationContext(wac);
			fail("DependencyCheck should have failed");
		}
		catch (UnsatisfiedDependencyException ex) {
			// Ok
		}
	}

	public void testSatisfiedBeanPropertyDependency() throws Exception {
		String path = "/BeanProperty.htm";
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.registerSingleton("test", TestBean.class, new MutablePropertyValues());

		HashUrlMapHandlerMapping hm = new HashUrlMapHandlerMapping();
		hm.register(BeanPropertyController.class, new PathMap(path));
		hm.setApplicationContext(wac);

		BeanPropertyController bpc = (BeanPropertyController) wac.getBean(BeanPropertyController.class.getName());
		assertSame(wac.getBean("test"), bpc.testBean);
		HandlerExecutionChain chain = hm.getHandler(new MockHttpServletRequest("GET", path));
		assertNotNull(chain);
		assertEquals("Path is mapped correctly based on attribute", bpc, chain.getHandler());
		chain = hm.getHandler(new MockHttpServletRequest("GET", "completeRubbish.html"));
		assertNull("Don't know anything about this path", chain);
	}

	public void testSatisfiedBeanPropertyDependencyWithAutowireByType() throws Exception {
		String path = "/BeanProperty.htm";
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.registerSingleton("test", TestBean.class, new MutablePropertyValues());

		HashUrlMapHandlerMapping hm = new HashUrlMapHandlerMapping();
		hm.setAutowireMode(AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE);
		hm.register(BeanPropertyController.class, new PathMap(path));
		hm.setApplicationContext(wac);

		BeanPropertyController bpc = (BeanPropertyController) wac.getBean(BeanPropertyController.class.getName());
		assertSame(wac.getBean("test"), bpc.testBean);
		HandlerExecutionChain chain = hm.getHandler(new MockHttpServletRequest("GET", path));
		assertNotNull(chain);
		assertEquals("Path is mapped correctly based on attribute", bpc, chain.getHandler());
		chain = hm.getHandler(new MockHttpServletRequest("GET", "completeRubbish.html"));
		assertNull("Don't know anything about this path", chain);
	}

	public void testUnsatisfiedBeanPropertyDependencyWithAutowireByType() throws Exception {
		String path = "/BeanProperty.htm";
		StaticWebApplicationContext wac = new StaticWebApplicationContext();

		HashUrlMapHandlerMapping hm = new HashUrlMapHandlerMapping();
		hm.setAutowireModeName("AUTOWIRE_BY_TYPE");
		hm.register(BeanPropertyController.class, new PathMap(path));
		try {
			hm.setApplicationContext(wac);
			fail("DependencyCheck should have failed");
		}
		catch (UnsatisfiedDependencyException ex) {
			// Ok
		}
	}

	public void testSatisfiedBeanPropertyDependencyWithAutowireByName() throws Exception {
		String path = "/BeanProperty.htm";
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.registerSingleton("testBean", TestBean.class, new MutablePropertyValues());

		HashUrlMapHandlerMapping hm = new HashUrlMapHandlerMapping();
		hm.setAutowireModeName("AUTOWIRE_BY_NAME");
		hm.register(BeanPropertyController.class, new PathMap(path));
		hm.setApplicationContext(wac);

		BeanPropertyController bpc = (BeanPropertyController) wac.getBean(BeanPropertyController.class.getName());
		assertSame(wac.getBean("testBean"), bpc.testBean);
		HandlerExecutionChain chain = hm.getHandler(new MockHttpServletRequest("GET", path));
		assertNotNull(chain);
		assertEquals("Path is mapped correctly based on attribute", bpc, chain.getHandler());
		chain = hm.getHandler(new MockHttpServletRequest("GET", "completeRubbish.html"));
		assertNull("Don't know anything about this path", chain);
	}

	public void testUnsatisfiedBeanPropertyDependencyWithAutowireByName() throws Exception {
		String path = "/BeanProperty.htm";
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.registerSingleton("test", TestBean.class, new MutablePropertyValues());

		HashUrlMapHandlerMapping hm = new HashUrlMapHandlerMapping();
		hm.setAutowireMode(AutowireCapableBeanFactory.AUTOWIRE_BY_NAME);
		hm.register(BeanPropertyController.class, new PathMap(path));
		try {
			hm.setApplicationContext(wac);
			fail("DependencyCheck should have failed");
		}
		catch (UnsatisfiedDependencyException ex) {
			// Ok
		}
	}

	public void testUnsatisfiedBeanPropertyDependencyWithNoDependencyCheck() throws Exception {
		String path = "/BeanProperty.htm";
		StaticWebApplicationContext wac = new StaticWebApplicationContext();

		HashUrlMapHandlerMapping hm = new HashUrlMapHandlerMapping();
		hm.setAutowireModeName("AUTOWIRE_BY_NAME");
		hm.setDependencyCheck(false);
		hm.register(BeanPropertyController.class, new PathMap(path));
		hm.setApplicationContext(wac);

		BeanPropertyController bpc = (BeanPropertyController) wac.getBean(BeanPropertyController.class.getName());
		assertNull("Not autowired but no dependency check", bpc.testBean);
		HandlerExecutionChain chain = hm.getHandler(new MockHttpServletRequest("GET", path));
		assertNotNull(chain);
		assertEquals("Path is mapped correctly based on attribute", bpc, chain.getHandler());
		chain = hm.getHandler(new MockHttpServletRequest("GET", "completeRubbish.html"));
		assertNull("Don't know anything about this path", chain);
	}

	public void testMultiplePaths() throws Exception {
		String path1 = "/Constructor.htm";
		String path2 = "path2.cgi";
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.registerSingleton("test", TestBean.class, new MutablePropertyValues());

		HashUrlMapHandlerMapping hm = new HashUrlMapHandlerMapping();
		hm.register(ConstructorController.class, new PathMap[] { new PathMap(path1), new PathMap(path2) });
		hm.setApplicationContext(wac);
		ConstructorController cc = (ConstructorController) wac.getBean(ConstructorController.class.getName());
		assertSame(wac.getBean("test"), cc.testBean);
		HandlerExecutionChain chain = hm.getHandler(new MockHttpServletRequest("GET", path1));
		assertNotNull(chain);
		assertEquals("Path is mapped correctly based on attribute 1", cc, chain.getHandler());
		chain = hm.getHandler(new MockHttpServletRequest(null, "GET", "/" + path2));
		assertEquals("Path is mapped correctly based on attribute 2", cc, chain.getHandler());
		chain = hm.getHandler(new MockHttpServletRequest(null, "GET", "completeRubbish.html"));
		assertNull("Don't know anything about this path", chain);
	}

	
	private static class HashUrlMapHandlerMapping extends AbstractPathMapHandlerMapping {

		private HashMap classToPathMaps = new HashMap();

		public void register(Class clazz, PathMap pm) {
			classToPathMaps.put(clazz, new PathMap[] { pm });
		}

		public void register(Class clazz, PathMap[] pms) {
			classToPathMaps.put(clazz, pms);
		}

		protected Class[] getClassesWithPathMapAttributes() {
			return (Class[]) classToPathMaps.keySet().toArray(new Class[classToPathMaps.size()]);
		}

		protected PathMap[] getPathMapAttributes(Class handlerClass) {
			return (PathMap[]) classToPathMaps.get(handlerClass);
		}
	}

}
