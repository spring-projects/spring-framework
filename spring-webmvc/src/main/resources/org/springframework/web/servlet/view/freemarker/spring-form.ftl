<#ftl output_format="HTML" strip_whitespace=true>
<#--
 * spring-form.ftl
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
 * @author Sascha Woo
 -->

<#--
 * form
 *
 * Render a form element in the scope of specified model attribute.
 *
 * @param modelAttribute name of the model attribute under which the form object is exposed.
 * @param method the method action attribute. Default is GET.
 * @param attributes additional attributes for the html element
 -->
<#macro form modelAttribute method="GET" attributes...>
    <#assign _bindModelAttribute = modelAttribute />
    <#assign _checkboxFieldPresentMarkers = [] />

    <#local attrs = {
        "id" : modelAttribute,
        "name" : modelAttribute,
        "method" : method
    } />

    <#list attributes as name, value>
        <#local attrs = attrs + {name : value} />
    </#list>

    <form${_outputAttributes(attrs)}>
        <#nested>
    </form>

    <#-- set to empty because unset a variable is not possible -->
    <#assign _bindModelAttribute = "" />
    <#assign _checkboxFieldPresentMarkers = [] />
</#macro>

<#--
 * input
 *
 * Render a form input field of the supplied type (default: text) and bind it to an attribute of a command or bean.
 *
 * @param path the name of the field to bind to
 * @param htmlEscape enable/disable HTML escaping of rendered values
 * @param type the html type attribute
 * @param id the html id attribute
 * @param class the html class attribute
 * @param cssClass the html class attribute
 * @param cssErrorClass the html class attribute if the field has associated errors
 * @param attributes additional attributes for the html element
 -->
<#macro input path htmlEscape="" type="text" id="" class="" cssClass=class cssErrorClass="" attributes...>
    <@_input path=path htmlEscape=htmlEscape type=type id=id cssClass=cssClass cssErrorClass=cssErrorClass attributes=attributes />
</#macro>

<#--
 * hidden
 *
 * Render a form input field of type 'hidden' and bind it to an attribute of a command or bean.
 *
 * @param path the name of the field to bind to
 * @param htmlEscape enable/disable HTML escaping of rendered values
 * @param id the html id attribute
 * @param class the html class attribute
 * @param cssClass the html class attribute
 * @param cssErrorClass the html class attribute if the field has associated errors
 * @param attributes additional attributes for the html element
 -->
<#macro hidden path htmlEscape="" id="" class="" cssClass=class cssErrorClass="" attributes...>
    <@_input path=path type="hidden" id=id cssClass=cssClass cssErrorClass=cssErrorClass attributes=attributes />
</#macro>

<#--
 * password
 *
 * Render a form input field of type 'password' and bind it to an attribute of a command or bean.
 *
 * @param path the name of the field to bind to
 * @param htmlEscape enable/disable HTML escaping of rendered values
 * @param id the html id attribute
 * @param class the html class attribute
 * @param cssClass the html class attribute
 * @param cssErrorClass the html class attribute if the field has associated errors
 * @param attributes additional attributes for the html element
 -->
<#macro password path htmlEscape="" id="" class="" cssClass=class cssErrorClass="" attributes...>
    <@_input path=path type="password" id=id cssClass=cssClass cssErrorClass=cssErrorClass attributes=attributes />
</#macro>

<#--
 * textarea
 *
 * Render a form textarea and bind it to an attribute of a command or bean.
 *
 * @param path the name of the field to bind to
 * @param htmlEscape enable/disable HTML escaping of rendered values
 * @param id the html id attribute
 * @param class the html class attribute
 * @param cssClass the html class attribute
 * @param cssErrorClass the html class attribute if the field has associated errors
 * @param attributes additional attributes for the html element
 -->
