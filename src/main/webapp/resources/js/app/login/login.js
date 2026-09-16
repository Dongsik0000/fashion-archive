App.login = (function () {
    var m$ = {

            loginForm: document.getElementById('loginForm'),
            useridInput: document.getElementById('user_id'),
            passwordInput: document.getElementById('user_pw')

        },

        settings = {
            submitting: false,
        },

        init = function () {
            bindEvent();
        },

        url = {
            login: contextPath + '/api/login',
        },

        bindEvent = function () {
            m$.loginForm.addEventListener('submit', function (e) {
                e.preventDefault();
                submitLogin();
            });

        },

        submitLogin = function () {
            if (settings.submitting) return;                    // 이미 요청 중이면 무시

            var param = App.formToObject(m$.loginForm);         // {loginId: '...', password: '...'}
            if (App.isEmpty(m$.useridInput.value.trim()) || App.isEmpty(m$.passwordInput.value.trim())) {
                App.error('알림', '아이디와 비밀번호를 입력해주세요.');
                return;
            }

            settings.submitting = true;
            App.post(url.login, param)
                .then(handleLoginResult)
                .catch(function () {})
                .then(function () { settings.submitting = false; });
        },

        handleLoginResult = function (res) {
            if (res.code === '00') {
                location.href = contextPath + nextPath();
            } else {
                App.error('로그인 실패', res.message);
            }
        },

        // ?next=/photos/4 처럼 이 사이트 안의 경로만 허용한다 ("//evil.com" 같은 외부 이동 차단)
        nextPath = function () {
            var next = new URLSearchParams(location.search).get('next') || '';
            return /^\/(?!\/)/.test(next) ? next : '/';
        };

    return {
        init: init
    };
}());

document.addEventListener('DOMContentLoaded', function(){
    App.login.init();
});