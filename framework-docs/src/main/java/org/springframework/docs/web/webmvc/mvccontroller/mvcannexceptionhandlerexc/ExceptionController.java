/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvccontroller.mvcannexceptionhandlerexc;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.rmi.RemoteException;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Controller
public class ExceptionController {

	// tag::narrow[]
	@ExceptionHandler({FileSystemException.class, RemoteException.class})
	public ResponseEntity<String> handleIOException(IOException ex) {
		return ResponseEntity.internalServerError().body(ex.getMessage());
	}
	// end::narrow[]


	// tag::general[]
	@ExceptionHandler({FileSystemException.class, RemoteException.class})
	public ResponseEntity<String> handleExceptions(Exception ex) {
		return ResponseEntity.internalServerError().body(ex.getMessage());
	}
	// end::general[]

}
