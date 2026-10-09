package com.koadernoa.app.funtzionalitateak.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import com.koadernoa.app.funtzionalitateak.admin.seed.service.KatalogoAkademikoaSeedService;
import com.koadernoa.app.objektuak.konfigurazioa.service.AplikazioAukeraService;
import com.koadernoa.app.objektuak.jokabidea.service.JokabideDesegokiaTxantiloiService;
import com.koadernoa.app.security.AuthProviderStatusService;

class AdminControllerTest {

    private final AplikazioAukeraService aukerak = mock(AplikazioAukeraService.class);
    private final AdminController controller = new AdminController(
            aukerak,
            mock(AuthProviderStatusService.class),
            mock(KatalogoAkademikoaSeedService.class),
            mock(JokabideDesegokiaTxantiloiService.class));

    @Test
    void ikastetxearenHerriaKonfigurazioSistemanGordetzenDu() {
        String view = controller.saveIkastetxea("  Gernika-Lumo  ");

        verify(aukerak).set(AplikazioAukeraService.IKASTETXEAREN_HERRIA, "Gernika-Lumo");
        assertThat(view).contains("tab=ikastetxea").contains("success=");
    }

    @Test
    void herriaHutsikGordetzeaOnartzenDu() {
        controller.saveIkastetxea("   ");

        verify(aukerak).set(AplikazioAukeraService.IKASTETXEAREN_HERRIA, "");
    }
}
