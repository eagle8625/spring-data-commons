/*
 * Copyright (c) 2017.
 */

package org.springframework.data.querydsl;

import com.querydsl.core.annotations.QueryEntity;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * Created by liqingsong on 23/05/2017.
 */
@QueryEntity
public class Other {
    public String prop1;
    public Integer prop2;
    public @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date dateOfBirth;

    public Other(String prop1, Integer prop2, Date dateOfBirth) {
        this.prop1 = prop1;
        this.prop2 = prop2;
        this.dateOfBirth = dateOfBirth;
    }
}
