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
 * @since 1.1
 -->

<#--
 * message
 *
 * Macro to translate a message code into a message.
 -->
<#macro message code>${springMacroRequestContext.getMessage(code)?no_esc}</#macro>

<#--
 * messageText
 *
 * Macro to translate a message code into a message,
 * using the given default text if no message found.
 -->
<#macro messageText code, text>${springMacroRequestContext.getMessage(code, text)?no_esc}</#macro>

<#--
 * messageArgs
 *
 * Macro to translate a message code with arguments into a message.
 -->
<#macro messageArgs code, args>${springMacroRequestContext.getMessage(code, args)?no_esc}</#macro>

<#--
 * messageArgsText
 *
 * Macro to translate a message code with arguments into a message,
 * using the given default text if no message found.
 -->
<#macro messageArgsText code, args, text>${springMacroRequestContext.getMessage(code, args, text)?no_esc}</#macro>

<#--
 * theme
 *
 * Macro to translate a theme message code into a message.
 -->
<#macro theme code>${springMacroRequestContext.getThemeMessage(code)?no_esc}</#macro>

<#--
 * themeText
 *
 * Macro to translate a theme message code into a message,
 * using the given default text if no message found.
 -->
<#macro themeText code, text>${springMacroRequestContext.getThemeMessage(code, text)?no_esc}</#macro>

<#--
 * themeArgs
 *
 * Macro to translate a theme message code with arguments into a message.
 -->
<#macro themeArgs code, args>${springMacroRequestContext.getThemeMessage(code, args)?no_esc}</#macro>

<#--
 * themeArgsText
 *
 * Macro to translate a theme message code with arguments into a message,
 * using the given default text if no message found.
 -->
<#macro themeArgsText code, args, text>${springMacroRequestContext.getThemeMessage(code, args, text)?no_esc}</#macro>

<#--
 * url
 *
 * Takes a relative URL and makes it absolute from the server root by
 * adding the context root for the web application.
 -->
<#macro url relativeUrl extra...><#if extra?? && extra?size!=0>${springMacroRequestContext.getContextUrl(relativeUrl,extra)?no_esc}<#else>${springMacroRequestContext.getContextUrl(relativeUrl)?no_esc}</#if></#macro>

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
 *   spring.status : a BindStatus instance holding the command object name,
 *   expression, value, and error messages and codes for the path supplied
 *
 * @param path the path (string value) of the value required to bind to.
 *     Spring defaults to a command name of "command" but this can be
 *     overridden by user configuration.
 -->
<#macro bind path>
    <#if htmlEscape?exists>
        <#assign status = springMacroRequestContext.getBindStatus(path, htmlEscape)>
    <#else>
        <#assign status = springMacroRequestContext.getBindStatus(path)>
    </#if>
    <#-- assign a temporary value, forcing a string representation for any
    kind of variable. This temp value is only used in this macro lib -->
    <#if status.value?exists && status.value?is_boolean>
        <#assign stringStatusValue=status.value?string>
    <#else>
        <#assign stringStatusValue=status.value?default("")>
    </#if>
</#macro>

<#--
 * bindEscaped
 *
 * Similar to spring:bind, but takes an explicit HTML escape flag rather
 * than relying on the default HTML escape setting.
 -->
<#macro bindEscaped path, htmlEscape>
    <#assign status = springMacroRequestContext.getBindStatus(path, htmlEscape)>
    <#-- assign a temporary value, forcing a string representation for any
    kind of variable. This temp value is only used in this macro lib -->
    <#if status.value?exists && status.value?is_boolean>
        <#assign stringStatusValue=status.value?string>
    <#else>
        <#assign stringStatusValue=status.value?default("")>
    </#if>
</#macro>

<#--
 * formInput
 *
 * Display a form input field of type 'text' and bind it to an attribute
 * of a command or bean.
 *
 * @param path the name of the field to bind to
 * @param attributes any additional attributes for the element
 *    (such as class or CSS styles or size)
 -->
