import java.io.IOException;
import java.io.Serializable;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


/**
Logger class
 */
public class LoggerCls implements Serializable {
    private static LoggerCls instance;
    private static Logger logger;
    private static String currentDir = System.getProperty("user.dir");

   private LoggerCls(){

       try{

           logger =  java.util.logging.Logger.getLogger("log");
           FileHandler fh = new FileHandler(currentDir + "/logFile.log");
           logger.addHandler(fh);
           SimpleFormatter formatter = new SimpleFormatter();
           fh.setFormatter(formatter);
           logger.setUseParentHandlers(false);//DO NOT WRITE TO OUTPUT/FILE ONLY

           logger.info("=========================================");
           logger.info("Logger constructor called | INIT completed");
           logger.info("=========================================");


       }catch (SecurityException e) {
           e.printStackTrace();
       } catch (IOException e) {
           e.printStackTrace();
       }
   }

    public static LoggerCls getLoggerInstance() {
        if (instance == null) {
            instance = new LoggerCls();
        }
        return instance;
    }


   public static Logger logThis(){
       return logger;
   }

}
