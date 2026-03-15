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
        // get fire spread radius
        final PoiManager poi = self.getPoiManager();
        final int MAX_RANGE = self.getGameRules().get(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER);
        final double MAX_RANGE_SQ = MAX_RANGE * MAX_RANGE;

        if ( MAX_RANGE <= 0 ) {
            // should be impossible?
            FireSuppressorMod.LOGGER.warn("any_cauldron_nearby, but MAX_RANGE is {}", MAX_RANGE);
            return false;
        }

        // search nearby cauldrons
        final var closest_poi_cauldron = poi.findClosest(
            holder -> holder.is(PoiTypes.LEATHERWORKER),
            pos -> at.distSqr(pos) <= MAX_RANGE_SQ,
            at, MAX_RANGE,
            PoiManager.Occupancy.ANY
        );

        if ( closest_poi_cauldron.isPresent() ) {
            FireSuppressorMod.LOGGER.info(
                "found cauldron at {} for block {}",
                closest_poi_cauldron.get().toShortString(), at.toShortString()
            );
        }

        // check any moving cauldrons
        boolean any_cached_cauldron = true;
        if ( closest_poi_cauldron.isEmpty() ) {
            var cache = cache_by_poimanager.get(poi);
            any_cached_cauldron = (
                cache != null
                && cache.stream().anyMatch(entry -> entry.getLeft().distSqr(at) <= MAX_RANGE_SQ)
            );
        }

        return closest_poi_cauldron.isPresent() || any_cached_cauldron;
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

        LOGGER.debug("cauldron destroyed at: {}", pos.toShortString());
    }


    public static void on_tick(MinecraftServer server) {
        if ( FireSuppressorMod.server != server ) {
            // invalidate cache if (for whatever reason) the server changes
            FireSuppressorMod.server = server;
            FireSuppressorMod.cache_by_poimanager = new HashMap<>();
            LOGGER.info("initialized");
        }

        int tick_at = server.getTickCount();

        // remove expired entries in each dimension
        for ( var poi_entries_pair : cache_by_poimanager.entrySet() ) {
            var poi = poi_entries_pair.getKey();
            var entries = poi_entries_pair.getValue();

            final int before_ct = entries.size();
            entries.removeIf(pos_expires_pair -> tick_at >= pos_expires_pair.getRight());

            final int removed_ct = before_ct - entries.size();
            if ( removed_ct > 0 ) {
                LOGGER.debug("removed {} expired entries from {}", removed_ct, poi);
            }
        }
    }
}
