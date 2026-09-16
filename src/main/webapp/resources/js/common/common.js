// 전역 네임스페이스. 화면별 js는 App.xxx = (function(){ ... return {init:...}; })(); 로 등록한다.
var App = window.App || {};

// App.toast(message, type): 화면 하단에 잠깐 떠 있다 사라지는 알림. type: 'info' | 'error'
App.toast = function (message, type) {
    var el = document.getElementById('appToast');
    if (!el) {
        el = document.createElement('div');
        el.id = 'appToast';
        el.className = 'toast';
        el.setAttribute('role', 'status');
        el.setAttribute('aria-live', 'polite');
        document.body.appendChild(el);
    }
    clearTimeout(el._timer);
    el.textContent = message;
    el.className = 'toast is-visible' + (type === 'error' ? ' is-error' : '');
    el._timer = setTimeout(function () { el.className = 'toast'; }, 3200);
};

App.error = function (title, message) {
    App.toast(message || title, 'error');
};

App.sessionExpired = function () {
    App.toast('로그인이 필요합니다. 로그인 화면으로 이동합니다.', 'error');
    setTimeout(function () {
        window.location.href = contextPath + '/login?next=' + encodeURIComponent(location.pathname + location.search);
    }, 900);
    return new Promise(function () {});   // 체인 중단
};

App._handle = function (fetchPromise, opts) {
    return fetchPromise
        .then(function (res) {
            if (!res.ok) throw new Error('HTTP ' + res.status);
            return res.json();
        })
        .then(function (data) {
            if (data && data.sessionExpired) return App.sessionExpired();
            return data;
        })
        .catch(function (err) {
            if (typeof opts.onError === 'function') opts.onError(err);
            else App.error('오류', '요청 처리 중 문제가 발생했습니다.');
            throw err;
        });
};

// App.ajax(url, data, opts): JSON 요청/응답. X-Requested-With 헤더는 인터셉터의 Ajax 판별 + CSRF 방어에 쓰인다.
App.ajax = function (url, data, opts) {
    opts = opts || {};
    var method = (opts.method || 'POST').toUpperCase();
    var fetchOpt = {
        method: method,
        headers: {
            'Content-Type': 'application/json; charset=UTF-8',
            'X-Requested-With': 'XMLHttpRequest'
        },
        credentials: 'same-origin'
    };
    if (method === 'GET') {
        if (data) url += (url.indexOf('?') === -1 ? '?' : '&') + new URLSearchParams(data).toString();
    } else {
        fetchOpt.body = JSON.stringify(data || {});
    }
    return App._handle(fetch(url, fetchOpt), opts);
};
App.get = function (url, data, opts) { return App.ajax(url, data, Object.assign({}, opts, { method: 'GET' })); };
App.post = function (url, data, opts) { return App.ajax(url, data, Object.assign({}, opts, { method: 'POST' })); };

// App.upload(url, formData): multipart 전송. Content-Type은 브라우저가 boundary와 함께 붙이므로 지정하지 않는다.
App.upload = function (url, formData, opts) {
    opts = opts || {};
    return App._handle(fetch(url, {
        method: 'POST',
        headers: { 'X-Requested-With': 'XMLHttpRequest' },
        credentials: 'same-origin',
        body: formData
    }), opts);
};

App.isEmpty = function (v) {
    if (v === null || v === undefined) return true;
    if (typeof v === 'string' || Array.isArray(v)) return v.length === 0;
    if (typeof v === 'object') return Object.keys(v).length === 0;
    return false;
};

// App.formToObject(form): <form> -> {name: value}. 같은 name이 여러 개면 배열.
App.formToObject = function (form) {
    var obj = {};
    new FormData(form).forEach(function (value, key) {
        if (obj[key] === undefined) obj[key] = value;
        else if (Array.isArray(obj[key])) obj[key].push(value);
        else obj[key] = [obj[key], value];
    });
    return obj;
};
