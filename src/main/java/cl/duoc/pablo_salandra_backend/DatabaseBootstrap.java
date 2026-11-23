package cl.duoc.pablo_salandra_backend;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.flyway.autoconfigure.FlywayProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.stereotype.Component;

/**
 * En MySQL y MariaDB crea la base configurada en el primer arranque si no existe,
 * conectando al servidor sin seleccionar ninguna base. Con eso el backend arranca
 * en un XAMPP limpio sin phpMyAdmin ni SQL manual; Flyway migrar y el perfil dev
 * siguen haciendo su trabajo normal. H2 y cualquier otra URL se ignoran.
 */
@Component
public class DatabaseBootstrap implements FlywayMigrationStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseBootstrap.class);
    private static final Pattern JDBC_URL =
        Pattern.compile("^(jdbc:(?:mysql|mariadb)://[^/?]+)(?:/([^/?]*))?(\\?.*)?$");
    private static final Pattern SAFE_DB_NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_$]{0,63}");
    // utf8mb4_general_ci existe en MySQL 8 y en todos los MariaDB de XAMPP.
    private static final String CHARACTER_SET = "utf8mb4";
    private static final String COLLATION = "utf8mb4_general_ci";

    private final FlywayProperties flywayProperties;
    private final DataSourceProperties dataSourceProperties;

    public DatabaseBootstrap(FlywayProperties flywayProperties, DataSourceProperties dataSourceProperties) {
        this.flywayProperties = flywayProperties;
        this.dataSourceProperties = dataSourceProperties;
    }

    @Override
    public void migrate(Flyway flyway) {
        createDatabaseIfMissing();
        flyway.migrate();
    }

    // Boot configura Flyway con el DataSource, no con una URL, así que se lee la
    // configuracion equivalente: flyway.url si existe, si no spring.datasource.*.
    private void createDatabaseIfMissing() {
        String url = flywayProperties.getUrl() != null ? flywayProperties.getUrl() : dataSourceProperties.getUrl();
        String user = flywayProperties.getUser() != null ? flywayProperties.getUser() : dataSourceProperties.getUsername();
        String password = flywayProperties.getPassword() != null
            ? flywayProperties.getPassword() : dataSourceProperties.getPassword();
        String[] serverAndDatabase = serverAndDatabase(url);
        if (serverAndDatabase == null) return;
        String serverUrl = serverAndDatabase[0];
        String database = serverAndDatabase[1];
        try (Connection connection = DriverManager.getConnection(serverUrl, user, password)) {
            if (databaseExists(connection, database)) return;
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE DATABASE `" + database + "` CHARACTER SET "
                    + CHARACTER_SET + " COLLATE " + COLLATION);
            }
            LOG.info("Base de datos '{}' creada automaticamente con {} / {}.", database, CHARACTER_SET, COLLATION);
        } catch (SQLException e) {
            throw new IllegalStateException(("No se pudo comprobar ni crear la base '%s' en el servidor %s. Motivo: %s%n"
                + "Revisa DB_USER y DB_PASSWORD, que el servidor escuche en ese puerto y que el usuario tenga "
                + "privilegios CREATE (en XAMPP suele ser root sin contraseña).")
                .formatted(database, serverUrl, e.getMessage()), e);
        }
    }

    /** [url del servidor sin base, nombre de base] o null si la URL no es de MySQL/MariaDB gestionable. */
    static String[] serverAndDatabase(String jdbcUrl) {
        if (jdbcUrl == null) return null;
        Matcher matcher = JDBC_URL.matcher(jdbcUrl);
        if (!matcher.matches()) return null;
        String database = matcher.group(2);
        if (database == null || database.isEmpty()) return null;
        if (!SAFE_DB_NAME.matcher(database).matches()) {
            LOG.warn("Nombre de base '{}' no valido para creacion automatica; se omite la creacion.", database);
            return null;
        }
        String query = matcher.group(3) == null ? "" : matcher.group(3);
        return new String[] { matcher.group(1) + "/" + query, database };
    }

    private boolean databaseExists(Connection connection, String database) throws SQLException {
        try (PreparedStatement check = connection.prepareStatement(
                "SELECT 1 FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = ?")) {
            check.setString(1, database);
            try (ResultSet rows = check.executeQuery()) {
                return rows.next();
            }
        }
    }
}
