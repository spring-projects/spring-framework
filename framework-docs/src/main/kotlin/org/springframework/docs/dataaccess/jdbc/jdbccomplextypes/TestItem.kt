/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.dataaccess.jdbc.jdbccomplextypes

import java.util.Date

data class TestItem(val id: Long, val description: String, val expirationDate: Date)