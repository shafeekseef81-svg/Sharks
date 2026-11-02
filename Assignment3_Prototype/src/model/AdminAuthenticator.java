package model;

import java.util.Arrays;
import java.util.function.LongSupplier;

/**
 * Simulated administrator login for the prototype (FR-51..FR-54, NFR-17).
 * Credentials are compared in memory: no PBKDF2 hashing or encrypted storage yet.
 */
public final class AdminAuthenticator {

    /** Login outcome. */
    public enum Result { SUCCESS, FAILURE, LOCKED }

    public static final int MAX_ATTEMPTS = 3;
    public static final long LOCK_MILLIS = 30_000;

    private final String username;
    private final char[] password;
    private final LongSupplier clock;
    private int failures;
    private long lockedUntil;
    private boolean signedIn;

    public AdminAuthenticator(String username, String password) {
        this(username, password, System::currentTimeMillis);
    }

    public AdminAuthenticator(String username, String password, LongSupplier clock) {
        this.username = username;
        this.password = password.toCharArray();
        this.clock = clock;
    }

    /** Checks the credentials; the third failure in a row locks the login for 30 seconds. */
    public Result login(String user, char[] pass) {
        if (lockRemainingMillis() > 0) {
            return Result.LOCKED;
        }
        boolean ok = username.equals(user == null ? "" : user.trim()) && Arrays.equals(password, pass);
        if (ok) {
            failures = 0;
            signedIn = true;
            return Result.SUCCESS;
        }
        failures++;
        if (failures >= MAX_ATTEMPTS) {
            failures = 0;
            lockedUntil = clock.getAsLong() + LOCK_MILLIS;
            return Result.LOCKED;
        }
        return Result.FAILURE;
    }

    public long lockRemainingMillis() {
        return Math.max(0, lockedUntil - clock.getAsLong());
    }

    public int remainingAttempts() {
        return MAX_ATTEMPTS - failures;
    }

    public boolean isSignedIn() {
        return signedIn;
    }

    /** Ends the administrator session (FR-54). */
    public void logout() {
        signedIn = false;
    }
}
