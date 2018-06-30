package com.spr3nk3ls.telegram.domain;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import java.util.Calendar;

@DynamoDBTable(tableName = "EVENT")
public class Event {

  @DynamoDBHashKey
  private Long timestamp;

  @DynamoDBAttribute
  private String drinkerId;

  @DynamoDBAttribute
  private String brandName;

  @DynamoDBAttribute
  private Long amount;

  public Event(){
  }

  public Event(String drinkerId, String brandName, Long amount){
    this.timestamp = Calendar.getInstance().getTimeInMillis();
    this.drinkerId = drinkerId;
    this.brandName = brandName;
    this.amount = amount;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
  }

  public String getDrinkerId() {
    return drinkerId;
  }

  public void setDrinkerId(String drinkerId) {
    this.drinkerId = drinkerId;
  }

  public String getBrandName() {
    return brandName;
  }

  public void setBrandName(String brandName) {
    this.brandName = brandName;
  }

  public Long getAmount() {
    return amount;
  }

  public void setAmount(Long amount) {
    this.amount = amount;
  }
}
