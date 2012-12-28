/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.mail.javamail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.activation.FileTypeMap;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;

import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Helper class for populating a {@link javax.mail.internet.MimeMessage}.
 *
 * <p>Mirrors the simple setters of {@link org.springframework.mail.SimpleMailMessage},
 * directly applying the values to the underlying MimeMessage. Allows for defining
 * a character encoding for the entire message, automatically applied by all methods
 * of this helper class.
 *
 * <p>Offers support for HTML text content, inline elements such as images, and typical
 * mail attachments. Also supports personal names that accompany mail addresses. Note that
 * advanced settings can still be applied directly to the underlying MimeMessage object!
 *
 * <p>Typically used in {@link MimeMessagePreparator} implementations or
 * {@link JavaMailSender} client code: simply instantiating it as a MimeMessage wrapper,
 * invoking setters on the wrapper, using the underlying MimeMessage for mail sending.
 * Also used internally by {@link JavaMailSenderImpl}.
 *
 * <p>Sample code for an HTML mail with an inline image and a PDF attachment:
 *
 * <pre class="code">
 * mailSender.send(new MimeMessagePreparator() {
 *   public void prepare(MimeMessage mimeMessage) throws MessagingException {
 *     MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, "UTF-8");
 *     message.setFrom("me@mail.com");
 *     message.setTo("you@mail.com");
 *     message.setSubject("my subject");
 *     message.setText("my text &lt;img src='cid:myLogo'&gt;", true);
 *     message.addInline("myLogo", new ClassPathResource("img/mylogo.gif"));
 *     message.addAttachment("myDocument.pdf", new ClassPathResource("doc/myDocument.pdf"));
 *   }
 * });</pre>
 *
 * Consider using {@link MimeMailMessage} (which implements the common
 * {@link org.springframework.mail.MailMessage} interface, just like
 * {@link org.springframework.mail.SimpleMailMessage}) on top of this helper,
 * in order to let message population code interact with a simple message
 * or a MIME message through a common interface.
 *
 * <p><b>Warning regarding multipart mails:</b> Simple MIME messages that
 * just contain HTML text but no inline elements or attachments will work on
 * more or less any email client that is capable of HTML rendering. However,
 * inline elements and attachments are still a major compatibility issue
 * between email clients: It's virtually impossible to get inline elements
 * and attachments working across Microsoft Outlook, Lotus Notes and Mac Mail.
 * Consider choosing a specific multipart mode for your needs: The javadoc
 * on the MULTIPART_MODE constants contains more detailed information.
 *
 * @author Juergen Hoeller
 * @since 19.01.2004
 * @see #setText(String, boolean)
 * @see #setText(String, String)
 * @see #addInline(String, org.springframework.core.io.Resource)
 * @see #addAttachment(String, org.springframework.core.io.InputStreamSource)
 * @see #MULTIPART_MODE_MIXED_RELATED
 * @see #MULTIPART_MODE_RELATED
 * @see #getMimeMessage()
 * @see JavaMailSender
 */
public class MimeMessageHelper {

	/**
	 * Constant indicating a non-multipart message.
	 */
	public static final int MULTIPART_MODE_NO = 0;

	/**
	 * Constant indicating a multipart message with a single root multipart
	 * element of type "mixed". Texts, inline elements and attachements
	 * will all get added to that root element.
	 * <p>This was Spring 1.0's default behavior. It is known to work properly
	 * on Outlook. However, other mail clients tend to misinterpret inline
	 * elements as attachments and/or show attachments inline as well.
	 */
	public static final int MULTIPART_MODE_MIXED = 1;

	/**
	 * Constant indicating a multipart message with a single root multipart
	 * element of type "related". Texts, inline elements and attachements
	 * will all get added to that root element.
	 * <p>This was the default behavior from Spring 1.1 up to 1.2 final.
	 * This is the "Microsoft multipart mode", as natively sent by Outlook.
	 * It is known to work properly on Outlook, Outlook Express, Yahoo Mail, and
	 * to a large degree also on Mac Mail (with an additional attachment listed
	 * for an inline element, despite the inline element also shown inline).
	 * Does not work properly on Lotus Notes (attachments won't be shown there).
	 */
	public static final int MULTIPART_MODE_RELATED = 2;

	/**
	 * Constant indicating a multipart message with a root multipart element
	 * "mixed" plus a nested multipart element of type "related". Texts and
	 * inline elements will get added to the nested "related" element,
	 * while attachments will get added to the "mixed" root element.
	 * <p>This is the default since Spring 1.2.1. This is arguably the most correct
	 * MIME structure, according to the MIME spec: It is known to work properly
	 * on Outlook, Outlook Express, Yahoo Mail, and Lotus Notes. Does not work
	 * properly on Mac Mail. If you target Mac Mail or experience issues with
	 * specific mails on Outlook, consider using MULTIPART_MODE_RELATED instead.
	 */
	public static final int MULTIPART_MODE_MIXED_RELATED = 3;


	private static final String MULTIPART_SUBTYPE_MIXED = "mixed";

	private static final String MULTIPART_SUBTYPE_RELATED = "related";

	private static final String MULTIPART_SUBTYPE_ALTERNATIVE = "alternative";

	private static final String CONTENT_TYPE_ALTERNATIVE = "text/alternative";

	private static final String CONTENT_TYPE_HTML = "text/html";

	private static final String CONTENT_TYPE_CHARSET_SUFFIX = ";charset=";

	private static final String HEADER_PRIORITY = "X-Priority";

	private static final String HEADER_CONTENT_ID = "Content-ID";


	private final MimeMessage mimeMessage;

	private MimeMultipart rootMimeMultipart;

	private MimeMultipart mimeMultipart;

