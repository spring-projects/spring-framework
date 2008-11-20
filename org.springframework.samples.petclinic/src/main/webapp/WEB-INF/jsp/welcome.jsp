<%@ include file="/WEB-INF/jsp/includes.jsp" %>
<%@ include file="/WEB-INF/jsp/header.jsp" %>

<img src="<spring:url value="/images/pets.png"/>" align="right" style="position:relative;right:30px;">
<h2><fmt:message key="welcome"/></h2>

<ul>
  <li><a href="<spring:url value="/clinic/owners/form"/>">Find owner</a></li>
  <li><a href="<spring:url value="/clinic/vets"/>">Display all veterinarians</a></li>
  <li><a href="<spring:url value="/html/petclinic.html"/>">Tutorial</a></li>
</ul>

<p>&nbsp;</p>

<%@ include file="/WEB-INF/jsp/footer.jsp" %>
