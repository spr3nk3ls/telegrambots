package com.spr3nk3ls.telegram.dao;

import com.spr3nk3ls.telegram.domain.DrinkUser;

import java.util.List;

public interface UserDao {
    List<DrinkUser> getDrinkUsers();

    void addDrinkUser(DrinkUser user);
}
