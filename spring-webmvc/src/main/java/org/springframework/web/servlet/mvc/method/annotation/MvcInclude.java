package org.springframework.web.servlet.mvc.method.annotation;


import org.springframework.web.servlet.support.ByteArrayServletOutputStream;
import org.springframework.web.servlet.support.RequestIncludeWrapper;
import org.springframework.web.servlet.support.ResponseIncludeWrapper;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class MvcInclude
{

    private String path;
    private RequestIncludeWrapper request;
    private HttpServletResponse response;

    public MvcInclude(String path, RequestIncludeWrapper request, HttpServletResponse response)
    {
        this.path = path;
        this.request = request;
        this.response = response;
    }

    public String execute() throws ServletException, IOException
    {
        RequestDispatcher dispatcher = request.getRequest().getRequestDispatcher(path);
        ByteArrayServletOutputStream outputStream = new ByteArrayServletOutputStream();
        ResponseIncludeWrapper wrappedResponse = new ResponseIncludeWrapper(response,outputStream);
        dispatcher.include(request,wrappedResponse);
        wrappedResponse.flushOutputStreamOrWriter();

        return outputStream.getBuffer().toString(response.getCharacterEncoding());
    }

}
