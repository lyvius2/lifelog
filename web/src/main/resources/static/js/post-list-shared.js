/* ═══ POST LIST SHARED ═══
 * index.html 과 post-list.html 에서 공통으로 사용하는 JS
 */

// ── URL 파라미터 ──
const urlParams = new URLSearchParams(window.location.search);
const currentCategorySeq = urlParams.get('categorySeq') ? parseInt(urlParams.get('categorySeq')) : null;

// ── 서버사이드 페이지네이션 링크에 현재 검색 파라미터 유지 (post-list.html) ──
document.querySelectorAll('.page-nav').forEach(function(link) {
  const baseHref = link.getAttribute('href');
  const params = new URLSearchParams(window.location.search);
  if (params.toString()) {
    link.setAttribute('href', baseHref + '?' + params.toString());
  }
});

// ── IntersectionObserver: post-item 등장 애니메이션 ──
const postItemObserver = new IntersectionObserver((entries) => {
  entries.forEach((entry, i) => {
    if (entry.isIntersecting) {
      setTimeout(() => entry.target.classList.add('visible'), i * 80);
      postItemObserver.unobserve(entry.target);
    }
  });
}, { threshold: 0.1 });

// ── Navbar 스크롤 동작 ──
const _sharedNav = document.getElementById('navbar');
if (_sharedNav) {
  window.addEventListener('scroll', () => {
    _sharedNav.style.borderBottomColor =
      window.scrollY > 80 ? 'rgba(255,255,255,0.12)' : 'rgba(255,255,255,0.07)';
  });
}

// ── #content 앵커 스무스 스크롤 ──
document.querySelectorAll('a[href="#content"]').forEach(a => {
  a.addEventListener('click', e => {
    e.preventDefault();
    const target = document.getElementById('content');
    if (target) target.scrollIntoView({ behavior: 'smooth' });
  });
});

// ── 카테고리 트리 토글 ──
function toggleNode(id) {
  const btn      = document.querySelector('#node-' + id + ' > .cat-node-row .cat-expand-btn');
  const children = document.getElementById('children-' + id);
  if (!btn || !children) return;
  const isOpen = children.classList.contains('open');
  children.classList.toggle('open', !isOpen);
  btn.classList.toggle('open', !isOpen);
}

function toggleCatTree() {
  document.getElementById('catTreeCard').classList.toggle('collapsed');
}

// ── 카테고리 트리 렌더링 ──
function renderCatTree(categories, parentEl, depth) {
  categories.forEach(function(cat) {
    const hasChildren = cat.children && cat.children.length > 0;
    const node = document.createElement('div');
    node.className = 'cat-node cat-lv' + depth;
    if (hasChildren) node.id = 'node-' + cat.slug;

    let rowHtml = '<div class="cat-node-row">';
    for (let i = 1; i < depth; i++) rowHtml += '<div class="cat-indent-line"></div>';
    if (hasChildren) {
      rowHtml += '<button class="cat-expand-btn open" onclick="toggleNode(\'' + cat.slug + '\')" aria-label="펼치기">'
          + '<svg width="8" height="8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="9 18 15 12 9 6"/></svg>'
          + '</button>';
    } else {
      rowHtml += '<div class="cat-leaf-spacer"></div>';
    }
    const icon     = hasChildren ? '📁' : '📄';
    const isActive = (cat.categorySeq === currentCategorySeq) ? ' active' : '';
    rowHtml += '<a href="/post-list/1?categorySeq=' + cat.categorySeq + '" class="cat-link' + isActive + '">'
        + '<span class="cat-link-icon">' + icon + '</span>'
        + '<span class="cat-link-name">' + cat.categoryName + '</span>'
        + '</a>';
    rowHtml += '</div>';
    node.innerHTML = rowHtml;

    if (hasChildren) {
      const childrenDiv = document.createElement('div');
      childrenDiv.className = 'cat-children open';
      childrenDiv.id = 'children-' + cat.slug;
      renderCatTree(cat.children, childrenDiv, depth + 1);
      node.appendChild(childrenDiv);
    }
    parentEl.appendChild(node);
  });
}

// ── 카테고리 트리 API 로드 ──
fetch('/api/post/category/tree')
    .then(res => res.json())
    .then(result => {
      const catTreeBody = document.getElementById('catTreeBody');
      if (catTreeBody && result.data && result.data.length > 0) {
        renderCatTree(result.data, catTreeBody, 1);
      }
    })
    .catch(err => console.error('카테고리 트리 로드 실패:', err));

// ── 검색 핸들러 ──
function handleSearch(e) {
  e.preventDefault();
  const keyword = document.getElementById('searchInput').value.trim();
  if (!keyword) return false;
  window.location.href = '/post-list/1?keyword=' + encodeURIComponent(keyword);
  return false;
}

// ── DOMContentLoaded: 서버사이드 렌더링된 post-item 애니메이션 적용 ──
document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.post-item').forEach(el => postItemObserver.observe(el));
});