	private final String encoding;

	private FileTypeMap fileTypeMap;

	private boolean validateAddresses = false;


	/**
	 * Create a new MimeMessageHelper for the given MimeMessage,
	 * assuming a simple text message (no multipart content,
	 * i.e. no alternative texts and no inline elements or attachments).
	 * <p>The character encoding for the message will be taken from
	 * the passed-in MimeMessage object, if carried there. Else,
	 * JavaMail's default encoding will be used.
	 * @param mimeMessage MimeMessage to work on
	 * @see #MimeMessageHelper(javax.mail.internet.MimeMessage, boolean)
	 * @see #getDefaultEncoding(javax.mail.internet.MimeMessage)
	 * @see JavaMailSenderImpl#setDefaultEncoding
	 */
	public MimeMessageHelper(MimeMessage mimeMessage) {
		this(mimeMessage, null);
	}

	/**
	 * Create a new MimeMessageHelper for the given MimeMessage,
	 * assuming a simple text message (no multipart content,
	 * i.e. no alternative texts and no inline elements or attachments).
	 * @param mimeMessage MimeMessage to work on
	 * @param encoding the character encoding to use for the message
	 * @see #MimeMessageHelper(javax.mail.internet.MimeMessage, boolean)
	 */
	public MimeMessageHelper(MimeMessage mimeMessage, String encoding) {
		this.mimeMessage = mimeMessage;
		this.encoding = (encoding != null ? encoding : getDefaultEncoding(mimeMessage));
		this.fileTypeMap = getDefaultFileTypeMap(mimeMessage);
	}

	/**
	 * Create a new MimeMessageHelper for the given MimeMessage,
	 * in multipart mode (supporting alternative texts, inline
	 * elements and attachments) if requested.
	 * <p>Consider using the MimeMessageHelper constructor that
	 * takes a multipartMode argument to choose a specific multipart
	 * mode other than MULTIPART_MODE_MIXED_RELATED.
	 * <p>The character encoding for the message will be taken from
	 * the passed-in MimeMessage object, if carried there. Else,
	 * JavaMail's default encoding will be used.
	 * @param mimeMessage MimeMessage to work on
	 * @param multipart whether to create a multipart message that
	 * supports alternative texts, inline elements and attachments
	 * (corresponds to MULTIPART_MODE_MIXED_RELATED)
	 * @throws MessagingException if multipart creation failed
	 * @see #MimeMessageHelper(javax.mail.internet.MimeMessage, int)
	 * @see #getDefaultEncoding(javax.mail.internet.MimeMessage)
	 * @see JavaMailSenderImpl#setDefaultEncoding
	 */
	public MimeMessageHelper(MimeMessage mimeMessage, boolean multipart) throws MessagingException {
		this(mimeMessage, multipart, null);
	}

	/**
	 * Create a new MimeMessageHelper for the given MimeMessage,
	 * in multipart mode (supporting alternative texts, inline
	 * elements and attachments) if requested.
	 * <p>Consider using the MimeMessageHelper constructor that
	 * takes a multipartMode argument to choose a specific multipart
	 * mode other than MULTIPART_MODE_MIXED_RELATED.
	 * @param mimeMessage MimeMessage to work on
	 * @param multipart whether to create a multipart message that
	 * supports alternative texts, inline elements and attachments
	 * (corresponds to MULTIPART_MODE_MIXED_RELATED)
	 * @param encoding the character encoding to use for the message
	 * @throws MessagingException if multipart creation failed
	 * @see #MimeMessageHelper(javax.mail.internet.MimeMessage, int, String)
	 */
	public MimeMessageHelper(MimeMessage mimeMessage, boolean multipart, String encoding)
			throws MessagingException {

		this(mimeMessage, (multipart ? MULTIPART_MODE_MIXED_RELATED : MULTIPART_MODE_NO), encoding);
	}

	/**
	 * Create a new MimeMessageHelper for the given MimeMessage,
	 * in multipart mode (supporting alternative texts, inline
	 * elements and attachments) if requested.
	 * <p>The character encoding for the message will be taken from
	 * the passed-in MimeMessage object, if carried there. Else,
	 * JavaMail's default encoding will be used.
	 * @param mimeMessage MimeMessage to work on
	 * @param multipartMode which kind of multipart message to create
	 * (MIXED, RELATED, MIXED_RELATED, or NO)
	 * @throws MessagingException if multipart creation failed
	 * @see #MULTIPART_MODE_NO
	 * @see #MULTIPART_MODE_MIXED
	 * @see #MULTIPART_MODE_RELATED
	 * @see #MULTIPART_MODE_MIXED_RELATED
	 * @see #getDefaultEncoding(javax.mail.internet.MimeMessage)
	 * @see JavaMailSenderImpl#setDefaultEncoding
	 */
	public MimeMessageHelper(MimeMessage mimeMessage, int multipartMode) throws MessagingException {
		this(mimeMessage, multipartMode, null);
	}

	/**
	 * Create a new MimeMessageHelper for the given MimeMessage,
	 * in multipart mode (supporting alternative texts, inline
	 * elements and attachments) if requested.
	 * @param mimeMessage MimeMessage to work on
	 * @param multipartMode which kind of multipart message to create
	 * (MIXED, RELATED, MIXED_RELATED, or NO)
	 * @param encoding the character encoding to use for the message
	 * @throws MessagingException if multipart creation failed
	 * @see #MULTIPART_MODE_NO
	 * @see #MULTIPART_MODE_MIXED
	 * @see #MULTIPART_MODE_RELATED
	 * @see #MULTIPART_MODE_MIXED_RELATED
	 */
	public MimeMessageHelper(MimeMessage mimeMessage, int multipartMode, String encoding)
			throws MessagingException {

		this.mimeMessage = mimeMessage;
		createMimeMultiparts(mimeMessage, multipartMode);
		this.encoding = (encoding != null ? encoding : getDefaultEncoding(mimeMessage));
		this.fileTypeMap = getDefaultFileTypeMap(mimeMessage);
	}


