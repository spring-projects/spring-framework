/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.web.portlet;

import java.io.IOException;
import java.util.Map;

import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.validation.BindException;
import org.springframework.web.portlet.context.StaticPortletApplicationContext;
import org.springframework.web.portlet.handler.ParameterHandlerMapping;
import org.springframework.web.portlet.mvc.SimpleFormController;

/**
 * @author Mark Fisher
 */
public class SimplePortletApplicationContext extends StaticPortletApplicationContext {

	private String renderCommandSessionAttributeName;
	private String formSessionAttributeName;

	public void refresh() throws BeansException {
		MutablePropertyValues pvs = new MutablePropertyValues();
		registerSingleton("controller1", TestFormController.class, pvs);
		
		pvs = new MutablePropertyValues();
		pvs.add("bindOnNewForm", "true");
		registerSingleton("controller2", TestFormController.class, pvs);

		pvs = new MutablePropertyValues();
		pvs.add("requireSession", "true");
		pvs.add("sessionForm", "true");
		pvs.add("bindOnNewForm", "true");
		registerSingleton("controller3", TestFormController.class, pvs);

		pvs = new MutablePropertyValues();
		pvs.add("requireSession", "true");
		pvs.add("sessionForm", "true");
		pvs.add("bindOnNewForm", "false");
		registerSingleton("controller4", TestFormController.class, pvs);

		pvs = new MutablePropertyValues();
		Map parameterMap = new ManagedMap();		
		parameterMap.put("form", new RuntimeBeanReference("controller1"));
		parameterMap.put("form-bind", new RuntimeBeanReference("controller2"));
		parameterMap.put("form-session-bind", new RuntimeBeanReference("controller3"));
		parameterMap.put("form-session-nobind", new RuntimeBeanReference("controller4"));
		pvs.addPropertyValue(new PropertyValue("parameterMap", parameterMap));
		registerSingleton("handlerMapping", ParameterHandlerMapping.class, pvs);

		super.refresh();

		TestFormController controller1 = (TestFormController) getBean("controller1");
		this.renderCommandSessionAttributeName = controller1.getRenderCommandName();
		this.formSessionAttributeName = controller1.getFormSessionName();
	}

	public String getRenderCommandSessionAttributeName() {
		return this.renderCommandSessionAttributeName;
	}

	public String getFormSessionAttributeName() {
		return this.formSessionAttributeName;
	}


	public static class TestFormController extends SimpleFormController {

		TestFormController() {
			super();
			this.setCommandClass(TestBean.class);
			this.setCommandName("testBean");
			this.setFormView("form");
		}

		public void doSubmitAction(Object command) {
			TestBean testBean = (TestBean) command;
			testBean.setAge(testBean.getAge() + 10);
		}

		public ModelAndView showForm(RenderRequest request, RenderResponse response, BindException errors) throws Exception {
			TestBean testBean = (TestBean) errors.getModel().get(getCommandName());
			this.writeResponse(response, testBean, false);
			return null;
		}

		public ModelAndView onSubmitRender(RenderRequest request, RenderResponse response, Object command, BindException errors)
				throws IOException {
			TestBean testBean = (TestBean) command;
			this.writeResponse(response, testBean, true);
			return null;
		}

		private String getRenderCommandName() {
			return this.getRenderCommandSessionAttributeName();
		}

		private String getFormSessionName() {
			return this.getFormSessionAttributeName();
		}

		private void writeResponse(RenderResponse response, TestBean testBean, boolean finished) throws IOException {
			response.getWriter().write((finished ? "finished" : "") + (testBean.getAge() + 5));
		}
	}

}
