/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.web.servlet.view.document;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import junit.framework.TestCase;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * @author Alef Arendsen
 * @author Juergen Hoeller
 */
public class PdfViewTests extends TestCase {

	public void testPdf() throws Exception {
		final String text = "this should be in the PDF";
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		AbstractPdfView pdfView = new AbstractPdfView() {
			protected void buildPdfDocument(Map model, Document document, PdfWriter writer,
					HttpServletRequest request, HttpServletResponse response) throws Exception {
				document.add(new Paragraph(text));
			}
		};

		pdfView.render(new HashMap(), request, response);
		byte[] pdfContent = response.getContentAsByteArray();
		assertEquals("correct response content type", "application/pdf", response.getContentType());
		assertEquals("correct response content length", pdfContent.length, response.getContentLength());

		// rebuild iText document for comparison
		Document document = new Document(PageSize.A4);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PdfWriter writer = PdfWriter.getInstance(document, baos);
		writer.setViewerPreferences(PdfWriter.AllowPrinting | PdfWriter.PageLayoutSinglePage);
		document.open();
		document.add(new Paragraph(text));
		document.close();
		byte[] baosContent = baos.toByteArray();
		assertEquals("correct size", pdfContent.length, baosContent.length);

		int diffCount = 0;
		for (int i = 0; i < pdfContent.length; i++) {
			if (pdfContent[i] != baosContent[i]) {
				diffCount++;
			}
		}
		assertTrue("difference only in encryption", diffCount < 70);
	}

}
