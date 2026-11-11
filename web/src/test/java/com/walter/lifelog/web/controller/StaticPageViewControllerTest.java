package com.walter.lifelog.web.controller;

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

    private final StaticPageService staticPageService = mock(StaticPageService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new StaticPageViewController(staticPageService))
            .build();

    @Test
    @DisplayName("serve - 등록된 경로면 저장된 마크업을 UTF-8 HTML 로 그대로 내려준다")
    void serve_registeredPath_returnsMarkupAsIs() throws Exception {
        // given
        when(staticPageService.getHtml("my-page")).thenReturn(MARKUP);

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
        when(staticPageService.getHtml(anyString())).thenReturn(MARKUP);

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
        when(staticPageService.getHtml("missing")).thenReturn(null);

        // when
        final var result = mockMvc.perform(get("/static/missing"))
                .andExpect(status().isNotFound())
                .andReturn();

        // then
        assertThat(result.getResolvedException()).isInstanceOf(NoResourceFoundException.class);
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
