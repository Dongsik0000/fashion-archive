<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/common/taglib.jsp"%>
<%--
  모델: mode("new"|"edit"), photo(Map, edit일 때만: id, title, memo, image_url), categoryIds(List<Integer>)
  categories(전체 카테고리)는 GlobalModelAdvice가 넣는다.
--%>
<h1 class="form-title">${mode eq 'new' ? '사진 올리기' : '사진 정보 수정'}</h1>

<form id="photoForm" class="photo-form" data-mode="${mode}" data-id="${photo.id}">
    <c:choose>
        <c:when test="${mode eq 'new'}">
            <label class="field">
                <span>사진 (jpg, png, webp / 10MB 이하)</span>
                <input type="file" name="file" accept="image/jpeg,image/png,image/webp" required>
            </label>
        </c:when>
        <c:otherwise>
            <img class="photo-form-preview" src="<c:out value='${photo.image_url}'/>" alt="">
        </c:otherwise>
    </c:choose>

    <label class="field">
        <span>제목</span>
        <input type="text" name="title" maxlength="100" required value="<c:out value='${photo.title}'/>" placeholder="예: 린넨 셔츠와 로퍼">
    </label>

    <label class="field">
        <span>메모 (선택)</span>
        <textarea name="memo" rows="4" maxlength="2000" placeholder="어디서 입었는지, 어떤 조합인지"><c:out value="${photo.memo}"/></textarea>
    </label>

    <fieldset class="category-check">
        <legend>카테고리 (하나 이상)</legend>
        <div class="chips">
            <c:forEach var="cat" items="${categories}">
                <label class="chip-check" data-season="${cat.slug}">
                    <input type="checkbox" name="categoryIds" value="${cat.id}"
                        <c:forEach var="cid" items="${categoryIds}"><c:if test="${cid eq cat.id}">checked</c:if></c:forEach>>
                    <c:out value="${cat.name}"/>
                </label>
            </c:forEach>
        </div>
    </fieldset>

    <div class="form-actions">
        <button type="submit" class="btn btn-primary">${mode eq 'new' ? '올리기' : '저장'}</button>
        <c:choose>
            <c:when test="${mode eq 'new'}"><a class="btn" href="<c:url value='/'/>">취소</a></c:when>
            <c:otherwise><a class="btn" href="<c:url value='/photos/${photo.id}'/>">취소</a></c:otherwise>
        </c:choose>
    </div>
</form>

<script defer src="<c:url value='/resources/js/app/photo/photoForm.js'/>"></script>
