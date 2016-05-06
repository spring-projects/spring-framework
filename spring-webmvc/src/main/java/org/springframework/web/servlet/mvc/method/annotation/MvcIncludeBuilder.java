package org.springframework.web.servlet.mvc.method.annotation;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.RequestIncludeWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

public class MvcIncludeBuilder
{
    private String path;
    private Map<String,String[]> parameterMap;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private boolean fallBackToOriginalRequestParameters = false;

    private static final Log logger = LogFactory.getLog(MvcIncludeBuilder.class);


    protected MvcIncludeBuilder(String path)
    {
        Assert.notNull(path);
        this.path = path;
    }

    public static MvcIncludeBuilder fromPath(String path) {
        Assert.notNull(path,"path must not be null.");
        return new MvcIncludeBuilder(path);
    }

    public MvcIncludeBuilder enableRequestParameterFallback(boolean value) {
        this.fallBackToOriginalRequestParameters = value;
        return this;
    }

    public MvcIncludeBuilder withRequestAndResponse(HttpServletRequest request,HttpServletResponse response) {
        Assert.notNull(request,"request must not be null.");
        Assert.notNull(request,"response must not be null.");
        this.request = request;
        this.response = response;
        return this;
    }


    public MvcIncludeBuilder param(String parameterName, String... values) {
        ensureParameterMap().put(parameterName,values);
        return this;
    }

    private Map<String,String[]> ensureParameterMap()
    {
        if (parameterMap == null)
            parameterMap = new LinkedHashMap<String, String[]>();
        return parameterMap;
    }

    public MvcInclude build() {

        HttpServletRequest request = this.request;
        HttpServletResponse response = this.response;

        if (request == null) {

            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            if (requestAttributes == null) {
                throw new IllegalStateException("No request bound to the current thread: is DispatcherSerlvet used?");
            }

            request = ((ServletRequestAttributes) requestAttributes).getRequest();
            if (request == null) {
                throw new IllegalStateException("Request bound to current thread is not an HttpServletRequest");
            }

            response = ((ServletRequestAttributes) requestAttributes).getResponse();
        }

        return new MvcInclude(path,new RequestIncludeWrapper(request,parameterMap,fallBackToOriginalRequestParameters),response);
    }

}
