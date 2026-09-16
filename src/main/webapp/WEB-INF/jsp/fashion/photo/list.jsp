<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/common/taglib.jsp"%>
<%-- 모델: currentSlug(카테고리 페이지일 때). 사진 데이터는 list.js가 POST /api/photos/list 로 가져와 그린다 --%>
<c:set var="pageTitle" value="최근 사진"/>
<c:forEach var="cat" items="${categories}"><c:if test="${cat.slug eq currentSlug}"><c:set var="pageTitle" value="${cat.name}"/></c:if></c:forEach>

<header class="list-head">
    <h1 class="list-title"><c:out value="${pageTitle}"/></h1>
    <p class="list-count" id="listCount"></p>
</header>

<ul class="photo-grid" id="photoGrid" data-slug="<c:out value='${currentSlug}'/>"></ul>

<p class="empty" id="listEmpty" hidden>
    아직 올린 사진이 없어요.
    <c:if test="${not empty sessionScope.loginId}">
        <a href="<c:url value='/admin/photos/new'/>">첫 사진 올리기</a>
    </c:if>
</p>

<script defer src="<c:url value='/resources/js/app/photo/list.js'/>"></script>
