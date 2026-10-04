package dev.claywars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.claywars.entity.MiniCreeperEntity;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class MiniCreeperRenderer extends MobRenderer<MiniCreeperEntity, CreeperModel<MiniCreeperEntity>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/creeper/creeper.png");

    public MiniCreeperRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new CreeperModel<>(ctx.bakeLayer(ModelLayers.CREEPER)), 0.1f);
    }

    @Override
    protected void scale(MiniCreeperEntity e, PoseStack pose, float partialTick) {
        pose.scale(0.2f, 0.2f, 0.2f);
    }

    @Override
    public ResourceLocation getTextureLocation(MiniCreeperEntity e) { return TEXTURE; }
}
