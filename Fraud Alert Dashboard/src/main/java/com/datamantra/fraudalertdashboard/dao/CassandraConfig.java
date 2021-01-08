package com.datamantra.fraudalertdashboard.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.data.cassandra.config.CassandraClusterFactoryBean;
import org.springframework.data.cassandra.config.java.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.mapping.BasicCassandraMappingContext;
import org.springframework.data.cassandra.mapping.CassandraMappingContext;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

/**
 * Spring bean configuration for Cassandra db.
 * 
 * @author kafka
 *
 */
@Configuration
@PropertySource(value = {"classpath:fraudalert.properties"})
@EnableCassandraRepositories(basePackages = {"com.datamantra.fraudalertdashboard.dao"})
public class CassandraConfig extends AbstractCassandraConfiguration{
	
    @Autowired
    private Environment environment;
    
    @Bean
    public CassandraClusterFactoryBean cluster() {
        CassandraClusterFactoryBean cluster = new CassandraClusterFactoryBean();
        cluster.setContactPoints(environment.getProperty("com.datamantra.fraudalert.cassandra.host"));
        cluster.setPort(Integer.parseInt(environment.getProperty("com.datamantra.fraudalert.cassandra.port")));
        return cluster;
    }
  
    @Bean
    public CassandraMappingContext cassandraMapping(){
         return new BasicCassandraMappingContext();
    }
    
	@Override
	@Bean
	protected String getKeyspaceName() {
		return environment.getProperty("com.datamantra.fraudalert.cassandra.keyspace");
	}
}
