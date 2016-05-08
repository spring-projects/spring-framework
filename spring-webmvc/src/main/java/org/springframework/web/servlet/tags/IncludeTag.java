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

package org.springframework.web.servlet.tags;


import org.springframework.util.Assert;
import org.springframework.web.servlet.support.RequestIncludeHelper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSP tag for including the response of a web resource within another response (a.k.a server side includes )
 * Given the "path" of the resource to be included, an include request is sent and its response is captured
 * and displayed.
 *
 * <p>Child request has its own request parameters which can be set in two ways:</p>
 * <ul>
 *     <li>By setting the "params" attribute which requires a {@link Map<String,String[]>}</li>
 *     <li>By using one or more &lt;spring:param&gt; tags within this tag's body.</li>
 * </ul>
 *
 * <p>
 * Multiple parameter values for the same parameter name can be provided using multiple &lt;spring:param&gt; tags
 * with the same parameter name.
 * </p>
 *
 * <p>If {@code includeRequestParams} attribute is set to 'true', then current request parameters are merged with the
 * custom parameters provided to the child request.Child request parameters always override the parent request parameters.</p>
 *
 * <p>Example usage:
 * <pre class="code">&lt;spring:include path="/path-to-the-resource-to-be-included"&gt;
 *   &lt;spring:param name="paramName" value="paramValue" /&gt;
 *   &lt;spring:param name="anotherParamName" value="value1,value2,value3" /&gt;
 * &lt;/spring:url&gt;</pre>
 * </p>
 *
 * @author Cagatay Kalan
 * @see org.springframework.web.servlet.support.ResponseIncludeWrapper
 */
@SuppressWarnings("serial")
public class IncludeTag extends HtmlEscapingAwareTag implements ParamAware
{
    private Map<String,String[]> params;
    private String path;
    private Boolean includeRequestParams;
    private RequestIncludeHelper includeHelper;

    public void setParams(Map<String, String[]> params)
    {
        getParams().putAll(params);
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public void setIncludeRequestParams(boolean includeRequestParams)
    {
        this.includeRequestParams = includeRequestParams;
    }

    @Override
    public void addParam(Param param)
    {
        String value = param.getValue();
        String[] current = getParams().get(param.getName());
        if (current == null)
            current = new String[] { value};
        else
        {
            String[] old = current;
            current = new String[current.length+1];
            System.arraycopy(old,0,current,0,old.length);
            current[current.length-1] = value;
        }
        getParams().put(param.getName(),current);
    }

    @Override
    protected int doStartTagInternal() throws Exception
    {
        getParams();
        this.includeHelper = getIncludeHelper();
        return EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag() throws JspException
    {
        Assert.notNull(path,"path parameter must be set.");
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        HttpServletResponse response = (HttpServletResponse) pageContext.getResponse();

        try
        {
            String content = includeRequestParams == null ? includeHelper.include(request,response,path,params) : includeHelper.include(request,response,path,params,includeRequestParams);
            pageContext.getOut().print(htmlEscape(content));
        } catch (ServletException e)
        {
            throw new JspException(e);
        } catch (IOException e)
        {
            throw new JspException(e);
        } finally
        {
            resetState();
        }

        return EVAL_PAGE;

    }

    private void resetState()
    {
        params = null;
        path = null;
        includeRequestParams = null;
    }

    protected RequestIncludeHelper getIncludeHelper()
    {
        return new RequestIncludeHelper();
    }

    protected Map<String,String[]> getParams()
    {
        if (params == null)
            params = new LinkedHashMap<String, String[]>();
        return params;
    }
}
