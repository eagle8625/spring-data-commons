/*
 * Copyright 2012-2017 the original author or authors.
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
package org.springframework.data.auditing;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Field;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.convert.JodaTimeConverters;
import org.springframework.data.convert.Jsr310Converters;
import org.springframework.data.convert.ThreeTenBackPortConverters;
import org.springframework.data.domain.Auditable;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.Assert;

/**
 * A factory class to {@link AuditableBeanWrapper} instances.
 * 
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @since 1.5
 */
class DefaultAuditableBeanWrapperFactory implements AuditableBeanWrapperFactory {

	/**
	 * Returns an {@link AuditableBeanWrapper} if the given object is capable of being equipped with auditing information.
	 * 
	 * @param source the auditing candidate.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Optional<AuditableBeanWrapper> getBeanWrapperFor(Optional<? extends Object> source) {

		return source.map(it -> {

			if (it instanceof Auditable) {
				return new AuditableInterfaceBeanWrapper((Auditable<Object, ?, TemporalAccessor>) it);
			}

			AnnotationAuditingMetadata metadata = AnnotationAuditingMetadata.getMetadata(it.getClass());

			if (metadata.isAuditable()) {
				return new ReflectionAuditingBeanWrapper(it);
			}

			return null;
		});
	}

	/**
	 * An {@link AuditableBeanWrapper} that works with objects implementing
	 * 
	 * @author Oliver Gierke
	 */
	@RequiredArgsConstructor
	static class AuditableInterfaceBeanWrapper extends DateConvertingAuditableBeanWrapper {

		private final @NonNull Auditable<Object, ?, TemporalAccessor> auditable;
		private final Class<? extends TemporalAccessor> type;

