package com.spr3nk3ls.telegram.domain;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "BRAND")
public class Brand {

  @DynamoDBHashKey
  private String brandName;

  @DynamoDBAttribute
  private Double unitVolume;

  @DynamoDBAttribute
  private Double unitPrice;

  public Brand(){
  }

  public Brand(String brandName){
    this.brandName = brandName;
  }

  public Brand(String brandName, Double unitVolume, Double unitPrice){
    this.brandName = brandName;
    this.unitVolume = unitVolume;
    this.unitPrice = unitPrice;
  }

  public Double getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(Double unitPrice) {
    this.unitPrice = unitPrice;
  }

  public String getBrandName() {
    return brandName;
  }

  public void setBrandName(String brandName) {
    this.brandName = brandName;
  }

  public Double getUnitVolume() {
    return unitVolume;
  }

  public void setUnitVolume(Double unitCost) {
    this.unitVolume = unitCost;
  }
}
