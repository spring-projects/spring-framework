/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.servlet.mvc.annotation;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;

public class Spr7839Tests {

	AnnotationMethodHandlerAdapter adapter = new AnnotationMethodHandlerAdapter();

	MockHttpServletRequest request = new MockHttpServletRequest();

	MockHttpServletResponse response = new MockHttpServletResponse();

	Spr7839Controller controller = new Spr7839Controller();

	@Before
	public void setUp() {
		ConfigurableWebBindingInitializer binder = new ConfigurableWebBindingInitializer();
		GenericConversionService service = new DefaultConversionService();
		service.addConverter(new Converter<String, NestedBean>() {
			public NestedBean convert(String source) {
				return new NestedBean(source);
			}
		});
		binder.setConversionService(service);
		adapter.setWebBindingInitializer(binder);
	}

	@Test
	public void object() throws Exception {
		request.setRequestURI("/nested");
		request.addParameter("nested", "Nested");
		adapter.handle(request, response, controller);
	}

	@Test
	public void list() throws Exception {
		request.setRequestURI("/nested/list");
		request.addParameter("nested.list", "Nested1,Nested2");
		adapter.handle(request, response, controller);
	}
	
	@Test
	public void listElement() throws Exception {
		request.setRequestURI("/nested/listElement");
		request.addParameter("nested.list[0]", "Nested");
		adapter.handle(request, response, controller);
	}

	@Test
	public void listElementAutogrowObject() throws Exception {
		request.setRequestURI("/nested/listElement");
		request.addParameter("nested.list[0].foo", "Nested");
		adapter.handle(request, response, controller);
	}

	@Test
	@Ignore
	public void listElementAutogrowOutOfMemory() throws Exception {
		request.setRequestURI("/nested/listElement");
		request.addParameter("nested.list[1000000000].foo", "Nested");
		adapter.handle(request, response, controller);
	}

	@Test
	public void listOfLists() throws Exception {
		request.setRequestURI("/nested/listOfLists");
		request.addParameter("nested.listOfLists[0]", "Nested1,Nested2");
		adapter.handle(request, response, controller);
	}

	@Test
	public void listOfListsElement() throws Exception {
		request.setRequestURI("/nested/listOfListsElement");
		request.addParameter("nested.listOfLists[0][0]", "Nested");
		adapter.handle(request, response, controller);
	}

	@Test
	public void listOfListsElementAutogrowObject() throws Exception {
		request.setRequestURI("/nested/listOfListsElement");
		request.addParameter("nested.listOfLists[0][0].foo", "Nested");
		adapter.handle(request, response, controller);
	}

	@Test
	@Ignore
	public void arrayOfLists() throws Exception {
		// TODO TypeDescriptor not capable of accessing nested element type for arrays
		request.setRequestURI("/nested/arrayOfLists");
		request.addParameter("nested.arrayOfLists[0]", "Nested1,Nested2");
		adapter.handle(request, response, controller);
	}

	@Test
	public void map() throws Exception {
		request.setRequestURI("/nested/map");		
		request.addParameter("nested.map['apple'].foo", "bar");
		adapter.handle(request, response, controller);
	}

	@Test
	public void mapOfLists() throws Exception {
		request.setRequestURI("/nested/mapOfLists");		
		request.addParameter("nested.mapOfLists['apples'][0]", "1");
		adapter.handle(request, response, controller);
	}

	@Controller
	public static class Spr7839Controller {

		@RequestMapping("/nested")
		public void handler(JavaBean bean) {
			assertEquals("Nested", bean.nested.foo);
		}

		@RequestMapping("/nested/list")
		public void handlerList(JavaBean bean) {
			assertEquals("Nested2", bean.nested.list.get(1).foo);
		}

		@RequestMapping("/nested/listElement")
		public void handlerListElement(JavaBean bean) {
			assertEquals("Nested", bean.nested.list.get(0).foo);
		}

		@RequestMapping("/nested/listOfLists")
		public void handlerListOfLists(JavaBean bean) {
			assertEquals("Nested2", bean.nested.listOfLists.get(0).get(1).foo);
		}

		@RequestMapping("/nested/listOfListsElement")
		public void handlerListOfListsElement(JavaBean bean) {
			assertEquals("Nested", bean.nested.listOfLists.get(0).get(0).foo);
		}

		@RequestMapping("/nested/arrayOfLists")
		public void handlerArrayOfLists(JavaBean bean) {
			assertEquals("Nested2", bean.nested.arrayOfLists[0].get(1).foo);
		}

		@RequestMapping("/nested/map")
		public void handlerMap(JavaBean bean) {
			assertEquals("bar", bean.nested.map.get("apple").foo);
		}
		
		@RequestMapping("/nested/mapOfLists")
		public void handlerMapOfLists(JavaBean bean) {
			assertEquals(new Integer(1), bean.nested.mapOfLists.get("apples").get(0));
		}

	}
	
	public static class JavaBean {

	    private NestedBean nested;

		public NestedBean getNested() {
			return nested;
		}

		public void setNested(NestedBean nested) {
			this.nested = nested;
		}

	    
	}

	public static class NestedBean {

	    private String foo;

	    private List<NestedBean> list;

	    private List<List<NestedBean>> listOfLists;

	    private List<NestedBean>[] arrayOfLists;

	    private Map<String, NestedBean> map;

	    private Map<String, List<Integer>> mapOfLists;
	    
	    public NestedBean() {
	    	
	    }
	    
	    public NestedBean(String foo) {
	    	this.foo = foo;
	    }
	    
		public String getFoo() {
			return foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

		public List<NestedBean> getList() {
			return list;
		}

		public void setList(List<NestedBean> list) {
			this.list = list;
		}

		public Map<String, NestedBean> getMap() {
			return map;
		}

		public void setMap(Map<String, NestedBean> map) {
			this.map = map;
		}

		public List<List<NestedBean>> getListOfLists() {
			return listOfLists;
		}

		public void setListOfLists(List<List<NestedBean>> listOfLists) {
			this.listOfLists = listOfLists;
		}

		public List<NestedBean>[] getArrayOfLists() {
			return arrayOfLists;
		}

		public void setArrayOfLists(List<NestedBean>[] arrayOfLists) {
			this.arrayOfLists = arrayOfLists;
		}

		public Map<String, List<Integer>> getMapOfLists() {
			return mapOfLists;
		}

		public void setMapOfLists(Map<String, List<Integer>> mapOfLists) {
			this.mapOfLists = mapOfLists;
		}
	   
	}
	
}
