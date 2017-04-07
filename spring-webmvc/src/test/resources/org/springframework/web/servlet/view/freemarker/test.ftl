<#--
test template for FreeMarker macro test class
-->
<#import "spring.ftl" as spring />
<#import "spring-form.ftl" as form />

NAME
${command.name}

AGE
${command.age}

MESSAGE
<@spring.message code="hello"/> <@spring.message code="world"/>

DEFAULTMESSAGE
<@spring.message code="no.such.code" text="hi"/> <@spring.messageText code="no.such.code" text="planet"/>

MESSAGEARGS
<@spring.message code="hello", args=msgArgs/>

MESSAGEARGSWITHDEFAULTMESSAGE
<@spring.messageArgsText code="no.such.code" args=msgArgs text="Hi"/>

THEME
<@spring.theme code="hello"/> <@spring.theme code="world"/>

DEFAULTTHEME
<@spring.theme code="no.such.code" text="hi!"/> <@spring.themeText code="no.such.code" text="planet!"/>

THEMEARGS
<@spring.theme code="hello" args=msgArgs/>

THEMEARGSWITHDEFAULTMESSAGE
<@spring.theme code="no.such.code" args=msgArgs text="Hi!"/>

URL
<@spring.url "/aftercontext.html"/>

URLPARAMS
<@spring.url relativeUrl="/aftercontext/{foo}?spam={spam}" foo="bar" spam="bucket"/>

FORM
<@form.form modelAttribute="command">
    FORM1
    <@form.input path="name" />

    FORM2
    <@form.input path="name" class="myCssClass" />

    FORM3
    <@form.textarea path="name" />

    FORM4
    <@form.textarea path="name" rows="10" cols="30" />

    FORM5
    <@form.select name="name" options=nameOptionMap />

    FORM6
    <@form.select name="spouses" options=nameOptionMap multiple="true" />

    FORM7
    <@form.radio path="name" value="1" />
    <@form.radio path="name" value="2" />
    <@form.radio path="name" value="3" />

    FORM8
    <@form.checkbox path="name" value="1" />
    <@form.checkbox path="name" value="2" />
    <@form.checkbox path="name" value="3" />

    FORM9
    <@form.password path="name" />

    FORM10
    <@form.hidden path="name" />

    FORM11
    <@form.input type="text" path="name" />

    FORM12
    <@form.input type="hidden" path="name" />

    FORM13
    <@form.input type="password" path="name" />

    FORM14
    <@form.select path="name">
        <@form.option value="first">First</@form.option>
        <@form.option value="second">Second</@form.option>
    </@form.select>

    FORM15
    <@form.checkbox path="name" value="" />

    FORM16
    <@form.checkbox path="jedi" value="" />

    FORM17
    <@form.input path="spouses[0].name" />

    FORM18
    <@form.checkbox path="spouses[0].jedi" value="" />
</@form.form>
