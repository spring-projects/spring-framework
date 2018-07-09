/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.jdbc.datasource.embedded;

/**
 * A supported embedded database type.
 *
 * @author Keith Donald
 * @author Oliver Gierke
 * @since 3.0
 */
public enum EmbeddedDatabaseType {

	/** The <a href="http://hsqldb.org">Hypersonic</a> Embedded Java SQL Database. */
	HSQL,

	/** The <a href="http://h2database.com">H2</a> Embedded Java SQL Database Engine. */
	H2,

	/** The <a href="http://db.apache.org/derby">Apache Derby</a> Embedded SQL Database. */
	DERBY

}
