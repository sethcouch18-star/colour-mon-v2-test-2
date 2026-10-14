package com.example.colormons.mixin;

import com.cobblemon.mod.common.client.render.pokemon.PokemonRenderer;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.example.colormons.ColorStore;
import com.example.colormons.TintCache;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PokemonRenderer.class, remap = false)
public abstract class PokemonRendererMixin {
    @Inject(method = "getTextureLocation(Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("RETURN"), cancellable = true)
    private void colormons$tint(PokemonEntity entity, CallbackInfoReturnable<ResourceLocation> cir) {
        Pokemon mon = entity.getPokemon();
        int[] c = ColorStore.get(mon.getUuid());
        if (c == null) return;
        String species = mon.getSpecies().getResourceIdentifier().getPath();
        cir.setReturnValue(TintCache.getOrCreate(mon.getUuid(), species, cir.getReturnValue(), c));
    }
}
