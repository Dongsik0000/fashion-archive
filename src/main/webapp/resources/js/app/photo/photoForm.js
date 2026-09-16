App.photoForm = (function () {
    var m$ = {
            form:    document.getElementById('photoForm'),
            preview: document.getElementById('photoPreview')      // 수정 모드에만 존재
        },

        settings = {
            mode: document.getElementById('photoForm').dataset.mode,   // 'new' | 'edit'
            id: document.getElementById('photoForm').dataset.id,
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
