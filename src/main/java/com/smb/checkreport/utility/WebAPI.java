package com.smb.checkreport.utility;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class WebAPI {

    private static final Logger logger = LogManager.getLogger(WebAPI.class);
    public static String getURL() {
        try {
            Resource resource = new FileSystemResource("./trello.properties");
            Properties prop = PropertiesLoaderUtils.loadProperties(resource);
            return prop.getProperty("trello.url","https://api.trello.com/1/");
        } catch (IOException e) {
            e.printStackTrace();
            return "https://api.trello.com/1/";
        }
    }

    public static String sendAPI_trello(String method){
        String url = "";
        try {
            url = getURL();

            URL restUrl = new URL(url + method);
            HttpURLConnection conn = (HttpURLConnection) restUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);

            // 送出的動作
            BufferedReader br = new BufferedReader((new InputStreamReader(conn.getInputStream())));
            String readLine = null;
            StringBuffer response = new StringBuffer();
            while((readLine = br.readLine()) != null){
                response.append(readLine);
            }
            br.close();
            conn.disconnect();

            return response.toString();
        } catch (IOException e){
            e.printStackTrace();
            return "";
        }
    }

    public static String sendAPI_trello_put(String method) {
        String url = "";
        try {
            url = getURL();

            URL restUrl = new URL(url + method);
            HttpURLConnection conn = (HttpURLConnection) restUrl.openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

//            PrintStream ps = new PrintStream(conn.getOutputStream());
//            ps.print(content);
//            ps.close();

            // 送出的動作
            BufferedReader br = new BufferedReader((new InputStreamReader(conn.getInputStream())));
            String readLine = null;
            StringBuffer response = new StringBuffer();
            while((readLine = br.readLine()) != null){
                response.append(readLine);
            }
            br.close();
            conn.disconnect();

            return response.toString();
        } catch (IOException e){
            e.printStackTrace();
            return "";
        }
    }
}
