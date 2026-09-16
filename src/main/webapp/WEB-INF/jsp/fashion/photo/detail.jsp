<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/common/taglib.jsp"%>
<%--
  모델: photoId. 데이터는 detail.js가 POST /api/photos/detail 로 가져와 그린다. 로그인 여부는 레이아웃의 isAdmin(JS 전역)
  관리자도 기본은 읽기 화면. "편집"을 누르면 data-editing="true"가 되어 .edit-only 요소와 링크 수정 폼이 나타난다.
--%>
<article class="photo-detail" id="photoDetail" data-id="${photoId}" data-editing="false" hidden>
    <div class="photo-detail-image">
        <img id="photoImage" src="" alt="">
    </div>

    <div class="photo-detail-body">
        <div class="detail-top">
            <p class="photo-categories" id="photoCategories"></p>
            <c:if test="${not empty sessionScope.loginId}">
                <button type="button" class="text-btn edit-toggle" id="editToggle" aria-pressed="false">편집</button>
            </c:if>
        </div>

        <h1 class="detail-title" id="photoTitle"></h1>
        <p class="detail-date" id="photoDate"></p>

        <c:if test="${not empty sessionScope.loginId}">
            <p class="admin-actions edit-only">
                <a class="text-btn" href="<c:url value='/admin/photos/${photoId}/edit'/>">제목·메모·카테고리 수정</a>
                <button type="button" class="text-btn is-danger" id="deletePhotoBtn">사진 삭제</button>
            </p>
        </c:if>

        <p class="photo-memo" id="photoMemo" hidden></p>

        <section class="link-groups" aria-label="비슷한 제품">
            <p class="empty" id="linkEmpty" hidden>아직 연결된 제품이 없어요.</p>
            <datalist id="labelOptions"></datalist>
            <div id="linkGroups"></div>

            <c:if test="${not empty sessionScope.loginId}">
                <div class="link-add edit-only">
                    <h2 class="link-label">링크 추가 <small>사진 속 아이템과 비슷한 제품</small></h2>
                    <form class="link-form" id="linkAddForm">
                        <input type="text" name="itemLabel" list="labelOptions" maxlength="30" placeholder="아이템 (예: 아우터)" aria-label="아이템" required>
                        <input type="url" name="url" placeholder="https://" aria-label="URL" required>
                        <input type="text" name="title" maxlength="100" placeholder="제품 이름 (선택)" aria-label="제품 이름">
                        <button type="submit" class="btn btn-sm btn-primary">추가</button>
                        <textarea name="note" rows="2" maxlength="500" placeholder="왜 이 제품인지 (선택) — 예: 소매를 접으면 사진이랑 같은 실루엣" aria-label="메모"></textarea>
                    </form>
                </div>
            </c:if>
        </section>
    </div>
</article>

<p class="empty" id="detailError" hidden>사진을 찾을 수 없어요. <a href="<c:url value='/'/>">홈으로</a></p>

<script defer src="<c:url value='/resources/js/app/photo/detail.js'/>?v=${applicationScope.assetVersion}"></script>
