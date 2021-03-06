package en.gregthegeek.gfactions.relation;
import net.canarymod.chat.TextFormat;
import en.gregthegeek.gfactions.faction.Faction;
import en.gregthegeek.util.Utils;

/**
 * Represents a relationship between two factions.
 * 
 * @author gregthegeek
 *
 */
public class Relation {
	public enum Type {
		NEUTRAL(TextFormat.WHITE, "neutral"),
		ALLY(TextFormat.PURPLE, "allies"),
		ENEMY(TextFormat.RED, "enemies"),
		SAME(TextFormat.LIGHT_GREEN, null); // readable isn't used
		
		private final String color;
		private final String readable;
		
		private Type(String color, String readable) {
			this.color = color;
			this.readable = readable;
		}
		
		public String getColor() {
			return color;
		}
		
		@Override
		public String toString() {
			return readable;
		}
	}
	
	public Type type;
	private final int one;
	private final int two;
	
	public Relation(Type type, Faction one, Faction two) {
		this(type, one.getId(), two.getId());
	}
	
	public Relation(Type type, int one, int two) {
	    this.type = type;
	    this.one = one;
	    this.two = two;
	}
	
	/**
	 * Gets the first faction described in the relation.
	 * 
	 * @return Faction
	 */
	public Faction getOne() {
		return Utils.plugin.getFactionManager().getFaction(one);
	}
	
	/**
	 * Gets the second faction described in the relation.
	 * 
	 * @return Faction
	 */
	public Faction getTwo() {
		return Utils.plugin.getFactionManager().getFaction(two);
	}
	
	/**
	 * Gets the id of the first faction described in the relation.
	 * 
	 * @return int
	 */
	public int getOneId() {
	    return one;
	}
	
	/**
	 * Gets the id of the second faction described in the relation
	 * 
	 * @return int 
	 */
	public int getTwoId() {
	    return two;
	}
	
	/**
	 * Returns whether or not the given faction is either one or two in this relation.
	 * 
	 * @param f The faction to check for.
	 * @return boolean
	 */
	public boolean isInvolved(Faction f) {
		int id = f.getId();
		return one == id || two == id;
	}
	
	/**
	 * Returns the faction in this relation that is not the one given.
	 * Returns null if the given faction is not in this relation.
	 * 
	 * @param f The faction not returned.
	 * @return Faction the other faction.
	 */
	public Faction getOther(Faction f) {
		int id = f.getId();
		if(one == id) {
			return getTwo();
		} else if(two == id) {
			return getOne();
		} else {
			return null;
		}
	}
	
	@Override
	public String toString() {
		return String.format("Relation[type=%s, one=%d, two=%d]", type, one, two);
	}
	
	@Override
	public boolean equals(Object obj) {
	    if(obj instanceof Relation) {
	        Relation other = (Relation) obj;
	        return (other.one == one && other.two == two) || (other.one == two && other.two == one);
	    }
	    return false;
	}
}
