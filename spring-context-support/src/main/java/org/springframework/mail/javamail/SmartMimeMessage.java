/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.mail.javamail;

import jakarta.activation.FileTypeMap;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.jspecify.annotations.Nullable;

/**
 * Special subclass of the standard JavaMail {@link MimeMessage}, carrying a
 * default encoding to be used when populating the message and a default Java
 * Activation {@link FileTypeMap} to be used for resolving attachment types.
 *
 * <p>Created by {@link JavaMailSenderImpl} in case of a specified default encoding
 * and/or default FileTypeMap. Autodetected by {@link MimeMessageHelper}, which
 * will use the carried encoding and FileTypeMap unless explicitly overridden.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see JavaMailSenderImpl#createMimeMessage()
 * @see MimeMessageHelper#getDefaultEncoding(jakarta.mail.internet.MimeMessage)
 * @see MimeMessageHelper#getDefaultFileTypeMap(jakarta.mail.internet.MimeMessage)
 */
class SmartMimeMessage extends MimeMessage {

	private final @Nullable String defaultEncoding;

	private final @Nullable FileTypeMap defaultFileTypeMap;


	/**
	 * Create a new SmartMimeMessage.
	 * @param session the JavaMail Session to create the message for
	 * @param defaultEncoding the default encoding, or {@code null} if none
	 * @param defaultFileTypeMap the default FileTypeMap, or {@code null} if none
	 */
	public SmartMimeMessage(
			Session session, @Nullable String defaultEncoding, @Nullable FileTypeMap defaultFileTypeMap) {

		super(session);
		this.defaultEncoding = defaultEncoding;
		this.defaultFileTypeMap = defaultFileTypeMap;
	}


	/**
	 * Return the default encoding of this message, or {@code null} if none.
	 */
	public final @Nullable String getDefaultEncoding() {
		return this.defaultEncoding;
	}

	/**
	 * Return the default FileTypeMap of this message, or {@code null} if none.
	 */
	public final @Nullable FileTypeMap getDefaultFileTypeMap() {
		return this.defaultFileTypeMap;
	}

}
