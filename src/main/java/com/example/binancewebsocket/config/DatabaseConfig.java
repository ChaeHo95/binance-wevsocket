package com.example.binancewebsocket.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    private Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    private EnvConfig envConfig;

    @Autowired
    public DatabaseConfig(EnvConfig envConfig) {
        this.envConfig = envConfig;
    }

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");

        String dbUrl = envConfig.getDbUrl();
        String dbUsername = envConfig.getDbUsername();
        String dbPassword = envConfig.getDbPassword();

        if (dbUsername == null) {
            logger.error("❌ DB_USERNAME 환경 변수가 설정되지 않았습니다!");
        } else {
            logger.info("✅ DB_USERNAME 로드 성공: {}", dbUsername);
        }

        if (dbPassword == null) {
            logger.error("❌ DB_PASSWORD 환경 변수가 설정되지 않았습니다!");
        } else {
            logger.info("✅ DB_PASSWORD 로드 성공 (보안상 값은 출력하지 않음)");
        }

        if (dbUrl == null) {
            logger.error("❌ DB_URL 환경 변수가 설정되지 않았습니다!");
        } else {
            logger.info("✅ DB_URL 로드 성공: {}", dbUrl);
        }

        dataSource.setUrl(dbUrl);
        dataSource.setUsername(dbUsername);
        dataSource.setPassword(dbPassword);

        return dataSource;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) {
        try {
            SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
            factoryBean.setDataSource(dataSource);

            // MyBatis Mapper XML 파일 로드
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:/mapper/**/*.xml");

            if (resources.length > 0) {
                for (Resource resource : resources) {
                    logger.info("MyBatis Mapper XML 로드됨: {}", resource.getFilename());
                }
            } else {
                logger.warn("MyBatis Mapper XML을 찾을 수 없음!");
            }

            factoryBean.setMapperLocations(resources);

            // MyBatis 설정 추가
            org.apache.ibatis.session.Configuration myBatisConfig = new org.apache.ibatis.session.Configuration();
            myBatisConfig.setMapUnderscoreToCamelCase(true); // 자동 Camel Case 매핑
            myBatisConfig.setDefaultStatementTimeout(3000); // SQL 실행 타임아웃 설정
            myBatisConfig.setLazyLoadingEnabled(true); // Lazy Loading 설정
            factoryBean.setConfiguration(myBatisConfig);

            return factoryBean.getObject();
        } catch (Exception e) {
            logger.error("MyBatis SqlSessionFactory 생성 실패", e);
            throw new RuntimeException("MyBatis SqlSessionFactory 생성 실패", e);
        }
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
