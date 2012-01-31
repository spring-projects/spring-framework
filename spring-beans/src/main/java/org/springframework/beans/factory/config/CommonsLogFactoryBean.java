/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.beans.factory.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Factory bean for
 * <a href="http://jakarta.apache.org/commons/logging.html">commons-logging</a>
 * {@link org.apache.commons.logging.Log} instances.
 *
 * <p>Useful for sharing Log instances among multiple beans instead of using
 * one Log instance per class name, e.g. for common log topics.
 *
 * @author Juergen Hoeller
 * @since 16.11.2003
 * @see org.apache.commons.logging.Log
 */
public class CommonsLogFactoryBean implements FactoryBean<Log>, InitializingBean {

	private Log log;


	/**
	 * The name of the log.
	 * <p>This property is required.
	 * @param logName the name of the log
	 */
	public void setLogName(String logName) {
		this.log = LogFactory.getLog(logName);
	}


	public void afterPropertiesSet() {
		if (this.log == null) {
			throw new IllegalArgumentException("'logName' is required");
		}
	}

	public Log getObject() {
		return this.log;
	}

	public Class<? extends Log> getObjectType() {
		return (this.log != null ? this.log.getClass() : Log.class);
	}

	public boolean isSingleton() {
		return true;
	}

}
