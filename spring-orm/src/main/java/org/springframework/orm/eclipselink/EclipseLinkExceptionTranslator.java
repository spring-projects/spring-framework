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

import org.eclipse.persistence.exceptions.EclipseLinkException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;

/**
 * {@link PersistenceExceptionTranslator} capable of translating {@link EclipseLinkException}
 * instances to Spring's {@link DataAccessException} hierarchy.
 *
 * @author Jan Stamer
 * @since 3.2
 * @see org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor
 */public class EclipseLinkExceptionTranslator implements PersistenceExceptionTranslator {

	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		if (ex instanceof EclipseLinkException) {
			return convertEclipseLinkAccessException((EclipseLinkException) ex);
		}
		return null;
	}

	/**
	 * Convert the given EclipseLinkException to an appropriate exception from
	 * the {@code org.springframework.dao} hierarchy.
	 * @param ex EclipseLinkException that occurred
	 * @return a corresponding DataAccessException
	 * @see SessionFactoryUtils#convertEclipseLinkAccessException
	 */
	protected DataAccessException convertEclipseLinkAccessException(EclipseLinkException ex) {
		return EclipseLinkUtils.convertEclipseLinkAccessException(ex);
	}

}
