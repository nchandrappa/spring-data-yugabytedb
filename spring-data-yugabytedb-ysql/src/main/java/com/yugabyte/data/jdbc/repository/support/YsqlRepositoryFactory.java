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

import java.util.Optional;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.jdbc.repository.QueryMappingConfiguration;
import org.springframework.data.mapping.callback.EntityCallbacks;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.PersistentEntityInformation;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.yugabyte.data.jdbc.core.YsqlTemplate;
import com.yugabyte.data.jdbc.core.convert.YsqlDataAccessStrategy;

/**
 * Creates repository implementation based on YugabyteDB YSQL.
 *
 * @author Nikhil Chandrappa
 */
public class YsqlRepositoryFactory extends RepositoryFactorySupport {
	
	private final RelationalMappingContext context;
	private final JdbcConverter converter;
	private final ApplicationEventPublisher publisher;
	private final YsqlDataAccessStrategy ysqlDataAccessStrategy;
	private final NamedParameterJdbcOperations operations;
	private final Dialect dialect;
	private final JdbcTemplate jdbcTemplate;
	@Nullable private BeanFactory beanFactory;

	private QueryMappingConfiguration queryMappingConfiguration = QueryMappingConfiguration.EMPTY;
	private EntityCallbacks entityCallbacks;
	
	public YsqlRepositoryFactory(YsqlDataAccessStrategy dataAccessStrategy, RelationalMappingContext context,
			JdbcConverter converter, Dialect dialect, ApplicationEventPublisher publisher,
			NamedParameterJdbcOperations operations, JdbcTemplate jdbcTemplate) {

		Assert.notNull(dataAccessStrategy, "DataAccessStrategy must not be null!");
		Assert.notNull(context, "RelationalMappingContext must not be null!");
		Assert.notNull(converter, "RelationalConverter must not be null!");
		Assert.notNull(dialect, "Dialect must not be null!");
		Assert.notNull(publisher, "ApplicationEventPublisher must not be null!");

		this.publisher = publisher;
		this.context = context;
		this.converter = converter;
		this.dialect = dialect;
		this.ysqlDataAccessStrategy = dataAccessStrategy;
		this.operations = operations;
		this.jdbcTemplate = jdbcTemplate;
	}

	public void setQueryMappingConfiguration(QueryMappingConfiguration queryMappingConfiguration) {

		Assert.notNull(queryMappingConfiguration, "QueryMappingConfiguration must not be null!");

		this.queryMappingConfiguration = queryMappingConfiguration;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T, ID> EntityInformation<T, ID> getEntityInformation(Class<T> aClass) {

		RelationalPersistentEntity<?> entity = context.getRequiredPersistentEntity(aClass);

		return (EntityInformation<T, ID>) new PersistentEntityInformation<>(entity);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected Object getTargetRepository(RepositoryInformation repositoryInformation) {

		YsqlTemplate template = new YsqlTemplate(publisher, context, converter, ysqlDataAccessStrategy);

		if (entityCallbacks != null) {
			template.setEntityCallbacks(entityCallbacks);
		}

		RelationalPersistentEntity<?> persistentEntity = context
				.getRequiredPersistentEntity(repositoryInformation.getDomainType());

		return getTargetRepositoryViaReflection(repositoryInformation.getRepositoryBaseClass(), template, persistentEntity);
	}

	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata repositoryMetadata) {
		return SimpleYsqlRepository.class;
	}

	@Override
	protected Optional<QueryLookupStrategy> getQueryLookupStrategy(@Nullable QueryLookupStrategy.Key key,
			QueryMethodEvaluationContextProvider evaluationContextProvider) {

		return Optional.of(new YsqlQueryLookupStrategy(publisher, entityCallbacks, context, converter, dialect,
				queryMappingConfiguration, operations, beanFactory));
	}

	public void setEntityCallbacks(EntityCallbacks entityCallbacks) {
		this.entityCallbacks = entityCallbacks;
	}

	/**
	 * @param beanFactory the {@link BeanFactory} used for looking up {@link org.springframework.jdbc.core.RowMapper} and
	 *          {@link org.springframework.jdbc.core.ResultSetExtractor} beans.
	 */
	public void setBeanFactory(@Nullable BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}
	
	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

}
