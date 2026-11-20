/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation3;

import example.scannable.FooDao;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

/**
 * @author Mark Fisher
 */
@Repository
@Qualifier("testing")
public class StubFooDao implements FooDao {

	@Override
	public String findFoo(int id) {
		return "bar";
	}

}
