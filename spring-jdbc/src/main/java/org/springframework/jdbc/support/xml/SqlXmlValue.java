/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.jdbc.support.xml;

import org.springframework.jdbc.support.SqlValue;

/**
 * Subinterface of {@link org.springframework.jdbc.support.SqlValue}
 * that specifically indicates passing in XML data to a specified column.
 *
 * @author Thomas Risberg
 * @since 2.5.5
 * @see org.springframework.jdbc.support.SqlValue
 * @deprecated as of 6.2, in favor of a direct {@link SqlValue} implementation
 */
@Deprecated(since = "6.2")
public interface SqlXmlValue extends SqlValue {

}
