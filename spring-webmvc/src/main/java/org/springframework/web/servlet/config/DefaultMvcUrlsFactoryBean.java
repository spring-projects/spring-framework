/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.config;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.UriComponentsContributor;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.support.DefaultMvcUrls;
import org.springframework.web.servlet.mvc.support.MvcUrls;


/**
 * A factory bean for creating an instance of {@link MvcUrls} that discovers
 * {@link UriComponentsContributor}s by obtaining the
 * {@link HandlerMethodArgumentResolver}s configured in a
 * {@link RequestMappingHandlerAdapter}.
 * <p>
 * This is mainly provided as a convenience in XML configuration. Otherwise call the
 * constructors of {@link DefaultMvcUrls} directly. Also note the MVC Java config and
 * XML namespace already create an instance of {@link MvcUrls} and when using either
 * of them this {@code FactoryBean} is not needed.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class DefaultMvcUrlsFactoryBean implements InitializingBean, FactoryBean<MvcUrls> {

	private RequestMappingHandlerAdapter handlerAdapter;

	private ConversionService conversionService;

	private MvcUrls mvcUrls;


	/**
	 * Provide a {@link RequestMappingHandlerAdapter} from which to obtain
	 * the list of configured {@link HandlerMethodArgumentResolver}s. This
	 * is provided for ease of configuration in XML.
	 */
	public void setHandlerAdapter(RequestMappingHandlerAdapter handlerAdapter) {
		this.handlerAdapter = handlerAdapter;
	}

	/**
	 * Configure the {@link ConversionService} instance that {@link MvcUrls} should
	 * use to format Object values being added to a URI.
	 */
	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.mvcUrls = new DefaultMvcUrls(this.handlerAdapter.getArgumentResolvers(), this.conversionService);
	}

	@Override
	public MvcUrls getObject() throws Exception {
		return this.mvcUrls;
	}

	@Override
	public Class<?> getObjectType() {
		return MvcUrls.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
