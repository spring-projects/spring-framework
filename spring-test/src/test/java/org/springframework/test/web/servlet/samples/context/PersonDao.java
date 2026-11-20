/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.samples.context;

import org.springframework.test.web.Person;

public interface PersonDao {

	Person getPerson(Long id);

}
