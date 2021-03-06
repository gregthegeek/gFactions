package en.gregthegeek.gfactions;

import java.util.ArrayList;

import net.canarymod.api.entity.living.humanoid.Player;
import net.canarymod.api.world.blocks.Block;
import net.canarymod.api.world.position.Location;
import net.canarymod.chat.TextFormat;
import net.canarymod.hook.HookHandler;
import net.canarymod.hook.entity.DamageHook;
import net.canarymod.hook.player.BlockDestroyHook;
import net.canarymod.hook.player.BlockPlaceHook;
import net.canarymod.hook.player.BlockRightClickHook;
import net.canarymod.hook.player.ChatHook;
import net.canarymod.hook.player.ConnectionHook;
import net.canarymod.hook.player.PlayerDeathHook;
import net.canarymod.hook.player.PlayerMoveHook;
import net.canarymod.hook.player.PlayerRespawnHook;
import net.canarymod.plugin.PluginListener;
import en.gregthegeek.gfactions.faction.Faction;
import en.gregthegeek.gfactions.faction.FactionManager;
import en.gregthegeek.gfactions.faction.SafeZone;
import en.gregthegeek.gfactions.faction.SpecialFaction;
import en.gregthegeek.gfactions.faction.WarZone;
import en.gregthegeek.gfactions.faction.Wilderness;
import en.gregthegeek.gfactions.faction.ZoneFaction;
import en.gregthegeek.gfactions.land.LandManager;
import en.gregthegeek.gfactions.player.gPlayer;
import en.gregthegeek.gfactions.relation.Relation;
import en.gregthegeek.util.Utils;

public class gFactionsListener implements PluginListener {
    @HookHandler
    public void onLogin(ConnectionHook hook) {
        onLogin(hook.getPlayer());
    }
    
    protected void onLogin(Player player) {
        String name = player.getName();
        Utils.plugin.getPlayerManager().initPlayer(name);
        Utils.plugin.getEconomy().initPlayer(name);
    }
    
    @HookHandler
    public void onChat(ChatHook hook) { // manages ally, faction, and public chat
        Player player = hook.getPlayer();
        String pName = player.getName();
        ArrayList<Player> receivers = new ArrayList<Player>();
        Faction f = Utils.plugin.getFactionManager().getFaction(pName);
        if(f == null || f instanceof SpecialFaction) {
            return;
        }
        gPlayer gp = Utils.plugin.getPlayerManager().getPlayer(pName);
        gPlayer.ChatChannel cc = gp.getChatChannel();
        switch(cc) {
        case ALLY:
            assert f != null && !(f instanceof SpecialFaction);
            Faction[] allies = Utils.plugin.getRelationManager().getRelations(f, Relation.Type.ALLY);
            for(Faction ally : allies) {
                Utils.addItems(ally.getOnlineMembers(), receivers);
            }
        case FACTION:
            assert f != null && !(f instanceof SpecialFaction);
            Utils.addItems(f.getOnlineMembers(), receivers);
            
            gPlayer[] spies = Utils.plugin.getPlayerManager().spying();
            for(gPlayer gP : spies) {
                Player p = gP.toPlayer();
                assert p != null;
                if(!receivers.contains(p)) {
                    receivers.add(p);
                }
            }
            
            hook.setPrefix(cc.getColor() + gp.getFormattedName() + TextFormat.WHITE); // title name
            hook.setReceiverList(receivers);
            return;
        default:
            // public chat
            if(f != null && !(f instanceof SpecialFaction)) {
                hook.setCanceled();
                Player[][] sorted = Utils.getOnlinePlayersSorted(f);
                
                String fName = f.getName();
                String pNamef = player.getDisplayName();
                String msg = hook.getMessage();
                String format = "[%s%s] <%s%s> %s";
                
                String neutral = String.format(format, Relation.Type.NEUTRAL.getColor(), fName, pNamef, TextFormat.WHITE, msg);
                for(Player p : sorted[0]) {
                    p.message(neutral);
                }
                
                String ally = String.format(format, Relation.Type.ALLY.getColor(), fName, pNamef, TextFormat.WHITE, msg);
                for(Player p : sorted[1]) {
                    p.message(ally);
                }
                
                String enemy = String.format(format, Relation.Type.ENEMY.getColor(), fName, pNamef, TextFormat.WHITE, msg);
                for(Player p : sorted[2]) {
                    p.message(enemy);
                }
            }
            return;
        }
    }
    
    @HookHandler
    public void onDeath(PlayerDeathHook hook) { // decreases players' power on death
        Player p = hook.getPlayer();
        gPlayer gp = Utils.plugin.getPlayerManager().getPlayer(p.getName());
        gp.decreasePower(Utils.plugin.getLandManager().getLandAt(p.getLocation()).claimedBy() instanceof WarZone);
        p.message(String.format("%sYour power is now %s%s", TextFormat.YELLOW, TextFormat.WHITE, gp.getPower()));
    }
    
