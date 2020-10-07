/* Copyright 2002-2017 the original author or authors.
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

package org.springframework.jdbc.datasource.init;

/**
 * SPI for ignore some sql statements on DataSourceInitializer.
 *
 * @author qxo
 * @since 5.3
 */
public interface InitSqlFilter {

	/**
	 * Check whether the sql should ignore before execute.
	 *
	 * @param sql
	 * @return true if only the sql should ignored.
	 */
	boolean shouldIgnore(String sql);

	/**
	 * Check whether the sql should ignore when execute failed.
	 *
	 * @param sql
	 * @return true if only the sql should ignored.
	 */
	boolean shouldIgnoreOnFailed(String sql);
}
