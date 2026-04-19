/**
 * Photos Gallery - JavaScript Module
 * 사진 갤러리 페이지의 주요 기능을 담당합니다.
 *
 * 전역 변수 initialPage, initialTotalPages는 HTML에서 Thymeleaf로 주입됩니다.
 */
(function() {
  'use strict';

  const grid = document.getElementById('galleryGrid');
  const loadMoreBtn = document.getElementById('loadMoreBtn');
  let currentPage = window.initialPage || 1;
  let totalPages = window.initialTotalPages || 1;
  let currentCategorySeq = null; // null = ALL
  let isMasonry = true;
  let currentIndex = 0;
  let allPhotos = []; // 현재 화면에 표시된 모든 사진 데이터

  // ── SSR로 렌더링된 카드에서 사진 데이터 추출 ──
  function parsePhotosFromCards() {
    const cards = grid.querySelectorAll('.photo-card');
    const photos = [];
    cards.forEach((card, idx) => {
      card.dataset.index = idx;
      photos.push(extractPhotoData(card));
    });
    return photos;
  }

  function extractPhotoData(card) {
    const tagsRaw = card.dataset.tags || '';
    let tags = [];
    if (tagsRaw.startsWith('[')) {
      try { tags = JSON.parse(tagsRaw); } catch(e) { tags = tagsRaw.replace(/[\[\]]/g, '').split(',').map(t => t.trim()).filter(t => t); }
    } else if (tagsRaw) {
      tags = tagsRaw.split(',').map(t => t.trim()).filter(t => t);
    }
    return {
      photoSeq: parseInt(card.dataset.photoSeq) || 0,
      src: card.dataset.src || '',
      thumb: card.dataset.thumb || '',
      title: card.dataset.title || '',
      caption: card.dataset.caption || '',
      categorySeq: card.dataset.categorySeq || null,
      categoryName: card.dataset.categoryName || '',
      tags: tags,
      likes: parseInt(card.dataset.likes) || 0,
      shotAt: card.dataset.shotAt || '',
      createdAt: card.dataset.createdAt || '',
      userSeq: parseInt(card.dataset.userSeq) || 0,
      photographerName: card.dataset.photographerName || '',
      photographerProfileImage: card.dataset.photographerProfileImage || '',
      photographerEmail: card.dataset.photographerEmail || '',
      exif: {
        maker: card.dataset.exifMaker || null,
        model: card.dataset.exifModel || null,
        aperture: card.dataset.exifAperture || null,
        shutter: card.dataset.exifShutter || null,
        iso: card.dataset.exifIso || null,
        focal: card.dataset.exifFocal || null,
        lens: card.dataset.exifLens || null,
      }
    };
  }

  // ── 사진 카드 HTML 생성 ──
  function createPhotoCardHtml(photo, idx) {
    const thumbSrc = photo.thumb || photo.src || '';
    const firstTag = (photo.tags && photo.tags.length > 0) ? photo.tags[0] : '';
    const tagsJson = JSON.stringify(photo.tags || []);

    return `<div class="photo-card${isMasonry && photo.size ? ' ' + photo.size : ''}"
       data-index="${idx}"
       data-photo-seq="${photo.photoSeq}"
       data-src="${photo.src || ''}"
       data-thumb="${photo.thumb || ''}"
       data-title="${escapeHtml(photo.title || '')}"
       data-caption="${escapeHtml(photo.caption || '')}"
       data-category-seq="${photo.categorySeq || ''}"
       data-category-name="${escapeHtml(photo.categoryName || '')}"
       data-likes="${photo.likes || 0}"
       data-shot-at="${photo.shotAt || ''}"
       data-created-at="${photo.createdAt || ''}"
       data-user-seq="${photo.userSeq || 0}"
       data-photographer-name="${escapeHtml(photo.photographerName || '')}"
       data-photographer-profile-image="${escapeHtml(photo.photographerProfileImage || '')}"
       data-photographer-email="${escapeHtml(photo.photographerEmail || '')}"
       data-tags='${tagsJson}'
       data-exif-maker="${escapeHtml(photo.exif?.maker || '')}"
       data-exif-model="${escapeHtml(photo.exif?.model || '')}"
       data-exif-aperture="${escapeHtml(photo.exif?.aperture || '')}"
       data-exif-shutter="${escapeHtml(photo.exif?.shutter || '')}"
       data-exif-iso="${escapeHtml(photo.exif?.iso || '')}"
       data-exif-focal="${photo.exif?.focal || ''}"
       data-exif-lens="${escapeHtml(photo.exif?.lens || '')}">
      <img src="${thumbSrc}" alt="${escapeHtml(photo.title || '')}" loading="lazy" />
      <div class="photo-overlay">
        <div class="overlay-stats">
          <div class="overlay-stat">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="white" stroke="white" stroke-width="1.5">
              <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
            </svg>
            <span>${(photo.likes || 0).toLocaleString()}</span>
          </div>
        </div>
        ${firstTag ? '<div class="overlay-tag">' + escapeHtml(firstTag) + '</div>' : ''}
      </div>
    </div>`;
  }

  function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  // ── API 호출하여 사진 로드 ──
  async function fetchPhotos(categorySeq, page) {
    let url = '/api/photo?page=' + page;
    if (categorySeq) {
      url += '&categorySeq=' + categorySeq;
    }
    const res = await fetch(url);
    const json = await res.json();
    return json.data; // PageResponse { content, page, size, totalCount, totalPages }
  }

  // ── 카테고리 클릭 → API 호출 후 새로 렌더링 ──
  async function loadCategoryPhotos(categorySeq) {
    currentCategorySeq = categorySeq;
    currentPage = 1;

    const pageData = await fetchPhotos(categorySeq, 1);
    totalPages = pageData.totalPages;

    // 그리드 초기화 후 새 데이터 렌더링
    grid.innerHTML = '';
    allPhotos = [];

    appendPhotos(pageData.content);
    updateLoadMoreButton();
  }

  // ── Load More 클릭 → 다음 페이지 API 호출 ──
  async function loadMorePhotos() {
    if (currentPage >= totalPages) return;

    currentPage++;
    const pageData = await fetchPhotos(currentCategorySeq, currentPage);
    totalPages = pageData.totalPages;

    appendPhotos(pageData.content);
    updateLoadMoreButton();
  }

  // ── 사진 카드 추가 렌더링 ──
  function appendPhotos(photos) {
    const startIdx = allPhotos.length;
    photos.forEach((photo, i) => {
      const idx = startIdx + i;
      // API 응답의 src/thumb에 /photo prefix 추가
      const prefixedPhoto = Object.assign({}, photo, {
        src: '/photo' + (photo.src || ''),
        thumb: '/photo' + (photo.thumb || photo.src || '')
      });
      allPhotos.push(prefixedPhoto);
      const cardHtml = createPhotoCardHtml(prefixedPhoto, idx);
      const temp = document.createElement('div');
      temp.innerHTML = cardHtml;
      const card = temp.firstElementChild;

      card.addEventListener('click', function() {
        openLightboxFromCard(this);
      });

      card.style.opacity = '0';
      card.style.transform = 'scale(0.96)';
      card.style.transition = `opacity 0.4s ${i * 45}ms, transform 0.4s ${i * 45}ms`;
      grid.appendChild(card);

      requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          card.style.opacity = '1';
          card.style.transform = 'scale(1)';
        });
      });
    });
  }

  // ── Load More 버튼 상태 업데이트 ──
  function updateLoadMoreButton() {
    if (currentPage >= totalPages) {
      loadMoreBtn.textContent = 'All Photos Loaded';
      loadMoreBtn.style.opacity = '0.4';
      loadMoreBtn.disabled = true;
    } else {
      loadMoreBtn.textContent = 'Load More Photos';
      loadMoreBtn.style.opacity = '1';
      loadMoreBtn.disabled = false;
    }
  }

  // ── Lightbox ──
  const lightbox = document.getElementById('lightbox');
  const lbImg    = document.getElementById('lbImg');
  const lbTitle  = document.getElementById('lbTitle');
  const lbText   = document.getElementById('lbText');
  const lbTags   = document.getElementById('lbTags');
  const lbDate   = document.getElementById('lbDate');
  const lbLikeCount    = document.getElementById('lbLikeCount');
  const lbLike   = document.getElementById('lbLike');
  const lbDelete = document.getElementById('lbDelete');
  const lbLoading = document.getElementById('lbLoading');
  const lbAuthorImg   = document.getElementById('lbAuthorImg');
  const lbAuthorName  = document.getElementById('lbAuthorName');
  const lbAuthorEmail = document.getElementById('lbAuthorEmail');

  // ── 라이트박스 초기화 (닫을 때 & 전환 전) ──
  function clearLightbox() {
    lbImg.src = '';
    lbImg.alt = '';
    lbImg.style.opacity = '0';
    document.querySelector('.lb-img-wrap').classList.remove('has-image');
    lbTitle.textContent = '';
    lbText.textContent = '';
    lbTags.innerHTML = '';
    lbDate.textContent = '';
    lbLikeCount.textContent = '0';
    lbLike.classList.remove('liked');
    if (lbDelete) lbDelete.style.display = 'none';
    lbAuthorImg.src = 'https://i.pravatar.cc/80?img=11';
    lbAuthorName.textContent = '';
    lbAuthorEmail.textContent = '';
    document.getElementById('exifModel').textContent    = '—';
    document.getElementById('exifMaker').textContent    = '—';
    document.getElementById('exifAperture').textContent = '—';
    document.getElementById('exifShutter').textContent  = '—';
    document.getElementById('exifIso').textContent      = '—';
    document.getElementById('exifFocal').textContent    = '—';
    document.getElementById('exifLens').textContent     = '—';
    lbLoading.classList.remove('active');
  }

  function showLoading() {
    lbLoading.classList.add('active');
  }

  function hideLoading() {
    lbLoading.classList.remove('active');
  }

  function openLightboxFromCard(card) {
    const idx = parseInt(card.dataset.index);
    currentIndex = idx;
    const photo = allPhotos[idx];
    if (!photo) return;
    clearLightbox();
    showLoading();
    // 모바일: 사이드바 접힌 상태로 시작
    const sidebar = document.querySelector('.lb-sidebar');
    if (window.innerWidth <= 640) {
      sidebar.classList.remove('expanded');
    }
    populateLightbox(photo);
    lightbox.classList.add('open');
    document.body.style.overflow = 'hidden';
  }

  function closeLightbox() {
    lightbox.classList.remove('open');
    document.body.style.overflow = '';
    document.querySelector('.lb-sidebar').classList.remove('expanded');
    clearLightbox();
  }

  function formatDate(dateStr) {
    if (!dateStr) return '';
    try {
      const d = new Date(dateStr);
      if (isNaN(d)) return dateStr;
      return d.getFullYear() + '.' + String(d.getMonth() + 1).padStart(2, '0') + '.' + String(d.getDate()).padStart(2, '0');
    } catch(e) { return dateStr; }
  }

  function populateLightbox(photo) {
    // 작성자 정보 매핑
    lbAuthorImg.src = photo.photographerProfileImage || 'https://i.pravatar.cc/80?img=11';
    lbAuthorName.textContent = photo.photographerName || 'Unknown';
    lbAuthorEmail.textContent = photo.photographerEmail ? '@' + photo.photographerEmail : '';

    // 메타데이터 즉시 표시
    lbTitle.textContent = photo.title || '';
    lbText.textContent = photo.caption || '';
    lbDate.textContent = formatDate(photo.shotAt || photo.createdAt);
    lbLikeCount.textContent = (photo.likes || 0).toLocaleString();

    const tags = photo.tags || [];
    lbTags.innerHTML = tags.map(t => `<span class="lb-tag">${escapeHtml(t)}</span>`).join(' ');
    lbLike.classList.remove('liked');
    if (isPhotoLiked(photo.photoSeq)) {
      lbLike.classList.add('liked');
    }
    // 관리자 삭제 버튼 표시
    if (lbDelete) {
      lbDelete.style.display = window.isAdmin ? 'inline-flex' : 'none';
    }

    const e = photo.exif || {};
    document.getElementById('exifModel').textContent    = e.model     || '—';
    document.getElementById('exifMaker').textContent    = e.maker     || '—';
    document.getElementById('exifAperture').textContent = e.aperture  || '—';
    document.getElementById('exifShutter').textContent  = e.shutter   || '—';
    document.getElementById('exifIso').textContent      = e.iso ? 'ISO ' + e.iso : '—';
    document.getElementById('exifFocal').textContent    = e.focal ? e.focal + 'mm' : '—';
    document.getElementById('exifLens').textContent     = e.lens      || '—';

    // 이미지: 로드 완료 시까지 숨기고 로딩 스피너 표시
    lbImg.style.opacity = '0';
    const imgSrc = photo.src || photo.thumb || '';
    const tempImg = new Image();
    tempImg.onload = function() {
      lbImg.src = imgSrc;
      lbImg.alt = photo.title || '';
      requestAnimationFrame(() => {
        document.querySelector('.lb-img-wrap').classList.add('has-image');
        lbImg.style.opacity = '1';
        hideLoading();
      });
    };
    tempImg.onerror = function() {
      lbImg.src = imgSrc;
      lbImg.alt = photo.title || '';
      document.querySelector('.lb-img-wrap').classList.add('has-image');
      lbImg.style.opacity = '1';
      hideLoading();
    };
    tempImg.src = imgSrc;
  }

  function navigate(dir) {
    currentIndex = (currentIndex + dir + allPhotos.length) % allPhotos.length;
    // 전환 시 이전 이미지 숨기고 로딩 표시
    lbImg.style.opacity = '0';
    document.querySelector('.lb-img-wrap').classList.remove('has-image');
    showLoading();
    populateLightbox(allPhotos[currentIndex]);
  }

  // ── 쿠키 기반 좋아요 중복 방지 (24시간 쿨다운) ──
  const LIKE_COOLDOWN_MS = 24 * 60 * 60 * 1000;

  function getLikedPhotos() {
    const match = document.cookie.match(/(?:^|;\s*)liked_photos=([^;]*)/);
    if (!match) return {};
    const map = {};
    match[1].split('|').forEach(entry => {
      const parts = entry.split(':');
      if (parts.length === 2) {
        const seq = parseInt(parts[0]);
        const ts = parseInt(parts[1]);
        if (!isNaN(seq) && !isNaN(ts)) map[seq] = ts;
      }
    });
    return map;
  }

  function isPhotoLiked(photoSeq) {
    const liked = getLikedPhotos();
    const ts = liked[photoSeq];
    return ts != null && (Date.now() - ts) < LIKE_COOLDOWN_MS;
  }

  function addLikedPhoto(photoSeq) {
    const liked = getLikedPhotos();
    const now = Date.now();
    liked[photoSeq] = now;
    // 만료된 항목 정리
    Object.keys(liked).forEach(k => {
      if (now - liked[k] >= LIKE_COOLDOWN_MS) delete liked[k];
    });
    const val = Object.entries(liked).map(([k, v]) => k + ':' + v).join('|');
    document.cookie = 'liked_photos=' + val + '; path=/; max-age=' + (60 * 60 * 24 * 2);
  }

  // ── 토스트 알림 ──
  function showToast(message) {
    const existing = document.getElementById('likeToast');
    if (existing) existing.remove();

    const toast = document.createElement('div');
    toast.id = 'likeToast';
    toast.style.cssText = 'position:fixed;top:24px;left:50%;transform:translateX(-50%) translateY(-20px);' +
      'background:rgba(30,28,26,0.92);color:#fff;padding:14px 28px;border-radius:8px;font-size:0.88rem;' +
      'z-index:100000;opacity:0;transition:opacity 0.3s,transform 0.3s;pointer-events:none;' +
      'backdrop-filter:blur(8px);box-shadow:0 4px 20px rgba(0,0,0,0.3);max-width:90vw;text-align:center;';
    toast.textContent = message;
    document.body.appendChild(toast);

    requestAnimationFrame(() => {
      toast.style.opacity = '1';
      toast.style.transform = 'translateX(-50%) translateY(0)';
    });

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateX(-50%) translateY(-20px)';
      setTimeout(() => toast.remove(), 300);
    }, 2000);
  }

  // ── 이벤트 바인딩 ──
  function initEventListeners() {
    // 초기 SSR 데이터 수집
    allPhotos = parsePhotosFromCards();

    // SSR 카드에 클릭 이벤트 바인딩
    grid.querySelectorAll('.photo-card').forEach(card => {
      card.addEventListener('click', function() {
        openLightboxFromCard(this);
      });
    });

    // Lightbox 이벤트
    document.getElementById('lbClose').addEventListener('click', closeLightbox);
    document.getElementById('lbBackdrop').addEventListener('click', closeLightbox);
    document.getElementById('lbPrev').addEventListener('click', () => navigate(-1));
    document.getElementById('lbNext').addEventListener('click', () => navigate(1));

    document.getElementById('lbExifToggle').addEventListener('click', () => {
      document.getElementById('lbExif').classList.toggle('collapsed');
    });

    // 모바일 사이드바 토글
    document.getElementById('lbMobileToggle').addEventListener('click', () => {
      document.querySelector('.lb-sidebar').classList.toggle('expanded');
    });

    // 좋아요 버튼
    lbLike.addEventListener('click', async () => {
      const p = allPhotos[currentIndex];
      if (!p) return;
      if (lbLike.classList.contains('liked') || isPhotoLiked(p.photoSeq)) {
        lbLike.classList.add('liked');
        showToast('24시간 이내에 이미 좋아요를 누른 사진입니다.');
        return;
      }
      lbLike.classList.add('liked');
      try {
        const res = await fetch('/api/photo/like-count?photoSeq=' + p.photoSeq, { method: 'POST' });
        const json = await res.json();
        if (res.ok) {
          const likeCount = json.data.likeCount || 0;
          p.likes = likeCount;
          lbLikeCount.textContent = likeCount.toLocaleString();
          addLikedPhoto(p.photoSeq);
          // 썸네일 카드의 좋아요 수도 업데이트
          const card = grid.querySelector(`.photo-card[data-photo-seq="${p.photoSeq}"]`);
          if (card) {
            card.dataset.likes = likeCount;
            const likesSpan = card.querySelector('.overlay-stat:first-child span');
            if (likesSpan) likesSpan.textContent = likeCount.toLocaleString();
          }
        } else {
          const msg = json.message || '좋아요 요청에 실패했습니다.';
          showToast(msg);
          addLikedPhoto(p.photoSeq);
        }
      } catch (e) {
        console.warn('좋아요 요청 실패:', e);
        lbLike.classList.remove('liked');
      }
    });

    // 삭제 버튼 (관리자 전용)
    if (lbDelete) {
      const deleteModal = document.getElementById('deleteConfirmModal');
      const deleteYes   = document.getElementById('deleteConfirmYes');
      const deleteNo    = document.getElementById('deleteConfirmNo');

      lbDelete.addEventListener('click', () => {
        deleteModal.style.display = 'flex';
      });
      deleteNo.addEventListener('click', () => {
        deleteModal.style.display = 'none';
      });
      deleteModal.addEventListener('click', e => {
        if (e.target === deleteModal) deleteModal.style.display = 'none';
      });
      deleteYes.addEventListener('click', async () => {
        const p = allPhotos[currentIndex];
        if (!p) return;
        deleteModal.style.display = 'none';
        try {
          const res = await fetch('/api/photo/delete?photoSeq=' + p.photoSeq, { method: 'DELETE' });
          if (res.ok) {
            // 라이트박스 닫기
            closeLightbox();
            // DOM에서 해당 카드 제거
            const card = grid.querySelector(`.photo-card[data-photo-seq="${p.photoSeq}"]`);
            if (card) card.remove();
            // allPhotos 배열에서 제거 후 인덱스 재정렬
            allPhotos.splice(currentIndex, 1);
            grid.querySelectorAll('.photo-card').forEach((c, i) => { c.dataset.index = i; });
            if (currentIndex >= allPhotos.length) currentIndex = Math.max(0, allPhotos.length - 1);
            showToast('사진이 삭제되었습니다.');
          } else {
            showToast('삭제에 실패했습니다.');
          }
        } catch (e) {
          console.warn('삭제 요청 실패:', e);
          showToast('삭제 요청 중 오류가 발생했습니다.');
        }
      });
    }

    // 키보드 네비게이션
    document.addEventListener('keydown', e => {
      if (!lightbox.classList.contains('open')) return;
      if (e.key === 'Escape') closeLightbox();
      if (e.key === 'ArrowLeft')  navigate(-1);
      if (e.key === 'ArrowRight') navigate(1);
    });

    // 카테고리 필터 버튼
    document.querySelectorAll('.filter-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        const categorySeq = btn.dataset.categorySeq || null;
        loadCategoryPhotos(categorySeq);
      });
    });

    // 뷰 토글
    document.getElementById('btnGrid').addEventListener('click', () => {
      isMasonry = false;
      grid.classList.remove('masonry');
      document.getElementById('btnGrid').classList.add('active');
      document.getElementById('btnMasonry').classList.remove('active');
    });
    document.getElementById('btnMasonry').addEventListener('click', () => {
      isMasonry = true;
      grid.classList.add('masonry');
      document.getElementById('btnMasonry').classList.add('active');
      document.getElementById('btnGrid').classList.remove('active');
    });

    // Load More 버튼
    loadMoreBtn.addEventListener('click', () => {
      if (!loadMoreBtn.disabled) {
        loadMorePhotos();
      }
    });
  }

  // ── 초기화 ──
  initEventListeners();
})();

