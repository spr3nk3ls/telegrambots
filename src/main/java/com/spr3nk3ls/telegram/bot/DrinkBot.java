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

import java.util.stream.Collectors;

public class DrinkBot extends AbstractBot {

    UserDao userDao = DynamoUserDao.instance();
    GroupDao groupIdDao = DynamoGroupDao.instance();
    EventDao eventDao = DynamoEventDao.instance();
    BrandDao brandDao = DynamoBrandDao.instance();

    @Override
    protected String handlePrivateResponse(Message message) {
        return handleResponse(message, false);
    }

    @Override
    protected String handleGroupResponse(Message message) {
        return handleResponse(message, true);
    }

    private String handleResponse(Message message, boolean groupMessage){
        try {
            if(userIsAuthorized(message.getFrom().getId())){
                if(message.getText().startsWith("/turf") || message.getText().startsWith("/hoeveel")){
                    return handleTurfOrHoeveel(message);
                }
                if(message.getText().startsWith("/init")){
                    return handleInit(message);
                }
            } else {
                return "Ik ken jou niet.";
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return "Er is iets misgegaan";
        }
        return null;
    }

    private boolean userIsAuthorized(Integer userId) throws TelegramApiException {
        if(userId == null){
            System.out.println("User id is null.");
            return false;
        }
        String groupId = getRegisteredGroupId();
        if(groupId != null) {
            DrinkUser dbUser = userDao.getDrinkUser(Integer.toString(userId));
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

    private String handleInit(Message message){
        String[] initStringArray = message.getText().split(" ");
        if(initStringArray.length < 5){
            return "Vul in: /init aantal merknaam volume prijs";
        }
        try {
            Long amount = Long.parseLong(initStringArray[1]);
            String brandName = initStringArray[2];
            Double volume = Double.parseDouble(initStringArray[3]);
            Double price = Double.parseDouble(initStringArray[4]);
            brandDao.addBrand(new Brand(brandName, volume, price));
            eventDao.addEvent(new Event(Integer.toString(message.getFrom().getId()), brandName, amount));
            return "" + amount + " blikken " + brandName + " toegevoegd.";
        } catch (NumberFormatException e){
            return "Vul in: /init aantal merknaam volume prijs";
        }
    }

    private String handleTurfOrHoeveel(Message message){
        String voor = null;
        String[] turfStringArray;
        if(message.getText().contains(" voor ")){
            String[] voorSplit = message.getText().split(" voor ");
            turfStringArray = voorSplit[0].split(" ");
            voor = voorSplit[1];
        } else {
            turfStringArray = message.getText().split(" ");
        }
        if(turfStringArray.length > 1){
            String brandName = turfStringArray[turfStringArray.length - 1];
            Brand brand = brandDao.getBrand(brandName.toLowerCase());
            if(brand == null){
                return "We hebben geen " + brandName + ".\n"
                        + "We hebben wel: "
                        + brandDao.getAllBrands().stream().map(Brand::getBrandName).collect(Collectors.joining("" + ", ")) + ".";
            } else {
                if(message.getText().startsWith("/turf")){
                    return handleTurf(message.getFrom().getId(), brand.getBrandName(), voor);
                }
                if(message.getText().startsWith("/hoeveel")){
                    return handleHoeveel(turfStringArray, brand);
                }
            }
        } else {
            if(message.getText().startsWith("/hoeveel")) {
                return handleHoeveelZeroArgs();
            }
        }
        return "Ik snap niet wat je bedoelt.";
    }

    private String handleTurf(Integer chatId, String brandName, String voor){
        String userId;
        DrinkUser user;
        if(voor != null){
            user = userDao.getDrinkUsers().stream()
                    .filter(dbUser -> dbUser.getFirstName().equalsIgnoreCase(voor.trim()))
                    .findFirst().orElse(null);
            if(user != null){
                userId = user.getUserId();
            } else {
                return "Ik snap niet wie " + voor.trim() + " is.";
            }
        } else {
            userId = Integer.toString(chatId);
        }
        eventDao.addEvent(new Event(userId, brandName, -1L));
        return "Ik heb een " + brandName + " voor je geturfd.";
    }

    private String handleHoeveel(String[] turfStringArray, Brand brand){
        int amountLeft = eventDao.getAllEvents().stream()
                .filter(event -> event.getBrandName().equalsIgnoreCase(brand.getBrandName()))
                .mapToInt(event -> event.getAmount().intValue())
                .sum();
        if(turfStringArray.length > 2 && turfStringArray[1].equalsIgnoreCase("liter")){
            return String.format("Er is nog %.1f liter %s over.", amountLeft*brand.getUnitVolume(), brand.getBrandName());
        }
        if(turfStringArray.length > 2 && turfStringArray[1].equalsIgnoreCase("euro")){
            return String.format("Er is nog %.2f euro %s over.", amountLeft*brand.getUnitPrice(), brand.getBrandName());
        }
        return String.format("Er %s nog %d %s %s over.", amountLeft == 1 ? "is" : "zijn", amountLeft, amountLeft == 1 ? "blik" : "blikken", brand.getBrandName());
    }

    private String handleHoeveelZeroArgs(){
        double totalAmountLeft = eventDao.getAllEvents().stream()
                .mapToDouble(event -> event.getAmount()*brandDao.getBrand(event.getBrandName()).getUnitVolume())
                .sum();
        double totalPriceLeft = eventDao.getAllEvents().stream()
                .mapToDouble(event -> event.getAmount()*brandDao.getBrand(event.getBrandName()).getUnitPrice())
                .sum();
        return String.format("Er is in totaal nog %.1f liter (%.2f euro) bier over", totalAmountLeft, totalPriceLeft);
    }
}
