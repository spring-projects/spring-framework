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
        String[] values = value.split(",");
        params.put(param.getName(),values);
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
        }

        return EVAL_PAGE;

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