<#macro formInput path attributes="" fieldType="text">
    <@bind path/>
    <input type="${fieldType}" id="${status.expression?replace('[','')?replace(']','')}" name="${status.expression}" value="<#if fieldType!="password">${stringStatusValue}</#if>" ${attributes?no_esc}<@closeTag/>
</#macro>

<#--
 * formPasswordInput
 *
 * Display a form input field of type 'password' and bind it to an attribute
 * of a command or bean. No value will ever be displayed. This functionality
 * can also be obtained by calling the formInput macro with a 'type' parameter
 * of 'password'.
 *
 * @param path the name of the field to bind to
 * @param attributes any additional attributes for the element
 *    (such as class or CSS styles or size)
 -->
<#macro formPasswordInput path attributes="">
    <@formInput path, attributes, "password"/>
</#macro>

<#--
 * formHiddenInput
 *
 * Generate a form input field of type 'hidden' and bind it to an attribute
 * of a command or bean. This functionality can also be obtained by calling
 * the formInput macro with a 'type' parameter of 'hidden'.
 *
 * @param path the name of the field to bind to
 * @param attributes any additional attributes for the element
 *    (such as class or CSS styles or size)
 -->
<#macro formHiddenInput path attributes="">
    <@formInput path, attributes, "hidden"/>
</#macro>

<#--
 * formTextarea
 *
 * Display a text area and bind it to an attribute of a command or bean.
 *
 * @param path the name of the field to bind to
 * @param attributes any additional attributes for the element
 *    (such as class or CSS styles or size)
 -->
<#macro formTextarea path attributes="">
    <@bind path/>
    <textarea id="${status.expression?replace('[','')?replace(']','')}" name="${status.expression}" ${attributes?no_esc}>
${stringStatusValue}</textarea>
</#macro>

<#--
 * formSingleSelect
 *
 * Show a selectbox (dropdown) input element allowing a single value to be chosen
 * from a list of options.
 *
 * @param path the name of the field to bind to
 * @param options a map (value=label) of all the available options
 * @param attributes any additional attributes for the element
 *    (such as class or CSS styles or size)
-->
<#macro formSingleSelect path options attributes="">
    <@bind path/>
    <select id="${status.expression?replace('[','')?replace(']','')}" name="${status.expression}" ${attributes?no_esc}>
        <#if options?is_hash>
            <#list options?keys as value>
            <option value="${value}"<@checkSelected value/>>${options[value]}</option>
            </#list>
        <#else> 
            <#list options as value>
            <option value="${value}"<@checkSelected value/>>${value}</option>
            </#list>
        </#if>
    </select>
</#macro>

<#--
 * formMultiSelect
 *
 * Show a listbox of options allowing the user to make 0 or more choices from
 * the list of options.
 *
 * @param path the name of the field to bind to
 * @param options a map (value=label) of all the available options
 * @param attributes any additional attributes for the element
 *    (such as class or CSS styles or size)
-->
<#macro formMultiSelect path options attributes="">
    <@bind path/>
    <select multiple="multiple" id="${status.expression?replace('[','')?replace(']','')}" name="${status.expression}" ${attributes?no_esc}>
        <#list options?keys as value>
        <#assign isSelected = contains(status.actualValue?default([""]), value)>
        <option value="${value}"<#if isSelected> selected="selected"</#if>>${options[value]}</option>
        </#list>
    </select>
</#macro>

<#--
 * formRadioButtons
 *
 * Show radio buttons.
 *
 * @param path the name of the field to bind to
 * @param options a map (value=label) of all the available options
 * @param separator the HTML tag or other character list that should be used to
 *    separate each option (typically '&nbsp;' or '<br>')
 * @param attributes any additional attributes for the element
 *    (such as class or CSS styles or size)
-->
<#macro formRadioButtons path options separator attributes="">
    <@bind path/>
    <#list options?keys as value>
    <#assign id="${status.expression?replace('[','')?replace(']','')}${value_index}">
    <input type="radio" id="${id}" name="${status.expression}" value="${value}"<#if stringStatusValue == value> checked="checked"</#if> ${attributes?no_esc}<@closeTag/>
    <label for="${id}">${options[value]}</label>${separator?no_esc}
    </#list>
