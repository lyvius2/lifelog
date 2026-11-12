package com.walter.lifelog.web.controller;

import com.walter.lifelog.content.dto.StaticPageContent;
import com.walter.lifelog.content.service.StaticPageService;
import com.walter.lifelog.shared.util.AdminAccessResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.nio.charset.StandardCharsets;

/**
 * 등록된 정적 웹페이지를 /static/{path} 로 내려줍니다.
 *
 * 주 용도는 claude.ai 에서 만든 단일 파일 아티팩트입니다. 마크업을 그대로 내보내야
 * 아티팩트가 claude.ai 에서와 같이 동작하므로, 감싸지 않고 본문으로 씁니다.
 *
 * 다만 이렇게 하면 업로드한 스크립트가 이 사이트와 같은 origin 에서 돌게 됩니다.
 * CSP 로 외부 전송과 form 전송을 막고, 스크립트 출처는 아티팩트가 실제로 쓰는
 * CDN 으로만 한정합니다. connect-src 에 'self' 를 넣지 않는 것이 핵심입니다 —
 * 넣으면 업로드한 스크립트가 관리자 세션으로 이 사이트의 API 를 부를 수 있습니다.
 *
 * 비공개로 등록한 페이지는 로그인한 관리자에게만 보입니다. 권한이 없을 때 403 을
 * 주면 그 경로에 무언가 있다는 사실이 드러나므로, 없는 경로와 똑같이 404 를 줍니다.
 */
@Controller
public class StaticPageViewController {

    private static final String CONTENT_SECURITY_POLICY = String.join(" ",
            "default-src 'self' data: blob:;",
            "script-src 'unsafe-inline' 'unsafe-eval' https://cdnjs.cloudflare.com",
            "https://cdn.jsdelivr.net https://unpkg.com https://cdn.tailwindcss.com",
            "https://code.jquery.com;",
            "style-src 'unsafe-inline' https://fonts.googleapis.com https://cdnjs.cloudflare.com",
            "https://cdn.jsdelivr.net https://unpkg.com;",
            "font-src data: https://fonts.gstatic.com https://cdnjs.cloudflare.com https://cdn.jsdelivr.net;",
            "img-src 'self' data: blob: https:;",
            "media-src 'self' data: blob: https:;",
            "connect-src https://cdnjs.cloudflare.com https://cdn.jsdelivr.net https://unpkg.com;",
            "frame-ancestors 'self';",
            "base-uri 'none';",
            "form-action 'none';",
            "object-src 'none'");

    private final StaticPageService staticPageService;
    private final String jwtSecretKey;

    public StaticPageViewController(StaticPageService staticPageService,
                                    @Value("${jwt.secret-key:tempKey}") String jwtSecretKey) {
        this.staticPageService = staticPageService;
        this.jwtSecretKey = jwtSecretKey;
    }

    @GetMapping("/static/{path}")
    @ResponseBody
    public ResponseEntity<String> serve(@PathVariable("path") String path,
                                        HttpServletRequest request) throws NoResourceFoundException {
        final StaticPageContent content = staticPageService.getContent(path);
        if (content == null || (!content.isPublic() && !isAdmin(request))) {
            throw new NoResourceFoundException(HttpMethod.GET, path, "/static/" + path);
        }
        return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
                .cacheControl(CacheControl.noCache())
                .header("Content-Security-Policy", CONTENT_SECURITY_POLICY)
                .header("Referrer-Policy", "no-referrer")
                .body(content.getHtml());
    }

    private boolean isAdmin(HttpServletRequest request) {
        final HttpSession session = request.getSession(false);
        final Object sessionUserSeq = session == null ? null : session.getAttribute("userSeq");
        return AdminAccessResolver
                .resolveUserSeq(request.getHeader("Authorization"), sessionUserSeq, jwtSecretKey)
                .isPresent();
    }
}
