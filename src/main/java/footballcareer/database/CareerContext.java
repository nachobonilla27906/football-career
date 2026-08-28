package footballcareer.database;

/** Identifies the career whose mutable world state is currently being used. */
public final class CareerContext {
    private static volatile Long careerId;

    private CareerContext() {}

    public static Long getCareerId() {
        return careerId;
    }

    public static void activate(long id) {
        if (id <= 0) throw new IllegalArgumentException("Career id must be positive.");
        careerId = id;
    }

    public static void clear() {
        careerId = null;
    }
}
