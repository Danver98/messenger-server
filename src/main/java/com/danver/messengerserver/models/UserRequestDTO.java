package com.danver.messengerserver.models;

import com.danver.messengerserver.models.util.Direction;
import com.danver.messengerserver.models.util.PagingNavigation;
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
/*    @JsonSetter(nulls = Nulls.SKIP)
    @Builder.Default
    private PagingNavigation navigation = PagingNavigation.builder().field("surname").additionalField("id").build();*/
    // TODO: remove params for navigation
    private String surname;
    private Long id;
    @Builder.Default
    private Direction direction = Direction.FUTURE;
}
