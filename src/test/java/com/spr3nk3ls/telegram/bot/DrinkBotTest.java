package com.spr3nk3ls.telegram.bot;

import com.spr3nk3ls.telegram.domain.Group;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DrinkBotTest extends TestCase {

    @Test
    public void test(){
        List<Group> groups = new ArrayList<Group>();
        Group group = new Group();
    }

    @Test
    public void test2(){
        Arrays.asList("/turf bla    voor     blaat".split("\\s+voor\\s+")).stream().forEach(System.out::println);
    }

}
