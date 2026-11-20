/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation2;

import org.springframework.stereotype.Repository;

/**
 * @author Juergen Hoeller
 */
@Repository("myNamedDao")
public class NamedStubDao2 {

	public String find(int id) {
		return "bar";
	}

}
