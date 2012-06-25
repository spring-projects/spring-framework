/*
 * Copyright 2012 the original author or authors.
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

package org.springframework.orm.eclipselink;

import junit.framework.TestCase;

import org.eclipse.persistence.exceptions.DatabaseException;
import org.hibernate.HibernateException;

/**
 * @author Jan Stamer
 * @since 25.06.2012
 */
public class EclipseLinkSytemExceptionTests extends TestCase {

	public void testWithNull() {
	   EclipseLinkSystemException exception = new EclipseLinkSystemException(null);
	   assertNull(exception.getCause());
	   assertNull(exception.getMessage());
	}

	public void testCreateWithCause() {
      DatabaseException dbExceptionWithCause = new DatabaseException("my custom exception cause") {
	   };
	   EclipseLinkSystemException elSystemException = new EclipseLinkSystemException(dbExceptionWithCause);
      assertEquals(dbExceptionWithCause, elSystemException.getCause());
      assertTrue(elSystemException.getMessage().contains("my custom exception cause"));
	}
	
	public void testCreateWithNullCause() throws HibernateException {
	   DatabaseException dbExceptionWithCause = new DatabaseException((String) null) {
	   };
	   EclipseLinkSystemException elSystemException = new EclipseLinkSystemException(dbExceptionWithCause);
	   assertEquals(dbExceptionWithCause, elSystemException.getCause());
	   assertTrue(elSystemException.getMessage().contains("null"));
	}

}
