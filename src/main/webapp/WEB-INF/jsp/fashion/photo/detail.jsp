<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/common/taglib.jsp"%>
<%--
  모델: photo(Map: id, title, memo, image_url, created_at), categoryIds(List<Integer>),
        linkGroups(Map<String, List<Map: id, item_label, url, title>>), isAdmin(boolean)
  categories(전체 카테고리)는 GlobalModelAdvice가 넣는다.
--%>
<article class="photo-detail">
    <div class="photo-detail-image">
        <img src="<c:out value='${photo.image_url}'/>" alt="<c:out value='${photo.title}'/>">
    </div>

    <div class="photo-detail-body">
        <%-- 이 사진이 속한 카테고리. 각 링크는 자기 계절색 점을 단다 --%>
        <p class="photo-categories">
            <c:forEach var="cat" items="${categories}">
                <c:forEach var="cid" items="${categoryIds}">
                    <c:if test="${cid eq cat.id}">
                        <a href="<c:url value='/c/${cat.slug}'/>" data-season="${cat.slug}"><c:out value="${cat.name}"/></a>
                    </c:if>
                </c:forEach>
            </c:forEach>
        </p>

        <h1 class="detail-title"><c:out value="${photo.title}"/></h1>
        <p class="detail-date"><fmt:formatDate value="${photo.created_at}" pattern="yyyy년 M월 d일"/></p>

        <c:if test="${isAdmin}">
            <p class="admin-actions">
                <a class="text-btn" href="<c:url value='/admin/photos/${photo.id}/edit'/>">사진 정보 수정</a>
                <button type="button" class="text-btn is-danger" id="deletePhotoBtn" data-id="${photo.id}">사진 삭제</button>
            </p>
        </c:if>

        <c:if test="${not empty photo.memo}">
            <p class="photo-memo"><c:out value="${photo.memo}"/></p>
        </c:if>

        <section class="link-groups" aria-label="비슷한 제품">
            <c:if test="${empty linkGroups and not isAdmin}">
                <p class="empty">아직 연결된 제품이 없어요.</p>
            </c:if>

            <c:if test="${isAdmin}">
                <datalist id="labelOptions">
                    <c:forEach var="group" items="${linkGroups}"><option value="<c:out value='${group.key}'/>"></c:forEach>
                </datalist>
            </c:if>

            <c:forEach var="group" items="${linkGroups}">
                <h2 class="link-label"><c:out value="${group.key}"/> <small>비슷한 제품 ${fn:length(group.value)}</small></h2>
                <ul class="link-list">
                    <c:forEach var="link" items="${group.value}">
                        <li>
                            <c:choose>
                                <c:when test="${isAdmin}">
                                    <form class="link-form link-edit" data-id="${link.id}">
                                        <input type="text" name="itemLabel" list="labelOptions" maxlength="30" value="<c:out value='${link.item_label}'/>" placeholder="아이템" aria-label="아이템" required>
                                        <input type="url" name="url" value="<c:out value='${link.url}'/>" placeholder="https://" aria-label="URL" required>
                                        <input type="text" name="title" maxlength="100" value="<c:out value='${link.title}'/>" placeholder="제품 이름 (선택)" aria-label="제품 이름">
                                        <button type="submit" class="btn btn-sm">저장</button>
                                        <button type="button" class="btn btn-sm btn-danger link-delete">삭제</button>
                                    </form>
                                </c:when>
                                <c:otherwise>
                                    <%-- 화살표 대신 어디로 가는지(도메인)를 보여준다 --%>
                                    <c:set var="afterScheme" value="${fn:substringAfter(link.url, '://')}"/>
                                    <c:set var="host" value="${fn:contains(afterScheme, '/') ? fn:substringBefore(afterScheme, '/') : afterScheme}"/>
                                    <c:if test="${fn:startsWith(host, 'www.')}"><c:set var="host" value="${fn:substring(host, 4, -1)}"/></c:if>
                                    <a class="link-row" href="<c:out value='${link.url}'/>" target="_blank" rel="noopener noreferrer">
                                        <span class="link-name"><c:out value="${empty link.title ? host : link.title}"/></span>
                                        <c:if test="${not empty link.title}"><span class="link-host"><c:out value="${host}"/></span></c:if>
                                    </a>
                                </c:otherwise>
                            </c:choose>
                        </li>
                    </c:forEach>
                </ul>
            </c:forEach>

            <c:if test="${isAdmin}">
                <div class="link-add">
                    <h2 class="link-label">링크 추가 <small>사진 속 아이템과 비슷한 제품</small></h2>
                    <form class="link-form" id="linkAddForm" data-photo-id="${photo.id}">
                        <input type="text" name="itemLabel" list="labelOptions" maxlength="30" placeholder="아이템 (예: 아우터)" aria-label="아이템" required>
                        <input type="url" name="url" placeholder="https://" aria-label="URL" required>
                        <input type="text" name="title" maxlength="100" placeholder="제품 이름 (선택)" aria-label="제품 이름">
                        <button type="submit" class="btn btn-sm btn-primary">추가</button>
                    </form>
                </div>
            </c:if>
        </section>
    </div>
</article>

<c:if test="${isAdmin}">
    <script defer src="<c:url value='/resources/js/app/photo/detail.js'/>"></script>
</c:if>
