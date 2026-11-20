/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.config.interfaces;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.EmptyDatabaseConfig;
import org.springframework.test.context.jdbc.SqlConfig;

/**
 * @author Sam Brannen
 * @since 4.3
 */
@ContextConfiguration(classes = EmptyDatabaseConfig.class)
@DirtiesContext
@SqlConfig(commentPrefixes = { "`", "%%" }, blockCommentStartDelimiter = "#$", blockCommentEndDelimiter = "$#", separator = "@@")
interface SqlConfigTestInterface {
}
