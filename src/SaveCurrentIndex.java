import java.io.*;

/**
 * Saves/Loads the last index
 */
public class SaveCurrentIndex implements Serializable {

    private static final long serialVersionUID = 1L;
    private int index;

    private LoggerCls logger;


    public SaveCurrentIndex(){

    }

    public SaveCurrentIndex(LoggerCls logger) {
        this.logger = logger;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }


    public void saveIndex(SaveCurrentIndex saveCurrentIndexObj){

        FileOutputStream fileOutputStream;
        ObjectOutputStream objectOutputStream = null;

        String localDir = System.getProperty("user.dir");
        String fileName = "save.dat";
        String saveLocation = localDir + "/" + fileName;

        logger.logThis().info("SaveCurrentIndex | saveIndex() | saveLocation: " + saveLocation);


       try{
           fileOutputStream = new FileOutputStream(saveLocation);
           objectOutputStream = new ObjectOutputStream(fileOutputStream);
           objectOutputStream.writeObject(saveCurrentIndexObj);
            objectOutputStream.flush();
       }catch (IOException e){
           e.printStackTrace();
           logger.logThis().info("SaveCurrentIndex | saveIndex() | IOException: " + e);

       }finally {
           try {
            objectOutputStream.close();
           }catch (IOException e){
               e.printStackTrace();
               logger.logThis().info("SaveCurrentIndex | saveIndex() | IOException: Error closing ObjectOutputStream: " + e);
           }
       }

    }

    public static SaveCurrentIndex loadIndex(){

        String localDir = System.getProperty("user.dir");
        String fileName = "save.dat";
        String loadLocation = localDir + "/" + fileName;

        FileInputStream fileInputStream;
        ObjectInputStream objectInputStream = null;
        SaveCurrentIndex retrieved = null;
        try{
            fileInputStream = new FileInputStream(loadLocation);
            objectInputStream = new ObjectInputStream(fileInputStream);
            retrieved = (SaveCurrentIndex) objectInputStream.readObject();
        }catch (IOException e){
            e.printStackTrace();
        }catch (ClassNotFoundException e){
            e.printStackTrace();
        }
        finally {
            try{
                objectInputStream.close();
            }catch (IOException e){

            }
        }
        return  retrieved;
    }


    @Override
    public String toString() {
        return "SaveCurrentIndex{" +
                "index=" + index +
                '}';
    }
}
