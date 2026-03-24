/* ================================================================
   notice.js  —  Post Published Notification WebSocket
   ================================================================ */

(function() {
    var notif = document.getElementById('post-pub-notif');
    var notifTitle = document.getElementById('post-pub-notif-title');
    var notifTime = document.getElementById('post-pub-notif-time');
    var hideTimer = null;

    function showNotification(data) {
        notif.href = '/post/' + data.postSeq;
        notifTitle.textContent = data.title || '';
        notifTime.textContent = data.publishedAt || '';
        if (hideTimer) clearTimeout(hideTimer);
        notif.classList.add('show');
        hideTimer = setTimeout(function() {
            notif.classList.remove('show');
        }, 10000);
    }

    function connect() {
        var protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
        var ws = new WebSocket(protocol + '//' + location.host + '/ws/notifications');
        ws.onmessage = function(e) {
            try { showNotification(JSON.parse(e.data)); } catch(err) {}
        };
        ws.onclose = function() {
            setTimeout(connect, 5000);
        };
    }
    connect();
})();