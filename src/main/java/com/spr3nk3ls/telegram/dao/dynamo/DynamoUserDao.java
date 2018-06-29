package com.spr3nk3ls.telegram.dao.dynamo;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.spr3nk3ls.telegram.dao.UserDao;
import com.spr3nk3ls.telegram.domain.DrinkUser;
import com.spr3nk3ls.telegram.domain.Group;
import com.spr3nk3ls.telegram.util.DynamoDBManager;

import java.util.HashMap;
import java.util.List;

public class DynamoUserDao implements UserDao {

    private static volatile DynamoUserDao instance;

    public static DynamoUserDao instance() {

        if (instance == null) {
            synchronized(DynamoUserDao.class) {
                if (instance == null)
                    instance = new DynamoUserDao();
            }
        }
        return instance;
    }

    @Override
    public List<DrinkUser> getDrinkUsers() {
        return DynamoDBManager.mapper().scan(DrinkUser.class, new DynamoDBScanExpression());
    }

    @Override
    public void addDrinkUser(DrinkUser user){
        DynamoDBManager.mapper().save(user);
    }
}
