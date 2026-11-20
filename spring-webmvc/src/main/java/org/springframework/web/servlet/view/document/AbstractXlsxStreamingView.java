/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.view.document;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

/**
 * Convenient superclass for Excel document views in the Office 2007 XLSX format,
 * using POI's streaming variant. Compatible with Apache POI 3.9 and higher.
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
public abstract class AbstractXlsxStreamingView extends AbstractXlsxView {

	/**
	 * This implementation creates a {@link SXSSFWorkbook} for streaming the XLSX format.
	 */
	@Override
	protected SXSSFWorkbook createWorkbook(Map<String, Object> model, HttpServletRequest request) {
		return new SXSSFWorkbook();
	}

	/**
	 * This implementation disposes of the {@link SXSSFWorkbook} when done with rendering.
	 * @see org.apache.poi.xssf.streaming.SXSSFWorkbook#dispose()
	 */
	@Override
	protected void renderWorkbook(Workbook workbook, HttpServletResponse response) throws IOException {
		super.renderWorkbook(workbook, response);

		// Dispose of temporary files in case of streaming variant...
		((SXSSFWorkbook) workbook).dispose();
	}

}
