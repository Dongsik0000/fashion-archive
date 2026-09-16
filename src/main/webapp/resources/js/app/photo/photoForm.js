App.photoFrom = (function(){

    

    init = function(){
        bindEvent();
    }

    return {
        init : init
    };
}());

document.addEventListener('DOMContentLoaded', function(){
    App.photoFrom.init();
});