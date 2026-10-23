package tt.acid.visuals;

/** Runtime settings for the first implemented combat module. */
public final class KillAuraSettings {
    private KillAuraSettings() {}
    public static volatile boolean enabled = false;
    public static volatile float range = 2.8f;
    public static volatile int attacksPerSecond = 2;
    /** Minimum vanilla attack cooldown charge, in percent, required before attacking. */
    public static volatile int damageCharge = 90;
    /** Camera turn speed setting from 1 (slow) to 10 (fast). */
    public static volatile int aimSpeed = 3;
}
