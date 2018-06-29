package com.spr3nk3ls.telegram.dao;

import com.spr3nk3ls.telegram.domain.Group;

import java.util.List;

public interface GroupDao {
    List<Group> getGroups();

    void addGroup(Group group);
}
