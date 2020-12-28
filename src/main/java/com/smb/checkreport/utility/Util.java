package com.smb.checkreport.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class Util {

    private static Logger logger = LoggerFactory.getLogger(Util.class);

    public static String getChinesePDFCode(String fileName)
    {
        try {
            fileName = new String(fileName.getBytes(), "iso-8859-1");
            return fileName;
        } catch (UnsupportedEncodingException e) {
            return "data.pdf";
        }
    }

    public static byte[] concat(byte[] a,byte[] b) {

        byte[] all = new byte[a.length + b.length];
        for (int i = 0 ; i < a.length ; i++) {
            all[i] = a[i];
        }
        for (int i = 0 ; i < b.length ; i++) {
            all[i+a.length] = b[i];
        }
        return all;
    }

}
