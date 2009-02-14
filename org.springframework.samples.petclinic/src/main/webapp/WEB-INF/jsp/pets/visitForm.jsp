<%@ include file="/WEB-INF/jsp/includes.jsp" %>
<%@ include file="/WEB-INF/jsp/header.jsp" %>

<h2><c:if test="${visit.new}">New </c:if>Visit:</h2>

<form:form modelAttribute="visit">
  <b>Pet:</b>
  <table width="333">
    <thead>
      <th>Name</th>
      <th>Birth Date</th>
      <th>Type</th>
      <th>Owner</th>
    </thead>
    <tr>
      <td>${visit.pet.name}</td>
      <td><fmt:formatDate value="${visit.pet.birthDate}" pattern="yyyy-MM-dd"/></td>
      <td>${visit.pet.type.name}</td>
      <td>${visit.pet.owner.firstName} ${visit.pet.owner.lastName}</td>
    </tr>
  </table>

  <table width="333">
    <tr>
      <th>
        Date:
        <br/><form:errors path="date" cssClass="errors"/>
      </th>
      <td>
        <form:input path="date" size="10" maxlength="10"/> (yyyy-mm-dd)
      </td>
    <tr/>
    <tr>
      <th valign="top">
        Description:
        <br/><form:errors path="description" cssClass="errors"/>
      </th>
      <td>
        <form:textarea path="description" rows="10" cols="25"/>
      </td>
    </tr>
    <tr>
      <td colspan="2">
        <input type="hidden" name="petId" value="${visit.pet.id}"/>
        <p class="submit"><input type="submit" value="Add Visit"/></p>
      </td>
    </tr>
  </table>
</form:form>

<br/>
<b>Previous Visits:</b>
<table width="333">
  <tr>
    <th>Date</th>
    <th>Description</th>
  </tr>
  <c:forEach var="visit" items="${visit.pet.visits}">
    <c:if test="${!visit.new}">
      <tr>
        <td><fmt:formatDate value="${visit.date}" pattern="yyyy-MM-dd"/></td>
        <td>${visit.description}</td>
      </tr>
    </c:if>
  </c:forEach>
</table>

<%@ include file="/WEB-INF/jsp/footer.jsp" %>
