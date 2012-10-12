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
package org.springframework.test.web.mock.servlet.samples.standalone;

import static org.springframework.test.web.mock.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.mock.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.mock.servlet.setup.MockMvcBuilders.standaloneSetup;

import java.io.IOException;

import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

public class FileUploadControllerTests {

	@Test
	public void readString() throws Exception {

		MockMultipartFile file = new MockMultipartFile("file", "orig", null, "bar".getBytes());

		standaloneSetup(new FileUploadController()).build()
				.perform(fileUpload("/fileupload").file(file))
				.andExpect(model().attribute("message", "File 'orig' uploaded successfully"));
	}


	@Controller
	private static class FileUploadController {

		@RequestMapping(value="/fileupload", method=RequestMethod.POST)
		public String processUpload(@RequestParam MultipartFile file, Model model) throws IOException {
			model.addAttribute("message", "File '" + file.getOriginalFilename() + "' uploaded successfully");
			return "redirect:/index";
		}

	}

}
