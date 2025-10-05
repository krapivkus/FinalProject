package app;

import org.flywaydb.core.Flyway;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class Db {
    private static final Properties props = new Properties();

    static {
        try (InputStream input = Db.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new RuntimeException("Не найден файл application.properties");
            }

            props.load(input);

            Class.forName(props.getProperty("db.driver"));

            Flyway flyway = Flyway.configure()
                .dataSource(
                    props.getProperty("db.url"),
                    props.getProperty("db.username"),
                    props.getProperty("db.password")
                )
                .locations("classpath:db/migration")
                .load();

            flyway.migrate();

        } catch (IOException e) {
            throw new RuntimeException("Ошибка чтения файла application.properties", e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Не найден JDBC-драйвер", e);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка запуска миграций: " + e.getMessage(), e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            props.getProperty("db.url"),
            props.getProperty("db.username"),
            props.getProperty("db.password")
        );
    }
}
