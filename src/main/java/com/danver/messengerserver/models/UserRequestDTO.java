package com.danver.messengerserver.models;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class UserRequestDTO {

    //@JsonSetter(nulls = Nulls.SKIP)
    private UserRequestFilter filter; // = new UserRequestFilter()
}
