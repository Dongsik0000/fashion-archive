<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/common/taglib.jsp"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>회원가입 - Fashion Archive</title>
    <link rel="stylesheet" href="<c:url value='/resources/css/reset.css'/>?v=${applicationScope.assetVersion}">
    <link rel="stylesheet" href="<c:url value='/resources/css/fashion.css'/>?v=${applicationScope.assetVersion}">
    <script>var contextPath = '${pageContext.request.contextPath}';</script>
    <script src="<c:url value='/resources/js/common/common.js'/>?v=${applicationScope.assetVersion}"></script>
    <script defer src="<c:url value='/resources/js/app/login/signup.js'/>?v=${applicationScope.assetVersion}"></script>
</head>
<body class="login-page">
<main class="login-box">
    <h1 class="site-title">Fashion Archive</h1>
    <form id="signupForm">
        <label>
            <span>아이디 (영문 소문자·숫자·_·- 3~30자)</span>
            <input type="text" name="loginId" minlength="3" maxlength="30" pattern="[a-z0-9_\-]{3,30}"
                   title="영문 소문자, 숫자, _, -로 3~30자" autocomplete="username" required autofocus>
        </label>
        <label>
            <span>비밀번호 (8자 이상)</span>
            <input type="password" name="password" minlength="8" autocomplete="new-password" required>
        </label>
        <label>
            <span>비밀번호 확인</span>
            <input type="password" name="passwordConfirm" minlength="8" autocomplete="new-password" required>
        </label>
        <label>
            <span>가입 코드</span>
            <input type="text" name="signupCode" autocomplete="off" aria-describedby="signupCodeHelp" required>
        </label>
        <p id="signupCodeHelp">전달받은 가입 코드가 있어야 가입할 수 있습니다.</p>
        <button type="submit" class="btn btn-primary">가입하기</button>
    </form>
    <p>이미 계정이 있으면 <a href="<c:url value='/login'/>">로그인</a></p>
</main>
</body>
</html>
