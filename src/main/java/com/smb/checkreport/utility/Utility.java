package com.smb.checkreport.utility;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFCreationHelper;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


public class Utility {
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

    public static XSSFCellStyle createCellStyle(XSSFWorkbook workbook, short fontsize, boolean horizontal, boolean bold, boolean wrap, String color, boolean date, CreationHelper creationHelper) {
        // TODO Auto-generated method stub
        XSSFCellStyle style = workbook.createCellStyle();
        //是否水平居中
        if(horizontal){
            style.setAlignment(HorizontalAlignment.CENTER);//水平居中
        }
        style.setVerticalAlignment(VerticalAlignment.CENTER);//垂直居中
        //建立字型
        XSSFFont font = workbook.createFont();
        //是否加粗字型
        font.setBold(bold);
        font.setFontHeightInPoints(fontsize);
        font.setFontName("微軟正黑體");
        //載入字型
        style.setFont(font);

        if(color != null){
            if(color.equals("PINK")) {
                style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            } else if(color.equals("YELLOW")){
                style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
            } else if(color.equals("ORANGE")){
                style.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
            } else {
                style.setFillForegroundColor(IndexedColors.WHITE.getIndex());
            }
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }

        style.setWrapText(wrap);
        style.setBorderBottom(BorderStyle.THICK);
        style.setBorderLeft(BorderStyle.THICK);
        style.setBorderRight(BorderStyle.THICK);
        style.setBorderTop(BorderStyle.THICK);

        if(date){
            style.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy-mm-dd"));
        }
        return style;
    }
}
