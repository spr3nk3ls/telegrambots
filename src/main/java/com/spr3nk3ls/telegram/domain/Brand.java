package com.spr3nk3ls.telegram.domain;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import java.util.List;

@DynamoDBTable(tableName = "BRAND")
public class Brand implements Aliassable {

  @DynamoDBHashKey
  private String brandName;

  @DynamoDBAttribute
  private Double unitVolume;

  @DynamoDBAttribute
  private Double unitPrice;

  @DynamoDBAttribute
  private Boolean depleted;

  @DynamoDBAttribute
  private List<String> aliases;

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

  @Override
  public boolean equals(Object aBrand){
    if(!(aBrand instanceof Brand)){
      return false;
    }
    Brand otherBrand = (Brand)aBrand;
    if(!brandName.equalsIgnoreCase(otherBrand.brandName)){
      return false;
    }
    if(!otherBrand.getUnitVolume().equals(unitVolume)){
      return false;
    }
    if(!otherBrand.getUnitPrice().equals(unitPrice)){
      return false;
    }
    return true;
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

  public Boolean isDepleted(){
    return depleted;
  }

  public void setDepleted(Boolean depleted){
    this.depleted = depleted;
  }

  @Override
  public List<String> getAliases() {
    return aliases;
  }

  @Override
  public void setAliases(List<String> aliases) {
    this.aliases = aliases;
  }

  @Override
  @DynamoDBIgnore
  public String getName(){
    return "het bier " + getBrandName();
  }
}
