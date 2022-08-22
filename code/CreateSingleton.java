package code;

public class CreateSingleton {
    private static volatile CreateSingleton singleton;

    public CreateSingleton getSingleton() {
        if (singleton == null) {
            synchronized (CreateSingleton.class) {
                if (singleton == null)
                    singleton = new CreateSingleton();
            }
        }
        return singleton;
    }
}
