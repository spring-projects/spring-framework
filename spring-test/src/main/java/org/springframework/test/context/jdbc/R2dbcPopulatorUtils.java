/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.test.context.jdbc;

import java.util.List;

import io.r2dbc.spi.ConnectionFactory;
import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

/**
 * R2dbcPopulatorUtils is a separate class to avoid name conflicts with existing
 * jdbc-related classes.
 *
 * <p><b>NOTE:</b> In the current architecture, MergedSqlConfig is implemented
 * as a package-private method, so it has been placed in
 * org.springframework.test.context.jdbc.
 *
 * @author jonghoon park
 * @since 7.0
 * @see SqlScriptsTestExecutionListener
 * @see MergedSqlConfig
 */
public abstract class R2dbcPopulatorUtils {

	static void execute(MergedSqlConfig mergedSqlConfig, ConnectionFactory connectionFactory, List<Resource> scriptResources) {
		ResourceDatabasePopulator populator = createResourceDatabasePopulator(mergedSqlConfig);
		populator.setScripts(scriptResources.toArray(new Resource[0]));

		Mono.from(connectionFactory.create())
				.flatMap(populator::populate)
				.block();
	}

	private static ResourceDatabasePopulator createResourceDatabasePopulator(MergedSqlConfig mergedSqlConfig) {
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
		populator.setSqlScriptEncoding(mergedSqlConfig.getEncoding());
		populator.setSeparator(mergedSqlConfig.getSeparator());
		populator.setCommentPrefixes(mergedSqlConfig.getCommentPrefixes());
		populator.setBlockCommentStartDelimiter(mergedSqlConfig.getBlockCommentStartDelimiter());
		populator.setBlockCommentEndDelimiter(mergedSqlConfig.getBlockCommentEndDelimiter());
		populator.setContinueOnError(mergedSqlConfig.getErrorMode() == SqlConfig.ErrorMode.CONTINUE_ON_ERROR);
		populator.setIgnoreFailedDrops(mergedSqlConfig.getErrorMode() == SqlConfig.ErrorMode.IGNORE_FAILED_DROPS);
		return populator;
	}
}
