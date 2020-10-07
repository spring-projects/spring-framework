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

import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

public class InitSqlFilterDefault implements InitSqlFilter {

	private boolean ignoreFailedDrops;
	
	private Pattern ignorePattern; 

	@Override
	public boolean shouldIgnore(String sql) {
		if (ignorePattern != null) {
			return ignorePattern.matcher(sql).matches();
		}
		return false;
	}

	@Override
	public boolean shouldIgnoreOnFailed(String sql) {
		if (ignoreFailedDrops) {
			return StringUtils.startsWithIgnoreCase(sql.trim(), "drop");
		}
		return false;
	}

	public void setIgnoreFailedDrops(boolean ignoreFailedDrops) {
		this.ignoreFailedDrops = ignoreFailedDrops;
	}
	
	public void setIgnorePattern(String pattern) {
		ignorePattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
	}
}
