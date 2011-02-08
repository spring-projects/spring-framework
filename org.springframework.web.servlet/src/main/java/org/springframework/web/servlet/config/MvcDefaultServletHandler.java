/*
 * Copyright 2002-2011 the original author or authors.
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
package org.springframework.web.servlet.config;

import org.springframework.beans.factory.parsing.SimpleProblemCollector;
import org.springframework.context.config.AbstractFeatureSpecification;
import org.springframework.context.config.FeatureSpecificationExecutor;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler;

/**
 * Specifies the Spring MVC "default-servlet-handler" container feature. The
 * feature provides the following fine-grained configuration:
 *
 * <ul>
 * 	<li>{@link DefaultServletHttpRequestHandler} for serving static files by 
 * 		forwarding to the Servlet container's "default" Servlet. 
 * 	<li>{@link SimpleUrlHandlerMapping} to map the above request handler to "/**"
 * 	<li>{@link HttpRequestHandlerAdapter} to enable the DispatcherServlet to be
 *     able to invoke the above request handler.
 * </ul>
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class MvcDefaultServletHandler extends AbstractFeatureSpecification {

	private static final Class<? extends FeatureSpecificationExecutor> EXECUTOR_TYPE = MvcDefaultServletHandlerExecutor.class;

	private String defaultServletName;

	/**
	 * <p>Creates an instance of MvcDefaultServletHandler without.
	 * If this constructor is used the {@link DefaultServletHttpRequestHandler} 
	 * will try to auto-detect the container's default Servlet at startup time 
	 * using a list of known names.
	 *   
	 * <p>If the default Servlet cannot be detected because of using an 
	 * unknown container or because it has been manually configured, an 
	 * alternate constructor provided here can be used to specify the 
	 * servlet name explicitly.
	 */
	public MvcDefaultServletHandler() {
		super(EXECUTOR_TYPE);
	}

	/**
	 * The name of the default Servlet to forward to for static resource requests.  
	 * The {@link DefaultServletHttpRequestHandler} will try to auto-detect the 
	 * container's default Servlet at startup time using a list of known names.  
	 * However if the default Servlet cannot be detected because of using an unknown 
	 * container or because it has been manually configured, you can use this 
	 * constructor to set the servlet name explicitly.
	 * 
	 * @param defaultServletName the name of the default servlet
	 */
	public MvcDefaultServletHandler(String defaultServletName) {
		this();
		this.defaultServletName = defaultServletName;
	}

	String defaultServletName() {
		return this.defaultServletName;
	}

	@Override
	protected void doValidate(SimpleProblemCollector reporter) {
	}

}
