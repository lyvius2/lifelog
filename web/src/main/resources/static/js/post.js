/**
 * Post Page - JavaScript Module
 * 블로그 게시글 상세 페이지의 주요 기능을 담당합니다.
 *
 * 전역 변수:
 * - window.currentCategorySeq: 현재 게시글의 카테고리 시퀀스 (Thymeleaf에서 주입)
 * - window.currentPostSeq: 현재 게시글 시퀀스 (Thymeleaf에서 주입)
 * - window.__isAdmin: 관리자 로그인 여부 (layout에서 주입)
 */
(function() {
    'use strict';

    var currentCategorySeq = window.currentCategorySeq;
    var currentPostSeq = window.currentPostSeq;

    // ══════════════════════════════════════════════════════════════════════
    // 카테고리 트리
    // ══════════════════════════════════════════════════════════════════════

    function renderCatTree(categories, parentEl, depth) {
        categories.forEach(function(cat) {
            var hasChildren = cat.children && cat.children.length > 0;
            var node = document.createElement('div');
            node.className = 'cat-node cat-lv' + depth;
            if (hasChildren) node.id = 'node-' + cat.slug;

            var rowHtml = '<div class="cat-node-row">';
            for (var i = 1; i < depth; i++) {
                rowHtml += '<div class="cat-indent-line"></div>';
            }
            if (hasChildren) {
                rowHtml += '<button class="cat-expand-btn open" onclick="window.toggleNode(\'' + cat.slug + '\')" aria-label="펼치기">'
                    + '<svg width="8" height="8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="9 18 15 12 9 6"/></svg>'
                    + '</button>';
            } else {
                rowHtml += '<div class="cat-leaf-spacer"></div>';
            }
            var isActive = (cat.categorySeq === currentCategorySeq) ? ' active' : '';
            var icon = hasChildren ? '📁' : '📄';
            rowHtml += '<a href="/post-list/1?categorySeq=' + cat.categorySeq + '" class="cat-link' + isActive + '">'
                + '<span class="cat-link-icon">' + icon + '</span>'
                + '<span class="cat-link-name">' + cat.categoryName + '</span>'
                + '</a>';
            rowHtml += '</div>';
            node.innerHTML = rowHtml;

            if (hasChildren) {
                var childrenDiv = document.createElement('div');
                childrenDiv.className = 'cat-children open';
                childrenDiv.id = 'children-' + cat.slug;
                renderCatTree(cat.children, childrenDiv, depth + 1);
                node.appendChild(childrenDiv);
            }
            parentEl.appendChild(node);
        });
    }

    function loadCategoryTree() {
        fetch('/api/post/category/tree')
            .then(function(res) { return res.json(); })
            .then(function(result) {
                var catTreeBody = document.getElementById('catTreeBody');
                if (catTreeBody && result.data && result.data.length > 0) {
                    renderCatTree(result.data, catTreeBody, 1);
                }
            })
            .catch(function(err) {
                console.error('카테고리 트리 로드 실패:', err);
            });
    }

    window.toggleNode = function(id) {
        var btn = document.querySelector('#node-' + id + ' > .cat-node-row .cat-expand-btn');
        var children = document.getElementById('children-' + id);
        if (!btn || !children) return;
        var isOpen = children.classList.contains('open');
        children.classList.toggle('open', !isOpen);
        btn.classList.toggle('open', !isOpen);
    };

    window.toggleCatTree = function() {
        document.getElementById('catTreeCard').classList.toggle('collapsed');
    };

    // ══════════════════════════════════════════════════════════════════════
    // 스크롤 진행률 표시
    // ══════════════════════════════════════════════════════════════════════

    function setupProgressBar() {
        var bar = document.getElementById('progress-bar');
        if (!bar) return;

        window.addEventListener('scroll', function() {
            var doc = document.documentElement;
            var scrollTop = window.scrollY;
            var scrollHeight = doc.scrollHeight - doc.clientHeight;
            bar.style.width = (scrollTop / scrollHeight * 100) + '%';
        }, { passive: true });
    }

    // ══════════════════════════════════════════════════════════════════════
    // Table of Contents (TOC)
    // ══════════════════════════════════════════════════════════════════════

    function setupTOC() {
        var tocListEl = document.getElementById('toc-list');
        if (!tocListEl) return;

        var headings = document.querySelectorAll('#post-content h5[id]');
        headings.forEach(function(h) {
            var li = document.createElement('li');
            var a = document.createElement('a');
            a.href = '#' + h.id;
            a.className = 'toc-link';
            a.textContent = h.textContent;
            li.appendChild(a);
            tocListEl.appendChild(li);
        });

        var tocLinks = document.querySelectorAll('.toc-link');
        var tocObserver = new IntersectionObserver(function(entries) {
            entries.forEach(function(entry) {
                if (entry.isIntersecting) {
                    tocLinks.forEach(function(l) { l.classList.remove('active'); });
                    var active = document.querySelector('.toc-link[href="#' + entry.target.id + '"]');
                    if (active) active.classList.add('active');
                }
            });
        }, { rootMargin: '-20% 0px -70% 0px' });

        headings.forEach(function(h) { tocObserver.observe(h); });
    }

    // ══════════════════════════════════════════════════════════════════════
    // 코드 복사 기능
    // ══════════════════════════════════════════════════════════════════════

    window.copyCode = function(btn) {
        var pre = btn.closest('.code-wrap').querySelector('pre');
        navigator.clipboard.writeText(pre.innerText).then(function() {
            btn.textContent = 'Copied!';
            btn.classList.add('copied');
            setTimeout(function() {
                btn.textContent = 'Copy';
                btn.classList.remove('copied');
            }, 2000);
        });
    };

    // ══════════════════════════════════════════════════════════════════════
    // 공유 버튼
    // ══════════════════════════════════════════════════════════════════════

    function setupShareButtons() {
        var currentUrl = encodeURIComponent(window.location.href);
        var titleEl = document.querySelector('.post-title');
        var pageTitle = encodeURIComponent(titleEl ? titleEl.textContent : document.title);

        var twitterBtn = document.getElementById('share-twitter');
        var facebookBtn = document.getElementById('share-facebook');

        if (twitterBtn) {
            twitterBtn.href = 'https://twitter.com/share?text=' + pageTitle + '&url=' + currentUrl;
        }
        if (facebookBtn) {
            facebookBtn.href = 'https://www.facebook.com/sharer/sharer.php?u=' + currentUrl;
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 관리자 버튼 (Edit, Archive)
    // ══════════════════════════════════════════════════════════════════════

    function showAdminButtonsIfAdmin() {
        function checkAndShow() {
            if (typeof window.__isAdmin !== 'undefined' && window.__isAdmin === true) {
                var editBtn = document.getElementById('postEditBtn');
                var archiveBtn = document.getElementById('postArchiveBtn');
                if (editBtn) editBtn.style.display = 'flex';
                if (archiveBtn) archiveBtn.style.display = 'flex';
            }
        }
        checkAndShow();
        setTimeout(checkAndShow, 100);
        setTimeout(checkAndShow, 500);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Archive 기능
    // ══════════════════════════════════════════════════════════════════════

    function setupArchiveButton() {
        var archiveBtn = document.getElementById('postArchiveBtn');
        if (!archiveBtn) return;

        archiveBtn.addEventListener('click', function() {
            var postSeq = currentPostSeq;
            if (!postSeq) return;

            if (!confirm('이 게시글을 보관 처리하시겠습니까?')) return;

            fetch('/api/post/archiving/' + postSeq, { method: 'POST' })
                .then(function(res) {
                    if (res.status === 401) {
                        throw new Error('인증이 필요합니다. 로그인 해주세요.');
                    }
                    return res.json();
                })
                .then(function(data) {
                    if (data.success && data.data === true) {
                        showArchivePopup();
                    } else {
                        alert(data.message || '보관 처리에 실패했습니다.');
                    }
                })
                .catch(function(err) {
                    alert(err.message || '서버 오류가 발생했습니다.');
                });
        });
    }

    function showArchivePopup() {
        var overlay = document.createElement('div');
        overlay.id = 'archivePopupOverlay';
        overlay.style.cssText = 'position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.6);z-index:10000;display:flex;align-items:center;justify-content:center;';

        var popup = document.createElement('div');
        popup.style.cssText = 'background:var(--card-bg,#2a2825);border:1px solid rgba(245,240,232,0.08);border-radius:12px;padding:32px 40px;text-align:center;max-width:360px;box-shadow:0 8px 32px rgba(0,0,0,0.4);';

        var icon = document.createElement('div');
        icon.innerHTML = '<svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#84a98c" stroke-width="1.5"><path d="M21 8v13H3V8"/><path d="M1 3h22v5H1z"/><path d="M10 12h4"/></svg>';
        icon.style.marginBottom = '16px';

        var message = document.createElement('p');
        message.textContent = '포스트가 보관 처리되었습니다.';
        message.style.cssText = 'color:#f5f0e8;font-size:1rem;margin:0 0 24px 0;';

        var confirmBtn = document.createElement('button');
        confirmBtn.textContent = '확인';
        confirmBtn.style.cssText = 'background:var(--accent,#84a98c);color:#1e1c1a;border:none;padding:10px 32px;border-radius:6px;font-size:0.9rem;cursor:pointer;font-weight:500;';
        confirmBtn.onclick = function() {
            window.location.href = '/post-list/1';
        };

        popup.appendChild(icon);
        popup.appendChild(message);
        popup.appendChild(confirmBtn);
        overlay.appendChild(popup);
        document.body.appendChild(overlay);
    }

    // ══════════════════════════════════════════════════════════════════════
    // 코드 하이라이팅
    // ══════════════════════════════════════════════════════════════════════

    function highlightCode() {
        if (typeof hljs !== 'undefined') {
            document.querySelectorAll('pre code').forEach(function(el) {
                hljs.highlightElement(el);
            });
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 초기화
    // ══════════════════════════════════════════════════════════════════════

    function init() {
        loadCategoryTree();
        setupProgressBar();
        setupTOC();
        setupShareButtons();
        showAdminButtonsIfAdmin();
        setupArchiveButton();
        highlightCode();
    }

    // DOM 로드 후 초기화
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();

