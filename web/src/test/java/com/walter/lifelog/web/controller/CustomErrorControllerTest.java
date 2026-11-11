package com.walter.lifelog.web.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CustomErrorController 테스트")
class CustomErrorControllerTest {

    private final CustomErrorController sut = new CustomErrorController();

    @Test
    @DisplayName("handleNotFound - 화면 경로면 안내 페이지를 404 상태로 내려준다")
    void handleNotFound_viewPath_returnsNotFoundStatus() {
        // given
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/static/missing");

        // when
        final Object result = sut.handleNotFound(request, notFound("/static/missing"));

        // then
        assertThat(result).isInstanceOf(ModelAndView.class);
        final ModelAndView modelAndView = (ModelAndView) result;
        assertThat(modelAndView.getViewName()).isEqualTo("not-found");
        assertThat(modelAndView.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("handleNotFound - API 경로면 JSON 404 로 내려준다")
    void handleNotFound_apiPath_returnsJsonNotFound() {
        // given
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/nope");

        // when
        final Object result = sut.handleNotFound(request, notFound("/api/nope"));

        // then
        assertThat(result).isInstanceOf(ResponseEntity.class);
        final ResponseEntity<?> response = (ResponseEntity<?>) result;
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat((Map<String, Object>) response.getBody()).containsEntry("statusCode", 404);
    }

    private static NoResourceFoundException notFound(String path) {
        return new NoResourceFoundException(HttpMethod.GET, path, path);
    }
}
