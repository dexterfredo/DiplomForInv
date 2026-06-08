package ru.inversion.LoaderMicexFX.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import ru.inversion.LoaderMicexFX.db.MutableDataSource;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfiguration {

    @Bean
    public MutableDataSource mutableDataSource() {
        return new MutableDataSource();
    }

    @Bean
    @Primary
    public DataSource dataSource(MutableDataSource mutableDataSource) {
        return mutableDataSource;
    }
}
