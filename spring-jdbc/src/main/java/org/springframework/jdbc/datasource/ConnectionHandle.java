/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.jdbc.datasource;

import java.sql.Connection;

/**
 * Simple interface to be implemented by handles for a JDBC Connection.
 * Used by JpaDialect, for example.
 * <p>连接句柄(ConnectionHandle)
 * <p>通过JDBC连接的句柄实现的简单接口。
 * <p>例如，JpaDialect使用。
 *
 * @author Juergen Hoeller
 * @see SimpleConnectionHandle
 * @see ConnectionHolder
 * @since 1.1
 */
@FunctionalInterface
public interface ConnectionHandle {

	/**
	 * Fetch the JDBC Connection that this handle refers to.
	 * <p>获取此句柄引用的JDBC连接。
	 */
	Connection getConnection();

	/**
	 * Release the JDBC Connection that this handle refers to.
	 * <p>The default implementation is empty, assuming that the lifecycle
	 * of the connection is managed externally.
	 *
	 * <p>释放此句柄引用的JDBC连接。
	 * <p>默认实现为空，假设连接的生命周期由外部管理。
	 *
	 * @param con the JDBC Connection to release
	 */
	default void releaseConnection(Connection con) {
	}

}
