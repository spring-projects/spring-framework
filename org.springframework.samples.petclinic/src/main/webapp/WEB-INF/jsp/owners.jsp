<%@ include file="/WEB-INF/jsp/includes.jsp" %>
<%@ include file="/WEB-INF/jsp/header.jsp" %>

<h2>Owners:</h2>

<table>
  <tr>
  <thead>
    <th>Name</th>
    <th>Address</th>
    <th>City</th>
    <th>Telephone</th>
    <th>Pets</th>
  </thead>
  </tr>
  <c:forEach var="owner" items="${selections}">
    <tr>
      <td>
          <a href="owner.do?ownerId=${owner.id}">${owner.firstName} ${owner.lastName}</a>
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
