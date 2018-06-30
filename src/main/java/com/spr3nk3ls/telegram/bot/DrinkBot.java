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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

public class DrinkBot extends AbstractBot {

    UserDao userDao = DynamoUserDao.instance();
    GroupDao groupIdDao = DynamoGroupDao.instance();
    EventDao eventDao = DynamoEventDao.instance();
    BrandDao brandDao = DynamoBrandDao.instance();

    @Override
    protected String handlePrivateResponse(Message message) {
        return handleResponse(message);
    }

    @Override
    protected String handleGroupResponse(Message message) {
        return handleResponse(message);
    }

    private String handleResponse(Message message){
        try {
            if(userIsAuthorized(message.getFrom().getId())){
                if(message.getText().startsWith("/turf") || message.getText().startsWith("/hoeveel")){
                    return handleTurfOrHoeveel(message);
                }
                if(message.getText().startsWith("/init")){
                    return handleInit(message);
                }
                if(message.getText().startsWith("/verbruik")){
                    return handleVerbruik(message);
                }
                if(message.getText().startsWith("/hoofdpijn")){
                    return handleHoofdpijn(message);
                }
                if(message.getText().startsWith("/telling")){
                    //TODO add table with telling
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
                sendMessage(Long.parseLong(groupId), "Ik heb " + chatMember.getUser().getFirstName() + " aan de turflijst toegevoegd. Welkom!");
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
            Brand brand = getBrandFromUserInput(brandName);
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
                return handleReport();
            }
        }
        return "Ik snap niet wat je bedoelt.";
    }

    private String handleTurf(Integer chatId, String brandName, String voor){
        String userId;
        DrinkUser user;
        String userName = "je";
        if(voor != null){
            user = userDao.getDrinkUsers().stream()
                    .filter(dbUser -> dbUser.getFirstName().equalsIgnoreCase(voor.trim()))
                    .findFirst().orElse(null);
            if(user != null){
                userId = user.getUserId();
                if(!Integer.toString(chatId).equals(userId)) {
                    userName = user.getFirstName();
                }
            } else {
                return "Ik snap niet wie " + voor.trim() + " is.";
            }
        } else {
            userId = Integer.toString(chatId);
        }
        eventDao.addEvent(new Event(userId, brandName, -1L));
        return "Ik heb een " + brandName + " voor " + userName + " geturfd.";
    }

    private String handleHoeveel(String[] turfStringArray, Brand brand){
        int amountLeft = getAmountLeftForBrand(brand);
        if(turfStringArray.length > 2 && turfStringArray[1].equalsIgnoreCase("liter")){
            return String.format("Er is nog %.1f liter %s over.", amountLeft*brand.getUnitVolume(), brand.getBrandName());
        }
        if(turfStringArray.length > 2 && turfStringArray[1].equalsIgnoreCase("euro")){
            return String.format("Er is nog %.2f euro %s over.", amountLeft*brand.getUnitPrice(), brand.getBrandName());
        }
        return String.format("Er %s nog %d %s %s over.", amountLeft == 1 ? "is" : "zijn", amountLeft, amountLeft == 1 ? "blik" : "blikken", brand.getBrandName());
    }

    private String handleReport(){
        List<String> reportList = new ArrayList<>();
        reportList.add("Dit is er nog over:");
        for(Brand brand : brandDao.getAllBrands()){
            reportList.add(String.format("%d %s", getAmountLeftForBrand(brand), brand.getBrandName()));
        }
        reportList.add(getLitersAndEuros());
        return String.join("\n", reportList);
    }

    private Integer getAmountLeftForBrand(Brand brand){
        return eventDao.getAllEvents().stream()
                .filter(event -> event.getBrandName().equalsIgnoreCase(brand.getBrandName()))
                .mapToInt(event -> event.getAmount().intValue())
                .sum();
    }

    private String getLitersAndEuros(){
        double totalAmountLeft = eventDao.getAllEvents().stream()
                .mapToDouble(event -> event.getAmount()*brandDao.getBrand(event.getBrandName()).getUnitVolume())
                .sum();
        double totalPriceLeft = eventDao.getAllEvents().stream()
                .mapToDouble(event -> event.getAmount()*brandDao.getBrand(event.getBrandName()).getUnitPrice())
                .sum();
        return String.format("Er is in totaal nog %.1f liter (%.2f euro) bier over.", totalAmountLeft, totalPriceLeft);
    }

    private String handleVerbruik(Message message){
        String[] verbruikArray = message.getText().split(" ");
        if(verbruikArray.length == 1){
            List<String> brandArray = new ArrayList<>();
            for(Brand brand : brandDao.getAllBrands()){
                brandArray.add(getVerbruikForBrand(message, brand.getBrandName()));
            }
            return String.join("\n", brandArray);
        }
        if(verbruikArray.length == 2){
            Brand brand = getBrandFromUserInput(verbruikArray[1]);
            if(brand == null){
                return "We hebben geen " + verbruikArray[1] + ".\n"
                        + "We hebben wel: "
                        + brandDao.getAllBrands().stream().map(Brand::getBrandName).collect(Collectors.joining("" + ", ")) + ".";
            } else {
                return getVerbruikForBrand(message, brand.getBrandName());
            }
        }
        //TODO
        return "Ik snap het niet.";
    }

    private String getVerbruikForBrand(Message message, String brand) {
        Long totalAmount = eventDao.getAllEvents().stream()
                .filter(event -> event.getDrinkerId().equals(Integer.toString(message.getFrom().getId())))
                .filter(event -> event.getBrandName().equals(brand))
                .filter(event -> event.getAmount() < 0)
                .mapToLong(event -> -event.getAmount())
                .sum();
        //TODO positive negative sum.
        return String.format("Je hebt %d %s %s gehad.", totalAmount, totalAmount == 1 ? "blik" : "blikken", brand);
    }

    private Brand getBrandFromUserInput(String brandName){
        return brandDao.getAllBrands().stream()
                .filter(brand -> brand.getBrandName().equalsIgnoreCase(brandName.trim()))
                .findFirst().orElse(null);
    }

    private String handleHoofdpijn(Message message){
        Calendar fourOClockToday = Calendar.getInstance();
        fourOClockToday.set(Calendar.HOUR_OF_DAY, 4);
        Calendar fourOClockYesterday = (Calendar)fourOClockToday.clone();
        fourOClockYesterday.add(Calendar.DAY_OF_YEAR, -1);
        System.out.println("yesterday: " + fourOClockYesterday.getTimeInMillis());
        System.out.println("today: " + fourOClockToday.getTimeInMillis());
        Double tooMuchToDrink = eventDao.getAllEvents().stream()
                .filter(event -> event.getTimestamp() > fourOClockYesterday.getTimeInMillis())
                .filter(event -> event.getTimestamp() < fourOClockToday.getTimeInMillis())
                .filter(event -> event.getDrinkerId().equals(Integer.toString(message.getFrom().getId())))
                .filter(event -> event.getAmount() < 0)
                .mapToDouble(event -> -event.getAmount()*brandDao.getBrand(event.getBrandName()).getUnitVolume())
                .sum();
        return String.format("Je hebt gisteren %.1f liter bier gedronken.", tooMuchToDrink);
    }
}