<#macro textarea path htmlEscape="" id="" class="" cssClass=class cssErrorClass="" attributes...>
    <@_bind path=_bindModelAttribute + "." + path>
        <#local id = _resolveId(id) />

        <#local attrs = {
            "class" : (_status.error && cssErrorClass?has_content)?then(cssErrorClass, cssClass)
        }>

        <#list attributes as name, value>
            <#local attrs = attrs + {name : value} />
        </#list>

        <#local valueDisplayString = htmlEscape?is_boolean?then(
            springMacroRequestContext.getDisplayString(_status, "textarea", htmlEscape),
            springMacroRequestContext.getDisplayString(_status, "textarea")) />

        <textarea id="${id}" name="${_status.expression}"${_outputAttributes(attrs)}>${valueDisplayString}</textarea>
    </@_bind>
</#macro>

<#--
 * radio
 *
 * Render a form input field of type 'radio' and bind it to an attribute of a command or bean.
 *
 * @param path the name of the field to bind to
 * @param value the value of the field
 * @param htmlEscape enable/disable HTML escaping of rendered values
 * @param id the html id attribute
 * @param class the html class attribute
 * @param cssClass the html class attribute
 * @param cssErrorClass the html class attribute if the field has associated errors
 * @param attributes additional attributes for the html element
-->
<#macro radio path value htmlEscape="" id="" class="" cssClass=class cssErrorClass="" attributes...>
    <@_bind path=_bindModelAttribute + "." + path>
        <#local id = _resolveId(id) />

        <#local attrs = {
            "class" : (_status.error && cssErrorClass?has_content)?then(cssErrorClass, cssClass)
        }>

        <#list attributes as name, value>
            <#local attrs = attrs + {name : value} />
        </#list>

        <#local valueDisplayString = htmlEscape?is_boolean?then(
            springMacroRequestContext.getDisplayString(value, _status.editor, htmlEscape),
            springMacroRequestContext.getDisplayString(value, _status.editor)) />

        <#local isSelected = springMacroRequestContext.isSelected(_status, value) />

        <input type="radio" id="${id}" name="${_status.expression}" value="${valueDisplayString}"${isSelected?then(' checked="checked"', '')}${_outputAttributes(attrs)} ${_terminateTag()}>
    </@_bind>
</#macro>

<#--
 * checkbox
 *
 * Render a form input field of type 'checkbox' and bind it to an attribute of a command or bean.
 *
 * @param path the name of the field to bind to
 * @param value the value of the field
 * @param htmlEscape enable/disable HTML escaping of rendered values
 * @param id the html id attribute
 * @param class the html class attribute
 * @param cssClass the html class attribute
 * @param cssErrorClass the html class attribute if the field has associated errors
 * @param attributes additional attributes for the html element
-->
<#macro checkbox path value htmlEscape="" id="" class="" cssClass=class cssErrorClass="" attributes...>
    <@_bind path=_bindModelAttribute + "." + path>
        <#local id = _resolveId(id) />

        <#local attrs = {
            "class" : (_status.error && cssErrorClass?has_content)?then(cssErrorClass, cssClass)
        }>

        <#list attributes as name, value>
            <#local attrs = attrs + {name : value} />
        </#list>

        <#local valueDisplayString = htmlEscape?is_boolean?then(
            springMacroRequestContext.getDisplayString(value, _status.editor, htmlEscape),
            springMacroRequestContext.getDisplayString(value, _status.editor)) />

        <#local isSelected = springMacroRequestContext.isSelected(_status, value) />

        <input type="checkbox" id="${id}" name="${_status.expression}" value="${valueDisplayString}"${isSelected?then(' checked="checked"', '')}${_outputAttributes(attrs)} ${_terminateTag()}>
        <#if !(attrs.disabled??)>
            <#-- write out 'field was present' marker -->
            <@_fieldPresentMarker />
        </#if>
    </@_bind>
</#macro>