	/**
	 * Return the underlying MimeMessage object.
	 */
	public final MimeMessage getMimeMessage() {
		return this.mimeMessage;
	}


	/**
	 * Determine the MimeMultipart objects to use, which will be used
	 * to store attachments on the one hand and text(s) and inline elements
	 * on the other hand.
	 * <p>Texts and inline elements can either be stored in the root element
	 * itself (MULTIPART_MODE_MIXED, MULTIPART_MODE_RELATED) or in a nested element
	 * rather than the root element directly (MULTIPART_MODE_MIXED_RELATED).
	 * <p>By default, the root MimeMultipart element will be of type "mixed"
	 * (MULTIPART_MODE_MIXED) or "related" (MULTIPART_MODE_RELATED).
	 * The main multipart element will either be added as nested element of
	 * type "related" (MULTIPART_MODE_MIXED_RELATED) or be identical to the root
	 * element itself (MULTIPART_MODE_MIXED, MULTIPART_MODE_RELATED).
	 * @param mimeMessage the MimeMessage object to add the root MimeMultipart
	 * object to
	 * @param multipartMode the multipart mode, as passed into the constructor
	 * (MIXED, RELATED, MIXED_RELATED, or NO)
	 * @throws MessagingException if multipart creation failed
	 * @see #setMimeMultiparts
	 * @see #MULTIPART_MODE_NO
	 * @see #MULTIPART_MODE_MIXED
	 * @see #MULTIPART_MODE_RELATED
	 * @see #MULTIPART_MODE_MIXED_RELATED
	 */
	protected void createMimeMultiparts(MimeMessage mimeMessage, int multipartMode) throws MessagingException {
		switch (multipartMode) {
			case MULTIPART_MODE_NO:
				setMimeMultiparts(null, null);
				break;
			case MULTIPART_MODE_MIXED:
				MimeMultipart mixedMultipart = new MimeMultipart(MULTIPART_SUBTYPE_MIXED);
				mimeMessage.setContent(mixedMultipart);
				setMimeMultiparts(mixedMultipart, mixedMultipart);
				break;
			case MULTIPART_MODE_RELATED:
				MimeMultipart relatedMultipart = new MimeMultipart(MULTIPART_SUBTYPE_RELATED);
				mimeMessage.setContent(relatedMultipart);
				setMimeMultiparts(relatedMultipart, relatedMultipart);
				break;
			case MULTIPART_MODE_MIXED_RELATED:
				MimeMultipart rootMixedMultipart = new MimeMultipart(MULTIPART_SUBTYPE_MIXED);
				mimeMessage.setContent(rootMixedMultipart);
				MimeMultipart nestedRelatedMultipart = new MimeMultipart(MULTIPART_SUBTYPE_RELATED);
				MimeBodyPart relatedBodyPart = new MimeBodyPart();
				relatedBodyPart.setContent(nestedRelatedMultipart);
				rootMixedMultipart.addBodyPart(relatedBodyPart);
				setMimeMultiparts(rootMixedMultipart, nestedRelatedMultipart);
				break;
			default:
				throw new IllegalArgumentException("Only multipart modes MIXED_RELATED, RELATED and NO supported");
		}
	}

	/**
	 * Set the given MimeMultipart objects for use by this MimeMessageHelper.
	 * @param root the root MimeMultipart object, which attachments will be added to;
	 * or {@code null} to indicate no multipart at all
	 * @param main the main MimeMultipart object, which text(s) and inline elements
	 * will be added to (can be the same as the root multipart object, or an element
	 * nested underneath the root multipart element)
	 */
	protected final void setMimeMultiparts(MimeMultipart root, MimeMultipart main) {
		this.rootMimeMultipart = root;
		this.mimeMultipart = main;
	}

	/**
	 * Return whether this helper is in multipart mode,
	 * i.e. whether it holds a multipart message.
	 * @see #MimeMessageHelper(MimeMessage, boolean)
	 */
	public final boolean isMultipart() {
		return (this.rootMimeMultipart != null);
	}

	/**
	 * Throw an IllegalStateException if this helper is not in multipart mode.
	 */
	private void checkMultipart() throws IllegalStateException {
		if (!isMultipart()) {
			throw new IllegalStateException("Not in multipart mode - " +
				"create an appropriate MimeMessageHelper via a constructor that takes a 'multipart' flag " +
				"if you need to set alternative texts or add inline elements or attachments.");
		}
	}

	/**
	 * Return the root MIME "multipart/mixed" object, if any.
	 * Can be used to manually add attachments.
	 * <p>This will be the direct content of the MimeMessage,
	 * in case of a multipart mail.
	 * @throws IllegalStateException if this helper is not in multipart mode
	 * @see #isMultipart
	 * @see #getMimeMessage
	 * @see javax.mail.internet.MimeMultipart#addBodyPart
	 */
	public final MimeMultipart getRootMimeMultipart() throws IllegalStateException {
		checkMultipart();
		return this.rootMimeMultipart;
	}

	/**
	 * Return the underlying MIME "multipart/related" object, if any.
	 * Can be used to manually add body parts, inline elements, etc.
	 * <p>This will be nested within the root MimeMultipart,
	 * in case of a multipart mail.
	 * @throws IllegalStateException if this helper is not in multipart mode
	 * @see #isMultipart
	 * @see #getRootMimeMultipart
	 * @see javax.mail.internet.MimeMultipart#addBodyPart
	 */
	public final MimeMultipart getMimeMultipart() throws IllegalStateException {
		checkMultipart();
		return this.mimeMultipart;
	}


