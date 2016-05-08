package org.springframework.web.servlet.support;


import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class RequestIncludeHelper
{


    public String include(HttpServletRequest request, HttpServletResponse response, String path, Map<String,String[]> params) throws ServletException, IOException
    {
        return include(request,response,path,params,false);
    }

    public String include(HttpServletRequest request, HttpServletResponse response, String path, Map<String,String[]> params, boolean fallbackToRequestParams) throws ServletException, IOException
    {

        RequestIncludeWrapper wrappedRequest = new RequestIncludeWrapper(request,params,fallbackToRequestParams);
        ResponseIncludeWrapper wrappedResponse = new ResponseIncludeWrapper(response, request, request.getServletContext());
        include(path,wrappedRequest,wrappedResponse);
        return wrappedResponse.getContent();
    }

    public void include(String path,RequestIncludeWrapper wrappedRequest, ResponseIncludeWrapper wrappedResponse) throws ServletException, IOException
    {
        RequestDispatcher dispatcher = wrappedRequest.getRequest().getRequestDispatcher(path);
        dispatcher.include(wrappedRequest,wrappedResponse);
        wrappedResponse.flushBuffer();
    }

}
