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

package org.springframework.ui.velocity;

import java.io.IOException;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;

/**
 * Factory bean that configures a VelocityEngine and provides it as bean
 * reference. This bean is intended for any kind of usage of Velocity in
 * application code, e.g. for generating email content. For web views,
 * VelocityConfigurer is used to set up a VelocityEngine for views.
 *
 * <p>The simplest way to use this class is to specify a "resourceLoaderPath";
 * you do not need any further configuration then. For example, in a web
 * application context:
 *
 * <pre class="code"> &lt;bean id="velocityEngine" class="org.springframework.ui.velocity.VelocityEngineFactoryBean"&gt;
 *   &lt;property name="resourceLoaderPath" value="/WEB-INF/velocity/"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * See the base class VelocityEngineFactory for configuration details.
 *
 * @author Juergen Hoeller
 * @see #setConfigLocation
 * @see #setVelocityProperties
 * @see #setResourceLoaderPath
 * @see org.springframework.web.servlet.view.velocity.VelocityConfigurer
 * @deprecated as of Spring 4.3, in favor of FreeMarker
 */
@Deprecated
public class VelocityEngineFactoryBean extends VelocityEngineFactory
		implements FactoryBean<VelocityEngine>, InitializingBean, ResourceLoaderAware {

	private VelocityEngine velocityEngine;


	@Override
	public void afterPropertiesSet() throws IOException, VelocityException {
		this.velocityEngine = createVelocityEngine();
	}


	@Override
	public VelocityEngine getObject() {
		return this.velocityEngine;
	}

	@Override
	public Class<? extends VelocityEngine> getObjectType() {
		return VelocityEngine.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