	/**
	 * Determine the default encoding for the given MimeMessage.
	 * @param mimeMessage the passed-in MimeMessage
	 * @return the default encoding associated with the MimeMessage,
	 * or {@code null} if none found
	 */
	protected String getDefaultEncoding(MimeMessage mimeMessage) {
		if (mimeMessage instanceof SmartMimeMessage) {
			return ((SmartMimeMessage) mimeMessage).getDefaultEncoding();
		}
		return null;
	}

	/**
	 * Return the specific character encoding used for this message, if any.
	 */
	public String getEncoding() {
		return this.encoding;
	}

	/**
	 * Determine the default Java Activation FileTypeMap for the given MimeMessage.
	 * @param mimeMessage the passed-in MimeMessage
	 * @return the default FileTypeMap associated with the MimeMessage,
	 * or a default ConfigurableMimeFileTypeMap if none found for the message
	 * @see ConfigurableMimeFileTypeMap
	 */
	protected FileTypeMap getDefaultFileTypeMap(MimeMessage mimeMessage) {
		if (mimeMessage instanceof SmartMimeMessage) {
			FileTypeMap fileTypeMap = ((SmartMimeMessage) mimeMessage).getDefaultFileTypeMap();
			if (fileTypeMap != null) {
				return fileTypeMap;
			}
		}
		ConfigurableMimeFileTypeMap fileTypeMap = new ConfigurableMimeFileTypeMap();
		fileTypeMap.afterPropertiesSet();
		return fileTypeMap;
	}

	/**
	 * Set the Java Activation Framework {@code FileTypeMap} to use
	 * for determining the content type of inline content and attachments
	 * that get added to the message.
	 * <p>Default is the {@code FileTypeMap} that the underlying
	 * MimeMessage carries, if any, or the Activation Framework's default
	 * {@code FileTypeMap} instance else.
	 * @see #addInline
	 * @see #addAttachment
	 * @see #getDefaultFileTypeMap(javax.mail.internet.MimeMessage)
	 * @see JavaMailSenderImpl#setDefaultFileTypeMap
	 * @see javax.activation.FileTypeMap#getDefaultFileTypeMap
	 * @see ConfigurableMimeFileTypeMap
	 */
	public void setFileTypeMap(FileTypeMap fileTypeMap) {
		this.fileTypeMap = (fileTypeMap != null ? fileTypeMap : getDefaultFileTypeMap(getMimeMessage()));
	}

	/**
	 * Return the {@code FileTypeMap} used by this MimeMessageHelper.
	 */
	public FileTypeMap getFileTypeMap() {
		return this.fileTypeMap;
	}


	/**
	 * Set whether to validate all addresses which get passed to this helper.
	 * Default is "false".
	 * <p>Note that this is by default just available for JavaMail >= 1.3.
	 * You can override the default {@code validateAddress method} for
	 * validation on older JavaMail versions (or for custom validation).
	 * @see #validateAddress
	 */
	public void setValidateAddresses(boolean validateAddresses) {
		this.validateAddresses = validateAddresses;
	}

	/**
	 * Return whether this helper will validate all addresses passed to it.
	 */
	public boolean isValidateAddresses() {
		return this.validateAddresses;
	}

	/**
	 * Validate the given mail address.
	 * Called by all of MimeMessageHelper's address setters and adders.
	 * <p>Default implementation invokes {@code InternetAddress.validate()},
	 * provided that address validation is activated for the helper instance.
	 * <p>Note that this method will just work on JavaMail >= 1.3. You can override
	 * it for validation on older JavaMail versions or for custom validation.
	 * @param address the address to validate
	 * @throws AddressException if validation failed
	 * @see #isValidateAddresses()
	 * @see javax.mail.internet.InternetAddress#validate()
	 */
	protected void validateAddress(InternetAddress address) throws AddressException {
		if (isValidateAddresses()) {
			address.validate();
		}
	}

	/**
	 * Validate all given mail addresses.
	 * Default implementation simply delegates to validateAddress for each address.
	 * @param addresses the addresses to validate
	 * @throws AddressException if validation failed
	 * @see #validateAddress(InternetAddress)
	 */
	protected void validateAddresses(InternetAddress[] addresses) throws AddressException {
		for (InternetAddress address : addresses) {
			validateAddress(address);
		}
	}


	public void setFrom(InternetAddress from) throws MessagingException {
		Assert.notNull(from, "From address must not be null");
		validateAddress(from);
		this.mimeMessage.setFrom(from);
	}

	public void setFrom(String from) throws MessagingException {
		Assert.notNull(from, "From address must not be null");
		setFrom(parseAddress(from));
	}

	public void setFrom(String from, String personal) throws MessagingException, UnsupportedEncodingException {
		Assert.notNull(from, "From address must not be null");
		setFrom(getEncoding() != null ?
			new InternetAddress(from, personal, getEncoding()) : new InternetAddress(from, personal));
	}

	public void setReplyTo(InternetAddress replyTo) throws MessagingException {
		Assert.notNull(replyTo, "Reply-to address must not be null");
		validateAddress(replyTo);
		this.mimeMessage.setReplyTo(new InternetAddress[] {replyTo});
	}

	public void setReplyTo(String replyTo) throws MessagingException {
		Assert.notNull(replyTo, "Reply-to address must not be null");
		setReplyTo(parseAddress(replyTo));
	}

