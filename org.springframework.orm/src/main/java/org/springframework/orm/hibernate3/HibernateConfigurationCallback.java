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

package org.springframework.orm.hibernate3;

import org.hibernate.cfg.Configuration;

/**
 * Callback for use in conjunction with {@link SessionFactoryBuilderSupport#doWithConfiguration}.
 *
 * @author Chris Beams
 * @since 3.1
 * @see SessionFactoryBuilderSupport#doWithConfiguration
 * @see SessionFactoryBuilder
 * @see org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBuilder
 */
public interface HibernateConfigurationCallback<C extends Configuration> {

	/**
	 * Configure the given Hibernate {@code Configuration type}. Note that methods
	 * only setter methods should be called, and methods such as
	 * {@link Configuration#buildSessionFactory()} should be avoided.
	 * @throws Exception to propagate any exception thrown by
	 * {@code Configuration} methods
	 */
	void configure(C configuration) throws Exception;

}
