/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.xml;

import java.util.Collection;

/**
 * Bean that exposes a simple property that can be set
 * to a mix of references and individual values.
 *
 * @author Rod Johnson
 * @since 27.05.2003
 */
public class MixedCollectionBean {

	private Collection<?> jumble;


	public void setJumble(Collection<?> jumble) {
		this.jumble = jumble;
	}

	public Collection<?> getJumble() {
		return jumble;
	}

}