    @HookHandler
    public void onPlayerMove(PlayerMoveHook hook) { // will alert players when they move from one territory to another
        LandManager lm = Utils.plugin.getLandManager();
        int start = lm.getLandAt(hook.getFrom()).getClaimerId();
        int finish = lm.getLandAt(hook.getTo()).getClaimerId();
        if(start != finish) {
            Player player = hook.getPlayer();
            String pName = player.getName();
            if(Utils.plugin.getPlayerManager().getPlayer(pName).autoClaim) {
                String msg = FactionCommand.claimHelper(player);
                if(msg != null) {
                    player.message(msg);
                }
            } else {
                FactionManager fm = Utils.plugin.getFactionManager();
                Faction landFac = fm.getFaction(finish);
                player.message(String.format("%s~ %s - %s", TextFormat.YELLOW, landFac.getNameRelative(fm.getFaction(pName)), landFac.getDescription()));
            }
        }
    }
    
    @HookHandler
    public boolean onBlockPlace(BlockPlaceHook hook) { // will restrict building to wilderness and owned land
        return landEditHelper(hook.getPlayer(), hook.getBlockPlaced());
    }
    
    @HookHandler
    public boolean onDamage(DamageHook hook) { // prevents damage in spawn and among faction, reduces damage in owned territory
        if(!(hook.getDefender() instanceof Player)) { // defender is not player, we don't care about them
            return false;
        }
        LandManager lm = Utils.plugin.getLandManager();
        Faction owner = lm.getLandAt(hook.getDefender().getLocation()).claimedBy();
        if(owner instanceof SafeZone) { // defender is in safe zone
            return true;
        }
        if(hook.getAttacker() == null || !(hook.getAttacker() instanceof Player)) { // attacker is non-player
            return false;
        }
        if(lm.getLandAt(hook.getAttacker().getLocation()).claimedBy() instanceof SafeZone) { // attacker is in safe zone
            ((Player) hook.getAttacker()).message(String.format("%sYou cannot hurt someone while you are in a safe zone.", TextFormat.YELLOW));
            return true;
        }
        // we now know: attacker nor defender is not in safe zone, attacker and defender are both players
        Player pDefend = (Player) hook.getDefender();
        Faction defense = Utils.plugin.getFactionManager().getFaction(pDefend.getName());
        Player pAttack = (Player) hook.getAttacker();
        if(defense.has(pAttack.getName())) { // attacker and defender are in the same faction
            pAttack.message(String.format("%sYou cannot hurt members of your own faction.", TextFormat.YELLOW));
            return true;
        }
        if(defense != null && !(defense instanceof SpecialFaction) && defense.equals(owner)) { // defender belongs to a faction and is in his own faction territory
            double reduction = Utils.plugin.getConfig().getHomeLandDamageReduction();
            if(reduction <= 0) {
                return false;
            }
            pDefend.dealDamage(hook.getDamageSource().getDamagetype(), (int) (hook.getDamageDealt() * reduction));
            // TODO replace for recode
            /*OPacket38EntityStatus pkt = new OPacket38EntityStatus(pDefend.getId(), (byte) 0x02);
            for(Player p : etc.getServer().getPlayerList()) {
                p.getUser().a.b(pkt);
            }*/
            pDefend.message(String.format("%sDamage reduced by %d%%", TextFormat.YELLOW, (int) (reduction * 100)));
            return true;
        }
        return false;
    }
    
    @HookHandler
    public boolean onBlockBreak(BlockDestroyHook hook) { // will restrict the breaking of blocks to wilderness and owned land
        return landEditHelper(hook.getPlayer(), hook.getBlock());
    }
    
    private boolean landEditHelper(Player player, Block block) { // used by onBlockBreak, onBlockPlace, and onBlockRightClick
        if(Utils.isBypass(player)) {
            return false;
        }
        Faction f = Utils.plugin.getLandManager().getLandAt(block.getLocation()).claimedBy();
        if(f instanceof ZoneFaction) {
            player.message(String.format("%sYou cannot build in %s.", f.getColorRelative(null), f.getName()));
            return true;
        }
        if(f == null || f instanceof Wilderness) {
            return false;
        }
        Faction me = Utils.plugin.getFactionManager().getFaction(player.getName());
        if(me == null || me instanceof Wilderness || !me.equals(f)) {
            player.message(Utils.rose("You cannot build in the territory of %s.", f.getName()));
            return true;
        }
        return false;
    }
    
    @HookHandler
    public void onPlayerRespawn(PlayerRespawnHook hook) { // players respawn at their faction's homes
        Faction f = Utils.plugin.getFactionManager().getFaction(hook.getPlayer().getName());
        if(f != null && !(f instanceof SpecialFaction) && Utils.plugin.getConfig().factionHomeOnDeath()) {
            Location home = f.getHome();
            if(home != null) {
                hook.setRespawnLocation(home);
            }
        }
    }
    
    @HookHandler
    public boolean onBlockRightClick(BlockRightClickHook hook) { // restricts the interaction with blocks to wilderness and owned land
        return landEditHelper(hook.getPlayer(), hook.getBlockClicked());
    }
}
