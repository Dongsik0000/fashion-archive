App.photoList = (function () {
    var m$ = {
            grid:  document.getElementById('photoGrid'),
            count: document.getElementById('listCount'),
            empty: document.getElementById('listEmpty')
        },

        settings = {
            slug: document.getElementById('photoGrid').dataset.slug || null
        },

        url = {
            list: contextPath + '/api/photos/list'
        },

        init = function () {
            load();
        },

        load = function () {
            App.post(url.list, { slug: settings.slug })
                .then(function (res) {
                    if (res.code !== '00') {
                        App.error('불러오기 실패', res.message);
                        return;
                    }
                    render(res.data || []);
                })
                .catch(function () {});
        },

        // 사진 배열 → 그리드. 서버 데이터는 전부 textContent/속성으로만 넣는다 (innerHTML 금지)
        render = function (photos) {
            m$.grid.textContent = '';
            m$.empty.hidden = photos.length > 0;
            m$.count.textContent = photos.length > 0 ? photos.length + '장' : '';

            photos.forEach(function (p) {
                var li = document.createElement('li'),
                    a = document.createElement('a'),
                    figure = document.createElement('figure'),
                    img = document.createElement('img'),
                    caption = document.createElement('figcaption'),
                    title = document.createElement('span'),
                    date = document.createElement('time');

                a.href = contextPath + '/photos/' + p.id;
                img.src = p.image_url;
                img.alt = p.title;
                img.loading = 'lazy';
                title.className = 'photo-title';
                title.textContent = p.title;
                date.className = 'photo-date';
                date.textContent = formatMonth(p.created_at);

                caption.appendChild(title);
                caption.appendChild(date);
                figure.appendChild(img);
                figure.appendChild(caption);
                a.appendChild(figure);
                li.appendChild(a);
                m$.grid.appendChild(li);
            });
        },

        // created_at: Jackson이 Timestamp를 epoch millis(숫자)로 내려준다
        formatMonth = function (value) {
            if (!value) return '';
            var d = new Date(value),
                m = d.getMonth() + 1;
            return d.getFullYear() + '.' + (m < 10 ? '0' + m : m);
        };

    return {
        init: init
    };
}());

document.addEventListener('DOMContentLoaded', function () {
    App.photoList.init();
});
