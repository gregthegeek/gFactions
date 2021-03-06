package en.gregthegeek.gfactions.land;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import en.gregthegeek.gfactions.faction.Faction;
import en.gregthegeek.util.Utils;

/**
 * Represents a chunk of land and stores ownership data for it.
 * 
 * @author gregthegeek
 *
 */
public class Land {
	private final int x;
	private final int z;
	private final String world;
	private final int dim;
	private int faction = -1;
	private final List<String> owners;
	
	public Land(int x, int z, String world, int dim) {
	    this(x, z, world, dim, new ArrayList<String>());
	}
	
	public Land(int x, int z, String world, int dim, String[] owners) {
	    this(x, z, world, dim, Arrays.asList(owners));
	}
	
	public Land(int x, int z, String world, int dim, List<String> owners) {
		this.x = x;
		this.z = z;
		this.world = world;
		this.dim = dim;
		this.owners = owners;
	}
	
	/**
	 * Returns this land's x chunk coordinate.
	 * 
	 * @return int
	 */
	public int getX() {
		return x;
	}
	
	/**
	 * Returns this land's z chunk coordinate.
	 * 
	 * @return int
	 */
	public int getZ() {
		return z;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Land) {
			Land other = (Land) obj;
			return getX() == other.getX() && getZ() == other.getZ() && getWorld().equals(other.getWorld()) && getDimension() == other.getDimension();
		}
		return false;
	}
	
	/**
	 * Returns the faction that owns this land.
	 * 
	 * @return Faction
	 */
	public Faction claimedBy() {
		return Utils.plugin.getFactionManager().getFaction(faction);
	}
	
	/**
	 * Set this land to be claimed by the given faction.
	 * 
	 * @param faction The faction to claim the land.
	 */
	public void claim(Faction faction) {
		claim(faction == null ? -1 : faction.getId());
	}
	
	/**
	 * Set this land to be claimed by the given faction.
	 * 
	 * @param fID The id of the faction to claim the land.
	 */
	public void claim(int fID) {
		faction = fID;
		if(faction < 0) {
			owners.clear();
			Utils.plugin.getDataSource().delete(this);
		} else {
			save();
		}
	}
	
	/**
	 * Returns the id of the faction that owns this land (faster than claimedBy()).
	 * 
	 * @return Faction
	 */
	public int getClaimerId() {
		return faction;
	}
	
	/**
	 * Returns true if this land is owned by something other than the wilderness.
	 * 
	 * @return boolean
	 */
	public boolean isClaimed() {
		return faction != -1;
	}
	
	/**
	 * Returns those who have build rights here.
	 * 
	 * @return String[]
	 */
	public String[] getOwners() {
		return owners.toArray(new String[0]);
	}
	
	/**
	 * Toggle whether or not a player can build here.
	 * 
	 * @param player The name of the player to toggle.
	 * @return boolean True if they can now build, false if they can't.
	 */
	public boolean toggleOwner(String player) {
		player = player.toLowerCase();
		if(owners.contains(player)) {
			owners.remove(player);
			save();
			return false;
		} else {
			owners.add(player);
			save();
			return true;
		}
	}
	
	private void save() {
		if(Utils.plugin.getConfig().getSaveInterval() < 0) {
			Utils.plugin.getDataSource().save(this);
		}
	}
	
	/**
	 * Gets the name of the world this land is in.
	 * 
	 * @return String
	 */
	public String getWorld() {
		return world;
	}
	
	/**
	 * Gets the dimension this land is in.
	 * 
	 * @return int
	 */
	public int getDimension() {
		return dim;
	}
}
