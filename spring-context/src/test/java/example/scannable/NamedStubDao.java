/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package example.scannable;

import org.springframework.stereotype.Repository;

/**
 * @author Juergen Hoeller
 */
@Repository("myNamedDao")
public class NamedStubDao {

	public String find(int id) {
		return "bar";
	}

}
