package com.dc.peer;

import java.util.Arrays;

public class InitConfig {
    static String my_ip = "127.0.0.1";
    static int my_port = 4132;
    static String my_username = "node1";
    static String bootstrap_ip = "10.8.106.144";
    static int bootstrap_port = 55555;
    static enum MessageType { MSG_REG, MSG_REGOK, MSG_UNROK, MSG_UNREG, MSG_JOINOK ,MSG_JOIN, MSG_LEAVE,MSG_LEAVEOK ,MSG_SER, MSG_SEROK, MSG_ERROR};

    static String[] file_set = { "Adventures of Tintin", "Jack and Jill",
            "Glee", "The Vampire Diarie", "King Arthur", "Windows XP",
            "Harry Potter", "Kung Fu Panda", "Lady Gaga", "Twilight",
            "Windows 8", "Mission Impossible", "Turn Up The Music",
            "Super Mario", "American Pickers", "Microsoft Office 2010",
            "Happy Feet", "Modern Family", "American Idol",
            "Hacking for Dummies" };

    static String[] query_set = { "Twilight", "Jack", "American Idol",
            "Happy Feet", "Twilight saga", "Happy Feet", "Happy Feet", "Feet",
            "Happy Feet", "Twilight", "Windows", "Happy Feet",
            "Mission Impossible", "Twilight", "Windows 8", "The", "Happy",
            "Windows 8", "Happy Feet", "Super Mario", "Jack and Jill",
            "Happy Feet", "Impossible", "Happy Feet", "Turn Up The Music",
            "Adventures of Tintin", "Twilight saga", "Happy Feet",
            "Super Mario", "American Pickers", "Microsoft Office 2010",
            "Twilight", "Modern Family", "Jack and Jill", "Jill", "Glee",
            "The Vampire Diarie", "King Arthur", "Jack and Jill",
            "King Arthur", "Windows XP", "Harry Potter", "Feet",
            "Kung Fu Panda", "Lady Gaga", "Gaga", "Happy Feet", "Twilight",
            "Hacking", "King" };

    public static String[] getRandomFiles(){
        int numFiles=3+(int) (Math.random()*2);
        int count=0;
        String[] result=new String[numFiles];
        while(count<numFiles){
            int index=(int) (Math.random()*(file_set.length-1));
            String temp=file_set[index];
            if(!Arrays.asList(result).contains(temp)){
                result[count]=temp;
                count++;
            }
        }
        return result;
    }

    public static String getRandomQuery(){
        int index=(int) (Math.random()*(file_set.length-1));
        String result=query_set[index];
        return result;
    }
}
