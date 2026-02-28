/* ══════════════════════════════════════════
   STATE
══════════════════════════════════════════ */
const state = {
  mode: 'write',
  tags: [],
  unsaved: false,
  saveTimer: null,
};

const SNIPPETS = {
  /* ── 텍스트 서식 ── */
  heading: `# 제목 1

## 소제목

### 하위 제목`,
  bold_italic: `**굵은 텍스트**

*기울임 텍스트*

~~취소선 텍스트~~

***굵은 기울임 텍스트***`,
  hr: `---`,

  /* ── 리스트 ── */
  ul: `- 항목 1
- 항목 2
  - 하위 항목 2-1
  - 하위 항목 2-2
- 항목 3`,
  ol: `1. 첫 번째 항목
2. 두 번째 항목
   1. 하위 항목 2-1
   2. 하위 항목 2-2
3. 세 번째 항목`,
  checklist: `- [x] 완료된 작업
- [x] 이것도 완료
- [ ] 아직 미완료
- [ ] 이것도 미완료`,

  /* ── 인용 & 강조 ── */
  callout: `> **📌 Note**
> 여기에 중요한 내용을 작성하세요.`,
  callout_warning: `> **⚠️ Warning**
> 이 작업을 수행하기 전에 반드시 백업을 해주세요.`,
  callout_tip: `> **💡 Tip**
> 이 방법을 사용하면 성능을 40% 이상 개선할 수 있습니다.`,
  blockquote: `> "프로그래밍은 생각하는 방법에 관한 것이지, 타이핑에 관한 것이 아니다."
> — 무명`,

  /* ── 링크 & 미디어 ── */
  link: `[링크 텍스트](https://example.com)`,
  image: `![이미지 설명](https://example.com/image.png)`,
  image_caption: `![이미지 설명](https://example.com/image.png)

*▲ 이미지에 대한 캡션을 여기에 작성하세요*`,
  youtube: `[![동영상 제목](https://img.youtube.com/vi/VIDEO_ID/maxresdefault.jpg)](https://www.youtube.com/watch?v=VIDEO_ID)`,
  badge: `![Build](https://img.shields.io/badge/build-passing-brightgreen)
![Version](https://img.shields.io/badge/version-1.0.0-blue)
![License](https://img.shields.io/badge/license-MIT-green)`,

  /* ── 표 ── */
  table: `| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| value | String | — | 대기열 이름 또는 URL |
| deletionPolicy | enum | DEFAULT | 메시지 삭제 정책 |
| concurrency | String | "1" | 동시 스레드 수 |`,
  table_align: `| 왼쪽 정렬 | 가운데 정렬 | 오른쪽 정렬 |
|:-----------|:----------:|----------:|
| 텍스트 | 텍스트 | 텍스트 |
| 왼쪽 | 가운데 | 오른쪽 |
| Left | Center | Right |`,

  /* ── 코드 ── */
  inline_code: `\`인라인 코드\`를 이렇게 사용합니다.`,
  codeblock_empty: `\`\`\`java
// 여기에 코드를 작성하세요
\`\`\``,
  springboot: `\`\`\`java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
\`\`\``,
  kafkalistener: `\`\`\`java
@Component
public class KafkaConsume {

    @KafkaListener(topics = "TOPIC-NAME", concurrency = "1", autoStartup = "false")
    public void listener(ConsumerRecord<String, String> payload) {
        System.out.println("Received Key : " + payload.key());
        System.out.println("Received Message : " + payload.value());
    }
}
\`\`\``,
  sqslistener: `\`\`\`java
@Component
public class SqsConsume {

    @SqsListener(value = "QUEUE-NAME", deletionPolicy = ON_SUCCESS)
    public void listener(@Payload String payload, @Headers Map<String, String> headers) {
        log.info("Received Payload : {}", payload);
    }
}
\`\`\``,
  restcontroller: `\`\`\`java
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ExampleController {

    private final ExampleService exampleService;

    @GetMapping("/items")
    public ResponseEntity<List<ItemResponse>> getItems() {
        return ResponseEntity.ok(exampleService.findAll());
    }
}
\`\`\``,
  service: `\`\`\`java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExampleService {

    private final ExampleRepository exampleRepository;

    public List<Example> findAll() {
        return exampleRepository.findAll();
    }
}
\`\`\``,
  entity: `\`\`\`java
@Entity
@Table(name = "example")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Example {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;
}
\`\`\``,
  kotlin_data: `\`\`\`kotlin
data class UserResponse(
    val id: Long,
    val name: String,
    val email: String,
    val createdAt: LocalDateTime
)
\`\`\``,
  python_func: `\`\`\`python
def process_data(items: list[dict]) -> list[dict]:
    """데이터를 처리하여 반환합니다."""
    result = []
    for item in items:
        processed = {
            "id": item["id"],
            "name": item["name"].strip(),
            "score": round(item["score"], 2)
        }
        result.append(processed)
    return result
\`\`\``,
  typescript_interface: `\`\`\`typescript
interface ApiResponse<T> {
  status: number;
  message: string;
  data: T;
  timestamp: string;
}

interface UserDto {
  id: number;
  name: string;
  email: string;
  roles: string[];
}
\`\`\``,
  sql_select: `\`\`\`sql
SELECT
    u.user_seq,
    u.name,
    u.email,
    COUNT(p.post_seq) AS post_count
FROM users u
LEFT JOIN posts p ON u.user_seq = p.user_seq
WHERE u.is_active = true
GROUP BY u.user_seq, u.name, u.email
ORDER BY post_count DESC
LIMIT 10;
\`\`\``,
  sql_create: `\`\`\`sql
CREATE TABLE example (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_name (name),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
\`\`\``,
  shell_cmd: `\`\`\`bash
# Docker 이미지 빌드 & 실행
docker build -t my-app:latest .
docker run -d -p 8080:8080 --name my-app my-app:latest

# 로그 확인
docker logs -f my-app

# API 호출 테스트
curl -s http://localhost:8080/api/health | jq .
\`\`\``,
  dockerfile: `\`\`\`dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY build/libs/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-Xms512m -Xmx512m -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
\`\`\``,
  docker_compose: `\`\`\`yaml
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=live
      - DB_HOST=db
    depends_on:
      db:
        condition: service_healthy

  db:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: mydb
    ports:
      - "3306:3306"
    volumes:
      - db-data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      retries: 5

volumes:
  db-data:
\`\`\``,
  nginx_conf: `\`\`\`nginx
server {
    listen 80;
    server_name example.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /static/ {
        alias /var/www/static/;
        expires 30d;
    }
}
\`\`\``,

  /* ── 설정 파일 ── */
  appyml: `\`\`\`yaml
spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/dbname
    username: root
    password: password
    driver-class-name: org.mariadb.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

server:
  port: 8080
\`\`\``,
  gradle: `\`\`\`groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.0'
    id 'io.spring.dependency-management' version '1.1.4'
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    runtimeOnly 'org.mariadb.jdbc:mariadb-java-client'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
\`\`\``,
  json_sample: `\`\`\`json
{
  "id": 1,
  "name": "홍길동",
  "email": "hong@example.com",
  "roles": ["ADMIN", "USER"],
  "profile": {
    "bio": "백엔드 개발자",
    "github": "https://github.com/example"
  },
  "createdAt": "2025-01-01T00:00:00Z"
}
\`\`\``,
  env_file: `\`\`\`bash
# Database
DB_HOST=localhost
DB_PORT=3306
DB_NAME=mydb
DB_USER=root
DB_PASSWORD=secret

# Application
APP_PORT=8080
APP_ENV=production

# External API
API_KEY=your-api-key-here
API_SECRET=your-api-secret-here
\`\`\``,

  /* ── 레이아웃 ── */
  toc: `## 📋 목차

- [개요](#개요)
- [배경](#배경)
- [구현](#구현)
- [결과](#결과)
- [마치며](#마치며)`,
  footnote: `여기에 각주가 필요한 텍스트입니다[^1]. 또 다른 참조[^2]도 있습니다.

[^1]: 첫 번째 각주 설명입니다.
[^2]: 두 번째 각주 설명입니다.`,
  details: `<details>
<summary>🔽 클릭하여 펼치기</summary>

여기에 접힌 상태로 숨겨둘 내용을 작성합니다.

- 목록도 가능합니다
- 코드 블록도 넣을 수 있습니다

</details>`,
  math_formula: `인라인 수식: $E = mc^2$

블록 수식:

$$
\\sum_{i=1}^{n} x_i = x_1 + x_2 + \\cdots + x_n
$$`,
  mermaid: `\`\`\`mermaid
graph TD
    A[Client] --> B[Load Balancer]
    B --> C[Server 1]
    B --> D[Server 2]
    C --> E[(Database)]
    D --> E
\`\`\``,
  post_template: `# 제목을 입력하세요

## 개요

이 글에서는 ...에 대해 다룹니다.

## 배경

왜 이 주제를 선택했는지 설명합니다.

## 구현

### 핵심 코드

\`\`\`java
// 핵심 로직
\`\`\`

### 실행 결과

> **📌 Note**
> 실행 시 주의사항

## 결과

| 항목 | Before | After |
|------|--------|-------|
| 응답시간 | 500ms | 200ms |
| 처리량 | 100 TPS | 300 TPS |

## 마치며

이번 작업을 통해 배운 점을 정리합니다.`,
};

