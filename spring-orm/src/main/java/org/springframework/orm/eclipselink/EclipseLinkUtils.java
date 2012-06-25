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

/**
 * Helper class featuring methods for Eclipse Link. Also provides support for
 * exception translation.
 * 
 * @author Jan Stamer
 * @since 3.2
 */
public abstract class EclipseLinkUtils {

	/**
	 * Convert the given EclipseLinkException to an appropriate exception from
	 * the <code>org.springframework.dao</code> hierarchy.
	 * @param ex EclipseLinkException that occured
	 * @return the corresponding DataAccessException instance
	 * @see EclipseLinkExceptionTranslator#convertEclipseLinkAccessException(EclipseLinkException)
	 */
	public static DataAccessException convertEclipseLinkAccessException(EclipseLinkException ex) {
		return new EclipseLinkSystemException(ex);
	}

}
