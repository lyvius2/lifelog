document.addEventListener('DOMContentLoaded', function() {
  'use strict';

  /* ════════════════════════════════════════
     STATE
  ════════════════════════════════════════ */
  var state = {
    queue: [],
    selected: null,
    applyAll: false,
    tags: {},
    uploaded: []
  };

  var uidCounter = 1;
  function uid() { return 'ph_' + (uidCounter++); }

  /* ════════════════════════════════════════
     DOM REFS
  ════════════════════════════════════════ */
  var dropZone      = document.getElementById('dropZone');
  var fileInput     = document.getElementById('file-input');
  var queueList     = document.getElementById('queueList');
  var queueHeader   = document.getElementById('queueHeader');
  var queueCount    = document.getElementById('queueCount');
  var uploadActions = document.getElementById('uploadActions');
  var uploadOverall = document.getElementById('uploadOverall');
  var metaThumb     = document.getElementById('metaThumb');
  var metaThumbPh   = document.getElementById('metaThumbPh');
  var applyAllBar   = document.getElementById('applyAllBar');
  var toastEl       = document.getElementById('toast');
  var galleryStrip  = document.getElementById('galleryStrip');
  var stripEmpty    = document.getElementById('stripEmpty');
  var stripSub      = document.getElementById('stripSub');
  var tagField      = document.getElementById('tagField');
  var tagWrap       = document.getElementById('tagWrap');

  if (!dropZone || !fileInput) {
    console.error('photo-upload.js: 필수 DOM 요소를 찾을 수 없습니다.');
    return;
  }

  /* ════════════════════════════════════════
     DRAG & DROP
  ════════════════════════════════════════ */
  ['dragenter','dragover'].forEach(function(evt) {
    dropZone.addEventListener(evt, function(e) {
      e.preventDefault();
      e.stopPropagation();
      dropZone.classList.add('drag-over');
    });
  });
  ['dragleave','dragend'].forEach(function(evt) {
    dropZone.addEventListener(evt, function(e) {
      e.preventDefault();
      e.stopPropagation();
      dropZone.classList.remove('drag-over');
    });
  });
  dropZone.addEventListener('drop', function(e) {
    e.preventDefault();
    e.stopPropagation();
    dropZone.classList.remove('drag-over');
    var files = [];
    if (e.dataTransfer && e.dataTransfer.files) {
      for (var i = 0; i < e.dataTransfer.files.length; i++) {
        if (e.dataTransfer.files[i].type.startsWith('image/')) {
          files.push(e.dataTransfer.files[i]);
        }
      }
    }
    if (files.length) handleFiles(files);
  });
  dropZone.addEventListener('click', function(e) {
    if (e.target.closest && e.target.closest('.drop-btn')) return;
    fileInput.click();
  });
  fileInput.addEventListener('change', function() {
    if (fileInput.files && fileInput.files.length) {
      var arr = [];
      for (var i = 0; i < fileInput.files.length; i++) arr.push(fileInput.files[i]);
      handleFiles(arr);
    }
    fileInput.value = '';
  });

  window.addMoreFiles = function() { fileInput.click(); };

  /* ════════════════════════════════════════
     FILE HANDLING
  ════════════════════════════════════════ */
  function handleFiles(files) {
    var MAX_SIZE = 20 * 1024 * 1024;
    var MAX_COUNT = 20;
    var allowed = files.slice(0, MAX_COUNT - state.queue.length);
    var rejected = 0;
    var promises = [];

    for (var fi = 0; fi < allowed.length; fi++) {
      (function(file) {
        if (file.size > MAX_SIZE) { rejected++; return; }
        var id = uid();
        var p = readAsDataURL(file).then(function(previewUrl) {
          var item = {
            id: id, file: file, preview: previewUrl,
            meta: { title: stripExt(file.name), caption: '', category: getDefaultCategory(), date: todayStr() },
            exif: null, tags: [], status: 'idle', progress: 0
          };
          state.queue.push(item);
          state.tags[id] = [];
          tryReadExif(id, file);
        });
        promises.push(p);
      })(allowed[fi]);
    }

    Promise.all(promises).then(function() {
      if (rejected) showToast(rejected + '개 파일은 20MB를 초과하여 제외되었습니다.', 'error');
      renderQueue();
      if (state.queue.length && !state.selected) selectItem(state.queue[0].id);
    });
  }

  function readAsDataURL(file) {
    return new Promise(function(resolve) {
      var reader = new FileReader();
      reader.onload = function(e) { resolve(e.target.result); };
      reader.readAsDataURL(file);
    });
  }

  function stripExt(name) {
    return name.replace(/\.[^.]+$/, '').replace(/[-_]/g, ' ');
  }

  function todayStr() {
    var d = new Date();
    return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
  }

  function getDefaultCategory() {
    var first = document.querySelector('.cat-opt');
    return first ? first.dataset.cat : '';
  }

  /* ════════════════════════════════════════
     EXIF READER
  ════════════════════════════════════════ */
  function tryReadExif(id, file) {
    if (typeof window.exifr === 'undefined') {
      console.log('exifr 라이브러리가 로드되지 않았습니다.');
      return;
    }
    try {
      window.exifr.parse(file, {
        tiff: true, xmp: false, icc: false, iptc: false,
        pick: ['Make','Model','LensModel','FocalLength','FNumber','ExposureTime','ISO','Flash','GPSLatitude','GPSLongitude','DateTimeOriginal']
      }).then(function(data) {
        if (!data) return;
        var item = findItem(id);
        if (!item) return;
        var exif = {
          maker: data.Make || null, model: data.Model || null, lens: data.LensModel || null,
          aperture: data.FNumber != null ? 'f/' + Number(data.FNumber).toFixed(1) : null,
          shutter: data.ExposureTime != null ? formatShutter(data.ExposureTime) : null,
          iso: data.ISO != null ? String(data.ISO) : null,
          focal: data.FocalLength != null ? Math.round(data.FocalLength) + 'mm' : null,
          flash: data.Flash != null ? (data.Flash > 0 ? 'On' : 'Off') : null,
          gps: (data.GPSLatitude != null && data.GPSLongitude != null)
                 ? toDecimal(data.GPSLatitude).toFixed(5) + '\u00B0, ' + toDecimal(data.GPSLongitude).toFixed(5) + '\u00B0' : null,
          date: data.DateTimeOriginal ? formatExifDate(data.DateTimeOriginal) : null
        };
        item.exif = exif;
        if (exif.date) item.meta.date = exif.date;
        if (state.selected === id) renderExifPanel(exif);
        renderQueue();
      }).catch(function(err) { console.log('EXIF 읽기 실패:', err); });
    } catch(e) { console.log('EXIF 파싱 오류:', e); }
  }

  function findItem(id) {
    for (var i = 0; i < state.queue.length; i++) {
      if (state.queue[i].id === id) return state.queue[i];
    }
    return null;
  }

  function formatShutter(v) {
    if (v >= 1) return v + 's';
    return '1/' + Math.round(1 / v) + 's';
  }

  function toDecimal(coord) {
    if (typeof coord === 'number') return coord;
    if (Array.isArray(coord)) {
      var d = coord[0] || 0;
      var m = coord[1] || 0;
      var s = coord[2] || 0;
      return d + m / 60 + s / 3600;
    }
    var n = Number(coord);
    return isNaN(n) ? 0 : n;
  }

  function formatExifDate(d) {
    if (!d) return todayStr();
    try {
      var dt = new Date(d);
      if (isNaN(dt.getTime())) return todayStr();
      return dt.getFullYear() + '-' + String(dt.getMonth() + 1).padStart(2, '0') + '-' + String(dt.getDate()).padStart(2, '0');
    } catch(e) { return todayStr(); }
  }

  function renderExifPanel(exif) {
    var badge = document.getElementById('exifBadge');
    if (!badge) return;
    if (!exif) { badge.textContent = 'Not Available'; badge.className = 'exif-badge none'; clearExifPanel(); return; }
    var hasAny = !!(exif.maker || exif.model || exif.lens || exif.aperture || exif.shutter || exif.iso || exif.focal || exif.flash || exif.gps);
    badge.textContent = hasAny ? 'Detected' : 'Not Available';
    badge.className = hasAny ? 'exif-badge found' : 'exif-badge none';
    var cam = [exif.maker, exif.model].filter(Boolean).join(' ') || null;
    setExifVal('exCamera', cam);
    setExifVal('exAperture', exif.aperture);
    setExifVal('exShutter', exif.shutter);
    setExifVal('exIso', exif.iso ? 'ISO ' + exif.iso : null);
    setExifVal('exFocal', exif.focal);
    setExifVal('exFlash', exif.flash);
    setExifVal('exLens', exif.lens);
    setExifVal('exGps', exif.gps);
  }

  function setExifVal(elId, val) {
    var el = document.getElementById(elId);
    if (el) el.textContent = val || '\u2014';
  }

  function clearExifPanel() {
    var ids = ['exCamera','exAperture','exShutter','exIso','exFocal','exFlash','exLens','exGps'];
    for (var i = 0; i < ids.length; i++) setExifVal(ids[i], null);
    var badge = document.getElementById('exifBadge');
    if (badge) { badge.textContent = 'Not Available'; badge.className = 'exif-badge none'; }
  }

  /* ════════════════════════════════════════
     QUEUE RENDERING
  ════════════════════════════════════════ */
  function renderQueue() {
    var hasItems = state.queue.length > 0;
    if (queueHeader) queueHeader.style.display = hasItems ? 'flex' : 'none';
    if (uploadActions) uploadActions.style.display = hasItems ? 'flex' : 'none';
    if (applyAllBar) applyAllBar.style.display = hasItems && state.queue.length > 1 ? 'flex' : 'none';
    if (queueCount) queueCount.textContent = state.queue.length;
    if (!queueList) return;
    queueList.innerHTML = '';

    for (var qi = 0; qi < state.queue.length; qi++) {
      (function(item) {
        var el = document.createElement('div');
        el.className = 'queue-item' + (item.id === state.selected ? ' selected' : '') + (item.status !== 'idle' ? ' ' + item.status : '');
        el.setAttribute('data-id', item.id);
        var fileSize = (item.file.size / 1024 / 1024).toFixed(1) + ' MB';
        var dim = item._dim ? item._dim.w + '\u00D7' + item._dim.h : '';
        var statusHtml = '';
        if (item.status === 'idle') statusHtml = '<div class="qi-status idle"><svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg></div>';
        else if (item.status === 'uploading') statusHtml = '<div class="qi-status uploading"><div class="spinner"></div></div>';
        else if (item.status === 'done') statusHtml = '<div class="qi-status done"><svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg></div>';
        else statusHtml = '<div class="qi-status error"><svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg></div>';

        el.innerHTML =
          '<img class="qi-thumb" src="' + item.preview + '" alt="" />' +
          '<div class="qi-info"><div class="qi-name">' + escapeHtml(item.meta.title || item.file.name) + '</div>' +
          '<div class="qi-meta"><span>' + fileSize + '</span>' + (dim ? '<span>' + dim + '</span>' : '') +
          (item.exif ? '<span>\uD83D\uDCF7 EXIF</span>' : '') + '</div></div>' +
          statusHtml +
          '<button class="qi-remove">\u00D7</button>' +
          '<div class="qi-progress"><div class="qi-progress-bar" style="width:' + item.progress + '%"></div></div>';

        var removeBtn = el.querySelector('.qi-remove');
        if (removeBtn) removeBtn.addEventListener('click', function(ev) { ev.stopPropagation(); removeItem(item.id); });
        el.addEventListener('click', function() { selectItem(item.id); });
        queueList.appendChild(el);

        if (!item._dim) {
          var img = new Image();
          img.onload = function() { item._dim = { w: img.naturalWidth, h: img.naturalHeight }; };
          img.src = item.preview;
        }
      })(state.queue[qi]);
    }
    updateOverallStatus();
    var uploadAllBtn = document.getElementById('uploadAllBtn');
    if (uploadAllBtn) {
      var allDone = state.queue.length === 0;
      if (!allDone) { allDone = true; for (var i = 0; i < state.queue.length; i++) { if (state.queue[i].status !== 'done') { allDone = false; break; } } }
      uploadAllBtn.disabled = state.queue.length === 0 || allDone;
    }
  }

  function escapeHtml(str) { var d = document.createElement('div'); d.textContent = str; return d.innerHTML; }

  function updateOverallStatus() {
    if (!uploadOverall) return;
    var total = state.queue.length, done = 0, errors = 0;
    for (var i = 0; i < state.queue.length; i++) { if (state.queue[i].status === 'done') done++; if (state.queue[i].status === 'error') errors++; }
    if (!total) { uploadOverall.innerHTML = ''; return; }
    uploadOverall.innerHTML = '\uC644\uB8CC <b>' + done + '/' + total + '</b>' + (errors ? ' &nbsp;\u00B7&nbsp; <span style="color:#e57373">' + errors + ' \uC624\uB958</span>' : '');
  }

  /* ════════════════════════════════════════
     SELECT ITEM
  ════════════════════════════════════════ */
  function selectItem(id) {
    state.selected = id;
    var item = findItem(id);
    if (!item) return;
    if (metaThumb) { metaThumb.src = item.preview; metaThumb.style.opacity = '1'; }
    if (metaThumbPh) metaThumbPh.style.display = 'none';
    var mTitle = document.getElementById('mTitle');
    var mCaption = document.getElementById('mCaption');
    var mDate = document.getElementById('mDate');
    if (mTitle) mTitle.value = item.meta.title || '';
    if (mCaption) mCaption.value = item.meta.caption || '';
    if (mDate) mDate.value = item.meta.date || todayStr();
    var catOpts = document.querySelectorAll('.cat-opt');
    for (var i = 0; i < catOpts.length; i++) {
      if (catOpts[i].dataset.cat === item.meta.category) catOpts[i].classList.add('active');
      else catOpts[i].classList.remove('active');
    }
    renderTagsFor(id);
    renderExifPanel(item.exif);
    var saveBtn = document.getElementById('saveMetaBtn');
    if (saveBtn) { saveBtn.disabled = false; saveBtn.textContent = '\uBA54\uD0C0\uB370\uC774\uD130 \uC800\uC7A5'; saveBtn.className = 'save-btn'; }
    renderQueue();
  }

  /* ════════════════════════════════════════
     TAGS
  ════════════════════════════════════════ */
  if (tagField) {
    tagField.addEventListener('keydown', function(e) {
      if ((e.key === 'Enter' || e.key === ',') && tagField.value.trim()) {
        e.preventDefault();
        if (!state.selected) return;
        var tag = tagField.value.trim().replace(/,$/, '');
        if (tag.charAt(0) !== '#') tag = '#' + tag;
        if (!state.tags[state.selected]) state.tags[state.selected] = [];
        var exists = false;
        for (var i = 0; i < state.tags[state.selected].length; i++) { if (state.tags[state.selected][i] === tag) { exists = true; break; } }
        if (!exists) { state.tags[state.selected].push(tag); renderTagsFor(state.selected); }
        tagField.value = '';
      }
      if (e.key === 'Backspace' && !tagField.value && state.selected) {
        var tags = state.tags[state.selected];
        if (tags && tags.length) { tags.pop(); renderTagsFor(state.selected); }
      }
    });
  }

  function renderTagsFor(id) {
    if (!tagWrap || !tagField) return;
    var chips = tagWrap.querySelectorAll('.tag-chip');
    for (var i = 0; i < chips.length; i++) chips[i].remove();
    var tags = state.tags[id] || [];
    for (var j = 0; j < tags.length; j++) {
      (function(tag, idx) {
        var chip = document.createElement('div');
        chip.className = 'tag-chip';
        chip.appendChild(document.createTextNode(tag));
        var btn = document.createElement('button');
        btn.className = 'tag-chip-x';
        btn.textContent = '\u00D7';
        btn.addEventListener('click', function(ev) { ev.stopPropagation(); removeTag(id, idx); });
        chip.appendChild(btn);
        tagWrap.insertBefore(chip, tagField);
      })(tags[j], j);
    }
  }

  function removeTag(id, idx) {
    if (state.tags[id]) { state.tags[id].splice(idx, 1); if (state.selected === id) renderTagsFor(id); }
  }
  window.removeTag = removeTag;

  /* ════════════════════════════════════════
     CATEGORY SELECTION
  ════════════════════════════════════════ */
  var catOptEls = document.querySelectorAll('.cat-opt');
  for (var ci = 0; ci < catOptEls.length; ci++) {
    catOptEls[ci].addEventListener('click', function() {
      var all = document.querySelectorAll('.cat-opt');
      for (var j = 0; j < all.length; j++) all[j].classList.remove('active');
      this.classList.add('active');
    });
  }

  /* ════════════════════════════════════════
     SAVE META (전역 노출)
  ════════════════════════════════════════ */
  window.saveCurrentMeta = function() {
    if (!state.selected) return;
    var item = findItem(state.selected);
    if (!item) return;
    var mTitle = document.getElementById('mTitle');
    var mCaption = document.getElementById('mCaption');
    var mDate = document.getElementById('mDate');
    if (mTitle) item.meta.title = mTitle.value;
    if (mCaption) item.meta.caption = mCaption.value;
    if (mDate) item.meta.date = mDate.value;
    var activeOpt = document.querySelector('.cat-opt.active');
    item.meta.category = activeOpt ? activeOpt.dataset.cat : getDefaultCategory();
    item.tags = (state.tags[state.selected] || []).slice();
    if (state.applyAll) {
      for (var i = 0; i < state.queue.length; i++) {
        if (state.queue[i].id === state.selected) continue;
        state.queue[i].meta.caption = item.meta.caption;
        state.queue[i].meta.category = item.meta.category;
        state.queue[i].tags = item.tags.slice();
      }
    }
    var btn = document.getElementById('saveMetaBtn');
    if (btn) {
      btn.textContent = '\u2713 \uC800\uC7A5\uB428';
      btn.classList.add('saved');
      setTimeout(function() { btn.textContent = '\uBA54\uD0C0\uB370\uC774\uD130 \uC800\uC7A5'; btn.classList.remove('saved'); }, 2000);
    }
    renderQueue();
  };

  /* ════════════════════════════════════════
     APPLY ALL TOGGLE (전역 노출)
  ════════════════════════════════════════ */
  window.toggleApplyAll = function() {
    state.applyAll = !state.applyAll;
    var toggle = document.getElementById('applyAllToggle');
    if (toggle) toggle.classList.toggle('on', state.applyAll);
  };

  /* ════════════════════════════════════════
     UPLOAD (전역 노출)
  ════════════════════════════════════════ */
  window.uploadAll = function() {
    var pending = [];
    for (var i = 0; i < state.queue.length; i++) { if (state.queue[i].status === 'idle') pending.push(state.queue[i]); }
    if (!pending.length) return;
    var uploadAllBtn = document.getElementById('uploadAllBtn');
    if (uploadAllBtn) uploadAllBtn.disabled = true;
    var idx = 0;
    function next() {
      if (idx >= pending.length) {
        var doneCount = 0;
        var errorCount = 0;
        for (var j = 0; j < pending.length; j++) {
          if (pending[j].status === 'done') doneCount++;
          else if (pending[j].status === 'error') errorCount++;
        }
        if (errorCount === 0) {
          showToast(doneCount + '\uC7A5 \uC5C5\uB85C\uB4DC \uC644\uB8CC!');
        } else if (doneCount === 0) {
          showToast('\uC5C5\uB85C\uB4DC\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4. (' + errorCount + '\uAC74 \uC624\uB958)', 'error');
        } else {
          showToast(doneCount + '\uC7A5 \uC644\uB8CC, ' + errorCount + '\uAC74 \uC2E4\uD328', 'error');
        }
        renderGalleryStrip();
        return;
      }
      uploadSingle(pending[idx], function() { idx++; next(); });
    }
    next();
  };

  function uploadSingle(item, callback) {
    item.status = 'uploading';
    renderQueue();

    var exif = item.exif || {};
    var shotAtStr = null;
    if (item.meta.date) {
      shotAtStr = item.meta.date + 'T00:00:00';
    }

    var uploadRequest = {
      title: item.meta.title || '',
      caption: item.meta.caption || '',
      categorySeq: Number(item.meta.category) || 0,
      tags: (state.tags[item.id] || []),
      maker: exif.maker || null,
      model: exif.model || null,
      lens: exif.lens || null,
      aperture: exif.aperture || null,
      shutter: exif.shutter || null,
      iso: exif.iso || null,
      focalLength: exif.focal ? parseInt(exif.focal) || null : null,
      flash: exif.flash || null,
      latitude: exif.gps ? parseGpsCoord(exif.gps, 0) : null,
      longitude: exif.gps ? parseGpsCoord(exif.gps, 1) : null,
      shotAt: shotAtStr
    };

    var formData = new FormData();
    formData.append('file', item.file);
    formData.append('uploadRequest', new Blob([JSON.stringify(uploadRequest)], { type: 'application/json' }));

    var xhr = new XMLHttpRequest();
    xhr.upload.onprogress = function(e) {
      if (e.lengthComputable) {
        item.progress = (e.loaded / e.total) * 100;
        if (queueList) { var bar = queueList.querySelector('[data-id="' + item.id + '"] .qi-progress-bar'); if (bar) bar.style.width = item.progress + '%'; }
      }
    };
    xhr.onload = function() {
      if (xhr.status < 200 || xhr.status >= 300) {
        item.status = 'error';
        var errMsg = '\uC5C5\uB85C\uB4DC \uC2E4\uD328 (HTTP ' + xhr.status + ')';
        try {
          var errRes = JSON.parse(xhr.responseText);
          if (errRes.message) errMsg = errRes.message;
        } catch(e) {}
        showToast(errMsg, 'error');
        renderQueue(); callback();
        return;
      }
      try {
        var res = JSON.parse(xhr.responseText);
        if (res.success && res.data) { item.status = 'done'; item.progress = 100; item.drivePath = res.data.drivePath || ''; item.fileId = res.data.fileId || ''; }
        else { item.status = 'error'; showToast(res.message || '\uC5C5\uB85C\uB4DC \uC2E4\uD328', 'error'); }
      } catch(err) { item.status = 'error'; showToast('\uC751\uB2F5 \uCC98\uB9AC \uC911 \uC624\uB958', 'error'); }
      renderQueue(); callback();
    };
    xhr.onerror = function() { item.status = 'error'; showToast('\uB124\uD2B8\uC6CC\uD06C \uC624\uB958', 'error'); renderQueue(); callback(); };
    xhr.open('POST', '/api/photo/upload');
    xhr.send(formData);
  }

  function parseGpsCoord(gpsStr, index) {
    if (!gpsStr) return null;
    var parts = gpsStr.split(',');
    if (parts.length <= index) return null;
    var val = parseFloat(parts[index].replace('\u00B0', '').trim());
    return isNaN(val) ? null : val;
  }

  /* ════════════════════════════════════════
     REMOVE / CLEAR (전역 노출)
  ════════════════════════════════════════ */
  function removeItem(id) {
    var idx = -1;
    for (var i = 0; i < state.queue.length; i++) { if (state.queue[i].id === id) { idx = i; break; } }
    if (idx === -1) return;
    state.queue.splice(idx, 1);
    delete state.tags[id];
    if (state.selected === id) {
      state.selected = state.queue.length > 0 ? state.queue[0].id : null;
      if (state.selected) selectItem(state.selected);
      else {
        if (metaThumb) metaThumb.style.opacity = '0';
        if (metaThumbPh) metaThumbPh.style.display = 'flex';
        var saveBtn = document.getElementById('saveMetaBtn');
        if (saveBtn) saveBtn.disabled = true;
        clearExifPanel();
      }
    }
    renderQueue();
  }
  window.removeItem = removeItem;

  window.clearQueue = function() {
    state.queue = []; state.tags = {}; state.selected = null;
    if (metaThumb) metaThumb.style.opacity = '0';
    if (metaThumbPh) metaThumbPh.style.display = 'flex';
    if (applyAllBar) applyAllBar.style.display = 'none';
    var saveBtn = document.getElementById('saveMetaBtn');
    if (saveBtn) saveBtn.disabled = true;
    clearExifPanel(); renderQueue();
  };

  /* ════════════════════════════════════════
     GALLERY STRIP
  ════════════════════════════════════════ */
  function renderGalleryStrip() {
    var done = [];
    for (var i = 0; i < state.queue.length; i++) { if (state.queue[i].status === 'done') done.push(state.queue[i]); }
    if (!done.length) return;
    for (var j = 0; j < done.length; j++) {
      var exists = false;
      for (var k = 0; k < state.uploaded.length; k++) { if (state.uploaded[k].id === done[j].id) { exists = true; break; } }
      if (!exists) state.uploaded.push(Object.assign({}, done[j]));
    }
    if (stripEmpty) stripEmpty.style.display = 'none';
    if (stripSub) stripSub.textContent = state.uploaded.length + '\uC7A5\uC758 \uC0AC\uC9C4\uC774 \uC5C5\uB85C\uB4DC\uB418\uC5C8\uC2B5\uB2C8\uB2E4';
    if (!galleryStrip) return;
    galleryStrip.innerHTML = '';
    for (var m = 0; m < state.uploaded.length; m++) {
      (function(item) {
        var card = document.createElement('div');
        card.className = 'strip-card';
        var img = document.createElement('img');
        img.src = item.preview; img.alt = item.meta.title || ''; img.loading = 'lazy';
        card.appendChild(img);
        var overlay = document.createElement('div');
        overlay.className = 'strip-card-overlay';
        var titleDiv = document.createElement('div');
        titleDiv.className = 'strip-card-title';
        titleDiv.textContent = item.meta.title || '\uC81C\uBAA9 \uC5C6\uC74C';
        overlay.appendChild(titleDiv);
        card.appendChild(overlay);
        var statusDiv = document.createElement('div');
        statusDiv.className = 'strip-card-status ' + item.status;
        statusDiv.innerHTML = item.status === 'done'
          ? '<svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>'
          : '<svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5"><circle cx="12" cy="12" r="10"/></svg>';
        card.appendChild(statusDiv);
        card.style.opacity = '0'; card.style.transition = 'opacity 0.35s, transform 0.35s'; card.style.transform = 'scale(0.95)';
        galleryStrip.appendChild(card);
        requestAnimationFrame(function() { requestAnimationFrame(function() { card.style.opacity = '1'; card.style.transform = 'scale(1)'; }); });
      })(state.uploaded[m]);
    }
  }

  /* ════════════════════════════════════════
     TOAST
  ════════════════════════════════════════ */
  function showToast(msg, type) {
    if (!toastEl) return;
    if (!type) type = 'success';
    toastEl.textContent = (type === 'success' ? '\u2713  ' : '\u26A0  ') + msg;
    toastEl.className = 'toast' + (type === 'error' ? ' error' : '');
    toastEl.classList.add('show');
    setTimeout(function() { toastEl.classList.remove('show'); }, 3000);
  }

  /* ════════════════════════════════════════
     INIT
  ════════════════════════════════════════ */
  var mDateInit = document.getElementById('mDate');
  if (mDateInit) mDateInit.value = todayStr();

  console.log('photo-upload.js 초기화 완료');
});
