package tt.acid.visuals;

/** Settings for the single-player God of the World abilities. */
public final class GodSettings {
    private GodSettings() {}
    public static volatile boolean flight = true;
    public static volatile boolean invulnerability = true;
    public static volatile boolean killAura = false;
    public static volatile boolean retaliation = true;
    public static volatile boolean homeLantern = false;
    public static volatile boolean eyeLaser = false;
    public static volatile boolean timeStop = false;
    public static volatile boolean telekinesis = false;
    public static volatile float killRange = 32.0f;
}
