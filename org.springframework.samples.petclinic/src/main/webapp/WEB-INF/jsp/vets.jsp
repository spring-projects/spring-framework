<%@ include file="/WEB-INF/jsp/includes.jsp" %>
<%@ include file="/WEB-INF/jsp/header.jsp" %>

<h2>Veterinarians:</h2>

<table>
  <tr>
  <thead>
    <th>Name</th>
    <th>Specialties</th>
  </thead>
  </tr>
  <c:forEach var="vet" items="${vetList}">
    <tr>
      <td>${vet.firstName} ${vet.lastName}</td>
      <td>
	    <c:forEach var="specialty" items="${vet.specialties}">
          ${specialty.name}
        </c:forEach>
        <c:if test="${vet.nrOfSpecialties == 0}">none</c:if>
      </td>
    </tr>
  </c:forEach>
</table>

<%@ include file="/WEB-INF/jsp/footer.jsp" %>
