<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/common/taglib.jsp"%>
<%--
  모델: pageTitle(String), currentSlug(String|null), photos(List<Map>: id, title, image_url, created_at)
--%>
<header class="list-head">
    <h1 class="list-title"><c:out value="${pageTitle}"/></h1>
    <c:if test="${not empty photos}">
        <p class="list-count">${fn:length(photos)}장</p>
    </c:if>
</header>

<c:choose>
    <c:when test="${empty photos}">
        <p class="empty">
            아직 올린 사진이 없어요.
            <c:if test="${not empty sessionScope.loginId}">
                <a href="<c:url value='/admin/photos/new'/>">첫 사진 올리기</a>
            </c:if>
        </p>
    </c:when>
    <c:otherwise>
        <ul class="photo-grid">
            <c:forEach var="p" items="${photos}">
                <li>
                    <a href="<c:url value='/photos/${p.id}'/>">
                        <figure>
                            <img src="<c:out value='${p.image_url}'/>" alt="<c:out value='${p.title}'/>" loading="lazy">
                            <figcaption>
                                <span class="photo-title"><c:out value="${p.title}"/></span>
                                <time class="photo-date" datetime="<fmt:formatDate value='${p.created_at}' pattern='yyyy-MM-dd'/>"><fmt:formatDate value="${p.created_at}" pattern="yyyy.MM"/></time>
                            </figcaption>
                        </figure>
                    </a>
                </li>
            </c:forEach>
        </ul>
    </c:otherwise>
</c:choose>
