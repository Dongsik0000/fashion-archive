<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles"%>
<%@ include file="/common/taglib.jsp"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><c:out value="${empty pageTitle ? 'Fashion Archive' : pageTitle.concat(' - Fashion Archive')}"/></title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Nanum+Myeongjo:wght@400;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/resources/css/reset.css'/>">
    <link rel="stylesheet" href="<c:url value='/resources/css/fashion.css'/>">
    <script>var contextPath = '${pageContext.request.contextPath}';</script>
    <script src="<c:url value='/resources/js/common/common.js'/>"></script>
</head>
<%-- data-season: 현재 카테고리 slug. CSS의 --season 색이 이 값으로 정해진다 --%>
<body data-season="<c:out value='${currentSlug}'/>">
<header class="site-header">
    <div class="inner">
        <a class="site-title" href="<c:url value='/'/>">Fashion Archive</a>
        <nav class="site-nav" aria-label="카테고리">
            <c:forEach var="cat" items="${categories}">
                <a href="<c:url value='/c/${cat.slug}'/>" data-season="${cat.slug}" class="${cat.slug eq currentSlug ? 'active' : ''}"
                   <c:if test="${cat.slug eq currentSlug}">aria-current="page"</c:if>><c:out value="${cat.name}"/></a>
            </c:forEach>
        </nav>
        <div class="site-account">
            <c:choose>
                <c:when test="${not empty sessionScope.loginId}">
                    <a href="<c:url value='/admin/photos/new'/>">사진 올리기</a>
                    <a href="<c:url value='/logout'/>">로그아웃</a>
                </c:when>
                <c:otherwise>
                    <a href="<c:url value='/login'/>">로그인</a>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</header>
<main class="site-main">
    <tiles:insertAttribute name="body" />
</main>
</body>
</html>
