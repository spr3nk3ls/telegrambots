package com.spr3nk3ls.telegram.dao;

import com.spr3nk3ls.telegram.domain.Event;

import java.util.List;

public interface EventDao {
  List<Event> getAllEvents();

  void addEvent(Event event);
}
