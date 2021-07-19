/*
 * Copyright (c) Yugabyte, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except 
 * in compliance with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License 
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express 
 * or implied.  See the License for the specific language governing permissions and limitations 
 * under the License.
*/
package com.yugabyte.data.jdbc.repository.support;

import java.io.Serializable;

import javax.sql.DataSource;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.jdbc.core.convert.SqlGeneratorSource;
import org.springframework.data.jdbc.repository.QueryMappingConfiguration;
import org.springframework.data.mapping.callback.EntityCallbacks;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.TransactionalRepositoryFactoryBeanSupport;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.util.Assert;

import com.yugabyte.data.jdbc.core.convert.DefaultYsqlDataAccessStrategy;
import com.yugabyte.data.jdbc.core.convert.YsqlDataAccessStrategy;

/**
 * Special adapter for Springs
 * {@link org.springframework.beans.factory.FactoryBean} interface to allow easy
 * setup of repository factories via Spring configuration.
 *
 * @author Nikhil Chandrappa
 */
public class YsqlRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable>
		extends TransactionalRepositoryFactoryBeanSupport<T, S, ID> implements ApplicationEventPublisherAware {
	
	private ApplicationEventPublisher publisher;
	private BeanFactory beanFactory;
	private RelationalMappingContext mappingContext;
	private JdbcConverter converter;
	private YsqlDataAccessStrategy ysqlDataAccessStrategy;
	private QueryMappingConfiguration queryMappingConfiguration = QueryMappingConfiguration.EMPTY;
	private NamedParameterJdbcOperations operations;
	private DataSource dataSource;
	private EntityCallbacks entityCallbacks;
	private Dialect dialect;

	protected YsqlRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
		super(repositoryInterface);
	}

	@Override
	protected RepositoryFactorySupport doCreateRepositoryFactory() {
		
		YsqlRepositoryFactory ysqlRepositoryFactory = new YsqlRepositoryFactory(ysqlDataAccessStrategy, mappingContext,
				converter, dialect, publisher, operations);
		ysqlRepositoryFactory.setQueryMappingConfiguration(queryMappingConfiguration);
		ysqlRepositoryFactory.setEntityCallbacks(entityCallbacks);
		ysqlRepositoryFactory.setBeanFactory(beanFactory);

		return ysqlRepositoryFactory; 
	}
	
	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {

		super.setApplicationEventPublisher(publisher);

		this.publisher = publisher;
	}
	
	@Autowired
	protected void setMappingContext(RelationalMappingContext mappingContext) {

		Assert.notNull(mappingContext, "MappingContext must not be null");

		super.setMappingContext(mappingContext);
		this.mappingContext = mappingContext;
	}

	@Autowired
	protected void setDialect(Dialect dialect) {

		Assert.notNull(dialect, "Dialect must not be null");

		this.dialect = dialect;
	}
	
	public void setDataAccessStrategy(YsqlDataAccessStrategy dataAccessStrategy) {

		Assert.notNull(dataAccessStrategy, "DataAccessStrategy must not be null");

		this.ysqlDataAccessStrategy = dataAccessStrategy;
	}
	
	@Autowired(required = false)
	public void setQueryMappingConfiguration(QueryMappingConfiguration queryMappingConfiguration) {

		Assert.notNull(queryMappingConfiguration, "QueryMappingConfiguration must not be null");

		this.queryMappingConfiguration = queryMappingConfiguration;
	}
	
	public void setJdbcOperations(NamedParameterJdbcOperations operations) {

		Assert.notNull(operations, "NamedParameterJdbcOperations must not be null");

		this.operations = operations;
	}

	@Autowired
	public void setConverter(JdbcConverter converter) {

		Assert.notNull(converter, "JdbcConverter must not be null");

		this.converter = converter;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {

		super.setBeanFactory(beanFactory);

		this.beanFactory = beanFactory;
	}

	@Override
	public void afterPropertiesSet() {

		Assert.state(this.mappingContext != null, "MappingContext is required and must not be null!");
		Assert.state(this.converter != null, "RelationalConverter is required and must not be null!");

		if (this.operations == null) {

			Assert.state(beanFactory != null, "If no JdbcOperations are set a BeanFactory must be available.");

			this.operations = beanFactory.getBean(NamedParameterJdbcOperations.class);
		}
		
		if (this.dataSource == null) {

			Assert.state(beanFactory != null, "If no DataSource are set a BeanFactory must be available.");

			this.dataSource = beanFactory.getBean(DataSource.class);
		}

		if (this.ysqlDataAccessStrategy == null) {

			Assert.state(beanFactory != null, "If no DataAccessStrategy is set a BeanFactory must be available.");

			this.ysqlDataAccessStrategy = this.beanFactory.getBeanProvider(YsqlDataAccessStrategy.class) //
					.getIfAvailable(() -> {

						Assert.state(this.dialect != null, "Dialect is required and must not be null!");

						SqlGeneratorSource sqlGeneratorSource = new SqlGeneratorSource(this.mappingContext, this.converter,
								this.dialect);
						return new DefaultYsqlDataAccessStrategy(sqlGeneratorSource, this.mappingContext, this.converter,
								this.operations, dataSource);
					});
		}

		if (this.queryMappingConfiguration == null) {
			this.queryMappingConfiguration = QueryMappingConfiguration.EMPTY;
		}

		if (beanFactory != null) {
			entityCallbacks = EntityCallbacks.create(beanFactory);
		}

		super.afterPropertiesSet();
	}

}
