<%@ include file="/WEB-INF/jsp/includes.jsp" %>
<%@ include file="/WEB-INF/jsp/header.jsp" %>

<h2><c:if test="${pet.new}">New </c:if>Pet</h2>

<b>Owner:</b> ${pet.owner.firstName} ${pet.owner.lastName}
<br/>

<form:form modelAttribute="pet">
  <table>
    <tr>
      <th>
        Name: <form:errors path="name" cssClass="errors"/>
        <br/>
        <form:input path="name" size="30" maxlength="30"/>
      </th>
    </tr>
    <tr>
      <th>
        Birth Date: <form:errors path="birthDate" cssClass="errors"/>
        <br/>
        <form:input path="birthDate" size="10" maxlength="10"/> (yyyy-mm-dd)
      </th>
    </tr>
    <tr>
      <th>
        Type: <form:errors path="type" cssClass="errors"/>
        <br/>
        <form:select path="type" items="${types}"/>
      </th>
    </tr>
    <tr>
      <td>
        <c:choose>
          <c:when test="${pet.new}">
            <p class="submit"><input type="submit" value="Add Pet"/></p>
          </c:when>
          <c:otherwise>
            <p class="submit"><input type="submit" value="Update Pet"/></p>
          </c:otherwise>
        </c:choose>
      </td>
    </tr>
  </table>
</form:form>

<%@ include file="/WEB-INF/jsp/footer.jsp" %>
