<%@ include file="/WEB-INF/jsp/includes.jsp" %>
<%@ include file="/WEB-INF/jsp/header.jsp" %>

<h2>Find Owners:</h2>

<form:form modelAttribute="owner">
  <table>
    <tr>
      <th>
        Last Name: <form:errors path="*" cssClass="errors"/>
        <br/> 
        <form:input path="lastName" size="30" maxlength="80"/>
      </th>
    </tr>
    <tr>
      <td><p class="submit"><input type="submit" value="Find Owners"/></p></td>
    </tr>
  </table>
</form:form>

<br/>
<a href="<c:url value="/addOwner.do"/>">Add Owner</a>

<%@ include file="/WEB-INF/jsp/footer.jsp" %>
