package com.smb.checkreport.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.smb.checkreport.page.PageInterceptor;
import org.apache.ibatis.plugin.Interceptor;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DruidDBConfig {

    private static Logger logger = LoggerFactory.getLogger(DruidDBConfig.class);

    @Bean(name = "mySqlDS")
    @ConfigurationProperties(prefix = "spring.datasource.druid")
    public DruidDataSource mainDataSource() throws Exception{

        DruidDataSource ds = new DruidDataSource();
//        ds.setUrl(url);
//        ds.setUsername(username);
//        ds.setPassword(password);
//        ds.setDriverClassName(driver);
        ds.setInitialSize(0);
        ds.setMaxActive(20);
        ds.setMinIdle(0);
        ds.setMaxWait(5000);
        ds.setValidationQuery("SELECT 1");
        ds.setTestOnBorrow(false);
        ds.setTestOnReturn(false);
        ds.setTestWhileIdle(true);
        ds.setTimeBetweenEvictionRunsMillis(60000);
        ds.setMinEvictableIdleTimeMillis(25200000);
        ds.setRemoveAbandoned(true);
        ds.setRemoveAbandonedTimeout(1800);
        ds.setLogAbandoned(true);
        try {
            ds.setFilters("mergeStat");
        }catch (Exception e){
            logger.error("mysqlDS error ",e);
        }

        return ds;
    }

    @Bean(name = "mySqlSessionFactory")
    public SqlSessionFactory mainSqlSessionFactory(@Qualifier("mySqlDS") DruidDataSource mySqlDS) throws Exception {
        final SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(mySqlDS);
        factoryBean.setPlugins(new Interceptor[]{new PageInterceptor()});
        SqlSessionFactory sqlSessionFactory = factoryBean.getObject();
        sqlSessionFactory.getConfiguration().setMapUnderscoreToCamelCase(true);
        return sqlSessionFactory;
    }

    @Bean
    public MapperScannerConfigurer mapperScannerConfigurer(){
        logger.info("config main mapper");
        MapperScannerConfigurer configurer = new MapperScannerConfigurer();
        configurer.setBasePackage("com.smb.checkreport.mapper");
        configurer.setSqlSessionFactoryBeanName("mySqlSessionFactory");
        return configurer;
    }
}
