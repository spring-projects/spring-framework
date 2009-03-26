<%@ include file="/WEB-INF/jsp/includes.jsp" %>
<%@ include file="/WEB-INF/jsp/header.jsp" %>

<%
Exception ex = (Exception) request.getAttribute("exception");
%>

<h2>Data access failure: <%= ex.getMessage() %></h2>
<p/>

<%
ex.printStackTrace(new java.io.PrintWriter(out));
%>

<p/>
<br/>
<a href="<spring:url value="/" htmlEscape="true" />">Home</a>

<%@ include file="/WEB-INF/jsp/footer.jsp" %>
