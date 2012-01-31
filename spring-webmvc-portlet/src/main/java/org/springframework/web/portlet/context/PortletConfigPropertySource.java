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

package org.springframework.web.portlet.context;

import javax.portlet.PortletConfig;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.CollectionUtils;

/**
 * {@link PropertySource} that reads init parameters from a {@link PortletConfig} object.
 *
 * @author Chris Beams
 * @since 3.1
 * @see PortletContextPropertySource
 */
public class PortletConfigPropertySource extends EnumerablePropertySource<PortletConfig> {

	public PortletConfigPropertySource(String name, PortletConfig portletConfig) {
		super(name, portletConfig);
	}

	@Override
	public String[] getPropertyNames() {
		return CollectionUtils.toArray(this.source.getInitParameterNames(), EMPTY_NAMES_ARRAY);
	}

	@Override
	public String getProperty(String name) {
		return this.source.getInitParameter(name);
	}
}