		@SuppressWarnings("unchecked")
		public AuditableInterfaceBeanWrapper(Auditable<Object, ?, TemporalAccessor> auditable) {

			this.auditable = auditable;
			this.type = (Class<? extends TemporalAccessor>) ResolvableType.forClass(Auditable.class, auditable.getClass())
					.getGeneric(2).getRawClass();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.auditing.AuditableBeanWrapper#setCreatedBy(java.util.Optional)
		 */
		@Override
		public Optional<? extends Object> setCreatedBy(Optional<? extends Object> value) {

			auditable.setCreatedBy(value);

			return value;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.auditing.AuditableBeanWrapper#setCreatedDate(java.util.Optional)
		 */
		@Override
		public Optional<TemporalAccessor> setCreatedDate(Optional<TemporalAccessor> value) {

			auditable.setCreatedDate(getAsTemporalAccessor(value, type));

			return value;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.auditing.DefaultAuditableBeanWrapperFactory.AuditableInterfaceBeanWrapper#setLastModifiedBy(java.util.Optional)
		 */
		@Override
		public Optional<? extends Object> setLastModifiedBy(Optional<? extends Object> value) {
			auditable.setLastModifiedBy(value);

			return value;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.auditing.AuditableBeanWrapper#getLastModifiedDate()
		 */
		@Override
		public Optional<TemporalAccessor> getLastModifiedDate() {
			return getAsTemporalAccessor(auditable.getLastModifiedDate(), TemporalAccessor.class);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.auditing.AuditableBeanWrapper#setLastModifiedDate(java.util.Optional)
		 */
		@Override
		public Optional<TemporalAccessor> setLastModifiedDate(Optional<TemporalAccessor> value) {

			auditable.setLastModifiedDate(getAsTemporalAccessor(value, type));

			return value;
		}
	}

	/**
	 * Base class for {@link AuditableBeanWrapper} implementations that might need to convert {@link Calendar} values into
	 * compatible types when setting date/time information.
	 * 
	 * @author Oliver Gierke
	 * @since 1.8
	 */
	abstract static class DateConvertingAuditableBeanWrapper implements AuditableBeanWrapper {

		private final ConversionService conversionService;

		/**
		 * Creates a new {@link DateConvertingAuditableBeanWrapper}.
		 */
		public DateConvertingAuditableBeanWrapper() {

			DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();

			JodaTimeConverters.getConvertersToRegister().forEach(it -> conversionService.addConverter(it));
			Jsr310Converters.getConvertersToRegister().forEach(it -> conversionService.addConverter(it));
			ThreeTenBackPortConverters.getConvertersToRegister().forEach(it -> conversionService.addConverter(it));

			this.conversionService = conversionService;
		}

		/**
		 * Returns the {@link Calendar} in a type, compatible to the given field.
		 * 
		 * @param value can be {@literal null}.
		 * @param targetType must not be {@literal null}.
		 * @param source must not be {@literal null}.
		 * @return
		 */
		protected Optional<Object> getDateValueToSet(Optional<TemporalAccessor> value, Class<?> targetType, Object source) {

			return value.map(it -> {

				if (TemporalAccessor.class.equals(targetType)) {
					return it;
				}

				if (conversionService.canConvert(it.getClass(), targetType)) {
					return conversionService.convert(it, targetType);
				}

				if (conversionService.canConvert(Date.class, targetType)) {

					if(!conversionService.canConvert(it.getClass(), Date.class)) {
						throw new IllegalArgumentException(String.format("Cannot convert date type for member %s! From %s to java.util.Date to %s.",
								source, it.getClass(), targetType));
					}

					Date date = conversionService.convert(it, Date.class);
					return conversionService.convert(date, targetType);
				}

				throw new IllegalArgumentException(String.format("Invalid date type for member %s! Supported types are %s.",
						source, AnnotationAuditingMetadata.SUPPORTED_DATE_TYPES));
			});
		}

		/**
		 * Returns the given object as {@link Calendar}.
		 * 
		 * @param source can be {@literal null}.
		 * @return
		 */
		@SuppressWarnings("unchecked")
		protected <T> Optional<T> getAsTemporalAccessor(Optional<?> source, Class<T> target) {

			return source.map(it -> {
				return target.isInstance(it) ? (T) it : conversionService.convert(it, target);
			});
		}
	}

	/**
	 * An {@link AuditableBeanWrapper} implementation that sets values on the target object using reflection.
	 * 
	 * @author Oliver Gierke
	 */
	static class ReflectionAuditingBeanWrapper extends DateConvertingAuditableBeanWrapper {

		private final AnnotationAuditingMetadata metadata;
		private final Object target;

		/**
		 * Creates a new {@link ReflectionAuditingBeanWrapper} to set auditing data on the given target object.
		 * 
		 * @param target must not be {@literal null}.
		 */
		public ReflectionAuditingBeanWrapper(Object target) {

			Assert.notNull(target, "Target object must not be null!");

			this.metadata = AnnotationAuditingMetadata.getMetadata(target.getClass());
			this.target = target;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.auditing.AuditableBeanWrapper#setCreatedBy(java.util.Optional)
		 */
		@Override
		public Optional<? extends Object> setCreatedBy(Optional<? extends Object> value) {
			return setField(metadata.getCreatedByField(), value);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.auditing.AuditableBeanWrapper#setCreatedDate(java.util.Optional)
		 */
		@Override
		public Optional<TemporalAccessor> setCreatedDate(Optional<TemporalAccessor> value) {
			return setDateField(metadata.getCreatedDateField(), value);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.auditing.AuditableBeanWrapper#setLastModifiedBy(java.util.Optional)
		 */
		@Override
		public Optional<? extends Object> setLastModifiedBy(Optional<? extends Object> value) {
			return setField(metadata.getLastModifiedByField(), value);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.auditing.AuditableBeanWrapper#getLastModifiedDate()
		 */
		@Override
		public Optional<TemporalAccessor> getLastModifiedDate() {

			return getAsTemporalAccessor(metadata.getLastModifiedDateField().map(field -> {

				Object value = org.springframework.util.ReflectionUtils.getField(field, target);
				return Optional.class.isInstance(value) ? ((Optional<?>) value).orElse(null) : value;

			}), TemporalAccessor.class);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.auditing.AuditableBeanWrapper#setLastModifiedDate(java.util.Optional)
		 */
		@Override
		public Optional<TemporalAccessor> setLastModifiedDate(Optional<TemporalAccessor> value) {
			return setDateField(metadata.getLastModifiedDateField(), value);
		}

		/**
		 * Sets the given field to the given value if present.
		 * 
		 * @param field
		 * @param value
		 */
		private Optional<? extends Object> setField(Optional<Field> field, Optional<? extends Object> value) {

			field.ifPresent(it -> {
				ReflectionUtils.setField(it, target,
						Optional.class.isAssignableFrom(it.getType()) ? value : value.orElse(null));
			});

			return value;
		}

		/**
		 * Sets the given field to the given value if the field is not {@literal null}.
		 * 
		 * @param field
		 * @param value
		 */
		private Optional<TemporalAccessor> setDateField(Optional<Field> field, Optional<TemporalAccessor> value) {

			field.ifPresent(it -> {
				Optional<Object> toSet = getDateValueToSet(value, it.getType(), it);
				ReflectionUtils.setField(it, target,
						Optional.class.isAssignableFrom(it.getType()) ? toSet : toSet.orElse(null));
			});

			return value;
		}
	}
}
