package com.spr3nk3ls.telegram.bot;

import com.spr3nk3ls.telegram.dao.GroupDao;
import com.spr3nk3ls.telegram.dao.UserDao;
import com.spr3nk3ls.telegram.dao.dynamo.DynamoGroupDao;
import com.spr3nk3ls.telegram.dao.dynamo.DynamoUserDao;
import com.spr3nk3ls.telegram.domain.DrinkUser;
import com.spr3nk3ls.telegram.domain.Group;
import org.telegram.telegrambots.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.api.objects.ChatMember;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.exceptions.TelegramApiException;

public class DrinkBot extends AbstractBot {

    UserDao userDao = DynamoUserDao.instance();
    GroupDao groupIdDao = DynamoGroupDao.instance();

    @Override
    protected String handlePrivateResponse(Message message) {
        try {
            if(userIsAuthorized(message.getChat().getId())){
                return "Ik ken jou!";
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return "Ik ken jou niet.";
    }

    @Override
    protected String handleGroupResponse(Message message) {
        return null;
    }

    private boolean userIsAuthorized(Long userId) throws TelegramApiException {
        if(userId == null){
            System.out.println("Chat id is null.");
            return false;
        }
        String groupId = getRegisteredGroupId();
        if(groupId != null) {
            if(userDao.getDrinkUsers().stream()
                    .filter(user -> user.getGroupId().equals(groupId))
                    .map(DrinkUser::getUserId)
                    .anyMatch(id -> id.equals(userId.toString()))){
                return true;
            }
            ChatMember chatMember = getSender().execute(new GetChatMember().setChatId(groupId).setUserId(userId.intValue()));
            if (chatMember != null) {
                userDao.addDrinkUser(new DrinkUser(chatMember.getUser().getId().toString(), groupId, chatMember.getUser().getFirstName()));
                return true;
            }
        }
        return false;
    }

    @Override
    protected String handleGroupAdd(Message message){
        if(getRegisteredGroupId() == null) {
            groupIdDao.addGroup(new Group(message.getChatId().toString()));
        }
        return "Hallo allemaal!";
    }

    private String getRegisteredGroupId() {
        return groupIdDao.getGroups().stream()
                .findFirst()
                .map(Group::getGroupId)
                .map(String::toString)
                .orElse(null);
    }

}
