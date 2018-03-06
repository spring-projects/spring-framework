<#ftl output_format="HTML" strip_whitespace=true>
<#--
 * spring.ftl
 *
 * This file consists of a collection of FreeMarker macros aimed at easing
 * some of the common requirements of web applications - in particular
 * handling of forms.
 *
 * Spring's FreeMarker support will automatically make this file and therefore
 * all macros within it available to any application using Spring's
 * FreeMarkerConfigurer.
 *
 * To take advantage of these macros, the "exposeSpringMacroHelpers" property
 * of the FreeMarker class needs to be set to "true". This will expose a
 * RequestContext under the name "springMacroRequestContext", as needed by
 * the macros in this library.
 *
 * @author Darren Davison
 * @author Juergen Hoeller
 * @author Sascha Woo
 * @since 1.1
 -->

<#--
 * message
 *
 * Macro to translate a message code into a message.
 *
 * @param code the code of the message
 * @param args arguments for the message (optional)
 * @param text the string to return if the lookup fails
 -->
<#macro message code args=[] text="">
    <#if args?has_content && text?has_content>
        ${springMacroRequestContext.getMessage(code, args, text)}
    <#elseif args?has_content && !text?has_content>
        ${springMacroRequestContext.getMessage(code, args)}
    <#elseif !args?has_content && text?has_content>
        ${springMacroRequestContext.getMessage(code, text)}
    <#else>
        ${springMacroRequestContext.getMessage(code)}
    </#if>
</#macro>

<#--
 * messageText
 *
 * Macro to translate a message code into a message,
 * using the given default text if no message found.
 *
 * @Deprecated use <@spring.message code args text /> instead
 -->
<#macro messageText code, text>${springMacroRequestContext.getMessage(code, text)}</#macro>

<#--
 * messageArgs
 *
 * Macro to translate a message code with arguments into a message.
 *
 * @Deprecated use <@spring.message code args text /> instead
 -->
<#macro messageArgs code, args>${springMacroRequestContext.getMessage(code, args)}</#macro>

<#--
 * messageArgsText
 *
 * Macro to translate a message code with arguments into a message,
 * using the given default text if no message found.
 *
 * @Deprecated use <@spring.message code args text /> instead
 -->
<#macro messageArgsText code, args, text>${springMacroRequestContext.getMessage(code, args, text)}</#macro>

<#--
 * theme
 *
 * Macro to translate a theme message code into a message.
 *
 * @param code the code of the message
 * @param args arguments for the message (optional)
 * @param text the string to return if the lookup fails
 -->
<#macro theme code args=[] text="">
    <#if args?has_content && text?has_content>
        ${springMacroRequestContext.getThemeMessage(code, args, text)}
    <#elseif args?has_content && !text?has_content>
        ${springMacroRequestContext.getThemeMessage(code, args)}
    <#elseif !args?has_content && text?has_content>
        ${springMacroRequestContext.getThemeMessage(code, text)}
    <#else>
        ${springMacroRequestContext.getThemeMessage(code)}
    </#if>
</#macro>

<#--
 * themeText
 *
 * Macro to translate a theme message code into a message,
 * using the given default text if no message found.
 * @Deprecated use <@spring.theme code args text /> instead
 -->
<#macro themeText code, text>${springMacroRequestContext.getThemeMessage(code, text)}</#macro>

<#--
 * themeArgs
 *
 * Macro to translate a theme message code with arguments into a message.
 *
 * @Deprecated use <@spring.theme code args text /> instead
 -->
<#macro themeArgs code, args>${springMacroRequestContext.getThemeMessage(code, args)}</#macro>

<#--
 * themeArgsText
 *
 * Macro to translate a theme message code with arguments into a message,
 * using the given default text if no message found.
 *
 * @Deprecated use <@spring.theme code args text /> instead
 -->
<#macro themeArgsText code, args, text>${springMacroRequestContext.getThemeMessage(code, args, text)}</#macro>

<#--
 * url
 *
 * Function to output a context-aware URL for the given relative URL with optional placeholders (named keys with braces {}).
 * For example, send in a relative URL foo/{bar}?spam={spam} and a parameter map
 * {@code {bar=baz,spam=nuts}} and the result will be [contextpath]/foo/baz?spam=nuts.
 *
 * @param relativeUrl the relative url
 * @param params a map of parameters to insert as placeholders in the url
 -->
<#macro url relativeUrl params={}>
    <#if params?? && params?size != 0>
        <#return springMacroRequestContext.getContextUrl(relativeUrl, params) />
    <#else>
        <#return springMacroRequestContext.getContextUrl(relativeUrl) />
    </#if>
</#macro>

<#--
 * bind
 *
 * Exposes a BindStatus object for the given bind path, which can be
 * a bean (e.g. "person") to get global errors, or a bean property
 * (e.g. "person.name") to get field errors. Can be called multiple times
 * within a form to bind to multiple command objects and/or field names.
 *
 * This macro will participate in the default HTML escape setting for the given
 * RequestContext. This can be customized by calling "setDefaultHtmlEscape"
 * on the "springMacroRequestContext" context variable, or via the
 * "defaultHtmlEscape" context-param in web.xml (same as for the JSP bind tag).
 * Also regards a "htmlEscape" variable in the namespace of this library.
 *
 * Producing no output, the following context variable will be available
 * each time this macro is referenced (assuming you import this library in
 * your templates with the namespace 'spring'):
 *
 * @param path the path (string value) of the value required to bind to.
 *   Spring defaults to a command name of "command" but this can be overridden
 *   by user config.
 * @param htmlEscape set HTML escaping for this tag, as boolean value.
 *   Overrides the default HTML escaping setting for the current page.
 -->
<#macro bind path htmlEscape="">
    <#if htmlEscape?is_boolean>
        <#assign status = springMacroRequestContext.getBindStatus(path, htmlEscape) />
    <#else>
        <#assign status = springMacroRequestContext.getBindStatus(path) />
    </#if>
    <#nested>
    <#assign status = "" />
</#macro>

<#--
 * hasBindErrors
 *
 * The hasBindErrors marco provides you with support for
 * binding the errors for an object. If they are available, an
 * Errors object gets bound in the page scope, which you can inspect.
 * Basically it's a bit like the bind macro, but this tag does not feature
 * the status object, it just binds all the errors for the object and its properties.
 * 
 * @param name the name of the bean in the request, that needs to be inspected for errors.
 *   If errors are available for this bean, they will be bound under the errors key.
 * @param htmlEscape set HTML escaping for this tag, as boolean value.
 *   Overrides the default HTML escaping setting for the current page. 
-->
<#macro hasBindErrors name htmlEscape="">
    <#if htmlEscape?is_boolean>
        <#local errors = springMacroRequestContext.getErrors(name, htmlEscape) />
    <#else>
        <#local errors = springMacroRequestContext.getErrors(name) />
    </#if>
    <#if errors?? && errors.hasErrors()>
        <#nested errors />
    </#if>
</#macro>

<#-- 
 * htmlEscape
 *
 * Sets default HTML escape value for the current page.
 * Overrides a "defaultHtmlEscape" context-param in web.xml, if any.
 *
 * @param defaultHtmlEscape set the default value for HTML escaping, to be put into the current PageContext.
  -->
<#macro htmlEscape defaultHtmlEscape>
    ${springMacroRequestContext.setDefaultHtmlEscape(defaultHtmlEscape)}
</#macro>
