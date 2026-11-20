/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.view.document;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Convenient superclass for Excel document views in the Office 2007 XLSX format
 * (as supported by POI-OOXML). Compatible with Apache POI 3.5 and higher.
 *
 * <p>For working with the workbook in subclasses, see
 * <a href="https://poi.apache.org">Apache's POI site</a>.
 *
 * @author Juergen Hoeller
 * @since 4.2
 * @deprecated since 7.0. Libraries or applications can provide their own {@code View}
 * class or perform rendering in web handlers directly.
 */
@Deprecated(since = "7.0", forRemoval = true)
@SuppressWarnings("removal")
public abstract class AbstractXlsxView extends AbstractXlsView {

	/**
	 * Default Constructor.
	 * <p>Sets the content type of the view to
	 * {@code "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"}.
	 */
	public AbstractXlsxView() {
		setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	}

	/**
	 * This implementation creates an {@link XSSFWorkbook} for the XLSX format.
	 */
	@Override
	protected Workbook createWorkbook(Map<String, Object> model, HttpServletRequest request) {
		return new XSSFWorkbook();
	}

}
