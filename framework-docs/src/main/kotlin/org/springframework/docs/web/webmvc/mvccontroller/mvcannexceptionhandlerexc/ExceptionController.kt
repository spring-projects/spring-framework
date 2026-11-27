/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvccontroller.mvcannexceptionhandlerexc

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ExceptionHandler
import java.io.IOException
import java.rmi.RemoteException

@Controller
class ExceptionController {

	// tag::narrow[]
	@ExceptionHandler(FileSystemException::class, RemoteException::class)
	fun handleIOException(ex: IOException): ResponseEntity<String?> {
		return ResponseEntity.internalServerError().body(ex.message)
	}
	// end::narrow[]


	// tag::general[]
	@ExceptionHandler(FileSystemException::class, RemoteException::class)
	fun handleExceptions(ex: Exception): ResponseEntity<String?> {
		return ResponseEntity.internalServerError().body(ex.message)
	}
	// end::general[]
}