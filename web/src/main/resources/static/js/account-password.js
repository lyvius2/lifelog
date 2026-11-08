/*
  비밀번호 변경 모달. 마크업은 fragments/modals.html 의 #password-modal 이고
  입력 필드와 버튼은 로그인 모달과 같은 .login-* 스타일을 씁니다.

  RSA 헬퍼(fetchServerPublicKey / encryptWith)와 doLogout 은
  fragments/common-scripts.html 에 있습니다. 이 파일은 그 뒤에 로드됩니다.
*/

var MIN_PASSWORD_LENGTH = 8;

function openPasswordModal() {
    var adminMenu = document.getElementById('adminMenu');
    if (adminMenu) adminMenu.style.display = 'none';

    clearInputs();
    document.getElementById('password-error').textContent = '';
    document.getElementById('password-form-view').style.display = '';
    document.getElementById('password-done-view').style.display = 'none';
    document.getElementById('password-close').style.display = '';
    document.getElementById('password-submit').disabled = false;
    document.getElementById('password-modal').classList.add('open');
    setTimeout(function() { document.getElementById('current-password').focus(); }, 100);
}

function closePasswordModal() {
    document.getElementById('password-modal').classList.remove('open');
    clearInputs();
}

async function changePassword() {
    var currentPassword = document.getElementById('current-password').value;
    var newPassword = document.getElementById('new-password').value;
    var newPasswordConfirm = document.getElementById('new-password-confirm').value;
    var errorEl = document.getElementById('password-error');
    var submitEl = document.getElementById('password-submit');

    errorEl.textContent = '';

    var validationMessage = validatePassword(currentPassword, newPassword, newPasswordConfirm);
    if (validationMessage) {
        errorEl.textContent = validationMessage;
        return;
    }

    submitEl.disabled = true;
    try {
        var cryptoKey = await fetchServerPublicKey();
        var res = await fetch('/api/user/change-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                currentPassword: await encryptWith(cryptoKey, currentPassword),
                newPassword: await encryptWith(cryptoKey, newPassword),
                newPasswordConfirm: await encryptWith(cryptoKey, newPasswordConfirm)
            })
        });
        var data = await res.json();

        if (data.success) {
            showPasswordChanged();
        } else {
            errorEl.textContent = data.message || '비밀번호를 변경할 수 없습니다.';
            submitEl.disabled = false;
        }
    } catch (e) {
        errorEl.textContent = '서버와 통신할 수 없습니다.';
        submitEl.disabled = false;
    }
}

/*
  변경에 성공하면 서버가 리프레시 토큰을 폐기합니다. 세션만 남아 반쪽 상태가
  되므로 안내를 띄우고 로그아웃까지 진행합니다. 닫기 버튼을 숨겨 모달을
  그냥 닫고 지나갈 수 없게 합니다.
*/
function showPasswordChanged() {
    clearInputs();
    document.getElementById('password-form-view').style.display = 'none';
    document.getElementById('password-close').style.display = 'none';
    document.getElementById('password-done-view').style.display = '';
}

function finishPasswordChange() {
    doLogout();
}

/* 서버도 같은 규칙을 검사합니다. 여기서 먼저 걸러 왕복을 줄입니다. */
function validatePassword(currentPassword, newPassword, newPasswordConfirm) {
    if (!currentPassword || !newPassword || !newPasswordConfirm) {
        return '세 항목을 모두 입력해주세요.';
    }
    if (newPassword !== newPasswordConfirm) {
        return '새 비밀번호가 서로 일치하지 않습니다.';
    }
    if (newPassword.length < MIN_PASSWORD_LENGTH) {
        return '새 비밀번호는 ' + MIN_PASSWORD_LENGTH + '자 이상이어야 합니다.';
    }
    if (newPassword === currentPassword) {
        return '새 비밀번호가 현재 비밀번호와 같습니다.';
    }
    return null;
}

function clearInputs() {
    ['current-password', 'new-password', 'new-password-confirm'].forEach(function(id) {
        var el = document.getElementById(id);
        if (el) el.value = '';
    });
}

document.addEventListener('keydown', function(e) {
    var modal = document.getElementById('password-modal');
    if (!modal || !modal.classList.contains('open')) return;

    var isDone = document.getElementById('password-done-view').style.display !== 'none';
    if (e.key === 'Enter') {
        if (isDone) finishPasswordChange();
        else changePassword();
    }
    if (e.key === 'Escape' && !isDone) closePasswordModal();
});
