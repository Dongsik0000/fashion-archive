<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles"%>
<%@ include file="/common/taglib.jsp"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Fashion Archive</title>
    <link rel="stylesheet" href="<c:url value='/resources/css/reset.css'/>">
    <link rel="stylesheet" href="<c:url value='/resources/css/fashion.css'/>">
    <script>var contextPath = '${pageContext.request.contextPath}';</script>
    <script src="<c:url value='/resources/js/common/common.js'/>"></script>
</head>
<body>
<header class="site-header">
    <div class="inner">
        <a class="site-title" href="<c:url value='/'/>">Fashion Archive</a>
        <nav class="site-nav">
            <c:forEach var="cat" items="${categories}">
                <a href="<c:url value='/c/${cat.slug}'/>" class="${cat.slug eq currentSlug ? 'active' : ''}"><c:out value="${cat.name}"/></a>
            </c:forEach>
        </nav>
        <div class="site-account">
            <c:choose>
                <c:when test="${not empty sessionScope.loginId}">
                    <a href="<c:url value='/admin/photos/new'/>">사진 등록</a>
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
