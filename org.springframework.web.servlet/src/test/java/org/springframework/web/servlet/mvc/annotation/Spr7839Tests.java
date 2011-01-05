package org.springframework.web.servlet.mvc.annotation;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;

public class Spr7839Tests {

	@Test
	@Ignore
	public void test() throws Exception {
		AnnotationMethodHandlerAdapter adapter = new AnnotationMethodHandlerAdapter();
		ConfigurableWebBindingInitializer binder = new ConfigurableWebBindingInitializer();
		GenericConversionService service = ConversionServiceFactory.createDefaultConversionService();
		binder.setConversionService(service);
		adapter.setWebBindingInitializer(binder);
		Spr7839Controller controller = new Spr7839Controller();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/nested");
		request.setPathInfo("/nested");
		request.addParameter("nested.map['apple'].foo", "bar");
		MockHttpServletResponse response = new MockHttpServletResponse();
		adapter.handle(request, response, controller);
	}
	
	@Controller
	public static class Spr7839Controller {

		@RequestMapping("/nested")
		public void handler(JavaBean bean) {
			assertEquals("bar", bean.nested.map.get("apple").foo);
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
		
	    private Map<String, NestedBean> map;

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

	    
	    
	}
	
}
