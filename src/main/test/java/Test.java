package com.vsubhuman.telegram;

import org.telegram.telegrambots.api.methods.BotApiMethod;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.AbsSender;
import org.telegram.telegrambots.bots.TelegramWebhookBot;

import java.util.function.Function;

public class Test {

    @Test
    public void test(){
        List<Group> groups = new ArrayList<Group>();
        Group group = new Group();
        group.setGroupId(8L);
        groupIdDao.getGroups().stream().findFirst().map(Group::getGroupId).
    }

}