	public void setReplyTo(String replyTo, String personal) throws MessagingException, UnsupportedEncodingException {
		Assert.notNull(replyTo, "Reply-to address must not be null");
		InternetAddress replyToAddress = (getEncoding() != null) ?
				new InternetAddress(replyTo, personal, getEncoding()) : new InternetAddress(replyTo, personal);
		setReplyTo(replyToAddress);
	}


	public void setTo(InternetAddress to) throws MessagingException {
		Assert.notNull(to, "To address must not be null");
		validateAddress(to);
		this.mimeMessage.setRecipient(Message.RecipientType.TO, to);
	}

	public void setTo(InternetAddress[] to) throws MessagingException {
		Assert.notNull(to, "To address array must not be null");
		validateAddresses(to);
		this.mimeMessage.setRecipients(Message.RecipientType.TO, to);
	}

	public void setTo(String to) throws MessagingException {
		Assert.notNull(to, "To address must not be null");
		setTo(parseAddress(to));
	}

	public void setTo(String[] to) throws MessagingException {
		Assert.notNull(to, "To address array must not be null");
		InternetAddress[] addresses = new InternetAddress[to.length];
		for (int i = 0; i < to.length; i++) {
			addresses[i] = parseAddress(to[i]);
		}
		setTo(addresses);
	}

	public void addTo(InternetAddress to) throws MessagingException {
		Assert.notNull(to, "To address must not be null");
		validateAddress(to);
		this.mimeMessage.addRecipient(Message.RecipientType.TO, to);
	}

	public void addTo(String to) throws MessagingException {
		Assert.notNull(to, "To address must not be null");
		addTo(parseAddress(to));
	}

	public void addTo(String to, String personal) throws MessagingException, UnsupportedEncodingException {
		Assert.notNull(to, "To address must not be null");
		addTo(getEncoding() != null ?
			new InternetAddress(to, personal, getEncoding()) :
			new InternetAddress(to, personal));
	}


	public void setCc(InternetAddress cc) throws MessagingException {
		Assert.notNull(cc, "Cc address must not be null");
		validateAddress(cc);
		this.mimeMessage.setRecipient(Message.RecipientType.CC, cc);
	}

	public void setCc(InternetAddress[] cc) throws MessagingException {
		Assert.notNull(cc, "Cc address array must not be null");
		validateAddresses(cc);
		this.mimeMessage.setRecipients(Message.RecipientType.CC, cc);
	}

	public void setCc(String cc) throws MessagingException {
		Assert.notNull(cc, "Cc address must not be null");
		setCc(parseAddress(cc));
	}

	public void setCc(String[] cc) throws MessagingException {
		Assert.notNull(cc, "Cc address array must not be null");
		InternetAddress[] addresses = new InternetAddress[cc.length];
		for (int i = 0; i < cc.length; i++) {
			addresses[i] = parseAddress(cc[i]);
		}
		setCc(addresses);
	}

	public void addCc(InternetAddress cc) throws MessagingException {
		Assert.notNull(cc, "Cc address must not be null");
		validateAddress(cc);
		this.mimeMessage.addRecipient(Message.RecipientType.CC, cc);
	}

	public void addCc(String cc) throws MessagingException {
		Assert.notNull(cc, "Cc address must not be null");
		addCc(parseAddress(cc));
	}

	public void addCc(String cc, String personal) throws MessagingException, UnsupportedEncodingException {
		Assert.notNull(cc, "Cc address must not be null");
		addCc(getEncoding() != null ?
			new InternetAddress(cc, personal, getEncoding()) :
			new InternetAddress(cc, personal));
	}


	public void setBcc(InternetAddress bcc) throws MessagingException {
		Assert.notNull(bcc, "Bcc address must not be null");
		validateAddress(bcc);
		this.mimeMessage.setRecipient(Message.RecipientType.BCC, bcc);
	}

	public void setBcc(InternetAddress[] bcc) throws MessagingException {
		Assert.notNull(bcc, "Bcc address array must not be null");
		validateAddresses(bcc);
		this.mimeMessage.setRecipients(Message.RecipientType.BCC, bcc);
	}

	public void setBcc(String bcc) throws MessagingException {
		Assert.notNull(bcc, "Bcc address must not be null");
		setBcc(parseAddress(bcc));
	}

	public void setBcc(String[] bcc) throws MessagingException {
		Assert.notNull(bcc, "Bcc address array must not be null");
		InternetAddress[] addresses = new InternetAddress[bcc.length];
		for (int i = 0; i < bcc.length; i++) {
			addresses[i] = parseAddress(bcc[i]);
		}
		setBcc(addresses);
	}

	public void addBcc(InternetAddress bcc) throws MessagingException {
		Assert.notNull(bcc, "Bcc address must not be null");
		validateAddress(bcc);
		this.mimeMessage.addRecipient(Message.RecipientType.BCC, bcc);
	}

	public void addBcc(String bcc) throws MessagingException {
		Assert.notNull(bcc, "Bcc address must not be null");
		addBcc(parseAddress(bcc));
	}

	public void addBcc(String bcc, String personal) throws MessagingException, UnsupportedEncodingException {
		Assert.notNull(bcc, "Bcc address must not be null");
		addBcc(getEncoding() != null ?
			new InternetAddress(bcc, personal, getEncoding()) :
			new InternetAddress(bcc, personal));
	}

	private InternetAddress parseAddress(String address) throws MessagingException {
		InternetAddress[] parsed = InternetAddress.parse(address);
		if (parsed.length != 1) {
			throw new AddressException("Illegal address", address);
		}
		InternetAddress raw = parsed[0];
		try {
			return (getEncoding() != null ?
					new InternetAddress(raw.getAddress(), raw.getPersonal(), getEncoding()) : raw);
		}
		catch (UnsupportedEncodingException ex) {
			throw new MessagingException("Failed to parse embedded personal name to correct encoding", ex);
		}
	}