<#--
 * select
 *
 * Render a form select field and bind it to an attribute of a command or bean.
 *
 * @param path the name of the field to bind to
 * @param htmlEscape enable/disable HTML escaping of rendered values
 * @param id the html id attribute
 * @param class the html class attribute
 * @param cssClass the html class attribute
 * @param cssErrorClass the html class attribute if the field has associated errors
 * @param attributes additional attributes for the html element
-->
<#macro select path htmlEscape="" id="" class="" cssClass=class cssErrorClass="" attributes...>
    <@_bind path=_bindModelAttribute + "." + path>
        <#assign _parentHtmlEscape = htmlEscape />
        <#local id = _resolveId(id) />

        <#local attrs = {
            "class" : (_status.error && cssErrorClass?has_content)?then(cssErrorClass, cssClass)
        }>

        <#list attributes as name, value>
            <#local attrs = attrs + {name : value} />
        </#list>

        <select id="${id}" name="${_status.expression}"${_outputAttributes(attrs)}>
            <#nested>
        </select>
        <#if attrs.multiple??>
            <#-- write out 'field was present' marker -->
            <@_fieldPresentMarker />
        </#if>
    </@_bind>
</#macro>

<#-- 
 * options
 * 
 * Renders a list of HTML 'option' tags. Sets 'selected' as appropriate based on bound value.
 *
 * @param items the collection, map or array of objects used to generate the inner 'option' tags
 * @param itemValue the name of the property mapped to 'value' attribute of the 'option' tag
 * @param itemLabel the name of the property mapped to the inner text of the 'option' tag
 -->
<#macro options items itemValue="" itemLabel="" htmlEscape="" attributes...>
    <#if items?is_hash>
        <#list items as key, value>
            <#local renderValue = itemValue?has_content?then(key[itemValue], key) />
            <#local renderLabel = itemLabel?has_content?then(value[itemLabel], value) />

            <@_option value=renderValue label=renderLabel htmlEscape=htmlEscape attributes=attributes />
        </#list>
    <#else>
        <#list items as value>
            <#local renderValue = itemValue?has_content?then(value[itemValue], value) />
            <#local renderLabel = itemLabel?has_content?then(value[itemLabel], value) />

            <@_option value=renderValue label=renderLabel htmlEscape=htmlEscape attributes=attributes />
        </#list>
    </#if>
</#macro>

<#--
 * option
 *
 * Render a from select option with the given attributes.
 *
 * @param value the value attribute of the option element
 * @param label the label text of the option element
 * @param htmlEscape enable/disable HTML escaping of rendered values
 * @param attributes additional attributes for the html element
-->
<#macro option value label="" htmlEscape="" attributes...>
    <#if !(label?has_content)>
        <#local nestedMarkup><#nested></#local>
        <#local label = nestedMarkup?markup_string />
    </#if>
    <@_option value=value label=label htmlEscape=htmlEscape attributes=attributes />
</#macro>

<#--
 * errors
 *
 * Expose errors for the given path to the nested block.
 *
 * If used inside a form the path can be omitted to expose global errors
 * or pointing to the nested object to expose the field errors.
 * Using the marco outside a form the path must point to the form to expose global errors
 * or poiting to the field object with leading form name.
 * 
 * @param path path to errors object for data binding
 * @param htmlEscape enable/disable HTML escaping of rendered values.
 -->
<#macro errors path="" htmlEscape="">
    <#if path?has_content>
        <#if _bindModelAttribute?? && _bindModelAttribute?has_content>
            <#local path = _bindModelAttribute + "." + path />
        </#if>
    <#else>
        <#local path = _bindModelAttribute>
    </#if>
    <#if htmlEscape?is_boolean>
        <#local status = springMacroRequestContext.getBindStatus(path, htmlEscape) />
    <#else>
        <#local status = springMacroRequestContext.getBindStatus(path) />
    </#if>
    <#if status??>
        <#nested status.errorMessages />
    </#if>
</#macro>

