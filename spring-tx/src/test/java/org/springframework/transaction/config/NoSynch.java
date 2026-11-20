/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.config;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.beans.factory.annotation.Qualifier;

/**
 * @author Juergen Hoeller
 */
@Qualifier("noSynch")
@Retention(RetentionPolicy.RUNTIME)
public @interface NoSynch {

}
