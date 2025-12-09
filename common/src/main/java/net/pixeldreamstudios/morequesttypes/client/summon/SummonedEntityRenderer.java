package net.pixeldreamstudios.morequesttypes.client.summon;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.pixeldreamstudios.morequesttypes.api.IQuestSummonedEntity;
import org.joml.Matrix4f;

import java.util.List;

public final class SummonedEntityRenderer {
    private static final float BASE_SIZE = 1.0f;
    private static final float Y_OFFSET = 0.5f;
    private static final double RENDER_DISTANCE = 128.0;

    private SummonedEntityRenderer() {}

    public static void render(PoseStack poseStack, Camera camera, Level level, float partialTick) {
        if (!(level instanceof ClientLevel clientLevel)) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }

        Vec3 camPos = camera.getPosition();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515); 
        RenderSystem.depthMask(true); 
        AABB renderBox = new AABB(camPos, camPos).inflate(RENDER_DISTANCE);
        List<Entity> nearbyEntities = clientLevel.getEntities((Entity) null, renderBox, entity -> true);

        for (Entity entity : nearbyEntities) {
            if (!(entity instanceof IQuestSummonedEntity questEntity)) {
                continue;
            }

            if (! questEntity.isQuestSummoned()) {
                continue;
            }

            String textureTag = questEntity.getQuestTexture();

            if (textureTag == null || textureTag.isEmpty()) {
                continue;
            }

            ResourceLocation textureId = ResourceLocation.tryParse(textureTag);

            if (textureId == null) {
                continue;
            }

            double ex = lerp(partialTick, entity.xOld, entity.getX());
            double ey = lerp(partialTick, entity.yOld, entity.getY());
            double ez = lerp(partialTick, entity.zOld, entity.getZ());

            float entityHeight = entity.getBbHeight();
            float entityWidth = entity.getBbWidth();

            double offsetX = questEntity.getQuestTextureOffsetX();
            double offsetY = questEntity.getQuestTextureOffsetY();
            double offsetZ = questEntity.getQuestTextureOffsetZ();

            double yPos = ey + entityHeight + Y_OFFSET + offsetY;

            poseStack.pushPose();
            poseStack.translate(
                    ex - camPos.x + offsetX,
                    yPos - camPos.y,
                    ez - camPos.z + offsetZ
            );
            faceCamera(poseStack, camera);

            TextureAtlasSprite sprite = tryGetSpriteFromAtlas(client, textureId);

            float scale = questEntity.getQuestTextureScale();

            if (sprite != null) {
                float worldW = BASE_SIZE * Math.max(entityWidth, 0.5f) * scale;
                float worldH = worldW * ((float) sprite.contents().height() / (float) sprite.contents().width());

                poseStack.scale(worldW, worldH, 1f);

                RenderSystem.setShader(GameRenderer::getPositionColorTexLightmapShader);
                RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);

                drawQuadLitUV(poseStack.last().pose(),
                        sprite.getU0(), sprite.getV0(),
                        sprite.getU1(), sprite.getV1());
            } else {
                ResourceLocation fullTexturePath = convertToTexturePath(textureId);

                float worldW = BASE_SIZE * Math.max(entityWidth, 0.5f) * scale;
                float worldH = worldW;

                poseStack.scale(worldW, worldH, 1f);

                RenderSystem.setShader(GameRenderer::getPositionColorTexLightmapShader);
                RenderSystem.setShaderTexture(0, fullTexturePath);

                drawQuadLitUV(poseStack.last().pose(), 0f, 0f, 1f, 1f);
            }

            poseStack.popPose();
        }

        RenderSystem.depthFunc(515);
        RenderSystem.depthMask(true);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static TextureAtlasSprite tryGetSpriteFromAtlas(Minecraft client, ResourceLocation textureId) {
        try {
            TextureAtlasSprite sprite = client.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(textureId);

            if (sprite == null) {
                return null;
            }

            ResourceLocation missingId = MissingTextureAtlasSprite.getLocation();
            ResourceLocation actualId = sprite.contents().name();

            if (actualId.equals(missingId)) {
                return null;
            }

            return sprite;
        } catch (Exception e) {
            return null;
        }
    }

    private static ResourceLocation convertToTexturePath(ResourceLocation textureId) {
        if (textureId.getPath().startsWith("textures/")) {
            return textureId;
        }

        return ResourceLocation.fromNamespaceAndPath(
                textureId.getNamespace(),
                "textures/" + textureId.getPath() + ".png"
        );
    }

    private static void drawQuadLitUV(Matrix4f mat, float u0, float v0, float u1, float v1) {
        int light = LightTexture.pack(15, 15);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);

        buf.addVertex(mat, -0.5f, 0.5f, 0f).setColor(255, 255, 255, 255).setUv(u0, v0).setLight(light);
        buf.addVertex(mat, 0.5f, 0.5f, 0f).setColor(255, 255, 255, 255).setUv(u1, v0).setLight(light);
        buf.addVertex(mat, 0.5f, -0.5f, 0f).setColor(255, 255, 255, 255).setUv(u1, v1).setLight(light);
        buf.addVertex(mat, -0.5f, -0.5f, 0f).setColor(255, 255, 255, 255).setUv(u0, v1).setLight(light);

        BufferUploader.drawWithShader(buf.buildOrThrow());
    }

    private static void faceCamera(PoseStack poseStack, Camera camera) {
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-camera.getYRot()));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(camera.getXRot()));
    }

    private static double lerp(float t, double a, double b) {
        return a + (b - a) * t;
    }
}