/*
 * Copyright 2011-2016 the original author or authors.
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
package org.springframework.data.querydsl;

import com.querydsl.core.annotations.QueryEntity;
import org.springframework.data.annotation.Transient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Christoph Strobl
 */
@QueryEntity
public class User {

    public String firstname, lastname;
    public
    @DateTimeFormat(iso = ISO.DATE)
    Date dateOfBirth;
    public Address address;
    public List<Address> addresses;
    public List<String> nickNames;
    public Long inceptionYear;

    public List<Map<String, Object>> mapAttrs;
    @Transient
    public Other other;

    public Map<String, Object> mapProperties;


    public User(String firstname, String lastname, Address address) {

        this.firstname = firstname;
        this.lastname = lastname;
        this.address = address;
        initMap ( );
    }

    public User(Other other) {
        this.other = other;
        initMap ( );
    }

    public void initMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put ("strkey", "stringValue");
        map.put ("intkey", 1);
        map.put ("datekey", new Date ( ));
        mapAttrs.add (map);
    }
}

@QueryEntity
class SpecialUser extends User {

    public String specialProperty;

    public String strkey;
    public Integer intkey;
    public Date datekey;

    public SpecialUser(String firstname, String lastname, Address address) {
        super (firstname, lastname, address);
    }
}


@QueryEntity
class UserWrapper {
    public User user;
    public SpecialUser mapAttrs;
}

//@QueryEntity
//class MapUser extends User {
//
//    public MapUser(String firstname, String lastname, Address address) {
//        super (firstname, lastname, address);
//    }
//
//    public String strkey;
//    public Integer intkey;
//    public Date datekey;
//}
//
//@QueryEntity
//class UserMapWrapper {
//    public User mapAttrs;
//}
