package com.spr3nk3ls.telegram.dao.dynamo;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.spr3nk3ls.telegram.dao.BrandDao;
import com.spr3nk3ls.telegram.domain.Brand;
import com.spr3nk3ls.telegram.util.DynamoDBManager;

import java.util.List;

public class DynamoBrandDao implements BrandDao {

  private static volatile DynamoBrandDao instance;

  public static DynamoBrandDao instance() {

    if (instance == null) {
      synchronized(DynamoBrandDao.class) {
        if (instance == null)
          instance = new DynamoBrandDao();
      }
    }
    return instance;
  }

  @Override
  public List<Brand> getAllBrands() {
    return DynamoDBManager.mapper().scan(Brand.class, new DynamoDBScanExpression());
  }

  @Override
  public Brand getBrand(String brandName) {
    return DynamoDBManager.mapper().load(new Brand(brandName));
  }

  @Override
  public void addBrand(Brand brand) {
    DynamoDBManager.mapper().save(brand);
  }
}
