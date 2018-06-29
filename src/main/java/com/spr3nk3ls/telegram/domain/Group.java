package com.spr3nk3ls.telegram.domain;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import java.io.Serializable;

@DynamoDBTable(tableName = "GROUP")
public class Group implements Serializable {

    public Group(){
    }

    public Group(String groupId){
        this.groupId = groupId;
    }

    @DynamoDBHashKey
    private String groupId;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
}
