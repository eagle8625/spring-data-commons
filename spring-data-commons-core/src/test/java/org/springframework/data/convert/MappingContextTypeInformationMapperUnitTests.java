/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.convert;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.util.ClassTypeInformation.*;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mapping.MappingMetadataTests;
import org.springframework.data.mapping.MappingMetadataTests.SampleMappingContext;
import org.springframework.data.mapping.MappingMetadataTests.SampleProperty;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

/**
 * Unit tests for {@link MappingContextTypeInformationMapper}.
 * 
 * @author Oliver Gierke
 */
public class MappingContextTypeInformationMapperUnitTests {

	SampleMappingContext mappingContext;
	TypeInformationMapper mapper;

	@Before
	public void setUp() {
		mappingContext = new MappingMetadataTests.SampleMappingContext();
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullMappingContext() {
		new MappingContextTypeInformationMapper(null);
	}

	@Test
	public void extractsAliasInfoFromMappingContext() {

		mappingContext.setInitialEntitySet(Collections.singleton(Entity.class));
		mappingContext.initialize();

		mapper = new MappingContextTypeInformationMapper(mappingContext);

		assertThat(mapper.createAliasFor(ClassTypeInformation.from(Entity.class)), is((Object) "foo"));
	}

	@Test
	public void extractsAliasForUnknownType() {

		SampleMappingContext mappingContext = new MappingMetadataTests.SampleMappingContext();
		mappingContext.initialize();

		mapper = new MappingContextTypeInformationMapper(mappingContext);

		assertThat(mapper.createAliasFor(from(Entity.class)), is((Object) "foo"));
	}

	@Test
	public void doesNotReturnTypeAliasForSimpleType() {

		SampleMappingContext mappingContext = new MappingMetadataTests.SampleMappingContext();
		mappingContext.initialize();

		mapper = new MappingContextTypeInformationMapper(mappingContext);
		assertThat(mapper.createAliasFor(from(String.class)), is(nullValue()));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void detectsTypeForUnknownEntity() {

		SampleMappingContext mappingContext = new MappingMetadataTests.SampleMappingContext();
		mappingContext.initialize();

		mapper = new MappingContextTypeInformationMapper(mappingContext);
		assertThat(mapper.resolveTypeFrom("foo"), is(nullValue()));

		PersistentEntity<?, SampleProperty> entity = mappingContext.getPersistentEntity(Entity.class);

		assertThat(entity, is(notNullValue()));
		assertThat(mapper.resolveTypeFrom("foo"), is((TypeInformation) from(Entity.class)));
	}

	@TypeAlias("foo")
	static class Entity {

	}
}