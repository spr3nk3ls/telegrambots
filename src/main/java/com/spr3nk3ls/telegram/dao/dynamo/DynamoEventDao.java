package com.spr3nk3ls.telegram.dao.dynamo;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.spr3nk3ls.telegram.dao.EventDao;
import com.spr3nk3ls.telegram.domain.Event;
import com.spr3nk3ls.telegram.util.DynamoDBManager;

import java.util.List;

public class DynamoEventDao implements EventDao{

  private static volatile DynamoEventDao instance;

  public static DynamoEventDao instance() {

    if (instance == null) {
      synchronized(DynamoEventDao.class) {
        if (instance == null)
          instance = new DynamoEventDao();
      }
    }
    return instance;
  }

  @Override
  public List<Event> getAllEvents() {
    return DynamoDBManager.mapper().scan(Event.class, new DynamoDBScanExpression());
  }

  @Override
  public void addEvent(Event event) {
    DynamoDBManager.mapper().save(event);
  }
}
