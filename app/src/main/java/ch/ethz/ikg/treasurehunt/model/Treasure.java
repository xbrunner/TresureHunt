package ch.ethz.ikg.treasurehunt.model;

import android.location.Location;

/**
 * A treasure consists of its name, the location where it can be retreived, and the maximal
 * number of coins a player can get when collecting the treasure.
 */
public class Treasure {
    private String name;
    private Location location;
    private int coins;

    /**
     * Creates a new treasure.
     *
     * @param name  The name of this treasure.
     * @param lon   The longitude where this treasure can be found.
     * @param lat   The latitude where this treasure can be found.
     * @param coins The maximal number of coins for this treasure.
     */
    public Treasure(String name, double lon, double lat, int coins) {
        this.name = name;
        this.location = new Location(name);
        this.location.setLongitude(lon);
        this.location.setLatitude(lat);
        this.coins = coins;
    }

    /**
     * Gets the name of this treasure.
     *
     * @return The name of this treasure.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this treasure. A name should be a descriptive and playful element of this
     * treasure.
     *
     * @param name The name of this treasure.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the location where this treasure can be found.
     *
     * @return The location of this treasure.
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Sets the location where this treasure can be found.
     *
     * @param location The new location of this treasure.
     */
    public void setLocation(Location location) {
        this.location = location;
    }

    /**
     * Gets the maximal number of coins a player can retrieve when collecting this treasure.
     *
     * @return The maximal number of coins.
     */
    public int getCoins() {
        return coins;
    }

    /**
     * Sets the maximal number of coins that a player can retrieve when collecting this treasure.
     *
     * @param coins The maximal number of coins.
     */
    public void setCoins(int coins) {
        this.coins = coins;
    }
}
