package com.dan.achievementstats.mixins;

import com.dan.achievementstats.AchievementStat;
import com.dan.achievementstats.server.AchievementStatsServer;
import com.dan.achievementstats.server.Config;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin extends PlayerEntity {

    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile profile) {
        super(world, pos, yaw, profile);
    }

    //applies stats on player spawn
    //Used when player dies
    @Inject(method = "onSpawn", at = @At("TAIL"))
    private void setAtt(CallbackInfo ci) {
        List<AchievementStat> list = Config.getInstance().getAllStats(this.getName().getString());
        AchievementStatsServer.executeChange((ServerPlayerEntity) ((Object) this), list, list);
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean isCreative() {
        return false;
    }
}
