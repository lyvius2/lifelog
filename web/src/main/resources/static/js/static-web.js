/*
  정적 웹페이지 등록 모달. 마크업은 fragments/modals.html 의 #static-web-modal 이고
  입력 필드와 버튼은 로그인 모달과 같은 .login-* 스타일을 씁니다.

  드롭다운 값에 따라 파일 업로드(multipart)와 URL 스크랩(JSON)으로 엔드포인트가
  갈립니다. 서버도 같은 규칙을 다시 검사합니다.
*/

var STATIC_WEB_PATH_PATTERN = /^[A-Za-z0-9_-]{1,20}$/;
var STATIC_WEB_MAX_BYTES = 5 * 1024 * 1024;

function openStaticWebModal() {
    var adminMenu = document.getElementById('adminMenu');
    if (adminMenu) adminMenu.style.display = 'none';

    clearStaticWebInputs();
    document.getElementById('static-web-source').value = 'FILE';
    switchStaticWebSource();
    document.getElementById('static-web-error').textContent = '';
    document.getElementById('static-web-form-view').style.display = '';
    document.getElementById('static-web-done-view').style.display = 'none';
    document.getElementById('static-web-close').style.display = '';
    document.getElementById('static-web-submit').disabled = false;
    document.getElementById('static-web-modal').classList.add('open');
    setTimeout(function() { document.getElementById('static-web-source').focus(); }, 100);
}

function closeStaticWebModal() {
    document.getElementById('static-web-modal').classList.remove('open');
    clearStaticWebInputs();
}

function switchStaticWebSource() {
    var isFile = document.getElementById('static-web-source').value === 'FILE';
    document.getElementById('static-web-file-field').style.display = isFile ? '' : 'none';
    document.getElementById('static-web-url-field').style.display = isFile ? 'none' : '';
    document.getElementById('static-web-error').textContent = '';
}

async function uploadStaticWeb() {
    var sourceType = document.getElementById('static-web-source').value;
    var path = document.getElementById('static-web-path').value.trim();
    var errorEl = document.getElementById('static-web-error');
    var submitEl = document.getElementById('static-web-submit');

    errorEl.textContent = '';

    var validationMessage = validateStaticWeb(sourceType, path);
    if (validationMessage) {
        errorEl.textContent = validationMessage;
        return;
    }

    submitEl.disabled = true;
    try {
        var res = sourceType === 'FILE' ? await sendStaticWebFile(path) : await sendStaticWebUrl(path);
        var data = await res.json();

        if (data.success && data.data) {
            showStaticWebRegistered(data.data.url);
        } else {
            errorEl.textContent = data.message || '등록할 수 없습니다.';
            submitEl.disabled = false;
        }
    } catch (e) {
        errorEl.textContent = '서버와 통신할 수 없습니다.';
        submitEl.disabled = false;
    }
}

function sendStaticWebFile(path) {
    var form = new FormData();
    form.append('file', document.getElementById('static-web-file').files[0]);
    var query = path ? '?path=' + encodeURIComponent(path) : '';
    return fetch('/api/static-page/upload' + query, { method: 'POST', body: form });
}

function sendStaticWebUrl(path) {
    return fetch('/api/static-page/scrape', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            url: document.getElementById('static-web-url').value.trim(),
            path: path || null
        })
    });
}

function showStaticWebRegistered(url) {
    var link = document.getElementById('static-web-link');
    link.href = url;
    link.textContent = location.origin + url;

    clearStaticWebInputs();
    document.getElementById('static-web-form-view').style.display = 'none';
    document.getElementById('static-web-done-view').style.display = '';
}

/* 서버도 같은 규칙을 검사합니다. 여기서 먼저 걸러 왕복을 줄입니다. */
function validateStaticWeb(sourceType, path) {
    if (path && !STATIC_WEB_PATH_PATTERN.test(path)) {
        return '경로는 영문·숫자·하이픈·언더바 조합으로 최대 20자까지 쓸 수 있습니다.';
    }

    if (sourceType === 'FILE') {
        var file = document.getElementById('static-web-file').files[0];
        if (!file) return 'HTML 파일을 선택해주세요.';
        if (!/\.html?$/i.test(file.name)) return 'HTML 파일만 등록할 수 있습니다 (.html, .htm).';
        if (file.size > STATIC_WEB_MAX_BYTES) return '파일이 너무 큽니다. 최대 5MB 까지 등록할 수 있습니다.';
        return null;
    }

    var url = document.getElementById('static-web-url').value.trim();
    if (!url) return '주소를 입력해주세요.';
    if (!/^https?:\/\//i.test(url)) return 'http 또는 https 주소만 등록할 수 있습니다.';
    return null;
}

function clearStaticWebInputs() {
    var file = document.getElementById('static-web-file');
    if (file) file.value = '';
    ['static-web-url', 'static-web-path'].forEach(function(id) {
        var el = document.getElementById(id);
        if (el) el.value = '';
    });
}

document.addEventListener('keydown', function(e) {
    var modal = document.getElementById('static-web-modal');
    if (!modal || !modal.classList.contains('open')) return;

    var isDone = document.getElementById('static-web-done-view').style.display !== 'none';
    if (e.key === 'Enter' && !isDone) uploadStaticWeb();
    if (e.key === 'Escape') closeStaticWebModal();
});
