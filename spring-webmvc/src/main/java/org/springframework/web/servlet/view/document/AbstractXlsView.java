/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.view.document;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;

import org.springframework.web.servlet.view.AbstractView;

/**
 * Convenient superclass for Excel document views in traditional XLS format.
 * Compatible with Apache POI 3.5 and higher.
 *
 * <p>For working with the workbook in the subclass, see
 * <a href="https://poi.apache.org">Apache's POI site</a>
 *
 * @author Juergen Hoeller
 * @since 4.2
 * @deprecated since 7.0. Libraries or applications can provide their own {@code View}
 * class or perform rendering in web handlers directly.
 */
@Deprecated(since = "7.0", forRemoval = true)
public abstract class AbstractXlsView extends AbstractView {

	/**
	 * Default Constructor.
	 * Sets the content type of the view to "application/vnd.ms-excel".
	 */
	public AbstractXlsView() {
		setContentType("application/vnd.ms-excel");
	}


	@Override
	protected boolean generatesDownloadContent() {
		return true;
	}

	/**
	 * Renders the Excel view, given the specified model.
	 */
	@Override
	protected final void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		// Create a fresh workbook instance for this render step.
		Workbook workbook = createWorkbook(model, request);

		// Delegate to application-provided document code.
		buildExcelDocument(model, workbook, request, response);

		// Set the content type.
		response.setContentType(getContentType());

		// Flush byte array to servlet output stream.
		renderWorkbook(workbook, response);
	}


	/**
	 * Template method for creating the POI {@link Workbook} instance.
	 * <p>The default implementation creates a traditional {@link HSSFWorkbook}.
	 * Spring-provided subclasses are overriding this for the OOXML-based variants;
	 * custom subclasses may override this for reading a workbook from a file.
	 * @param model the model Map
	 * @param request current HTTP request (for taking the URL or headers into account)
	 * @return the new {@link Workbook} instance
	 */
	protected Workbook createWorkbook(Map<String, Object> model, HttpServletRequest request) {
		return new HSSFWorkbook();
	}

	/**
	 * The actual render step: taking the POI {@link Workbook} and rendering
	 * it to the given response.
	 * @param workbook the POI Workbook to render
	 * @param response current HTTP response
	 * @throws IOException when thrown by I/O methods that we're delegating to
	 */
	protected void renderWorkbook(Workbook workbook, HttpServletResponse response) throws IOException {
		ServletOutputStream out = response.getOutputStream();
		workbook.write(out);
		workbook.close();
	}

	/**
	 * Application-provided subclasses must implement this method to populate
	 * the Excel workbook document, given the model.
	 * @param model the model Map
	 * @param workbook the Excel workbook to populate
	 * @param request in case we need locale etc. Shouldn't look at attributes.
	 * @param response in case we need to set cookies. Shouldn't write to it.
	 */
	protected abstract void buildExcelDocument(
			Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response)
			throws Exception;

}
