/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.servlet.support;


import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * @author Cagatay Kalan
 * @since 4.3.0
 */
public class RequestIncludeHelper
{


    public String include(HttpServletRequest request, HttpServletResponse response, String path, Map<String,String[]> params) throws ServletException, IOException
    {
        return include(request,response,path,params,false);
    }

    public String include(HttpServletRequest request, HttpServletResponse response, String path, Map<String,String[]> params, boolean fallbackToRequestParams) throws ServletException, IOException
    {

        RequestIncludeWrapper wrappedRequest = new RequestIncludeWrapper(request,params,fallbackToRequestParams);
        ResponseIncludeWrapper wrappedResponse = new ResponseIncludeWrapper(response);
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
