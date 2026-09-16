<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true"%>
<%@ include file="/common/taglib.jsp"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>오류 - Fashion Archive</title>
    <link rel="stylesheet" href="<c:url value='/resources/css/reset.css'/>?v=${applicationScope.assetVersion}">
    <link rel="stylesheet" href="<c:url value='/resources/css/fashion.css'/>?v=${applicationScope.assetVersion}">
</head>
<body>
<main class="site-main">
    <h1 class="page-title">
        <c:choose>
            <c:when test="${pageContext.errorData.statusCode == 404}">페이지를 찾을 수 없습니다.</c:when>
            <c:otherwise>문제가 발생했습니다.</c:otherwise>
        </c:choose>
    </h1>
    <p><a href="<c:url value='/'/>">홈으로</a></p>
</main>
</body>
</html>