	/**
	 * Set the priority ("X-Priority" header) of the message.
	 * @param priority the priority value;
	 * typically between 1 (highest) and 5 (lowest)
	 * @throws MessagingException in case of errors
	 */
	public void setPriority(int priority) throws MessagingException {
		this.mimeMessage.setHeader(HEADER_PRIORITY, Integer.toString(priority));
	}

	/**
	 * Set the sent-date of the message.
	 * @param sentDate the date to set (never {@code null})
	 * @throws MessagingException in case of errors
	 */
	public void setSentDate(Date sentDate) throws MessagingException {
		Assert.notNull(sentDate, "Sent date must not be null");
		this.mimeMessage.setSentDate(sentDate);
	}

	/**
	 * Set the subject of the message, using the correct encoding.
	 * @param subject the subject text
	 * @throws MessagingException in case of errors
	 */
	public void setSubject(String subject) throws MessagingException {
		Assert.notNull(subject, "Subject must not be null");
		if (getEncoding() != null) {
			this.mimeMessage.setSubject(subject, getEncoding());
		}
		else {
			this.mimeMessage.setSubject(subject);
		}
	}


	/**
	 * Set the given text directly as content in non-multipart mode
	 * or as default body part in multipart mode.
	 * Always applies the default content type "text/plain".
	 * <p><b>NOTE:</b> Invoke {@link #addInline} <i>after</i> {@code setText};
	 * else, mail readers might not be able to resolve inline references correctly.
	 * @param text the text for the message
	 * @throws MessagingException in case of errors
	 */
	public void setText(String text) throws MessagingException {
		setText(text, false);
	}

	/**
	 * Set the given text directly as content in non-multipart mode
	 * or as default body part in multipart mode.
	 * The "html" flag determines the content type to apply.
	 * <p><b>NOTE:</b> Invoke {@link #addInline} <i>after</i> {@code setText};
	 * else, mail readers might not be able to resolve inline references correctly.
	 * @param text the text for the message
	 * @param html whether to apply content type "text/html" for an
	 * HTML mail, using default content type ("text/plain") else
	 * @throws MessagingException in case of errors
	 */
	public void setText(String text, boolean html) throws MessagingException {
		Assert.notNull(text, "Text must not be null");
		MimePart partToUse;
		if (isMultipart()) {
			partToUse = getMainPart();
		}
		else {
			partToUse = this.mimeMessage;
		}
		if (html) {
			setHtmlTextToMimePart(partToUse, text);
		}
		else {
			setPlainTextToMimePart(partToUse, text);
		}
	}

	/**
	 * Set the given plain text and HTML text as alternatives, offering
	 * both options to the email client. Requires multipart mode.
	 * <p><b>NOTE:</b> Invoke {@link #addInline} <i>after</i> {@code setText};
	 * else, mail readers might not be able to resolve inline references correctly.
	 * @param plainText the plain text for the message
	 * @param htmlText the HTML text for the message
	 * @throws MessagingException in case of errors
	 */
	public void setText(String plainText, String htmlText) throws MessagingException {
		Assert.notNull(plainText, "Plain text must not be null");
		Assert.notNull(htmlText, "HTML text must not be null");

		MimeMultipart messageBody = new MimeMultipart(MULTIPART_SUBTYPE_ALTERNATIVE);
		getMainPart().setContent(messageBody, CONTENT_TYPE_ALTERNATIVE);

		// Create the plain text part of the message.
		MimeBodyPart plainTextPart = new MimeBodyPart();
		setPlainTextToMimePart(plainTextPart, plainText);
		messageBody.addBodyPart(plainTextPart);

		// Create the HTML text part of the message.
		MimeBodyPart htmlTextPart = new MimeBodyPart();
		setHtmlTextToMimePart(htmlTextPart, htmlText);
		messageBody.addBodyPart(htmlTextPart);
	}

	private MimeBodyPart getMainPart() throws MessagingException {
		MimeMultipart mimeMultipart = getMimeMultipart();
		MimeBodyPart bodyPart = null;
		for (int i = 0; i < mimeMultipart.getCount(); i++) {
			BodyPart bp = mimeMultipart.getBodyPart(i);
			if (bp.getFileName() == null) {
				bodyPart = (MimeBodyPart) bp;
			}
		}
		if (bodyPart == null) {
			MimeBodyPart mimeBodyPart = new MimeBodyPart();
			mimeMultipart.addBodyPart(mimeBodyPart);
			bodyPart = mimeBodyPart;
		}
		return bodyPart;
	}

	private void setPlainTextToMimePart(MimePart mimePart, String text) throws MessagingException {
		if (getEncoding() != null) {
			mimePart.setText(text, getEncoding());
		}
		else {
			mimePart.setText(text);
		}
	}

	private void setHtmlTextToMimePart(MimePart mimePart, String text) throws MessagingException {
		if (getEncoding() != null) {
			mimePart.setContent(text, CONTENT_TYPE_HTML + CONTENT_TYPE_CHARSET_SUFFIX + getEncoding());
		}
		else {
			mimePart.setContent(text, CONTENT_TYPE_HTML);
		}
	}


