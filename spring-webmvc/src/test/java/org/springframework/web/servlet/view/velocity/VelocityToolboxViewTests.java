package org.springframework.web.servlet.view.velocity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.MathTool;
import org.apache.velocity.tools.view.context.ChainedContext;
import org.apache.velocity.tools.view.tools.LinkTool;
import org.junit.Test;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.support.StaticWebApplicationContext;


/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Dave Syer
 */
public class VelocityToolboxViewTests {

	@Test
	public void testVelocityToolboxView() throws Exception {
		final String templateName = "test.vm";

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(new MockServletContext());
		final Template expectedTemplate = new Template();
		VelocityConfig vc = new VelocityConfig() {
			@Override
			public VelocityEngine getVelocityEngine() {
				return new TestVelocityEngine(templateName, expectedTemplate);
			}
		};
		wac.getDefaultListableBeanFactory().registerSingleton("velocityConfigurer", vc);

		final HttpServletRequest expectedRequest = new MockHttpServletRequest();
		final HttpServletResponse expectedResponse = new MockHttpServletResponse();

		VelocityToolboxView vv = new VelocityToolboxView() {
			@Override
			protected void mergeTemplate(Template template, Context context, HttpServletResponse response) throws Exception {
				assertTrue(template == expectedTemplate);
				assertTrue(response == expectedResponse);
				assertTrue(context instanceof ChainedContext);

				assertEquals("this is foo.", context.get("foo"));
				assertTrue(context.get("map") instanceof HashMap<?,?>);
				assertTrue(context.get("date") instanceof DateTool);
				assertTrue(context.get("math") instanceof MathTool);

				assertTrue(context.get("link") instanceof LinkTool);
				LinkTool linkTool = (LinkTool) context.get("link");
				assertNotNull(linkTool.getContextURL());

				assertTrue(context.get("link2") instanceof LinkTool);
				LinkTool linkTool2 = (LinkTool) context.get("link2");
				assertNotNull(linkTool2.getContextURL());
			}
		};

		vv.setUrl(templateName);
		vv.setApplicationContext(wac);
		Map<String, Class<?>> toolAttributes = new HashMap<String, Class<?>>();
		toolAttributes.put("math", MathTool.class);
		toolAttributes.put("link2", LinkTool.class);
		vv.setToolAttributes(toolAttributes);
		vv.setToolboxConfigLocation("org/springframework/web/servlet/view/velocity/toolbox.xml");
		vv.setExposeSpringMacroHelpers(false);

		vv.render(new HashMap<String,Object>(), expectedRequest, expectedResponse);
	}

}
