/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.orm.jdo;

import javax.jdo.JDOHelper;
import javax.jdo.JDOOptimisticVerificationException;

import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * JDO-specific subclass of ObjectOptimisticLockingFailureException.
 * Converts JDO's JDOOptimisticVerificationException.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see PersistenceManagerFactoryUtils#convertJdoAccessException
 */
@SuppressWarnings("serial")
public class JdoOptimisticLockingFailureException extends ObjectOptimisticLockingFailureException {

	public JdoOptimisticLockingFailureException(JDOOptimisticVerificationException ex) {
		// Extract information about the failed object from the JDOException, if available.
		super((ex.getFailedObject() != null ? ex.getFailedObject().getClass() : null),
				(ex.getFailedObject() != null ? JDOHelper.getObjectId(ex.getFailedObject()) : null),
				ex.getMessage(), ex);
	}

}
