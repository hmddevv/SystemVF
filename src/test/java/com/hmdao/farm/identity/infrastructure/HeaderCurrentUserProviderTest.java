package com.hmdao.farm.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hmdao.farm.identity.application.port.UserRepository;
import com.hmdao.farm.shared.domain.UnauthenticatedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Cửa ngõ của mọi request: sai ở đây thì dữ liệu của chủ nông trại này lọt sang chủ nông trại
 * khác. Ở MVP danh tính chỉ là một header, nhưng chính vì rẻ như vậy nên càng phải kiểm kỹ —
 * và khi Phase 5 thay bằng JWT, bộ test này là bản mô tả hành vi phải giữ nguyên.
 */
class HeaderCurrentUserProviderTest {

    private static final long DEFAULT_USER = 1L;

    private final UserRepository users = mock(UserRepository.class);
    private final HeaderCurrentUserProvider provider = new HeaderCurrentUserProvider(users, DEFAULT_USER);

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void noHeaderMeansTheConfiguredDefaultOwner() {
        givenRequestWithHeader(null);
        when(users.existsById(DEFAULT_USER)).thenReturn(true);

        assertThat(provider.currentUserId()).isEqualTo(DEFAULT_USER);
    }

    @Test
    void aBlankHeaderIsTreatedAsAbsentRatherThanAsAnError() {
        givenRequestWithHeader("   ");
        when(users.existsById(DEFAULT_USER)).thenReturn(true);

        assertThat(provider.currentUserId()).isEqualTo(DEFAULT_USER);
    }

    @Test
    void theHeaderDecidesWhoseDataIsBeingRead() {
        givenRequestWithHeader(" 2 ");
        when(users.existsById(2L)).thenReturn(true);

        assertThat(provider.currentUserId()).isEqualTo(2L);
    }

    @Test
    void anUnknownUserIs401NotATreatedAsTheDefaultOwner() {
        givenRequestWithHeader("999");
        when(users.existsById(999L)).thenReturn(false);

        assertThatThrownBy(provider::currentUserId)
                .isInstanceOf(UnauthenticatedException.class)
                .hasMessageContaining("999");
    }

    @Test
    void aHeaderThatIsNotANumberIs401AndSaysWhatWasReceived() {
        givenRequestWithHeader("abc");

        assertThatThrownBy(provider::currentUserId)
                .isInstanceOf(UnauthenticatedException.class)
                .hasMessageContaining("abc");
    }

    /** Ngoài ngữ cảnh HTTP — ví dụ một job nền sau này — vẫn phải có chủ sở hữu để làm việc. */
    @Test
    void outsideAnHttpRequestItFallsBackToTheDefaultOwner() {
        RequestContextHolder.resetRequestAttributes();
        when(users.existsById(DEFAULT_USER)).thenReturn(true);

        assertThat(provider.currentUserId()).isEqualTo(DEFAULT_USER);
    }

    private static void givenRequestWithHeader(String value) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (value != null) {
            request.addHeader(HeaderCurrentUserProvider.HEADER, value);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
