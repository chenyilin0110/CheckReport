package com.smb.checkreport.page;


import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by muye on 17/6/14.
 */
public class FieldUtil {

    /**
     * 将对象的空字符属性转成null
     * @param o
     */
    public static void spaceToNull(Object o){

        Assert.notNull(o, "param cant be null");
        Field[] fields = o.getClass().getDeclaredFields();
        Pattern pattern = Pattern.compile("^\\s*$");
        Arrays.asList(fields).forEach(field -> {
            field.setAccessible(true);
            try {
                if (field.get(o) instanceof String){
                    Matcher matcher = pattern.matcher((String)field.get(o));
                    if (matcher.find()){
                        field.set(o, null);
                    }
                }
            }catch (IllegalAccessException e){
                throw new RuntimeException("spaceToNull err");
            }
        });
    }

    /**
     * 获取指定对象的指定属性值
     * @param obj
     * @param fieldName
     * @return
     */
    public static Object getFieldValue(Object obj, String fieldName){
        Assert.notNull(obj, "obj cant be null");
        Assert.notNull(fieldName, "fieldName cant be null");
        Object result = null;
        Field field = getField(obj, fieldName);
        if (field != null) {
            field.setAccessible(true);
            try {
                result = field.get(obj);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return result;

    }

    /**
     * 利用反射获取指定对象里面的指定属性
     * @param obj 目标对象
     * @param fieldName 目标属性
     * @return 目标字段
     */
    public static Field getField(Object obj, String fieldName) {
        Assert.notNull(obj, "obj cant be null");
        Assert.notNull(fieldName, "fieldName cant be null");
        Field field = null;
        for (Class<?> clazz=obj.getClass(); clazz != Object.class; clazz=clazz.getSuperclass()) {
            try {
                field = clazz.getDeclaredField(fieldName);
                break;
            } catch (NoSuchFieldException e) {
                //这里不用做处理，子类没有该字段可能对应的父类有，都没有就返回null。
            }
        }
        return field;
    }

    /**
     * 利用反射设置指定对象的指定属性为指定的值
     * @param obj 目标对象
     * @param fieldName 目标属性
     * @param fieldValue 目标值
     */
    public static void setFieldValue(Object obj, String fieldName, String fieldValue) {
        Field field = getField(obj, fieldName);
        if (field != null) {
            try {
                field.setAccessible(true);
                field.set(obj, fieldValue);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static String diffField(Object a, Object b){

        Object id = null;
        /*if (a instanceof Country) id = ((Country) a).getCountryId();
        else if (a instanceof Message) id = ((Message) a).getMsgId();
        else if (a instanceof NetUser) id = ((NetUser) a).getNetUserId();
        else if (a instanceof Order) id = ((Order) a).getOrderId();
        else if (a instanceof Product) id = ((Product) a).getProductId();
        else if (a instanceof Share) id = ((Share) a).getShareId();
        else if (a instanceof ShareBuy) id = ((ShareBuy) a).getBuyId();
        else if (a instanceof ShareSell) id = ((ShareSell) a).getSellId();
        else if (a instanceof User) id = ((User) a).getUserId();
        else if (a instanceof UserAsset) id = ((UserAsset) a).getUserId();
        else if (a instanceof UserCashBuy) id = ((UserCashBuy) a).getBuyId();
        else if (a instanceof UserCashSell) id = ((UserCashSell) a).getSellId();
        else if (a instanceof UserProfile) id = ((UserProfile) a).getUserId();
        else {
            try {
                Field f = a.getClass().getField("id");
                f.setAccessible(true);
                id = f.get(a);
            }catch (Exception e){}
        }*/
        
        try {
            Field f = a.getClass().getField("id");
            f.setAccessible(true);
            id = f.get(a);
        }catch (Exception e){}

        StringBuffer sb = new StringBuffer();
        if (id != null){
            sb.append("id:" + id + "|");
        }
        Arrays.asList(a.getClass().getDeclaredFields()).forEach(f -> {
            f.setAccessible(true);
            try {
                if (!f.get(b).equals(f.get(a))){
                    sb.append(f.getName() + ":" + f.get(a) + "->" + f.get(b) + "|");
                }
            }catch (Exception e){
            }
        });

        return sb.toString();
    }

}
