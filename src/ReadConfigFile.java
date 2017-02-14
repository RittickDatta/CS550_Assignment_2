/**
 * Created by rittick on 2/14/17.
 */

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/***
 * This class will read the appropriate config file(linear network or star network) and return a ConcurrentHashMap
 * containing network information for communication.
 */
public class ReadConfigFile {

    public static ConcurrentHashMap<Integer, String> readFile(String configFile){

        ConcurrentHashMap<Integer, String> configFileData = new ConcurrentHashMap<>();

        //---------------------------------------------------

        FileInputStream fileInputStream = null;

        try {
            fileInputStream = new FileInputStream("src/"+configFile);
        }catch (FileNotFoundException e){
            System.out.println("File Not Found.");
        }

        Properties properties = new Properties();
        try {
            properties.load(fileInputStream);
            Set<String> keys = properties.stringPropertyNames();

            for(String key: keys){
                configFileData.put(Integer.parseInt(key), properties.getProperty(key));
            }

        }catch (IOException e){
            System.out.println("IOException");
        }


        //---------------------------------------------------

        return configFileData;
    }

    /*public static void main(String[] args) {
        ConcurrentHashMap<Integer, String> integerStringConcurrentHashMap = readFile("network_star.config");
        for(Integer key: integerStringConcurrentHashMap.keySet()){
            System.out.println("Key: "+key+" Value:"+integerStringConcurrentHashMap.get(key));
        }
    }*/

}
