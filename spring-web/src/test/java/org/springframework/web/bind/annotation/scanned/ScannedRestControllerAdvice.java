/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.bind.annotation.scanned;

import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author Sam Brannen
 * @since 6.1
 */
@RestControllerAdvice(name = "myRestControllerAdvice", value = "org.my.pkg")
class ScannedRestControllerAdvice {
}
