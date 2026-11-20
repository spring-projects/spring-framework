/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.env.scan2;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile(org.springframework.core.env.Constants.PROD_ENV_NAME)
@Component(org.springframework.core.env.Constants.PROD_BEAN_NAME)
class ProdBean {

}
