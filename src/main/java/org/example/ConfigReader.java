package org.example;

import java.io.IOException;
import java.util.Properties;

public class ConfigReader {
    private final Properties properties;

    public ConfigReader(String filename) {
        properties = new Properties();
        try {
            properties.load(ConfigReader.class.getClassLoader().getResourceAsStream(filename));
        } catch (IOException e) {
            throw new RuntimeException("Не удалось загрузить файл конфигурации: " + filename, e);
        }
    }

    public String getServerHost() {
        return properties.getProperty("server.host");
    }

    public String getServerPort() {
        return properties.getProperty("server.port");
    }

    public String getLogFile() {
        return properties.getProperty("log.file");
    }

    public String getLogFileMaxSize() {
        return properties.getProperty("log.file.maxSize");
    }
}