/* ══════════════════════════════════════════
   EDITOR REFS
══════════════════════════════════════════ */
const ed     = document.getElementById('editor-area');
const lineNs = document.getElementById('line-numbers');

/* ══════════════════════════════════════════
   LINE NUMBERS
══════════════════════════════════════════ */
function updateLineNumbers() {
  const lines = ed.value.split('\n');
  const total = lines.length;
  let html = '';
  for (let i = 1; i <= total; i++) html += `<div class="line-number">${i}</div>`;
  lineNs.innerHTML = html;
  syncLineNumberScroll();
  updateCursorPos();
  updateWordCount();
  updateOutline();
  if (state.mode !== 'write') renderPreview();
}

function syncLineNumberScroll() {
  lineNs.scrollTop = ed.scrollTop;
}

function updateCursorPos() {
  const val = ed.value.substring(0, ed.selectionStart);
  const lines = val.split('\n');
  const ln = lines.length;
  const col = lines[lines.length - 1].length + 1;
  document.getElementById('sb-ln').textContent = ln;
  document.getElementById('sb-col').textContent = col;

  // Highlight current line number
  document.querySelectorAll('.line-number').forEach((el, i) => {
    el.classList.toggle('active', i + 1 === ln);
  });
}

ed.addEventListener('input', updateLineNumbers);
ed.addEventListener('scroll', syncLineNumberScroll);
ed.addEventListener('keyup', updateCursorPos);
ed.addEventListener('click', updateCursorPos);
ed.addEventListener('input', () => markUnsaved());