</#macro>

<#--
 * formCheckboxes
 *
 * Show checkboxes.
 *
 * @param path the name of the field to bind to
 * @param options a map (value=label) of all the available options
 * @param separator the HTML tag or other character list that should be used to
 *    separate each option (typically '&nbsp;' or '<br>')
 * @param attributes any additional attributes for the element
 *    (such as class or CSS styles or size)
-->
<#macro formCheckboxes path options separator attributes="">
    <@bind path/>
    <#list options?keys as value>
    <#assign id="${status.expression?replace('[','')?replace(']','')}${value_index}">
    <#assign isSelected = contains(status.actualValue?default([""]), value)>
    <input type="checkbox" id="${id}" name="${status.expression}" value="${value}"<#if isSelected> checked="checked"</#if> ${attributes?no_esc}<@closeTag/>
    <label for="${id}">${options[value]}</label>${separator?no_esc}
    </#list>
    <input type="hidden" name="_${status.expression}" value="on"/>
</#macro>

<#--
 * formCheckbox
 *
 * Show a single checkbox.
 *
 * @param path the name of the field to bind to
 * @param attributes any additional attributes for the element
 *    (such as class or CSS styles or size)
-->
<#macro formCheckbox path attributes="">
	<@bind path />
    <#assign id="${status.expression?replace('[','')?replace(']','')}">
    <#assign isSelected = status.value?? && status.value?string=="true">
	<input type="hidden" name="_${status.expression}" value="on"/>
	<input type="checkbox" id="${id}" name="${status.expression}"<#if isSelected> checked="checked"</#if> ${attributes?no_esc}/>
</#macro>

<#--
 * showErrors
 *
 * Show validation errors for the currently bound field, with
 * optional style attributes.
 *
 * @param separator the HTML tag or other character list that should be used to
 *    separate each option (typically '&nbsp;' or '<br>')
 * @param classOrStyle either the name of a CSS class element (which is defined in
 *    the template or an external CSS file) or an inline style. If the value passed
 *    in here contains a colon (:) then a 'style=' attribute will be used,
 *    otherwise a 'class=' attribute will be used.
-->
<#macro showErrors separator classOrStyle="">
    <#list status.errorMessages as error>
    <#if classOrStyle == "">
        <b>${error}</b>
    <#else>
        <#if classOrStyle?index_of(":") == -1><#assign attr="class"><#else><#assign attr="style"></#if>
        <span ${attr}="${classOrStyle}">${error}</span>
    </#if>
    <#if error_has_next>${separator?no_esc}</#if>
    </#list>
</#macro>

<#--
 * checkSelected
 *
 * Check a value in a list to see if it is the currently selected value.
 * If so, add the 'selected="selected"' text to the output.
 * Handles values of numeric and string types.
 * This function is used internally but can be accessed by user code if required.
 *
 * @param value the current value in a list iteration
-->
<#macro checkSelected value>
    <#if stringStatusValue?is_number && stringStatusValue == value?number>selected="selected"</#if>
    <#if stringStatusValue?is_string && stringStatusValue == value>selected="selected"</#if>
</#macro>

<#--
 * contains
 *
 * Macro to return true if the list contains the scalar, false if not.
 * Surprisingly not a FreeMarker builtin.
 * This function is used internally but can be accessed by user code if required.
 *
 * @param list the list to search for the item
 * @param item the item to search for in the list
 * @return true if item is found in the list, false otherwise
-->
<#function contains list item>
    <#list list as nextInList>
    <#if nextInList == item><#return true></#if>
    </#list>
    <#return false>
</#function>

<#--
 * closeTag
 *
 * Simple macro to close an HTML tag that has no body with '>' or '/>',
 * depending on the value of a 'xhtmlCompliant' variable in the namespace
 * of this library.
-->
<#macro closeTag>
    <#if xhtmlCompliant?exists && xhtmlCompliant>/><#else>></#if>
</#macro>
