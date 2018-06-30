package com.spr3nk3ls.telegram.dao;

import com.spr3nk3ls.telegram.domain.Brand;

import java.util.List;

public interface BrandDao {
  List<Brand> getAllBrands();
  Brand getBrand(String brandName);
  void addBrand(Brand brand);
}
