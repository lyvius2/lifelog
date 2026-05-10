/* coupler.js — Random Coupler 쌍방향 터미널 클라이언트 */
(function () {
    'use strict';

    var ws        = null;
    var isRunning = false;

    var consoleEl = null;
    var statusEl  = null;
    var inputEl   = null;
    var inputBar  = null;

    document.addEventListener('DOMContentLoaded', function () {
        consoleEl = document.getElementById('console-output');
        statusEl  = document.getElementById('console-status');
        inputEl   = document.getElementById('console-input');
        inputBar  = document.getElementById('input-bar');

        // 페이지 로드 시 자동 연결
        startSession();
    });

    /* ── Public API ── */

    window.restartSession = function () {
        if (ws) {
            try { ws.close(); } catch (e) {}
            ws = null;
        }
        clearConsole();
        startSession();
    };

    window.handleInputKey = function (e) {
        if (e.key === 'Enter') sendInput();
    };

    window.sendInput = function () {
        if (!isRunning || !ws || ws.readyState !== WebSocket.OPEN) return;
        var text = inputEl ? inputEl.value : '';

        // 사용자 입력을 콘솔에 에코 (입력한 줄 표시)
        appendLine(text || '', 'echo');

        ws.send(text);          // 서버 측에서 \n 붙여서 Ruby stdin 에 전달
        if (inputEl) inputEl.value = '';
    };

    /* ── WebSocket ── */

    function startSession() {
        var proto = location.protocol === 'https:' ? 'wss:' : 'ws:';
        var url   = proto + '//' + location.host + '/ws/coupler';

        setStatus('연결 중...', 'running');
        ws = new WebSocket(url);

        ws.onopen = function () {
            setRunning(true);
            setStatus('실행 중', 'running');
        };

        ws.onmessage = function (evt) {
            var msg = evt.data;

            if (msg === '__DONE__') {
                setStatus('완료 ✓', 'done');
                setRunning(false);
                return;
            }
            if (msg.startsWith('__ERROR__:')) {
                appendLine(msg.substring('__ERROR__:'.length).trim(), 'error');
                setStatus('오류', 'error');
                setRunning(false);
                return;
            }
            // Ruby stdout 한 줄 — 빈 문자열이면 빈 줄(blank line)로 렌더링
            appendLine(msg, '');
        };

        ws.onerror = function () {
            appendLine('WebSocket 연결 오류가 발생했습니다.', 'error');
            setStatus('오류', 'error');
            setRunning(false);
        };

        ws.onclose = function () {
            if (isRunning) {
                setRunning(false);
            }
        };
    }

    /* ── 헬퍼 ── */

    /**
     * 콘솔에 한 줄을 추가한다.
     * <div class="cl-line [cl-{cls}]">text</div> 방식으로 렌더링.
     * text 가 빈 문자열이면 빈 줄(blank line)로 min-height: 1.35em 유지.
     */
    function appendLine(text, cls) {
        if (!consoleEl) return;
        var div = document.createElement('div');
        div.className = 'cl-line' + (cls ? ' cl-' + cls : '');
        div.textContent = text;   // 개행 없이 텍스트만 — <div> 자체가 줄 구분
        consoleEl.appendChild(div);
        consoleEl.scrollTop = consoleEl.scrollHeight;
    }

    function clearConsole() {
        if (consoleEl) consoleEl.innerHTML = '';
        setStatus('');
    }

    function setStatus(text, cls) {
        if (!statusEl) return;
        statusEl.textContent = text;
        statusEl.className   = 'console-status' + (cls ? ' ' + cls : '');
    }

    function setRunning(running) {
        isRunning = running;
        if (inputBar) {
            inputBar.style.opacity       = running ? '1' : '0.35';
            inputBar.style.pointerEvents = running ? 'auto' : 'none';
        }
        if (running && inputEl) {
            setTimeout(function () { inputEl.focus(); }, 80);
        }
    }
})();
