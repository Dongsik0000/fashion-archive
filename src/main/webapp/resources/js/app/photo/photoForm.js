App.photoForm = (function () {
    var m$ = {
            form:    document.getElementById('photoForm'),
            preview: document.getElementById('photoPreview'),
            file:    document.querySelector('#photoForm input[name=file]')   // 등록 모드에만 존재
        },

        settings = {
            mode: document.getElementById('photoForm').dataset.mode,   // 'new' | 'edit'
            id: document.getElementById('photoForm').dataset.id,
            maxBytes: 10 * 1024 * 1024,   // web.xml의 max-file-size와 같은 값
            submitting: false
        },

        url = {
            detail: contextPath + '/api/photos/detail',
            create: contextPath + '/api/admin/photos',
            update: contextPath + '/api/admin/photos/' + document.getElementById('photoForm').dataset.id
        },

        init = function () {
            bindEvent();
            if (settings.mode === 'edit') {
                loadPhoto();
            }
        },

        bindEvent = function () {
            m$.form.addEventListener('submit', function (e) {
                e.preventDefault();
                submitPhoto();
            });
            if (m$.file) {
                m$.file.addEventListener('change', previewFile);
            }
        },

        // 고른 파일을 바로 보여주고, 서버에서 거절될 크기는 여기서 미리 알려준다
        previewFile = function () {
            var file = m$.file.files[0];
            if (!file) {
                m$.preview.hidden = true;
                return;
            }
            if (file.size > settings.maxBytes) {
                App.error('알림', '10MB 이하의 사진만 올릴 수 있어요. (' + (file.size / 1024 / 1024).toFixed(1) + 'MB)');
                m$.file.value = '';
                m$.preview.hidden = true;
                return;
            }
            m$.preview.src = URL.createObjectURL(file);
            m$.preview.hidden = false;
        },

        // 수정 모드: 기존 값을 폼에 채운다
        loadPhoto = function () {
            App.post(url.detail, { id: settings.id })
                .then(function (res) {
                    if (res.code !== '00') {
                        App.error('불러오기 실패', res.message);
                        return;
                    }
                    var photo = res.data.photo,
                        ids = res.data.categoryIds || [];

                    m$.preview.src = photo.image_url;
                    m$.form.elements.title.value = photo.title;
                    m$.form.elements.memo.value = photo.memo || '';
                    m$.form.querySelectorAll('input[name=categoryIds]').forEach(function (cb) {
                        cb.checked = ids.indexOf(Number(cb.value)) !== -1;
                    });
                })
                .catch(function () {});
        },

        submitPhoto = function () {
            if (settings.submitting) return;

            var fd = new FormData(m$.form);

            if (App.isEmpty(fd.get('title').trim())) {
                App.error('알림', '제목을 입력해주세요.');
                return;
            }
            if (fd.getAll('categoryIds').length === 0) {
                App.error('알림', '카테고리를 하나 이상 선택해주세요.');
                return;
            }
            if (settings.mode === 'new' && !(fd.get('file') && fd.get('file').size > 0)) {
                App.error('알림', '사진 파일을 선택해주세요.');
                return;
            }
            if (settings.mode === 'new' && fd.get('file').size > settings.maxBytes) {
                App.error('알림', '10MB 이하의 사진만 올릴 수 있어요.');
                return;
            }

            settings.submitting = true;

            // 등록은 파일이 있어 multipart, 수정은 JSON
            var request = settings.mode === 'new'
                ? App.upload(url.create, fd)
                : App.post(url.update, {
                    title: fd.get('title'),
                    memo: fd.get('memo'),
                    categoryIds: fd.getAll('categoryIds')
                });

            request
                .then(handleSubmitResult)
                .catch(function () {})
                .then(function () { settings.submitting = false; });
        },

        handleSubmitResult = function (res) {
            if (res.code !== '00') {
                App.error('저장 실패', res.message);
                return;
            }
            var id = settings.mode === 'new' ? res.data.id : settings.id;
            location.href = contextPath + '/photos/' + id;
        };

    return {
        init: init
    };
}());

document.addEventListener('DOMContentLoaded', function () {
    App.photoForm.init();
});
