App.login = (function () {
    var form = document.getElementById('loginForm'),
        submitting = false;

    function init() {
        form.addEventListener('submit', function (e) {
            e.preventDefault();
            submit();
        });
    }

    function submit() {
        if (submitting) return;
        var param = App.formToObject(form);
        if (App.isEmpty(param.loginId) || App.isEmpty(param.password)) {
            App.error('알림', '아이디와 비밀번호를 입력해주세요.');
            return;
        }
        submitting = true;
        App.post(contextPath + '/api/login', param)
            .then(function (res) {
                if (res.code === '00') {
                    location.href = contextPath + '/';
                } else {
                    App.error('로그인 실패', res.message);
                }
            })
            .catch(function () {})
            .then(function () { submitting = false; });
    }

    return { init: init };
}());

document.addEventListener('DOMContentLoaded', App.login.init);
