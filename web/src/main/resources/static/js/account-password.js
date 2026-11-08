/* account/password.html — 비밀번호 변경 */

var MIN_PASSWORD_LENGTH = 8;

async function changePassword() {
    var currentPassword = document.getElementById('current-password').value;
    var newPassword = document.getElementById('new-password').value;
    var newPasswordConfirm = document.getElementById('new-password-confirm').value;
    var errorEl = document.getElementById('password-error');
    var successEl = document.getElementById('password-success');
    var submitEl = document.getElementById('password-submit');

    errorEl.textContent = '';
    successEl.textContent = '';

    var validationMessage = validate(currentPassword, newPassword, newPasswordConfirm);
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
            successEl.textContent = '비밀번호가 변경되었습니다.';
            clearInputs();
        } else {
            errorEl.textContent = data.message || '비밀번호를 변경할 수 없습니다.';
        }
    } catch (e) {
        errorEl.textContent = '서버와 통신할 수 없습니다.';
    } finally {
        submitEl.disabled = false;
    }
}

/* 서버도 같은 규칙을 검사합니다. 여기서 먼저 걸러 왕복을 줄입니다. */
function validate(currentPassword, newPassword, newPasswordConfirm) {
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
    document.getElementById('current-password').value = '';
    document.getElementById('new-password').value = '';
    document.getElementById('new-password-confirm').value = '';
}

document.addEventListener('keydown', function(e) {
    if (e.key !== 'Enter') return;
    var active = document.activeElement;
    if (active && active.classList.contains('login-input') && active.closest('.account-card')) {
        changePassword();
    }
});
