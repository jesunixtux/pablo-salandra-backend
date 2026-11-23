package cl.duoc.pablo_salandra_backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseBootstrapTests {
    @Test
    void parsesMysqlAndMariadbUrlsIntoServerAndDatabase() {
        assertArrayEquals(new String[] { "jdbc:mysql://127.0.0.1:3306/?serverTimezone=America/Santiago", "bitacora" },
            DatabaseBootstrap.serverAndDatabase("jdbc:mysql://127.0.0.1:3306/bitacora?serverTimezone=America/Santiago"));
        assertArrayEquals(new String[] { "jdbc:mariadb://localhost:3307/", "bitacora_dsy1105_dev" },
            DatabaseBootstrap.serverAndDatabase("jdbc:mariadb://localhost:3307/bitacora_dsy1105_dev"));
    }

    @Test
    void ignoresEverythingThatIsNotASimpleMysqlOrMariadbUrl() {
        assertNull(DatabaseBootstrap.serverAndDatabase("jdbc:h2:file:./data/bitacora;MODE=MySQL"));
        assertNull(DatabaseBootstrap.serverAndDatabase("jdbc:mariadb://127.0.0.1:3306/"));
        assertNull(DatabaseBootstrap.serverAndDatabase(null));
        // Nombres raros no se aceptan: podrian romper el SQL de creacion.
        assertNull(DatabaseBootstrap.serverAndDatabase("jdbc:mysql://127.0.0.1:3306/base;DROP TABLE x"));
        assertNull(DatabaseBootstrap.serverAndDatabase("jdbc:mysql://127.0.0.1:3306/1base"));
    }
}
