/*
  정적 웹페이지 등록 모달. 마크업은 fragments/modals.html 의 #static-web-modal 이고
  입력 필드와 버튼은 로그인 모달과 같은 .login-* 스타일을 씁니다.

  드롭다운 값에 따라 파일 업로드(multipart)와 URL 스크랩(JSON)으로 엔드포인트가
  갈립니다. 서버도 같은 규칙을 다시 검사합니다.
*/

var STATIC_WEB_PATH_PATTERN = /^[A-Za-z0-9_-]{1,20}$/;
var STATIC_WEB_MAX_BYTES = 5 * 1024 * 1024;

/* 409 를 받았을 때 "덮어쓰기" 로 그대로 재전송하기 위해 직전 요청을 기억합니다. */
var staticWebPending = null;

function openStaticWebModal() {
    var adminMenu = document.getElementById('adminMenu');
    if (adminMenu) adminMenu.style.display = 'none';

    clearStaticWebInputs();
    document.getElementById('static-web-source').value = 'FILE';
    switchStaticWebSource();
    document.getElementById('static-web-error').textContent = '';
    document.getElementById('static-web-form-view').style.display = '';
    document.getElementById('static-web-confirm-view').style.display = 'none';
    document.getElementById('static-web-done-view').style.display = 'none';
    document.getElementById('static-web-close').style.display = '';
    staticWebPending = null;
    document.getElementById('static-web-submit').disabled = false;
    document.getElementById('static-web-modal').classList.add('open');
    setTimeout(function() { document.getElementById('static-web-source').focus(); }, 100);
}

function closeStaticWebModal() {
    document.getElementById('static-web-modal').classList.remove('open');
    clearStaticWebInputs();
    staticWebPending = null;
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
        await sendStaticWeb(sourceType, path, false);
    } catch (e) {
        errorEl.textContent = '서버와 통신할 수 없습니다.';
        submitEl.disabled = false;
    }
}

/*
  경로가 이미 쓰이고 있으면 서버가 409 와 함께 기존 페이지 정보를 돌려줍니다.
  그때는 바로 실패로 처리하지 않고 덮어쓸지 묻습니다.
*/
async function sendStaticWeb(sourceType, path, overwrite) {
    var errorEl = document.getElementById('static-web-error');
    var submitEl = document.getElementById('static-web-submit');

    var res = sourceType === 'FILE'
        ? await sendStaticWebFile(path, overwrite)
        : await sendStaticWebUrl(path, overwrite);
    var data = await res.json();

    if (res.status === 409 && data.data) {
        staticWebPending = { sourceType: sourceType, path: path };
        showStaticWebConflict(data.data);
        return;
    }
    if (data.success && data.data) {
        showStaticWebRegistered(data.data.url);
        return;
    }
    errorEl.textContent = data.message || '등록할 수 없습니다.';
    submitEl.disabled = false;
}

function sendStaticWebFile(path, overwrite) {
    var form = new FormData();
    form.append('file', document.getElementById('static-web-file').files[0]);
    var query = '?overwrite=' + (overwrite ? 'true' : 'false');
    if (path) query += '&path=' + encodeURIComponent(path);
    return fetch('/api/static-page/upload' + query, { method: 'POST', body: form });
}

function sendStaticWebUrl(path, overwrite) {
    return fetch('/api/static-page/scrape', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            url: document.getElementById('static-web-url').value.trim(),
            path: path || null,
            overwrite: !!overwrite
        })
    });
}

function showStaticWebConflict(existing) {
    document.getElementById('static-web-conflict-path').textContent = '/static/' + existing.path;
    document.getElementById('static-web-conflict-info').textContent = describeStaticWebPage(existing);
    document.getElementById('static-web-form-view').style.display = 'none';
    document.getElementById('static-web-confirm-view').style.display = '';
    document.getElementById('static-web-overwrite').disabled = false;
}

function describeStaticWebPage(existing) {
    var parts = [];
    if (existing.title) parts.push(existing.title);
    parts.push(existing.sourceType === 'URL' ? (existing.sourceUrl || 'Web URL') : (existing.sourceFileName || '파일'));
    if (existing.createdAt) parts.push(existing.createdAt.replace('T', ' ').slice(0, 16) + ' 등록');
    return parts.join(' · ');
}

async function confirmStaticWebOverwrite() {
    if (!staticWebPending) return;
    document.getElementById('static-web-overwrite').disabled = true;
    var pending = staticWebPending;
    try {
        document.getElementById('static-web-confirm-view').style.display = 'none';
        document.getElementById('static-web-form-view').style.display = '';
        await sendStaticWeb(pending.sourceType, pending.path, true);
    } catch (e) {
        document.getElementById('static-web-error').textContent = '서버와 통신할 수 없습니다.';
        document.getElementById('static-web-submit').disabled = false;
    }
    staticWebPending = null;
}

/* 덮어쓰지 않기로 하면 입력은 그대로 두고 폼으로 돌아갑니다 — 경로만 바꿔 다시 올릴 수 있게. */
function cancelStaticWebOverwrite() {
    staticWebPending = null;
    document.getElementById('static-web-confirm-view').style.display = 'none';
    document.getElementById('static-web-form-view').style.display = '';
    document.getElementById('static-web-submit').disabled = false;
    document.getElementById('static-web-error').textContent = '';
    document.getElementById('static-web-path').focus();
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
    var isConfirm = document.getElementById('static-web-confirm-view').style.display !== 'none';
    if (e.key === 'Enter') {
        if (isDone) closeStaticWebModal();
        else if (isConfirm) confirmStaticWebOverwrite();
        else uploadStaticWeb();
    }
    if (e.key === 'Escape') {
        if (isConfirm) cancelStaticWebOverwrite();
        else closeStaticWebModal();
    }
});
