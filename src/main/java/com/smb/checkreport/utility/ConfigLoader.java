package com.smb.checkreport.utility;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class ConfigLoader {
    public static Properties loadConfig(String filename) {
        String cwd = System.getProperty("user.dir");
        Path configFileName = Paths.get(cwd, filename);
        Properties prop = new Properties();
        try (FileInputStream fis = new FileInputStream(configFileName.toString())){
            InputStreamReader read = new InputStreamReader(fis,"UTF-8");
            prop.load(read);
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
        return prop;
    }
}
