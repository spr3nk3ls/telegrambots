package com.spr3nk3ls.telegram.domain;

import java.util.List;

public interface Aliassable {
    List<String> getAliases();

    void setAliases(List<String> aliases);

    String getName();
}
