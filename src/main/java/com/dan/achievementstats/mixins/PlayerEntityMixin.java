package com.dan.achievementstats.mixins;

import com.dan.achievementstats.AchievementStat;
import com.dan.achievementstats.server.Config;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tag.FluidTags;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Shadow public abstract Text getName();

    @Shadow @Final private PlayerInventory inventory;

    //Mining speed modifier
    //Original method, just modifying the return value
    @Inject(method = "getBlockBreakingSpeed", at = @At("HEAD"), cancellable = true)
    private void sex(BlockState block, CallbackInfoReturnable<Float> cir) {
        if(this.world.isClient) return;
        float totalBoost = 0.0f;
        for(AchievementStat stat : Config.getInstance().getAllStats(getName().getString())) {
            totalBoost += stat.getMiningSpeed().getValue();
        }
        if(totalBoost != 0.0f) {
            float f = this.inventory.getBlockBreakingSpeed(block);
            if (f > 1.0F) {
                int i = EnchantmentHelper.getEfficiency(this);
                ItemStack itemStack = this.getMainHandStack();
                if (i > 0 && !itemStack.isEmpty()) {
                    f += (float)(i * i + 1);
                }
            }

            if (StatusEffectUtil.hasHaste(this)) {
                f *= 1.0F + (float)(StatusEffectUtil.getHasteAmplifier(this) + 1) * 0.2F;
            }

            if (this.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
                float g = switch (this.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
                    case 0 -> 0.3F;
                    case 1 -> 0.09F;
                    case 2 -> 0.0027F;
                    default -> 8.1E-4F;
                };

                f *= g;
            }

            if (this.isSubmergedIn(FluidTags.WATER) && !EnchantmentHelper.hasAquaAffinity(this)) {
                f /= 5.0F;
            }

            if (!this.onGround) {
                f /= 5.0F;
            }

            cir.setReturnValue(f + totalBoost * 0.5f);
        }
    }
}
