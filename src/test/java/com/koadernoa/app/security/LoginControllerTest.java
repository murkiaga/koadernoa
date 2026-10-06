package com.koadernoa.app.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ExtendedModelMap;

class LoginControllerTest {

    private final LoginController controller = new LoginController(
            null,
            null,
            new StubAuthProviderStatusService());

    @Test
    void hasierakIrakasleGuneraBirbideratzenDu() {
        assertThat(controller.hasiera()).isEqualTo("redirect:/irakasle");
    }

    @Test
    void loginakAutentikatutakoErabiltzaileaIrakasleGuneraBirbideratzenDu() {
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("irakaslea", null, "ROLE_IRAKASLEA");

        assertThat(controller.loginPage(new ExtendedModelMap(), authentication))
                .isEqualTo("redirect:/irakasle");
    }

    @Test
    void loginakErabiltzaileAnonimoariLoginPantailaErakustenDio() {
        AnonymousAuthenticationToken authentication = new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.loginPage(model, authentication)).isEqualTo("login");
        assertThat(model)
                .containsEntry("googleEnabled", true)
                .containsEntry("googleConfigured", true)
                .containsEntry("ldapEnabled", false)
                .containsEntry("ldapConfigured", true);
    }

    private static class StubAuthProviderStatusService extends AuthProviderStatusService {

        StubAuthProviderStatusService() {
            super(null, null);
        }

        @Override
        public boolean isGoogleEnabled() {
            return true;
        }

        @Override
        public boolean isGoogleConfigured() {
            return true;
        }

        @Override
        public boolean isLdapEnabled() {
            return false;
        }

        @Override
        public boolean isLdapConfigured() {
            return true;
        }
    }
}
