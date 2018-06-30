package com.spr3nk3ls.telegram.bot;

import com.spr3nk3ls.telegram.dao.BrandDao;
import com.spr3nk3ls.telegram.dao.EventDao;
import com.spr3nk3ls.telegram.dao.GroupDao;
import com.spr3nk3ls.telegram.dao.UserDao;
import com.spr3nk3ls.telegram.dao.dynamo.DynamoBrandDao;
import com.spr3nk3ls.telegram.dao.dynamo.DynamoEventDao;
import com.spr3nk3ls.telegram.dao.dynamo.DynamoGroupDao;
import com.spr3nk3ls.telegram.dao.dynamo.DynamoUserDao;
import com.spr3nk3ls.telegram.domain.Brand;
import com.spr3nk3ls.telegram.domain.DrinkUser;
import com.spr3nk3ls.telegram.domain.Event;
import com.spr3nk3ls.telegram.domain.Group;
import org.telegram.telegrambots.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.api.objects.ChatMember;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.exceptions.TelegramApiException;

public class DrinkBot extends AbstractBot {

    UserDao userDao = DynamoUserDao.instance();
    GroupDao groupIdDao = DynamoGroupDao.instance();
    EventDao eventDao = DynamoEventDao.instance();
    BrandDao brandDao = DynamoBrandDao.instance();

    @Override
    protected String handlePrivateResponse(Message message) {
        try {
            if(userIsAuthorized(message.getChatId())){
                if(message.getText().startsWith("/turf")){
                   return handleTurf(message);
                }
                if(message.getText().startsWith("/init")){
                    return handleInit(message);
                }
                if(message.getText().startsWith("/hoeveel")){
                    return handleHoeveel(message);
                }
            } else {
                return "Ik ken jou niet.";
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return "Er is iets misgegaan";
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
            DrinkUser dbUser = userDao.getDrinkUser(Long.toString(userId));
            if(dbUser != null && dbUser.getGroupId().equals(groupId)){
                return true;
            }
            ChatMember chatMember = getSender().execute(new GetChatMember().setChatId(groupId).setUserId(userId.intValue()));
            if (chatMember != null) {
                userDao.addDrinkUser(new DrinkUser(chatMember.getUser().getId().toString(), groupId, chatMember.getUser().getFirstName()));
                //TODO give feedback that new user was added
                return true;
            }
        }
        return false;
    }

    @Override
    protected String handleGroupAdd(Message message){
        if(getRegisteredGroupId() == null) {
            groupIdDao.addGroup(new Group(message.getChatId().toString()));
            return "Hallo allemaal!";
        } else if (getRegisteredGroupId().equals(message.getChatId().toString())){
            return "Daar ben ik weer!";
        }
        //TODO leave group
        sendMessage(message.getChatId(), "Ik zit al in een andere groep.");
        return null;
    }

    private String getRegisteredGroupId() {
        return groupIdDao.getGroups().stream()
                .findFirst()
                .map(Group::getGroupId)
                .map(String::toString)
                .orElse(null);
    }

    protected String handleTurf(Message message){
        String[] turfStringArray = message.getText().split(" ");
        if(turfStringArray.length > 1){
            String brandName = turfStringArray[1];
            Brand brand = brandDao.getBrand(brandName);
            if(brand == null){
                return "We hebben geen " + brandName;
            } else {
                eventDao.addEvent(new Event(Long.toString(message.getChatId()), brandName, -1L));
                return "Ik heb een " + brandName + " voor je geturfd.";
            }
        } else {
            return "Ik snap niet wat je bedoelt.";
        }
    }

    protected String handleInit(Message message){
        String[] initStringArray = message.getText().split(" ");
        if(initStringArray.length < 5){
            return "Ik snap niet wat je bedoelt";
        }
        try {
            Long amount = Long.parseLong(initStringArray[1]);
            String brandName = initStringArray[2];
            Double volume = Double.parseDouble(initStringArray[3]);
            Double price = Double.parseDouble(initStringArray[4]);
            brandDao.addBrand(new Brand(brandName, volume, price));
            eventDao.addEvent(new Event(Long.toString(message.getChatId()), brandName, amount));
            return "" + amount + " blikken " + brandName + " toegevoegd.";
        } catch (NumberFormatException e){
            return "Je hebt geen getal ingevuld.";
        }
    }

    protected String handleHoeveel(Message message){
      //TODO merge with turf
        String[] turfStringArray = message.getText().split(" ");
        if(turfStringArray.length > 1){
            String brandName = turfStringArray[1];
            Brand brand = brandDao.getBrand(brandName);
            if(brand == null){
                return "We hebben geen " + brandName;
            } else {
                int amountLeft = eventDao.getAllEvents().stream()
                        .filter(event -> event.getBrandName().equals(brandName))
                        .mapToInt(event -> event.getAmount().intValue())
                        .sum();
                //TODO is/zijn
                return "Er zijn nog " + amountLeft + " blikken " + brandName + " over.";
            }
        } else {
            return "Ik snap niet wat je bedoelt.";
        }
    }

}
