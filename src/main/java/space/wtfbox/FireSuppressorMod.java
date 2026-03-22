package space.wtfbox;

import java.util.ArrayList;
import java.util.HashMap;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.gamerules.GameRules;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




public class FireSuppressorMod implements ModInitializer {
    public static final String MOD_ID = "firesuppressor";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);


    public static HashMap<PoiManager, ArrayList<ImmutablePair<BlockPos, Integer>>> cache_by_poimanager =
        new HashMap<>();
    public static MinecraftServer server = null;


    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(FireSuppressorMod::on_tick);
    }


    public static boolean any_cauldron_nearby(
        net.minecraft.server.level.ServerLevel self,
        BlockPos at
    ) {
        // the server is determined to ignite the block at `at`. check if this block is covered by a cauldron.
        int fire_spread_radius = self.getGameRules().get(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER);

        // derive the protection radius from the `fire_spread_radius` gamerule.
        // from testing, it seems fire spread is effectively disabled for values 0, 1, and 2.
        //
        // R > 2          gamerule range
        // -1 <= R <= 2   default range
        // -1 <  R        not possible
        //
        int protect_radius = fire_spread_radius;
        if ( fire_spread_radius <= 2 ) {
            // when radius is 0, fire/lava spreads, but only to the block a player occupies (in theory)
            // for other values up to 2, such a small protection range is effectively useless
            //
            // in either case, use the default protection range
            protect_radius = 128;
        }

        // search nearby cauldrons
        final int PROTECT_RADIUS_SQ = protect_radius * protect_radius;
        final PoiManager poi = self.getPoiManager();
        final var any_poi_cauldron = poi.find(
            holder -> holder.is(PoiTypes.LEATHERWORKER),
            pos -> at.distSqr(pos) <= PROTECT_RADIUS_SQ,
            at, protect_radius,
            PoiManager.Occupancy.ANY
        );

        // check any moving cauldrons
        boolean any_cached_cauldron = true;
        if ( any_poi_cauldron.isEmpty() ) {
            var cache = cache_by_poimanager.get(poi);
            any_cached_cauldron = (
                cache != null
                && cache.stream().anyMatch(entry -> entry.getLeft().distSqr(at) <= PROTECT_RADIUS_SQ)
            );
        }

        return any_poi_cauldron.isPresent() || any_cached_cauldron;
    }


    public static void on_poi_removed(PoiManager self, BlockPos pos) {
        if ( FireSuppressorMod.server == null || !self.existsAtPosition(PoiTypes.LEATHERWORKER, pos) ) {
            // not initialized or not a cauldron
            return;
        }

        // insert entry
        var cache = cache_by_poimanager.computeIfAbsent(self, k -> new ArrayList<>());
        int expires = FireSuppressorMod.server.getTickCount() + 8;
        cache.add(new ImmutablePair<>(pos, expires));
    }


    public static void on_tick(MinecraftServer server) {
        if ( FireSuppressorMod.server != server ) {
            // invalidate cache if (for whatever reason) the server changes
            FireSuppressorMod.server = server;
            FireSuppressorMod.cache_by_poimanager = new HashMap<>();
        }


        // remove expired entries in each dimension
        int tick_at = server.getTickCount();
        for ( var entries : cache_by_poimanager.values() ) {
            entries.removeIf(pos_expires_pair -> tick_at >= pos_expires_pair.getRight());
        }
    }
}
