package com.walter.lifelog.web.controller;

import com.walter.lifelog.content.dto.StaticPageContent;
import com.walter.lifelog.content.service.StaticPageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("StaticPageViewController 테스트")
class StaticPageViewControllerTest {

    private static final String MARKUP = "<!DOCTYPE html><html><head><title>대시보드</title></head>"
            + "<body><p>한글 본문</p></body></html>";

    private static final StaticPageContent PUBLIC_PAGE = new StaticPageContent(MARKUP, true);
    private static final StaticPageContent PRIVATE_PAGE = new StaticPageContent(MARKUP, false);

    private final StaticPageService staticPageService = mock(StaticPageService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new StaticPageViewController(staticPageService, "test-secret-key"))
            .build();

    @Test
    @DisplayName("serve - 등록된 경로면 저장된 마크업을 UTF-8 HTML 로 그대로 내려준다")
    void serve_registeredPath_returnsMarkupAsIs() throws Exception {
        // given
        when(staticPageService.getContent("my-page")).thenReturn(PUBLIC_PAGE);

        // when & then
        mockMvc.perform(get("/static/my-page"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(content().string(MARKUP));
    }

    @Test
    @DisplayName("serve - 업로드한 스크립트가 이 사이트 API 를 부르지 못하도록 connect-src 에 self 를 넣지 않는다")
    void serve_cspDoesNotAllowConnectToSelf() throws Exception {
        // given
        when(staticPageService.getContent(anyString())).thenReturn(PUBLIC_PAGE);

        // when
        final String csp = mockMvc.perform(get("/static/my-page"))
                .andExpect(header().exists("Content-Security-Policy"))
                .andReturn()
                .getResponse()
                .getHeader("Content-Security-Policy");

        // then
        assertThat(csp).isNotNull();
        final String connectSrc = directive(csp, "connect-src");
        assertThat(connectSrc).doesNotContain("'self'").contains("https://cdn.jsdelivr.net");
        assertThat(directive(csp, "form-action")).contains("'none'");
        assertThat(directive(csp, "object-src")).contains("'none'");
        assertThat(directive(csp, "base-uri")).contains("'none'");
    }

    /*
      실제 앱에서는 CustomErrorController(@ControllerAdvice) 가 이 예외를 받아
      사이트의 not-found 페이지를 렌더링합니다. advice 가 없는 standalone 에서는
      Spring 기본 resolver 가 404 로 변환합니다.
    */
    @Test
    @DisplayName("serve - 등록되지 않은 경로면 NoResourceFoundException 을 던져 404 가 된다")
    void serve_unknownPath_throwsNoResourceFound() throws Exception {
        // given
        when(staticPageService.getContent("missing")).thenReturn(null);

        // when
        final var result = mockMvc.perform(get("/static/missing"))
                .andExpect(status().isNotFound())
                .andReturn();

        // then
        assertThat(result.getResolvedException()).isInstanceOf(NoResourceFoundException.class);
    }

    @Test
    @DisplayName("serve - 비공개 페이지는 로그인하지 않았으면 없는 경로와 똑같이 404 가 된다")
    void serve_privatePageWithoutLogin_returnsNotFound() throws Exception {
        // given
        when(staticPageService.getContent("secret")).thenReturn(PRIVATE_PAGE);

        // when
        final var result = mockMvc.perform(get("/static/secret"))
                .andExpect(status().isNotFound())
                .andReturn();

        // then
        assertThat(result.getResolvedException()).isInstanceOf(NoResourceFoundException.class);
    }

    @Test
    @DisplayName("serve - 비공개 페이지도 세션에 userSeq 가 있으면 내려준다")
    void serve_privatePageWithAdminSession_returnsMarkup() throws Exception {
        // given
        when(staticPageService.getContent("secret")).thenReturn(PRIVATE_PAGE);

        // when & then
        mockMvc.perform(get("/static/secret").sessionAttr("userSeq", 1L))
                .andExpect(status().isOk())
                .andExpect(content().string(MARKUP));
    }

    @Test
    @DisplayName("serve - 세션이 있어도 userSeq 가 없으면 비공개 페이지는 404 가 된다")
    void serve_privatePageWithSessionButNoUserSeq_returnsNotFound() throws Exception {
        // given
        when(staticPageService.getContent("secret")).thenReturn(PRIVATE_PAGE);

        // when
        final var result = mockMvc.perform(get("/static/secret").sessionAttr("other", "value"))
                .andExpect(status().isNotFound())
                .andReturn();

        // then
        assertThat(result.getResolvedException()).isInstanceOf(NoResourceFoundException.class);
    }

    @Test
    @DisplayName("serve - 공개 페이지는 로그인하지 않아도 내려준다")
    void serve_publicPageWithoutLogin_returnsMarkup() throws Exception {
        // given
        when(staticPageService.getContent("open")).thenReturn(PUBLIC_PAGE);

        // when & then
        mockMvc.perform(get("/static/open"))
                .andExpect(status().isOk())
                .andExpect(content().string(MARKUP));
    }

    private static String directive(String csp, String name) {
        for (String part : csp.split(";")) {
            final String trimmed = part.trim();
            if (trimmed.startsWith(name + " ") || trimmed.equals(name)) {
                return trimmed;
            }
        }
        throw new AssertionError("CSP 에 " + name + " 지시문이 없습니다: " + csp);
    }
}