updateLineNumbers();

/* ══════════════════════════════════════════
   WORD COUNT
══════════════════════════════════════════ */
function updateWordCount() {
  const txt = ed.value;
  const chars = txt.length;
  const words = txt.trim() ? txt.trim().split(/\s+/).length : 0;
  const lines = txt.split('\n').length;
  const readMin = Math.max(1, Math.round(words / 200));
  document.getElementById('wc-chars').textContent = chars.toLocaleString();
  document.getElementById('wc-words').textContent = words.toLocaleString();
  document.getElementById('wc-lines').textContent = lines.toLocaleString();
  document.getElementById('wc-read').textContent = readMin + 'm';
}

/* ══════════════════════════════════════════
   OUTLINE
══════════════════════════════════════════ */
function updateOutline() {
  const lines = ed.value.split('\n');
  const items = [];
  lines.forEach((line, idx) => {
    const m = line.match(/^(#{1,3})\s+(.+)/);
    if (m) items.push({ level: m[1].length, text: m[2].trim(), line: idx });
  });

  const empty = document.getElementById('outline-empty');
  const list  = document.getElementById('outline-list');

  if (items.length === 0) {
    empty.style.display = 'block';
    list.innerHTML = '';
    return;
  }
  empty.style.display = 'none';
  list.innerHTML = items.map(it => `
    <div class="outline-item h${it.level}" onclick="jumpToLine(${it.line})">
      <span class="outline-hash">${'#'.repeat(it.level)}</span>
      <span>${it.text}</span>
    </div>`).join('');
}

function jumpToLine(lineIdx) {
  const lines = ed.value.split('\n');
  let pos = 0;
  for (let i = 0; i < lineIdx; i++) pos += lines[i].length + 1;
  ed.focus();
  ed.setSelectionRange(pos, pos);
  const lineHeight = 26;
  ed.scrollTop = lineIdx * lineHeight - 80;
}

/* ══════════════════════════════════════════
   TOOLBAR HELPERS
══════════════════════════════════════════ */
function getSelection() {
  return { start: ed.selectionStart, end: ed.selectionEnd, text: ed.value.substring(ed.selectionStart, ed.selectionEnd) };
}

function replaceSelection(newText, cursorOffset = null) {
  const { start, end } = getSelection();
  ed.focus();
  ed.setRangeText(newText, start, end, 'end');
  if (cursorOffset !== null) {
    ed.setSelectionRange(start + cursorOffset, start + cursorOffset);
  }
  updateLineNumbers();
  markUnsaved();
}

function wrapSelection(before, after) {
  const { text } = getSelection();
  if (text) {
    replaceSelection(before + text + after);
  } else {
    const { start } = getSelection();
    replaceSelection(before + after, before.length);
  }
}

function insertHeading(level) {
  if (!level) return;
  const { start } = getSelection();
  const val = ed.value;
  const lineStart = val.lastIndexOf('\n', start - 1) + 1;
  const lineEnd = val.indexOf('\n', start);
  const line = val.substring(lineStart, lineEnd === -1 ? undefined : lineEnd);
  const newLine = '#'.repeat(parseInt(level)) + ' ' + line.replace(/^#+\s*/, '');
  ed.setRangeText(newLine, lineStart, lineEnd === -1 ? val.length : lineEnd, 'end');
  updateLineNumbers();
  markUnsaved();
}

function insertLink() {
  const { text } = getSelection();
  const label = text || '링크 텍스트';
  replaceSelection(`[${label}](https://)`, label.length + 3);
}

function insertImage() {
  replaceSelection(`![이미지 설명](이미지 URL)\n`, 0);
}

function insertListItem(prefix) {
  const { start } = getSelection();
  const val = ed.value;
  const lineStart = val.lastIndexOf('\n', start - 1) + 1;
  const atStart = start === lineStart || val.substring(lineStart, start).trim() === '';
  if (atStart) {
    ed.setRangeText(`${prefix} `, lineStart, lineStart, 'end');
  } else {
    ed.setRangeText(`\n${prefix} `, start, start, 'end');
  }
  updateLineNumbers();
  markUnsaved();
}

function insertQuote() {
  const { text, start, end } = getSelection();
  if (text) {
    replaceSelection(text.split('\n').map(l => '> ' + l).join('\n'));
  } else {
    ed.setRangeText('\n> ', start, end, 'end');
    updateLineNumbers();
    markUnsaved();
  }
}

function insertHR() {
  const { start } = getSelection();
  ed.setRangeText('\n\n---\n\n', start, start, 'end');
  updateLineNumbers();
  markUnsaved();
}

function insertCodeBlock() {
  const { text } = getSelection();
  const inner = text || '// 코드를 여기에 작성하세요';
  replaceSelection(`\`\`\`java\n${inner}\n\`\`\`\n`);
}

function insertTable() {
  replaceSelection(`\n| 컬럼1 | 컬럼2 | 컬럼3 |\n|-------|-------|-------|\n| 값1   | 값2   | 값3   |\n| 값4   | 값5   | 값6   |\n`);
}

function insertSnippet(key) {
  const snippet = SNIPPETS[key];
  if (!snippet) return;
  const { start } = getSelection();
  ed.setRangeText('\n' + snippet + '\n', start, start, 'end');
  updateLineNumbers();
  markUnsaved();
  ed.focus();
}

/* ══════════════════════════════════════════
   KEYBOARD SHORTCUTS
══════════════════════════════════════════ */
ed.addEventListener('keydown', e => {
  const mod = e.metaKey || e.ctrlKey;

  if (mod && e.key === 's') { e.preventDefault(); savePost(); return; }
  if (mod && e.key === 'b') { e.preventDefault(); wrapSelection('**','**'); return; }
  if (mod && e.key === 'i') { e.preventDefault(); wrapSelection('*','*'); return; }
  if (mod && e.key === 'k') { e.preventDefault(); insertLink(); return; }
  if (mod && e.key === 'e') { e.preventDefault(); wrapSelection('`','`'); return; }
  if (mod && e.shiftKey && e.key === 'C') { e.preventDefault(); insertCodeBlock(); return; }
  if (mod && e.shiftKey && e.key === 'B') { e.preventDefault(); insertQuote(); return; }
  if (mod && e.key === 'p') { e.preventDefault(); setMode(state.mode === 'preview' ? 'write' : 'preview'); return; }
  if (mod && e.shiftKey && e.key === 'P') { e.preventDefault(); setMode(state.mode === 'split' ? 'write' : 'split'); return; }

  // Tab → 2 spaces
  if (e.key === 'Tab') {
    e.preventDefault();
    const { start, end } = getSelection();
    ed.setRangeText('  ', start, end, 'end');
    updateLineNumbers();
    return;
  }

  // ? → shortcuts
  if (e.key === '?' && !mod && document.activeElement !== ed) {
    toggleShortcuts();
  }
});

document.addEventListener('keydown', e => {
  if (e.key === 'Escape') {
    document.getElementById('shortcut-sheet').classList.remove('open');
    closeModal();
  }
  if (e.key === '?' && document.activeElement !== ed) {
    toggleShortcuts();
  }
});

/* ══════════════════════════════════════════
   MODE TOGGLE
══════════════════════════════════════════ */
function setMode(mode) {
  state.mode = mode;
  const writePan   = document.getElementById('write-pane');
  const previewPan = document.getElementById('preview-pane');

  document.getElementById('mode-write').classList.toggle('active', mode === 'write');
  document.getElementById('mode-split').classList.toggle('active', mode === 'split');
  document.getElementById('mode-preview').classList.toggle('active', mode === 'preview');

  if (mode === 'write') {
    writePan.style.display = 'flex';
    writePan.classList.remove('split');
    previewPan.classList.remove('split', 'visible');
    previewPan.style.display = 'none';
  } else if (mode === 'split') {
    writePan.style.display = 'flex';
    writePan.classList.add('split');
    previewPan.style.display = 'block';
    previewPan.classList.add('split');
    previewPan.classList.remove('visible');
    renderPreview();
  } else {
    writePan.style.display = 'none';
    previewPan.style.display = 'block';
    previewPan.classList.add('visible');
    previewPan.classList.remove('split');
    renderPreview();
  }
}

/* ══════════════════════════════════════════
   MARKDOWN PREVIEW RENDERER
══════════════════════════════════════════ */
function renderPreview() {
  const title = document.getElementById('post-title').value || '제목 없음';
  document.getElementById('preview-title').textContent = title;

  const now = new Date();
  document.getElementById('preview-date').textContent =
    `${now.getFullYear()}. ${String(now.getMonth()+1).padStart(2,'0')}. ${String(now.getDate()).padStart(2,'0')}`;

  const tagHtml = state.tags.map(t => `<span class="preview-tag">${t}</span>`).join(' ');
  document.getElementById('preview-tags').innerHTML = tagHtml;

  document.getElementById('preview-body').innerHTML = markdownToHtml(ed.value);
}

// Code block store for placeholder protection
const codeBlockStore = [];
function restoreCodeBlocks(html) {
  return html.replace(/\x00CODEBLOCK(\d+)\x00/g, (_, i) => codeBlockStore[i] || '');
}

function markdownToHtml(md) {
  /*
   * MarkdownConverter.java (서버사이드 CommonMark) 와 동일한 결과를 내기 위한
   * 클라이언트사이드 변환 함수.
   *
   * 매핑 규칙:
   *   # H1 → <h3>   ## H2 → <h5>   ### H3 → <h6>
   *   ```lang → <div class="code-wrap"> 구조 (toolbar+dots+lang+copy)
   *   들여쓰기 코드블록(4스페이스) → 동일 code-wrap (lang: text)
   *   > 인용 → <div class="callout">
   *   --- → <hr class="post-divider" />
   *   ![alt](src) → <figure class="post-image"> + <figcaption>
   *   GFM table → <table> (thead/tbody, 정렬)
   *   - [ ] / - [x] → task list checkbox
   *   중첩 리스트 → 재귀 파싱
   *   autolink → URL 자동 링크 변환
   */

  // ─── 1) fenced code block → placeholder ───
  codeBlockStore.length = 0;
  let html = md.replace(/```(\w*)\n([\s\S]*?)```/g, (_, lang, code) => {
    // 서버: 끝의 \n 제거
    if (code.endsWith('\n')) code = code.slice(0, -1);
    const block = buildCodeWrap(lang, code);
    codeBlockStore.push(block);
    return `\x00CODEBLOCK${codeBlockStore.length - 1}\x00`;
  });

  // ─── 2) indented code block (4 spaces) → placeholder ───
  html = html.replace(/((?:^(?: {4}|\t).+\n?)+)/gm, block => {
    let code = block.replace(/^(?: {4}|\t)/gm, '');
    if (code.endsWith('\n')) code = code.slice(0, -1);
    const blk = buildCodeWrap('', code);
    codeBlockStore.push(blk);
    return `\x00CODEBLOCK${codeBlockStore.length - 1}\x00`;
  });

  // ─── 3) Headings (H1→h3, H2→h5, H3→h6) ───
  html = html.replace(/^### (.+)$/gm, (_, t) => `<h6 id="${slugify(stripInline(t))}">${inlineMarkup(t)}</h6>`);
  html = html.replace(/^## (.+)$/gm,  (_, t) => `<h5 id="${slugify(stripInline(t))}">${inlineMarkup(t)}</h5>`);
  html = html.replace(/^# (.+)$/gm,   (_, t) => `<h3 id="${slugify(stripInline(t))}">${inlineMarkup(t)}</h3>`);

  // ─── 4) Blockquote → callout ───
  html = html.replace(/((?:^> .*\n?)+)/gm, block => {
    const inner = block.replace(/^> ?/gm, '').trim();
    const innerHtml = inner.split('\n\n').map(p => `<p>${inlineMarkup(p.replace(/\n/g, ' '))}</p>`).join('\n');
    return `<div class="callout">\n${innerHtml}\n</div>`;
  });

  // ─── 5) HR → post-divider ───
  html = html.replace(/^---$/gm, '<hr class="post-divider" />');

  // ─── 6) Table (GFM: thead/tbody, 정렬) ───
  html = html.replace(/((?:^\|.+\|\n?)+)/gm, m => {
    const rows = m.trim().split('\n');
    if (rows.length < 2) return m;

    // separator row (2번째 줄) 에서 정렬 파싱
    const sepRow = rows[1];
    if (!/^\|[\s:_\-|]+\|$/.test(sepRow.replace(/_/g,'-'))) return m;

    const aligns = sepRow.split('|').slice(1, -1).map(c => {
      c = c.trim();
      if (c.startsWith(':') && c.endsWith(':')) return 'center';
      if (c.endsWith(':')) return 'right';
      return null; // left (default)
    });

    const cellStyle = (i) => aligns[i] ? ` style="text-align:${aligns[i]}"` : '';

    // header
    const hCells = rows[0].split('|').slice(1, -1);
    let out = '<table>\n<thead>\n<tr>' +
      hCells.map((c, i) => `<th${cellStyle(i)}>${inlineMarkup(c.trim())}</th>`).join('') +
      '</tr>\n</thead>\n<tbody>\n';

    // body rows (skip separator at index 1)
    for (let r = 2; r < rows.length; r++) {
      const cells = rows[r].split('|').slice(1, -1);
      out += '<tr>' + cells.map((c, i) => `<td${cellStyle(i)}>${inlineMarkup(c.trim())}</td>`).join('') + '</tr>\n';
    }
    return out + '</tbody>\n</table>';
  });

  // ─── 7) Lists (중첩 + task list) ───
  html = html.replace(/((?:^[ ]*(?:[-*+]|\d+\.) .+\n?)+)/gm, block => {
    return parseList(block.trimEnd().split('\n'), 0).html;
  });

  // ─── 8) Images → figure (서버와 동일) ───
  html = html.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, (_, alt, src) => {
    const caption = alt ? `<figcaption>${alt}</figcaption>` : '';
    return `<figure class="post-image"><img src="${src}" alt="${alt.replace(/"/g,'&quot;')}" loading="lazy" />${caption}</figure>`;
  });

  // ─── 9) Links ───
  html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2">$1</a>');

  // ─── 10) Inline markup for remaining text ───
  html = applyInlineToPlainText(html);

  // ─── 11) Autolink (URL을 자동으로 링크) ───
  html = html.replace(/(^|[^"'>])(https?:\/\/[^\s<]+)/g, '$1<a href="$2">$2</a>');

  // ─── 12) Paragraphs ───
  html = html.split('\n\n').map(block => {
    block = block.trim();
    if (!block) return '';
    if (/^<(h[1-6]|ul|ol|pre|blockquote|hr|div|table|img|figure)/.test(block)) return block;
    if (/^\x00CODEBLOCK\d+\x00$/.test(block)) return block;
    return '<p>' + block.replace(/\n/g, '<br>') + '</p>';
  }).join('\n');

  return restoreCodeBlocks(html);
}

// ─── 코드 블록 HTML 빌더 (서버 renderCodeWrap과 동일 구조) ───
function buildCodeWrap(lang, code) {
  const langDisplay = lang ? lang.toLowerCase() : 'text';
  const langClass = lang ? ` class="language-${lang}"` : '';
  const escaped = code.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  return `<div class="code-wrap">` +
    `<div class="code-toolbar">` +
    `<div class="code-toolbar-dots"><span></span><span></span><span></span></div>` +
    `<span class="code-toolbar-lang">${langDisplay}</span>` +
    `<button class="code-copy-btn" onclick="copyCode(this)">Copy</button>` +
    `</div>` +
    `<pre><code${langClass}>${escaped}</code></pre>` +
    `</div>`;
}

// ─── 인라인 마크업 변환 (bold, italic, strikethrough, inline code) ───
function inlineMarkup(text) {
  let s = text;
  s = s.replace(/\*\*\*(.+?)\*\*\*/g, '<strong><em>$1</em></strong>');
  s = s.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
  s = s.replace(/\*(.+?)\*/g, '<em>$1</em>');
  s = s.replace(/~~(.+?)~~/g, '<del>$1</del>');
  s = s.replace(/`([^`]+)`/g, '<code>$1</code>');
  return s;
}

// ─── 인라인 마크업에서 마크업 기호를 제거하여 순수 텍스트 추출 (slugify용) ───
function stripInline(text) {
  return text
    .replace(/\*\*\*(.+?)\*\*\*/g, '$1')
    .replace(/\*\*(.+?)\*\*/g, '$1')
    .replace(/\*(.+?)\*/g, '$1')
    .replace(/~~(.+?)~~/g, '$1')
    .replace(/`([^`]+)`/g, '$1');
}

// ─── 블록 단위가 아닌 남은 plain text에 인라인 마크업 적용 ───
function applyInlineToPlainText(html) {
  // 이미 태그 안에 있는 부분은 건드리지 않고, 태그 밖 텍스트만 인라인 변환
  return html.replace(/(^|>)([^<]+)(<|$)/g, (m, pre, text, post) => {
    if (/^\x00CODEBLOCK/.test(text)) return m;
    return pre + inlineMarkup(text) + post;
  });
}

// ─── 중첩 리스트 파서 (task list 포함) ───
function parseList(lines, baseIndent) {
  let result = '';
  let i = 0;
  // 첫 줄로 ordered/unordered 판별
  const firstLine = lines[0] ? lines[0].trimStart() : '';
  const isOrdered = /^\d+\. /.test(firstLine);
  const tag = isOrdered ? 'ol' : 'ul';

  result += `<${tag}>`;
  while (i < lines.length) {
    const line = lines[i];
    const indent = line.search(/\S/);
    if (indent < baseIndent) break;

    if (indent > baseIndent) {
      // 하위 리스트 수집
      const subLines = [];
      while (i < lines.length) {
        const subIndent = lines[i].search(/\S/);
        if (subIndent <= baseIndent) break;
        subLines.push(lines[i]);
        i++;
      }
      const sub = parseList(subLines, subLines[0].search(/\S/));
      // 이전 </li>을 제거하고 서브리스트를 삽입한 후 다시 닫기
      if (result.endsWith('</li>')) {
        result = result.slice(0, -5) + sub.html + '</li>';
      } else {
        result += sub.html;
      }
      continue;
    }

    const content = line.trimStart().replace(/^(?:[-*+]|\d+\.)\s+/, '');

    // Task list 체크박스 처리
    let itemHtml;
    const taskMatch = content.match(/^\[([ xX])\]\s*(.*)/);
    if (taskMatch) {
      const checked = taskMatch[1] !== ' ' ? ' checked="checked" disabled="disabled"' : ' disabled="disabled"';
      itemHtml = `<li><input type="checkbox"${checked} /> ${inlineMarkup(taskMatch[2])}</li>`;
    } else {
      itemHtml = `<li>${inlineMarkup(content)}</li>`;
    }
    result += itemHtml;
    i++;
  }
  result += `</${tag}>`;
  return { html: result, consumed: i };
}

function slugify(text) {
  return text.toLowerCase()
    .replace(/[^\p{L}\p{N}\s-]/gu, '')
    .replace(/\s+/g, '-')
    .replace(/-+/g, '-')
    .replace(/^-|-$/g, '');
}


// Copy code button in preview
function copyCode(btn) {
  const pre = btn.closest('.code-wrap').querySelector('pre');
  navigator.clipboard.writeText(pre.innerText).then(() => {
    btn.textContent = 'Copied!';
    btn.classList.add('copied');
    setTimeout(() => {
      btn.textContent = 'Copy';
      btn.classList.remove('copied');
    }, 2000);
  });
}

// Live preview sync
ed.addEventListener('input', () => {
  if (state.mode !== 'write') renderPreview();
});
document.getElementById('post-title').addEventListener('input', () => {
  if (state.mode !== 'write') renderPreview();
  markUnsaved();
});

/* ══════════════════════════════════════════
   TAGS
══════════════════════════════════════════ */
const tagInput = document.getElementById('tag-input');
tagInput.addEventListener('keydown', e => {
  if ((e.key === 'Enter' || e.key === ',') && tagInput.value.trim()) {
    e.preventDefault();
    addTag(tagInput.value.trim().replace(/,$/, ''));
    tagInput.value = '';
  }
  if (e.key === 'Backspace' && !tagInput.value && state.tags.length) {
    removeTag(state.tags.length - 1);
  }
});

function addTag(t) {
  if (!t || state.tags.includes(t)) return;
  state.tags.push(t);
  renderTags();
  markUnsaved();
}

function removeTag(i) {
  state.tags.splice(i, 1);
  renderTags();
  markUnsaved();
}

function renderTags() {
  const wrap = document.getElementById('tags-wrap');
  wrap.querySelectorAll('.tag-chip').forEach(el => el.remove());
  state.tags.forEach((t, i) => {
    const chip = document.createElement('div');
    chip.className = 'tag-chip';
    chip.innerHTML = `${t}<button class="tag-chip-remove" onclick="removeTag(${i})">×</button>`;
    wrap.insertBefore(chip, tagInput);
  });
}

/* ══════════════════════════════════════════
   CATEGORY
══════════════════════════════════════════ */
document.querySelectorAll('.category-opt').forEach(opt => {
  opt.addEventListener('click', () => {
    document.querySelectorAll('.category-opt').forEach(o => o.classList.remove('selected'));
    opt.classList.add('selected');
    opt.querySelector('input[type="radio"]').checked = true;
    document.getElementById('sb-category').textContent = opt.dataset.val;
    markUnsaved();
  });
});

/* ══════════════════════════════════════════
   SAVE
══════════════════════════════════════════ */
function markUnsaved() {
  state.unsaved = true;
  document.getElementById('status-dot').className = 'status-dot unsaved';
  document.getElementById('status-text').textContent = '미저장';

  clearTimeout(state.saveTimer);
  state.saveTimer = setTimeout(() => autoSave(), 3000);
}

function autoSave() {
  savePost(true);
}

function savePost(silent = false) {
  // Collect data
  const selectedCat = document.querySelector('.category-opt.selected');
  const data = {
    postSeq: SERVER_DATA.postSeq,
    title: document.getElementById('post-title').value,
    content: ed.value,
    excerpt: document.getElementById('post-excerpt').value,
    tags: state.tags,
    category: selectedCat?.dataset.val || '',
    categorySeq: selectedCat?.dataset.seq || null,
    savedAt: new Date().toISOString(),
  };

  // TODO: POST /api/posts 로 서버에 저장

  state.unsaved = false;
  document.getElementById('status-dot').className = 'status-dot saved';
  document.getElementById('status-text').textContent = '저장됨';

  if (!silent) showToast();
}

function showToast() {
  const t = document.getElementById('toast');
  t.classList.add('show');
  setTimeout(() => t.classList.remove('show'), 2200);
}

/* ══════════════════════════════════════════
   PUBLISH MODAL
══════════════════════════════════════════ */
function openPublishModal() {
  const title = document.getElementById('post-title').value || '제목 없음';
  const tags  = state.tags.join(', ') || '태그 없음';
  const words = ed.value.trim() ? ed.value.trim().split(/\s+/).length : 0;

  document.getElementById('modal-title-preview').textContent = title;
  document.getElementById('modal-tags-preview').textContent = tags;
  document.getElementById('modal-wc-preview').textContent = `${words} words`;

  // Auto slug
  const slug = title.toLowerCase()
    .replace(/[가-힣]+/g, match => match)
    .replace(/\s+/g, '-')
    .replace(/[^a-z0-9\-가-힣]/g, '');
  document.getElementById('modal-slug').value = slug;

  // Default date/time
  const now = new Date();
  const pad = n => String(n).padStart(2,'0');
  document.getElementById('modal-date').value =
    `${now.getFullYear()}-${pad(now.getMonth()+1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`;

  document.getElementById('publish-modal').classList.add('open');
}

function closeModal() {
  document.getElementById('publish-modal').classList.remove('open');
}

function doPublish() {
  savePost(true);
  closeModal();
  // In real Spring Boot → POST /api/posts
  setTimeout(() => {
    alert('✅ 게시 완료!\n\nSpring Boot 백엔드와 연동하면 실제 발행됩니다.\n(POST /api/posts)');
  }, 100);
}

/* ══════════════════════════════════════════
   SHORTCUTS PANEL
══════════════════════════════════════════ */
function toggleShortcuts() {
  document.getElementById('shortcut-sheet').classList.toggle('open');
}

/* ══════════════════════════════════════════
   RIGHT PANEL TABS
══════════════════════════════════════════ */
function switchRightTab(tab) {
  document.getElementById('tab-outline').classList.toggle('active', tab === 'outline');
  document.getElementById('tab-snippets').classList.toggle('active', tab === 'snippets');
  document.getElementById('outline-panel').style.display = tab === 'outline' ? 'block' : 'none';
  document.getElementById('snippets-panel').style.display = tab === 'snippets' ? 'block' : 'none';
}

/* ══════════════════════════════════════════
   INIT
══════════════════════════════════════════ */
// 이전에 localStorage에 저장된 draft 데이터 정리
try { localStorage.removeItem('walter_blog_draft'); } catch(e) {}

// 서버 데이터로 초기화
(function initFromServer() {
  // 카테고리 선택 상태 초기화 (selected 클래스를 서버 데이터에 맞게 설정)
  const selectedOpt = document.querySelector('.category-opt.selected');
  if (selectedOpt) {
    document.getElementById('sb-category').textContent = selectedOpt.dataset.val;
  } else {
    // 선택된 카테고리가 없으면 첫 번째를 선택
    const firstOpt = document.querySelector('.category-opt');
    if (firstOpt) {
      firstOpt.classList.add('selected');
      firstOpt.querySelector('input[type="radio"]').checked = true;
      document.getElementById('sb-category').textContent = firstOpt.dataset.val;
    }
  }

  // 기존 게시글 수정 시 에디터 내용 초기화
  if (SERVER_DATA.markdownContent) {
    ed.value = SERVER_DATA.markdownContent;
    updateLineNumbers();
  }
})();

// Initial date in statusbar
updateWordCount();
updateOutline();

