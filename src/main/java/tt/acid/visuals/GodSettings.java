package tt.acid.visuals;

/** Only the five requested modules are exposed in the client menu. */
public final class GodSettings {
    private GodSettings() {}
    public static volatile boolean flight = false;
    public static volatile boolean invulnerability = false;
    public static volatile boolean killAura = false;
    public static volatile boolean retaliation = false;
    public static volatile boolean esp = false;

    // Legacy fields retained temporarily so existing resource/API references remain source-compatible.
    public static volatile boolean homeLantern = false;
    public static volatile boolean eyeLaser = false;
    public static volatile boolean timeStop = false;
    public static volatile boolean telekinesis = false;
    public static volatile boolean jumpCircle = false;
    public static volatile boolean wetFloor = false;
    public static volatile int skyPreset = 0;
    public static volatile float killRange = 3.0f;
}
