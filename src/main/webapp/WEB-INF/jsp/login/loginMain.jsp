<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/common/taglib.jsp"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>로그인 - Fashion Archive</title>
    <link rel="stylesheet" href="<c:url value='/resources/css/reset.css'/>">
    <link rel="stylesheet" href="<c:url value='/resources/css/fashion.css'/>">
    <script>var contextPath = '${pageContext.request.contextPath}';</script>
    <script src="<c:url value='/resources/js/common/common.js'/>"></script>
    <script defer src="<c:url value='/resources/js/app/login/login.js'/>"></script>
</head>
<body class="login-page">
<main class="login-box">
    <h1 class="site-title">Fashion Archive</h1>
    <form id="loginForm" autocomplete="off">
        <label>
            <span>아이디</span>
            <input type="text" name="loginId" required autofocus>
        </label>
        <label>
            <span>비밀번호</span>
            <input type="password" name="password" required>
        </label>
        <button type="submit" class="btn btn-primary">로그인</button>
    </form>
    <p><a href="<c:url value='/'/>">홈으로</a></p>
</main>
</body>
</html>