	/**
	 * Add an inline element to the MimeMessage, taking the content from a
	 * {@code javax.activation.DataSource}.
	 * <p>Note that the InputStream returned by the DataSource implementation
	 * needs to be a <i>fresh one on each call</i>, as JavaMail will invoke
	 * {@code getInputStream()} multiple times.
	 * <p><b>NOTE:</b> Invoke {@code addInline} <i>after</i> {@link #setText};
	 * else, mail readers might not be able to resolve inline references correctly.
	 * @param contentId the content ID to use. Will end up as "Content-ID" header
	 * in the body part, surrounded by angle brackets: e.g. "myId" -> "&lt;myId&gt;".
	 * Can be referenced in HTML source via src="cid:myId" expressions.
	 * @param dataSource the {@code javax.activation.DataSource} to take
	 * the content from, determining the InputStream and the content type
	 * @throws MessagingException in case of errors
	 * @see #addInline(String, java.io.File)
	 * @see #addInline(String, org.springframework.core.io.Resource)
	 */
	public void addInline(String contentId, DataSource dataSource) throws MessagingException {
		Assert.notNull(contentId, "Content ID must not be null");
		Assert.notNull(dataSource, "DataSource must not be null");
		MimeBodyPart mimeBodyPart = new MimeBodyPart();
		mimeBodyPart.setDisposition(MimeBodyPart.INLINE);
		// We're using setHeader here to remain compatible with JavaMail 1.2,
		// rather than JavaMail 1.3's setContentID.
		mimeBodyPart.setHeader(HEADER_CONTENT_ID, "<" + contentId + ">");
		mimeBodyPart.setDataHandler(new DataHandler(dataSource));
		getMimeMultipart().addBodyPart(mimeBodyPart);
	}

	/**
	 * Add an inline element to the MimeMessage, taking the content from a
	 * {@code java.io.File}.
	 * <p>The content type will be determined by the name of the given
	 * content file. Do not use this for temporary files with arbitrary
	 * filenames (possibly ending in ".tmp" or the like)!
	 * <p><b>NOTE:</b> Invoke {@code addInline} <i>after</i> {@link #setText};
	 * else, mail readers might not be able to resolve inline references correctly.
	 * @param contentId the content ID to use. Will end up as "Content-ID" header
	 * in the body part, surrounded by angle brackets: e.g. "myId" -> "&lt;myId&gt;".
	 * Can be referenced in HTML source via src="cid:myId" expressions.
	 * @param file the File resource to take the content from
	 * @throws MessagingException in case of errors
	 * @see #setText
	 * @see #addInline(String, org.springframework.core.io.Resource)
	 * @see #addInline(String, javax.activation.DataSource)
	 */
	public void addInline(String contentId, File file) throws MessagingException {
		Assert.notNull(file, "File must not be null");
		FileDataSource dataSource = new FileDataSource(file);
		dataSource.setFileTypeMap(getFileTypeMap());
		addInline(contentId, dataSource);
	}

	/**
	 * Add an inline element to the MimeMessage, taking the content from a
	 * {@code org.springframework.core.io.Resource}.
	 * <p>The content type will be determined by the name of the given
	 * content file. Do not use this for temporary files with arbitrary
	 * filenames (possibly ending in ".tmp" or the like)!
	 * <p>Note that the InputStream returned by the Resource implementation
	 * needs to be a <i>fresh one on each call</i>, as JavaMail will invoke
	 * {@code getInputStream()} multiple times.
	 * <p><b>NOTE:</b> Invoke {@code addInline} <i>after</i> {@link #setText};
	 * else, mail readers might not be able to resolve inline references correctly.
	 * @param contentId the content ID to use. Will end up as "Content-ID" header
	 * in the body part, surrounded by angle brackets: e.g. "myId" -> "&lt;myId&gt;".
	 * Can be referenced in HTML source via src="cid:myId" expressions.
	 * @param resource the resource to take the content from
	 * @throws MessagingException in case of errors
	 * @see #setText
	 * @see #addInline(String, java.io.File)
	 * @see #addInline(String, javax.activation.DataSource)
	 */
	public void addInline(String contentId, Resource resource) throws MessagingException {
		Assert.notNull(resource, "Resource must not be null");
		String contentType = getFileTypeMap().getContentType(resource.getFilename());
		addInline(contentId, resource, contentType);
	}

	/**
	 * Add an inline element to the MimeMessage, taking the content from an
	 * {@code org.springframework.core.InputStreamResource}, and
	 * specifying the content type explicitly.
	 * <p>You can determine the content type for any given filename via a Java
	 * Activation Framework's FileTypeMap, for example the one held by this helper.
	 * <p>Note that the InputStream returned by the InputStreamSource implementation
	 * needs to be a <i>fresh one on each call</i>, as JavaMail will invoke
	 * {@code getInputStream()} multiple times.
	 * <p><b>NOTE:</b> Invoke {@code addInline} <i>after</i> {@code setText};
	 * else, mail readers might not be able to resolve inline references correctly.
	 * @param contentId the content ID to use. Will end up as "Content-ID" header
	 * in the body part, surrounded by angle brackets: e.g. "myId" -> "&lt;myId&gt;".
	 * Can be referenced in HTML source via src="cid:myId" expressions.
	 * @param inputStreamSource the resource to take the content from
	 * @param contentType the content type to use for the element
	 * @throws MessagingException in case of errors
	 * @see #setText
	 * @see #getFileTypeMap
	 * @see #addInline(String, org.springframework.core.io.Resource)
	 * @see #addInline(String, javax.activation.DataSource)
	 */
	public void addInline(String contentId, InputStreamSource inputStreamSource, String contentType)
		throws MessagingException {

		Assert.notNull(inputStreamSource, "InputStreamSource must not be null");
		if (inputStreamSource instanceof Resource && ((Resource) inputStreamSource).isOpen()) {
			throw new IllegalArgumentException(
					"Passed-in Resource contains an open stream: invalid argument. " +
					"JavaMail requires an InputStreamSource that creates a fresh stream for every call.");
		}
		DataSource dataSource = createDataSource(inputStreamSource, contentType, "inline");
		addInline(contentId, dataSource);
	}

