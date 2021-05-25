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
package com.yugabyte.data.jdbc.core;

import org.springframework.context.ApplicationContext;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.util.Assert;

import com.yugabyte.data.jdbc.core.convert.YugabyteDbDataAccessStrategy;

/**
 * {@link YugabyteDbYsqlOperations} implementation, storing entity objects in and obtaining them 
 * from a YugabyteDB cluster.
 *
 * @author Nikhil Chandrappa
 */
public class YugabyteDbYsqlJdbcTemplate extends JdbcAggregateTemplate implements YugabyteDbYsqlOperations {
	
	private YugabyteDbDataAccessStrategy accessStrategy;

	public YugabyteDbYsqlJdbcTemplate(ApplicationContext publisher, RelationalMappingContext context,
			JdbcConverter converter, YugabyteDbDataAccessStrategy dataAccessStrategy) {
		super(publisher, context, converter, dataAccessStrategy);
		this.accessStrategy = dataAccessStrategy;
	}
	
	public long count(Class<?> domainType, QueryOptions queryOptions) {

		Assert.notNull(domainType, "Domain type must not be null");
		
		return accessStrategy.count(domainType, queryOptions);
	}

//	@Override
//	public <T> void insertAll(Iterable<T> instances) {
//		// TODO Auto-generated method stub
//		
//	}
	
	

}
