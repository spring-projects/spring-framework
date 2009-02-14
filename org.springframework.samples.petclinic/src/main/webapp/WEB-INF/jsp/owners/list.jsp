<%@ include file="/WEB-INF/jsp/includes.jsp" %>
<%@ include file="/WEB-INF/jsp/header.jsp" %>

<h2>Owners:</h2>

<table>
  <thead>
    <th>Name</th>
    <th>Address</th>
    <th>City</th>
    <th>Telephone</th>
    <th>Pets</th>
  </thead>
  <c:forEach var="owner" items="${selections}">
    <tr>
      <td>
          <spring:url value="owners/{ownerId}" var="ownerUrl">
              <spring:param name="ownerId" value="${owner.id}"/>
          </spring:url>
          <a href="${fn:escapeXml(ownerUrl)}">${owner.firstName} ${owner.lastName}</a>
      </td>
      <td>${owner.address}</td>
      <td>${owner.city}</td>
      <td>${owner.telephone}</td>
      <td>
        <c:forEach var="pet" items="${owner.pets}">
          ${pet.name} &nbsp;
        </c:forEach>
      </td>
    </tr>
  </c:forEach>
</table>

<%@ include file="/WEB-INF/jsp/footer.jsp" %>
