/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.orm.hibernate5;

import org.hibernate.HibernateException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.WrongClassException;

import org.springframework.lang.Nullable;
import org.springframework.orm.ObjectRetrievalFailureException;
import org.springframework.util.ReflectionUtils;

/**
 * Hibernate-specific subclass of ObjectRetrievalFailureException.
 * Converts Hibernate's UnresolvableObjectException and WrongClassException.
 *
 * @author Juergen Hoeller
 * @since 4.2
 * @see SessionFactoryUtils#convertHibernateAccessException
 */
@SuppressWarnings("serial")
public class HibernateObjectRetrievalFailureException extends ObjectRetrievalFailureException {

	public HibernateObjectRetrievalFailureException(UnresolvableObjectException ex) {
		super(ex.getEntityName(), getIdentifier(ex), ex.getMessage(), ex);
	}

	public HibernateObjectRetrievalFailureException(WrongClassException ex) {
		super(ex.getEntityName(), getIdentifier(ex), ex.getMessage(), ex);
	}


	@Nullable
	static Object getIdentifier(HibernateException hibEx) {
		try {
			// getIdentifier declares Serializable return value on 5.x but Object on 6.x
			// -> not binary compatible, let's invoke it reflectively for the time being
			return ReflectionUtils.invokeMethod(hibEx.getClass().getMethod("getIdentifier"), hibEx);
		}
		catch (NoSuchMethodException ex) {
			return null;
		}
	}

}
