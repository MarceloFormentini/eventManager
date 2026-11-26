package br.com.formentini.eventManager.config;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import jakarta.servlet.ServletException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter =
            new RequestCorrelationFilter();

    @Test
    void shouldPreserveSafeRequestIdAndExposeItToTheRequestScope() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/v1/events");
        request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "trace-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdDuringRequest = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                requestIdDuringRequest.set(MDC.get(
                        RequestCorrelationFilter.REQUEST_ID_MDC_KEY
                ))
        );

        assertEquals("trace-123", requestIdDuringRequest.get());
        assertEquals("trace-123", response.getHeader(
                RequestCorrelationFilter.REQUEST_ID_HEADER
        ));
        assertNull(MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY));
    }

    @Test
    void shouldReplaceUnsafeRequestIdWithGeneratedUuid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/v1/events");
        request.addHeader(
                RequestCorrelationFilter.REQUEST_ID_HEADER,
                "bad\r\nforged-log-entry"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
        });

        String requestId = response.getHeader(
                RequestCorrelationFilter.REQUEST_ID_HEADER
        );
        assertNotNull(requestId);
        assertTrue(requestId.matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
        ));
    }

    @Test
    void shouldGenerateRequestIdWhenHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
        });

        assertNotNull(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER));
        assertTrue(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER)
                .matches("[0-9a-f-]{36}"));
    }

    @Test
    void shouldClearRequestIdFromMdcWhenRequestProcessingFails() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "trace-failure");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThrows(ServletException.class, () -> filter.doFilter(
                request,
                response,
                (servletRequest, servletResponse) -> {
                    assertEquals("trace-failure",
                            MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY));
                    throw new ServletException("expected test failure");
                }
        ));

        assertEquals("trace-failure",
                response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER));
        assertNull(MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY));
    }
}
