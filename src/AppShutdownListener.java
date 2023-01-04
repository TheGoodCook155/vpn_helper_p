/**
 * AppShutdownListener instance
 */
public class AppShutdownListener {

    private boolean hookIsRunning = false;

    private static AppShutdownListener instance;

    public static AppShutdownListener getAppShutdownListenerInstance(){

        if (instance == null){
            instance = new AppShutdownListener();
        }
        return instance;
    }

    public void setHookIsRunning(boolean hookIsRunning) {
        this.hookIsRunning = hookIsRunning;
    }

    public boolean isHookIsRunning() {
        return hookIsRunning;
    }



}
