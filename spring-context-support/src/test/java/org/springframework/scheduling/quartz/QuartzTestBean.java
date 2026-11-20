/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.quartz;

/**
 * @author Rob Harrop
 */
public class QuartzTestBean {

	private int importCount;

	private int exportCount;


	public void doImport() {
		++importCount;
	}

	public void doExport() {
		++exportCount;
	}

	public int getImportCount() {
		return importCount;
	}

	public int getExportCount() {
		return exportCount;
	}

}
