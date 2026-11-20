/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation5;

import example.scannable.FooDao;

/**
 * @author Juergen Hoeller
 */
@MyRepository
public class OtherFooDao implements FooDao {

	@Override
	public String findFoo(int id) {
		return "other";
	}

}
