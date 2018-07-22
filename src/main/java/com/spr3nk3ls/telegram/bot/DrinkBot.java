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
import org.telegram.telegrambots.api.methods.groupadministration.LeaveChat;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.ChatMember;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

public class DrinkBot extends AbstractBot {

    private UserDao userDao = DynamoUserDao.instance();
    private GroupDao groupIdDao = DynamoGroupDao.instance();
    private EventDao eventDao = DynamoEventDao.instance();
    private BrandDao brandDao = DynamoBrandDao.instance();

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
                if(message.getText() == null){
                    return null;
                }
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
                if(message.getText().startsWith("/help")){
                    return handleHelp();
                }
                if(message.getText().startsWith("/info")){
                    return handleInfo(message);
                }
                if(message.getText().startsWith("/op")){
                    return handleOp(message);
                }
                if(message.getText().startsWith("/alias")){
                    return handleAlias(message);
                }
                if(message.getText().startsWith("/correctie")){
                    return handleCorrectie(message);
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
        sendMessage(message.getChatId(), "Ik zit al in een andere groep. Doei!");
        try {
            getSender().execute(new LeaveChat().setChatId(message.getChatId()));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
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
        String[] initStringArray = message.getText().split("\\s+");
        if(initStringArray.length != 5){
            return "Vul in: /init aantal biermerk volume prijs";
        }
        String brandName = initStringArray[2];
        try {
            Long amount = Long.parseLong(initStringArray[1]);
            if(amount <= 0){
                return "Het aantal moet een positief getal zijn.";
            }
            Double volume = Double.parseDouble(initStringArray[3]);
            if(amount <= 0){
                return "De inhoud moet een positief getal zijn.";
            }
            Double price = Double.parseDouble(initStringArray[4]);
            if(amount <= 0){
                return "De prijs moet een positief getal zijn.";
            }
            Brand oldBrand = brandDao.getBrand(brandName);
            Brand newBrand = new Brand(brandName, volume, price);
            if(brandDao.getBrand(brandName) != null && !oldBrand.equals(newBrand)){
                return "We hebben al een ander bier met de naam " + brandName;
            }
            brandDao.addBrand(newBrand);
            eventDao.addEvent(new Event(Integer.toString(message.getFrom().getId()), brandName, amount, Event.EventType.INIT));
            return "" + amount + " blikken " + brandName + " toegevoegd.";
        } catch (NumberFormatException e){
            return "Vul in: /init aantal biermerk inhoud prijs. Aantal, volume en prijs moeten een getal zijn.";
        }
    }

    private String handleTurfOrHoeveel(Message message) throws TelegramApiException {
        String voor = null;
        String[] turfStringArray;
        if(message.getText().contains(" voor ")){
            String[] voorSplit = message.getText().split("\\s+voor\\s+");
            turfStringArray = voorSplit[0].split("\\s+");
            voor = voorSplit[1];
        } else {
            turfStringArray = message.getText().split("\\s+");
        }
        if(turfStringArray.length > 1){
            String brandName = turfStringArray[turfStringArray.length - 1];
            Brand brand = getBrandFromUserInput(brandName);
            if(brand == null){
                return weHaveNoMessage(brandName);
            } else {
                if(message.getText().startsWith("/turf")){
                    if(brand.isDepleted() != null && brand.isDepleted()){
                        return "De " + brandName + " is op.";
                    }
                    if(turfStringArray.length == 3){
                        try {
                            Long amount = Long.parseLong(turfStringArray[1]);
                            if (amount <= 0) {
                                return "Het aantal moet een positief getal zijn.";
                            }
                            return handleTurf(message.getFrom().getId(), brand.getBrandName(), voor, amount);
                        } catch (NumberFormatException e){
                            return "Het aantal moet een getal zijn.";
                        }
                    }
                    return handleTurf(message.getFrom().getId(), brand.getBrandName(), voor, 1L);
                }
                if(message.getText().startsWith("/hoeveel")){
                    return handleHoeveel(turfStringArray, brand);
                }
            }
        } else {
            if(message.getText().startsWith("/hoeveel")) {
                return handleReport();
            }
            if(message.getText().startsWith("/turf")) {
                return "Welk bier wil je turven?";
            }
        }
        return "Ik snap niet wat je bedoelt.";
    }

    private String handleTurf(Integer chatId, String brandName, String voor, Long amount) throws TelegramApiException {
        String userId;
        DrinkUser user;
        String userName = "je";
        String amountString = (amount == 1 ? "een" : amount) + " " + brandName;
        if(voor != null){
            user = getUser(voor);
            if(user != null){
                userId = user.getUserId();
                if(!Integer.toString(chatId).equals(userId)) {
                    userName = user.getFirstName();
                    DrinkUser door = userDao.getDrinkUser(Integer.toString(chatId));
                    if(door != null){
                        try {
                            getSender().execute(new SendMessage().setChatId(Long.parseLong(user.getUserId())).setText(door.getFirstName() + " heeft " + amountString + " voor je geturfd."));
                        } catch (TelegramApiRequestException e){
                            //Request to user failed, reroute to group
                            Group group = groupIdDao.getGroups().stream().findFirst().orElse(null);
                            if(group != null) {
                                getSender().execute(new SendMessage().setChatId(group.getGroupId()).setText(door.getFirstName() + " heeft " + amountString + " voor " + userName + " geturfd."));
                            }
                        }
                    }
                }
            } else {
                return "Ik snap niet wie " + voor.trim() + " is.";
            }
        } else {
            userId = Integer.toString(chatId);
        }
        eventDao.addEvent(new Event(userId, brandName, -amount, Event.EventType.TURF));
        return "Ik heb " + amountString + " voor " + userName + " geturfd.";
    }

    private String handleHoeveel(String[] turfStringArray, Brand brand){
      //TODO also note if it is op
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
        String voor = null;
        String[] verbruikArray;
        DrinkUser oneUser = null;
        if(message.getText().contains(" van ")){
            String[] voorSplit = message.getText().split("\\s+van\\s+");
            verbruikArray = voorSplit[0].split("\\s+");
            voor = voorSplit[1];
            final String userName = voor;
            oneUser = getUser(voor);
        } else {
            verbruikArray = message.getText().split("\\s+");
        }
        if(verbruikArray.length == 1){
            List<String> brandArray = new ArrayList<>();
            for(Brand brand : brandDao.getAllBrands()){
                if("iedereen".equals(voor)){
                    for(DrinkUser user : userDao.getDrinkUsers()){
                        brandArray.add(getVerbruikForBrandAndUser(user, brand.getBrandName()));
                    }
                } else if(oneUser != null){
                    brandArray.add(getVerbruikForBrandAndUser(oneUser, brand.getBrandName()));
                } else {
                    brandArray.add(getVerbruikForBrand(message, brand.getBrandName()));
                }
            }
            return String.join("\n", brandArray);
        }
        if(verbruikArray.length == 2){
            Brand brand = getBrandFromUserInput(verbruikArray[1]);
            if(brand == null){
                return weHaveNoMessage(verbruikArray[1]);
            } else {
                return getVerbruikForBrand(message, brand.getBrandName());
            }
        }
        return "Ik snap het niet.";
    }

    private String getVerbruikForBrand(Message message, String brand) {
        Long totalAmount = eventDao.getAllEvents().stream()
                .filter(event -> !Event.EventType.CORRECTION.equals(event.getEventType()))
                .filter(event -> event.getDrinkerId().equals(Integer.toString(message.getFrom().getId())))
                .filter(event -> event.getBrandName().equals(brand))
                .filter(event -> event.getAmount() < 0)
                .mapToLong(event -> -event.getAmount())
                .sum();
        return String.format("Je hebt %d %s %s gehad.", totalAmount, totalAmount == 1 ? "blik" : "blikken", brand);
    }

    private String getVerbruikForBrandAndUser(DrinkUser user, String brand) {
        Long totalAmount = eventDao.getAllEvents().stream()
                .filter(event -> !Event.EventType.CORRECTION.equals(event.getEventType()))
                .filter(event -> event.getDrinkerId().equals(user.getUserId()))
                .filter(event -> event.getBrandName().equals(brand))
                .filter(event -> event.getAmount() < 0)
                .mapToLong(event -> -event.getAmount())
                .sum();
        return String.format("%s heeft %d %s %s gehad.", user.getFirstName(), totalAmount, totalAmount == 1 ? "blik" : "blikken", brand);
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
        Double tooMuchToDrink = eventDao.getAllEvents().stream()
                .filter(event -> !Event.EventType.CORRECTION.equals(event.getEventType()))
                .filter(event -> event.getTimestamp() > fourOClockYesterday.getTimeInMillis())
                .filter(event -> event.getTimestamp() < fourOClockToday.getTimeInMillis())
                .filter(event -> event.getDrinkerId().equals(Integer.toString(message.getFrom().getId())))
                .filter(event -> event.getAmount() < 0)
                .mapToDouble(event -> -event.getAmount()*brandDao.getBrand(event.getBrandName()).getUnitVolume())
                .sum();
        return String.format("Je hebt gisteren %.1f liter bier gedronken.", tooMuchToDrink);
    }

    private String handleHelp(){
        List<String> helpText = new ArrayList<>();
        helpText.add("'/help' geeft een lijst met commando's");
        helpText.add("'/turf [n] biermerk [voor persoon]' turft een (optioneel n) biertjes (optioneel voor persoon)");
        helpText.add("/hoeveel' [eenheid] biermerk' "
                + " print het aantal blikken (optioneel: eenheid euro of liter) bier dat over is.");
        helpText.add("'/hoeveel' print het aantal liter/euro bier dat in totaal nog over is.");
        helpText.add("'/verbruik [biermerk]' geeft de hoeveelheid bier (optioneel voor biermerk) die je gehad hebt.");
        helpText.add("'/verbruik van [naam]' geeft de hoeveelheid bier (optioneel voor biermerk) die [naam] gehad hebt.");
        helpText.add("'/verbruik van iedereen' geeft de hoeveelheid bier (optioneel voor biermerk) die iedereen gehad hebt.");
        helpText.add("'/info biermerk' geeft de inhoud en prijs van een biertje.");
        helpText.add("'/init aantal biermerk inhoud prijs' voegt nieuw bier toe.");
        helpText.add("'/op biermerk' markeert dit bier als op in de biervoorraad. Je kunt hem vanaf dan niet meer turven.");
        helpText.add("'/correctie biermerk aantal' als bij tellingen blijkt dat de turfjes niet overeenkomen met de vooraad.");
        return String.join("\n", helpText);
    }

    private String handleInfo(Message message){
        String[] info = message.getText().split("\\s+");
        if(info.length == 2) {
            Brand brand = getBrandFromUserInput(info[1]);
            if(brand == null){
                return weHaveNoMessage(info[1]);
            }
            return "1 blik " + brand.getBrandName() + " kost " + brand.getUnitPrice() + " euro en bevat " + brand.getUnitVolume() + " bier.";
        } else {
            return "Gebruik: /info [biermerk].";
        }
    }

    private String handleOp(Message message) throws TelegramApiException {
        String[] info = message.getText().split("\\s+");
        if(info.length == 2) {
            Brand brand = getBrandFromUserInput(info[1]);
            if(brand == null){
                return weHaveNoMessage(info[1]);
            }
            String output;
            if(brand.isDepleted() != null && brand.isDepleted()){
                brand.setDepleted(false);
                output = brand.getBrandName() + " is nu niet meer op.";
            } else {
                brand.setDepleted(true);
                output = brand.getBrandName() + " is nu op en kan niet meer geturfd worden.";
            }
            brandDao.addBrand(brand);
            Group group = groupIdDao.getGroups().stream().findFirst().orElse(null);
            if(group != null){
                getSender().execute(new SendMessage().setChatId(group.getGroupId()).setText(output));
            }
            return null;
        } else {
            return "Gebruik: /op [biermerk].";
        }
    }

    private String weHaveNoMessage(String beerWeDontHave){
        List<Brand> allBrands = brandDao.getAllBrands();
        String weDoHaveMessage;
        if(allBrands.isEmpty()){
            weDoHaveMessage = "We hebben helemaal geen bier.";
        } else {
            weDoHaveMessage = "We hebben wel: "
                    + brandDao.getAllBrands().stream().map(Brand::getBrandName).collect(Collectors.joining("" + ", ")) + ".";
        }
        return "We hebben geen " + beerWeDontHave + ".\n" + weDoHaveMessage;
    }

    private String handleAlias(Message message){
        String[] aliasArray = message.getText().split("\\s+");
        if(aliasArray.length == 3) {
            DrinkUser user = getUser(aliasArray[1]);
            if (user == null) {
                //TODO format
                return "Ik ken " + aliasArray[1] + " niet";
            }
            DrinkUser aliasUser = getUser(aliasArray[2]);
            if (aliasUser != null) {
                if(aliasArray[2].equalsIgnoreCase(aliasUser.getFirstName())){
                    return "Ik ken al een " + aliasUser.getFirstName() + ".";
                }
                return "Ik ken al een " + aliasArray[2] + ", want dat is " + aliasUser.getFirstName() + ".";
            }
            List<String> aliases = user.getAliases();
            if (aliases == null) {
                aliases = new ArrayList<>();
            }
            aliases.add(aliasArray[2]);
            user.setAliases(aliases);
            userDao.addDrinkUser(user);
            //TODO format
            return "Ik weet nu dat " + user.getFirstName() + " ook " + aliasArray[2] + " heet.";
        }
        if(aliasArray.length == 2){
            DrinkUser aliasUser = getUser(aliasArray[1]);
            if(aliasUser == null){
                return "Ik ken " + aliasArray[1] + " niet.";
            }
            if(aliasUser.getAliases() == null || aliasUser.getAliases().isEmpty()){
                return aliasUser.getFirstName() + " heeft nog geen bijnamen.";
            }
            return aliasUser.getFirstName() + " heet ook: " + String.join(", ", aliasUser.getAliases()) + ".";
        }
        return "Gebruik: /alias [naam] of /alias [naam] [bijnaam]";
    }

    private String handleCorrectie(Message message) throws TelegramApiException {
        String[] corArray = message.getText().split("\\s+");
        if(corArray.length == 1){
            List<String> outputString = new ArrayList<>();
            boolean noCorrection = eventDao.getAllEvents().stream()
                    .noneMatch(event -> Event.EventType.CORRECTION.equals(event.getEventType()));
            if(noCorrection){
                return "Er is nog niets gecorrigeerd.";
            }
            outputString.add("Er is gecorrigeerd voor:");
            for(Brand brand : brandDao.getAllBrands()){
                long missing = eventDao.getAllEvents().stream()
                        .filter(event -> Event.EventType.CORRECTION.equals(event.getEventType()))
                        .filter(event -> event.getBrandName().equals(brand.getBrandName()))
                        .mapToLong(event -> -event.getAmount())
                        .sum();
                if(missing > 0){
                    //TODO format
                    outputString.add(missing + " blikken " + brand.getBrandName());
                }
            }
            return String.join("\n", outputString);
        }
        if(corArray.length >= 2) {
            Brand brand = getBrandFromUserInput(corArray[1]);
            if (corArray.length == 2) {
                long missing = eventDao.getAllEvents().stream()
                        .filter(event -> Event.EventType.CORRECTION.equals(event.getEventType()))
                        .filter(event -> event.getBrandName().equals(brand.getBrandName()))
                        .mapToLong(event -> -event.getAmount())
                        .sum();
                //TODO format
                //TODO add events and timestamps
                return "Er is eerder gecorrigeerd voor " + missing + " ontbrekende blikken " + brand.getBrandName();
            }
            if (corArray.length == 3) {
                try {
                    Long correction = Long.parseLong(corArray[2]);
                    Integer oldAmount = getAmountLeftForBrand(brand);
                    eventDao.addEvent(new Event(Integer.toString(message.getFrom().getId()), brand.getBrandName(), correction - oldAmount, Event.EventType.CORRECTION));
                    DrinkUser user = userDao.getDrinkUser(Integer.toString(message.getFrom().getId()));
                    if(user != null){
                        //TODO format
                        String output = user.getFirstName() +
                                " heeft de hoeveelheid " + brand.getBrandName() +
                                " gecorrigeerd naar " + correction + " blikken.";
                        Group group = groupIdDao.getGroups().stream().findFirst().orElse(null);
                        if(group != null){
                            getSender().execute(new SendMessage().setChatId(group.getGroupId()).setText(output));
                        }
                        //TODO format
                        return "Gecorrigeerd voor " + (oldAmount - correction) + " ontbrekende blikken " + brand.getBrandName() + ".";
                    }
                } catch (NumberFormatException e) {
                    return "De hoeveelheid moet een getal zijn.";
                }
            }
        }
        return "Gebruik: /correctie [biermerk] [ontbrekende turfjes]";
    }

    private DrinkUser getUser(String userNameOrAlias){
        DrinkUser user = userDao.getDrinkUsers().stream()
                .filter(dbUser -> dbUser.getFirstName().equalsIgnoreCase(userNameOrAlias.trim()))
                .findFirst().orElse(null);
        if(user != null) {
            return user;
        }
        for(DrinkUser someUser : userDao.getDrinkUsers()){
            if(someUser.getAliases() != null && !someUser.getAliases().isEmpty()){
                for(String alias : someUser.getAliases()){
                    if(userNameOrAlias.equalsIgnoreCase(alias)){
                        return someUser;
                    }
                }
            }
        }
        return null;
    }
}
