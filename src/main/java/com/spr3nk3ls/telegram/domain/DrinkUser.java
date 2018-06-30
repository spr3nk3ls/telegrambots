package com.spr3nk3ls.telegram.domain;

import com.amazonaws.services.dynamodbv2.datamodeling.*;

import java.io.Serializable;
import java.util.List;

@DynamoDBTable(tableName = "USER")
public class DrinkUser implements Serializable {

    public static final String GROUP_INDEX = "Group-Index";

    @DynamoDBHashKey
    private String userId;

    @DynamoDBIndexHashKey(globalSecondaryIndexName = GROUP_INDEX)
    private String groupId;

    @DynamoDBAttribute
    private String firstName;

    @DynamoDBAttribute
    private String handle;

    @DynamoDBAttribute
    private List<String> aliases;

    public DrinkUser(){
    }

    public DrinkUser(String userId){
        this.userId = userId;
    }

    public DrinkUser(String userId, String groupId, String firstName){
        this.userId = userId;
        this.groupId = groupId;
        this.firstName = firstName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }


}
