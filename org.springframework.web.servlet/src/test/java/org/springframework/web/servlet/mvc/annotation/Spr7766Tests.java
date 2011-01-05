package org.springframework.web.servlet.mvc.annotation;

import java.awt.Color;

import org.junit.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;

public class Spr7766Tests {

	@Test
	public void test() throws Exception {
		AnnotationMethodHandlerAdapter adapter = new AnnotationMethodHandlerAdapter();
		ConfigurableWebBindingInitializer binder = new ConfigurableWebBindingInitializer();
		GenericConversionService service = ConversionServiceFactory.createDefaultConversionService();
		service.addConverter(new ColorConverter());
		binder.setConversionService(service);
		adapter.setWebBindingInitializer(binder);
		Spr7766Controller controller = new Spr7766Controller();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/colors");
		request.setPathInfo("/colors");
		request.addParameter("colors", "#ffffff,000000");
		MockHttpServletResponse response = new MockHttpServletResponse();
		adapter.handle(request, response, controller);
	}
	
	public class ColorConverter implements Converter<String, Color> {
		public Color convert(String source) { if (!source.startsWith("#")) source = "#" + source; return Color.decode(source); }
	}

}
