package io.codegitz.demo.resource;

import org.springframework.core.io.FileSystemResource;

import java.io.*;

/**
 * @author 张观权
 * @date 2020/8/11 16:04
 **/
public class ResourceDemo {
	public static void main(String[] args) throws IOException {
		FileSystemResource fileSystemResource = new FileSystemResource("C:\\my_study_project\\spring-framework\\spring-demo\\src\\main\\java\\io\\codegitz\\demo\\resource\\resource.txt");
		File file = fileSystemResource.getFile();
		System.out.println(file.length());
		OutputStream outputStream = fileSystemResource.getOutputStream();
		BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
		bufferedWriter.write("Helloworld");
		bufferedWriter.flush();
		outputStream.close();
		bufferedWriter.close();
	}
}