<#-- 
 * hasErrors
 *
 * The hasErrors function provides you with support for identifing if there are errors for an object.
 *
 * @param name The name of the bean in the request, that needs to be inspected for errors.
 * @return If errors are available for this bean, the function will return true otherwise false.
  -->
<#function hasErrors name>
    <#local errors = springMacroRequestContext.getErrors(_bindModelAttribute) />
    <#if errors?? && errors.hasErrors()>
        <#return errors.hasFieldErrors(name) />
    </#if>
    <#return false />
</#function>

<#macro _bind path>
    <#assign _status = springMacroRequestContext.getBindStatus(path, false) />
    <#nested>
    <#-- set to empty because unset a variable is not possible -->
    <#assign _status = "" />
</#macro>

<#macro _input path htmlEscape="" type="text" id="" class="" cssClass=class cssErrorClass="" attributes={}>
    <@_bind path=_bindModelAttribute + "." + path>
        <#local id = _resolveId(id) />

        <#local attrs = {
            "class" : (_status.error && cssErrorClass?has_content)?then(cssErrorClass, cssClass)
        } />

        <#list attributes as name, value>
            <#local attrs = attrs + {name : value} />
        </#list>

        <#local valueDisplayString = htmlEscape?is_boolean?then(
            springMacroRequestContext.getDisplayString(_status, type, htmlEscape),
            springMacroRequestContext.getDisplayString(_status, type)) />

        <input type="${type}" id="${id}" name="${_status.expression}" value="${valueDisplayString}"${_outputAttributes(attrs)} ${_terminateTag()}>
    </@_bind>
</#macro>

<#macro _option value label="" htmlEscape="" attributes={}>
    <#local attrs = {} />
    <#list attributes as name, value>
        <#local attrs = attrs + {name : value} />
    </#list>

    <#local valueDisplayString = htmlEscape?is_boolean?then(
        springMacroRequestContext.getDisplayString(value, _status.editor, htmlEscape),
        springMacroRequestContext.getDisplayString(value, _status.editor)) />
    <#local labelDisplayString = htmlEscape?is_boolean?then(
        springMacroRequestContext.getDisplayString(label, _status.editor, htmlEscape),
        springMacroRequestContext.getDisplayString(label, _status.editor)) />

    <#local isSelected = springMacroRequestContext.isSelected(_status, value) />

    <option value="${valueDisplayString}"${isSelected?then(' selected="selected"', '')}${_outputAttributes(attrs)}>${label}</option>
</#macro>

<#macro _fieldPresentMarker>
    <#local fieldNamePresentMarker = springMacroRequestContext.getFieldMarkerPrefix() + _status.expression />
    <#if !_checkboxFieldPresentMarkers?seq_contains(fieldNamePresentMarker)>
        <#assign _checkboxFieldPresentMarkers += [fieldNamePresentMarker] />
        <input type="hidden" name="${fieldNamePresentMarker}" value="${springMacroRequestContext.processFieldValue(fieldNamePresentMarker, "on", "checkbox")}" />
    </#if>
</#macro>

<#--
 * _terminateTag
 *
 * Simple private function to terminate empty elements with a slash '/' or without depending
 * on the value of 'xhtmlCompliant' variable in the namespace of this library.
-->
<#function _terminateTag>
    <#return (xhtmlCompliant?? && xhtmlCompliant)?then("/", "") />
</#function>

<#function _resolveId id>
    <#if id?has_content>
        <#return id />
    </#if>
    <#return _status.expression?replace('[','')?replace(']','') />
</#function>

<#function _outputAttributes attributes>
    <#local attributeString = "" />
    <#list attributes?keys as key>
        <#if attributes[key]?has_content>
            <#local attributeName = key?replace("_", "-") />
            <#local attributeString = attributeString + " ${attributeName}=\"${attributes[key]}\"" />
        </#if>
    </#list>
    <#return attributeString />
</#function>