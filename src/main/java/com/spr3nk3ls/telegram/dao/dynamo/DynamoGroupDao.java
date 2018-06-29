package com.spr3nk3ls.telegram.dao.dynamo;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.spr3nk3ls.telegram.dao.GroupDao;
import com.spr3nk3ls.telegram.domain.Group;
import com.spr3nk3ls.telegram.util.DynamoDBManager;

import java.util.List;

public class DynamoGroupDao implements GroupDao {

    private static volatile DynamoGroupDao instance;

    public static DynamoGroupDao instance() {

        if (instance == null) {
            synchronized(DynamoGroupDao.class) {
                if (instance == null)
                    instance = new DynamoGroupDao();
            }
        }
        return instance;
    }

    @Override
    public List<Group> getGroups() {
        return DynamoDBManager.mapper().scan(Group.class, new DynamoDBScanExpression());
    }

    @Override
    public void addGroup(Group group) { DynamoDBManager.mapper().save(group); }
}
