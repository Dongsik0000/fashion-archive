App.signup = (function () {
    var m$ = {
            signupForm: document.getElementById('signupForm')
        },

        settings = {
            submitting: false
        },

        url = {
            signup: contextPath + '/api/signup'
        },

        init = function () {
            m$.signupForm.addEventListener('submit', function (e) {
                e.preventDefault();
                submitSignup();
            });
        },

        submitSignup = function () {
            if (settings.submitting) return;                    // 이미 요청 중이거나 가입이 끝났으면 무시

            var param = App.formToObject(m$.signupForm);        // {loginId, password, passwordConfirm, signupCode}
            if (param.password !== param.passwordConfirm) {
                App.error('알림', '비밀번호 확인이 일치하지 않습니다.');
                return;
            }

            settings.submitting = true;
            App.post(url.signup, param)
                .then(function (res) {
                    if (res.code === '00') {
                        // 이동하는 동안 다시 제출되지 않도록 submitting을 풀지 않는다
                        App.toast('가입했어요. 로그인 화면으로 이동합니다.');
                        setTimeout(function () { location.href = contextPath + '/login'; }, 900);
                        return;
                    }
                    settings.submitting = false;
                    App.error('가입 실패', res.message);
                })
                .catch(function () { settings.submitting = false; });
        };

    return {
        init: init
    };
}());

document.addEventListener('DOMContentLoaded', function () {
    App.signup.init();
});
