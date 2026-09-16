App.photoDetail = (function () {
    var m$ = {
            article:    document.getElementById('photoDetail'),
            image:      document.getElementById('photoImage'),
            categories: document.getElementById('photoCategories'),
            title:      document.getElementById('photoTitle'),
            date:       document.getElementById('photoDate'),
            memo:       document.getElementById('photoMemo'),
            groups:     document.getElementById('linkGroups'),
            labels:     document.getElementById('labelOptions'),
            linkEmpty:  document.getElementById('linkEmpty'),
            error:      document.getElementById('detailError'),
            deleteBtn:  document.getElementById('deletePhotoBtn'),   // 관리자만 존재
            addForm:    document.getElementById('linkAddForm')       // 관리자만 존재
        },

        settings = {
            photoId: document.getElementById('photoDetail').dataset.id,
            submitting: false
        },

        url = {
            detail:      contextPath + '/api/photos/detail',
            deletePhoto: contextPath + '/api/admin/photos/' + document.getElementById('photoDetail').dataset.id + '/delete',
            addLink:     contextPath + '/api/admin/photos/' + document.getElementById('photoDetail').dataset.id + '/links',
            link:        function (id) { return contextPath + '/api/admin/links/' + id; }
        },

        init = function () {
            bindEvent();
            load();
        },

        bindEvent = function () {
            if (m$.deleteBtn) {
                m$.deleteBtn.addEventListener('click', deletePhoto);
            }
            if (m$.addForm) {
                m$.addForm.addEventListener('submit', function (e) {
                    e.preventDefault();
                    send(url.addLink, App.formToObject(m$.addForm));
                });
            }
        },

        load = function () {
            App.post(url.detail, { id: settings.photoId })
                .then(function (res) {
                    if (res.code !== '00') {
                        m$.error.hidden = false;
                        return;
                    }
                    render(res.data);
                })
                .catch(function () {});
        },

        /* ---------- 렌더링. 서버 값은 textContent/속성으로만 넣는다 ---------- */

        render = function (data) {
            var photo = data.photo;

            m$.image.src = photo.image_url;
            m$.image.alt = photo.title;
            m$.title.textContent = photo.title;
            m$.date.textContent = formatDate(photo.created_at);
            document.title = photo.title + ' - Fashion Archive';

            if (photo.memo) {
                m$.memo.textContent = photo.memo;
                m$.memo.hidden = false;
            }

            renderCategories(data.categoryIds || []);
            renderLinkGroups(data.linkGroups || {});
            m$.article.hidden = false;
        },

        // 레이아웃이 내려준 categories(전역)에서 이 사진의 카테고리만 골라 계절색 점과 함께 표시
        renderCategories = function (ids) {
            m$.categories.textContent = '';
            categories.forEach(function (cat) {
                if (ids.indexOf(cat.id) === -1) return;
                var a = document.createElement('a');
                a.href = contextPath + '/c/' + cat.slug;
                a.dataset.season = cat.slug;
                a.textContent = cat.name;
                m$.categories.appendChild(a);
            });
        },

        // linkGroups: { "아우터": [link, ...], "바지": [...] } — 서버가 LinkedHashMap이라 순서가 유지된다
        renderLinkGroups = function (groups) {
            var labels = Object.keys(groups);
            m$.groups.textContent = '';
            m$.labels.textContent = '';
            m$.linkEmpty.hidden = labels.length > 0 || isAdmin;

            labels.forEach(function (label) {
                var links = groups[label],
                    h2 = document.createElement('h2'),
                    small = document.createElement('small'),
                    ul = document.createElement('ul'),
                    option = document.createElement('option');

                h2.className = 'link-label';
                h2.textContent = label + ' ';
                small.textContent = '비슷한 제품 ' + links.length;
                h2.appendChild(small);

                ul.className = 'link-list';
                links.forEach(function (link) {
                    var li = document.createElement('li');
                    li.appendChild(isAdmin ? linkEditForm(link) : linkRow(link));
                    ul.appendChild(li);
                });

                option.value = label;
                m$.labels.appendChild(option);
                m$.groups.appendChild(h2);
                m$.groups.appendChild(ul);
            });
        },

        // 방문자용: 제목 + 도메인. 화살표 대신 어디로 가는지를 보여준다
        linkRow = function (link) {
            var a = document.createElement('a'),
                name = document.createElement('span'),
                host = hostOf(link.url);

            a.className = 'link-row';
            a.href = link.url;
            a.target = '_blank';
            a.rel = 'noopener noreferrer';
            name.className = 'link-name';
            name.textContent = link.title || host;
            a.appendChild(name);

            if (link.title) {
                var hostEl = document.createElement('span');
                hostEl.className = 'link-host';
                hostEl.textContent = host;
                a.appendChild(hostEl);
            }
            return a;
        },

        // 관리자용: 인라인 수정 폼
        linkEditForm = function (link) {
            var form = document.createElement('form'),
                label = input('text', 'itemLabel', link.item_label, '아이템'),
                urlInput = input('url', 'url', link.url, 'https://'),
                title = input('text', 'title', link.title || '', '제품 이름 (선택)'),
                save = button('submit', 'btn btn-sm', '저장'),
                del = button('button', 'btn btn-sm btn-danger', '삭제');

            form.className = 'link-form link-edit';
            label.setAttribute('list', 'labelOptions');
            label.maxLength = 30;
            title.maxLength = 100;
            label.required = urlInput.required = true;

            form.appendChild(label);
            form.appendChild(urlInput);
            form.appendChild(title);
            form.appendChild(save);
            form.appendChild(del);

            form.addEventListener('submit', function (e) {
                e.preventDefault();
                send(url.link(link.id), App.formToObject(form));
            });
            del.addEventListener('click', function () {
                if (!confirm('이 링크를 삭제할까요?')) return;
                send(url.link(link.id) + '/delete');
            });
            return form;
        },

        /* ---------- 관리자 동작 ---------- */

        // 성공하면 다시 불러와 서버가 정한 그룹 순서를 그대로 쓴다
        send = function (target, data) {
            if (settings.submitting) return;
            settings.submitting = true;
            App.post(target, data)
                .then(function (res) {
                    if (res.code !== '00') {
                        App.error('실패', res.message);
                        return;
                    }
                    if (m$.addForm) m$.addForm.reset();
                    load();
                })
                .catch(function () {})
                .then(function () { settings.submitting = false; });
        },

        deletePhoto = function () {
            if (!confirm('이 사진과 링크를 모두 삭제할까요?')) return;
            App.post(url.deletePhoto)
                .then(function (res) {
                    if (res.code !== '00') {
                        App.error('삭제 실패', res.message);
                        return;
                    }
                    location.href = contextPath + '/';
                })
                .catch(function () {});
        },

        /* ---------- 유틸 ---------- */

        hostOf = function (value) {
            try {
                return new URL(value).hostname.replace(/^www\./, '');
            } catch (e) {
                return value;
            }
        },

        formatDate = function (value) {
            if (!value) return '';
            var d = new Date(value);
            return d.getFullYear() + '년 ' + (d.getMonth() + 1) + '월 ' + d.getDate() + '일';
        },

        input = function (type, name, value, placeholder) {
            var el = document.createElement('input');
            el.type = type;
            el.name = name;
            el.value = value;
            el.placeholder = placeholder;
            el.setAttribute('aria-label', placeholder);
            return el;
        },

        button = function (type, className, text) {
            var el = document.createElement('button');
            el.type = type;
            el.className = className;
            el.textContent = text;
            return el;
        };

    return {
        init: init
    };
}());

document.addEventListener('DOMContentLoaded', function () {
    App.photoDetail.init();
});
