package en.gregthegeek.gfactions.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import en.gregthegeek.gfactions.land.Land;
import net.canarymod.database.Column;
import net.canarymod.database.DataAccess;

public class LandDataAccess extends DataAccess {
    private static final String NAME = "land";
    
    @Column(columnName = "x", dataType = Column.DataType.INTEGER)
    public int x;
    @Column(columnName = "z", dataType = Column.DataType.INTEGER)
    public int z;
    @Column(columnName = "world", dataType = Column.DataType.STRING)
    public String world;
    @Column(columnName = "dim", dataType = Column.DataType.INTEGER)
    public int dim;
    @Column(columnName = "faction", dataType = Column.DataType.INTEGER)
    public int faction;
    @Column(columnName = "owners", dataType = Column.DataType.STRING, isList = true)
    public List<String> owners;
    
    public LandDataAccess() {
        super(CanarySource.PREFIX, NAME);
        this.x = 0;
        this.z = 0;
        this.world = "";
        this.dim = -2;
        this.faction = -1;
        this.owners = new ArrayList<String>(0);
    }
    
    public LandDataAccess(Land land) {
        super(CanarySource.PREFIX, NAME);
        this.x = land.getX();
        this.z = land.getZ();
        this.world = land.getWorld();
        this.dim = land.getDimension();
        this.faction = land.getClaimerId();
        this.owners = Arrays.asList(land.getOwners());
    }
    
    public Land toLand() {
        return new Land(x, z, world, dim, owners);
    }

    @Override
    public DataAccess getInstance() {
        return new LandDataAccess();
    }
}
