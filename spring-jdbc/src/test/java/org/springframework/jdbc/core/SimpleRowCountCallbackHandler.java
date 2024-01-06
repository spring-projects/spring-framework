/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.jdbc.core;

import java.sql.ResultSet;

/**
 * Simple row count callback handler for testing purposes.
 * Does not call any JDBC methods on the given ResultSet.
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
public class SimpleRowCountCallbackHandler implements RowCallbackHandler {

	private int count;


	@Override
	public void processRow(ResultSet rs) {
		count++;
	}

	public int getCount() {
		return count;
	}

}
