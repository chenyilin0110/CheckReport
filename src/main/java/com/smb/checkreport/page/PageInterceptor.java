package com.smb.checkreport.page;



import org.apache.ibatis.executor.statement.RoutingStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Created by muye on 17/6/16.
 *
 * 分页拦截器
 */
@Intercepts({
        @Signature(method = "prepare", type = StatementHandler.class, args = {Connection.class, Integer.class})
})
public class PageInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        RoutingStatementHandler handler = (RoutingStatementHandler) invocation.getTarget();
        StatementHandler delegate = (StatementHandler) FieldUtil.getFieldValue(handler, "delegate");
        BoundSql boundSql = delegate.getBoundSql();

        Object obj = boundSql.getParameterObject();

        Page page = null;
        if (obj instanceof Page) {
            page = (Page) obj;
        }

        if (obj instanceof Map) {
            Optional<Page> optional = ((Map) obj).values().stream().filter(param -> param instanceof Page).findFirst();
            page = optional.isPresent() ? optional.get() : null;
        }

        if (page != null) {
            String sql = boundSql.getSql();
            this.setTotalRecord(invocation, page);
            String pageSql = buildPageSql(page, sql);
            FieldUtil.setFieldValue(boundSql, "sql", pageSql);
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object o) {
        return Plugin.wrap(o, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }

    /**
     * 给当前的参数对象page设置总记录数
     */
    private void setTotalRecord(Invocation invocation, Page page) throws SQLException {

        RoutingStatementHandler handler = (RoutingStatementHandler) invocation.getTarget();
        StatementHandler delegate = (StatementHandler) FieldUtil.getFieldValue(handler, "delegate");
        BoundSql boundSql = delegate.getBoundSql();
        String sql = boundSql.getSql();
        final String countSql = "select count(1) from ("+sql+") tmp";
        FieldUtil.setFieldValue(boundSql, "sql", countSql);
        Connection connection = (Connection)invocation.getArgs()[0];
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = connection.prepareStatement(countSql);
            delegate.getParameterHandler().setParameters(pstmt);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                int totalRecord = rs.getInt(1);
                page.setTotalRecord(totalRecord);
            }
        } finally {
            if (rs != null)
                rs.close();
            if (pstmt != null)
                pstmt.close();
        }
    }

    private String buildPageSql(Page page, String sql){
    	
        StringBuffer sqlBuffer = new StringBuffer(sql);
        sqlBuffer.append(" limit ").append(page.getStart()).append(",").append( page.getPageSize());
        return sqlBuffer.toString();
    }

}