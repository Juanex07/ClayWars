package dev.claywars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.claywars.ClayWars;
import dev.claywars.entity.SoldierEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class SoldierRenderer extends HumanoidMobRenderer<SoldierEntity, HumanoidModel<SoldierEntity>> {
    public SoldierRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.1f);
    }

    @Override
    protected void scale(SoldierEntity e, PoseStack pose, float partialTick) {
        float s = 0.22f * e.stage().renderScale();   // los bebés se ven más chicos
        pose.scale(s, s, s);
    }

    @Override
    public ResourceLocation getTextureLocation(SoldierEntity e) {
        return ResourceLocation.fromNamespaceAndPath(ClayWars.MODID, "textures/entity/" + e.team().id() + "_soldier" + (e.isFemale() ? "_f" : "") + ".png");
    }
}