	/**
	 * Add an attachment to the MimeMessage, taking the content from a
	 * {@code javax.activation.DataSource}.
	 * <p>Note that the InputStream returned by the DataSource implementation
	 * needs to be a <i>fresh one on each call</i>, as JavaMail will invoke
	 * {@code getInputStream()} multiple times.
	 * @param attachmentFilename the name of the attachment as it will
	 * appear in the mail (the content type will be determined by this)
	 * @param dataSource the {@code javax.activation.DataSource} to take
	 * the content from, determining the InputStream and the content type
	 * @throws MessagingException in case of errors
	 * @see #addAttachment(String, org.springframework.core.io.InputStreamSource)
	 * @see #addAttachment(String, java.io.File)
	 */
	public void addAttachment(String attachmentFilename, DataSource dataSource) throws MessagingException {
		Assert.notNull(attachmentFilename, "Attachment filename must not be null");
		Assert.notNull(dataSource, "DataSource must not be null");
		MimeBodyPart mimeBodyPart = new MimeBodyPart();
		mimeBodyPart.setDisposition(MimeBodyPart.ATTACHMENT);
		mimeBodyPart.setFileName(attachmentFilename);
		mimeBodyPart.setDataHandler(new DataHandler(dataSource));
		getRootMimeMultipart().addBodyPart(mimeBodyPart);
	}

	/**
	 * Add an attachment to the MimeMessage, taking the content from a
	 * {@code java.io.File}.
	 * <p>The content type will be determined by the name of the given
	 * content file. Do not use this for temporary files with arbitrary
	 * filenames (possibly ending in ".tmp" or the like)!
	 * @param attachmentFilename the name of the attachment as it will
	 * appear in the mail
	 * @param file the File resource to take the content from
	 * @throws MessagingException in case of errors
	 * @see #addAttachment(String, org.springframework.core.io.InputStreamSource)
	 * @see #addAttachment(String, javax.activation.DataSource)
	 */
	public void addAttachment(String attachmentFilename, File file) throws MessagingException {
		Assert.notNull(file, "File must not be null");
		FileDataSource dataSource = new FileDataSource(file);
		dataSource.setFileTypeMap(getFileTypeMap());
		addAttachment(attachmentFilename, dataSource);
	}

	/**
	 * Add an attachment to the MimeMessage, taking the content from an
	 * {@code org.springframework.core.io.InputStreamResource}.
	 * <p>The content type will be determined by the given filename for
	 * the attachment. Thus, any content source will be fine, including
	 * temporary files with arbitrary filenames.
	 * <p>Note that the InputStream returned by the InputStreamSource
	 * implementation needs to be a <i>fresh one on each call</i>, as
	 * JavaMail will invoke {@code getInputStream()} multiple times.
	 * @param attachmentFilename the name of the attachment as it will
	 * appear in the mail
	 * @param inputStreamSource the resource to take the content from
	 * (all of Spring's Resource implementations can be passed in here)
	 * @throws MessagingException in case of errors
	 * @see #addAttachment(String, java.io.File)
	 * @see #addAttachment(String, javax.activation.DataSource)
	 * @see org.springframework.core.io.Resource
	 */
	public void addAttachment(String attachmentFilename, InputStreamSource inputStreamSource)
		throws MessagingException {

		String contentType = getFileTypeMap().getContentType(attachmentFilename);
		addAttachment(attachmentFilename, inputStreamSource, contentType);
	}

	/**
	 * Add an attachment to the MimeMessage, taking the content from an
	 * {@code org.springframework.core.io.InputStreamResource}.
	 * <p>Note that the InputStream returned by the InputStreamSource
	 * implementation needs to be a <i>fresh one on each call</i>, as
	 * JavaMail will invoke {@code getInputStream()} multiple times.
	 * @param attachmentFilename the name of the attachment as it will
	 * appear in the mail
	 * @param inputStreamSource the resource to take the content from
	 * (all of Spring's Resource implementations can be passed in here)
	 * @param contentType the content type to use for the element
	 * @throws MessagingException in case of errors
	 * @see #addAttachment(String, java.io.File)
	 * @see #addAttachment(String, javax.activation.DataSource)
	 * @see org.springframework.core.io.Resource
	 */
	public void addAttachment(
			String attachmentFilename, InputStreamSource inputStreamSource, String contentType)
		throws MessagingException {

		Assert.notNull(inputStreamSource, "InputStreamSource must not be null");
		if (inputStreamSource instanceof Resource && ((Resource) inputStreamSource).isOpen()) {
			throw new IllegalArgumentException(
					"Passed-in Resource contains an open stream: invalid argument. " +
					"JavaMail requires an InputStreamSource that creates a fresh stream for every call.");
		}
		DataSource dataSource = createDataSource(inputStreamSource, contentType, attachmentFilename);
		addAttachment(attachmentFilename, dataSource);
	}

	/**
	 * Create an Activation Framework DataSource for the given InputStreamSource.
	 * @param inputStreamSource the InputStreamSource (typically a Spring Resource)
	 * @param contentType the content type
	 * @param name the name of the DataSource
	 * @return the Activation Framework DataSource
	 */
	protected DataSource createDataSource(
		final InputStreamSource inputStreamSource, final String contentType, final String name) {

		return new DataSource() {
			@Override
			public InputStream getInputStream() throws IOException {
				return inputStreamSource.getInputStream();
			}
			@Override
			public OutputStream getOutputStream() {
				throw new UnsupportedOperationException("Read-only javax.activation.DataSource");
			}
			@Override
			public String getContentType() {
				return contentType;
			}
			@Override
			public String getName() {
				return name;
			}
		};
	}

}
