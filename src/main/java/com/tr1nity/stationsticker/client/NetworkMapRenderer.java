package com.tr1nity.stationsticker.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tr1nity.stationsticker.block.NetworkMapBlock;
import com.tr1nity.stationsticker.block.entity.NetworkMapBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class NetworkMapRenderer implements BlockEntityRenderer<NetworkMapBlockEntity> {
    private static final ResourceLocation WHITE = new ResourceLocation("minecraft", "textures/misc/white.png");
    private static final int SEGMENTS = 96;
    public NetworkMapRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(NetworkMapBlockEntity entity, float tick, PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
        poses.pushPose();
        poses.translate(0.5, 0, 0.5);
        poses.mulPose(Axis.YP.rotationDegrees(-entity.getBlockState().getValue(NetworkMapBlock.FACING).toYRot()));
        VertexConsumer body = buffers.getBuffer(RenderType.text(WHITE));
        box(body, poses.last().pose(), -1.15f, 0, -0.25f, 1.15f, 0.4f, 0.25f, light);
        box(body, poses.last().pose(), -0.15f, 0.4f, -0.1f, 0.15f, 0.8f, 0.1f, light);
        disk(body, poses.last().pose(), NetworkMapBlock.RADIUS, 0.08f, 0xFF404247, light);
        disk(body, poses.last().pose(), NetworkMapBlock.RADIUS, -0.08f, 0xFF404247, light);
        for (int i = 0; i < SEGMENTS; i++) {
            double a = i * Math.PI * 2 / SEGMENTS, b = (i + 1) * Math.PI * 2 / SEGMENTS;
            float ax = (float) Math.cos(a) * NetworkMapBlock.RADIUS, ay = NetworkMapBlock.CENTER_Y + (float) Math.sin(a) * NetworkMapBlock.RADIUS;
            float bx = (float) Math.cos(b) * NetworkMapBlock.RADIUS, by = NetworkMapBlock.CENTER_Y + (float) Math.sin(b) * NetworkMapBlock.RADIUS;
            quad(body, poses.last().pose(), ax, ay, -0.08f, bx, by, -0.08f, bx, by, 0.08f, ax, ay, 0.08f, 0xFF292B30, light);
        }
        ResourceLocation texture = NetworkMapCache.texture(entity.getBlockPos());
        for (int side = 0; side < 2; side++) {
            poses.pushPose();
            if (side == 1) poses.mulPose(Axis.YP.rotationDegrees(180));
            VertexConsumer face = buffers.getBuffer(RenderType.text(texture == null ? WHITE : texture));
            disk(face, poses.last().pose(), NetworkMapBlock.RADIUS - 0.06f, 0.083f,
                    texture == null ? 0xFFFAF8F1 : 0xFFFFFFFF, LightTexture.FULL_BRIGHT);
            if (texture == null) {
                String loading = I18n.get("display.stationsticker.network_map.loading");
                poses.translate(-Minecraft.getInstance().font.width(loading) * 0.006 / 2, NetworkMapBlock.CENTER_Y, 0.086);
                poses.scale(0.006f, -0.006f, 0.006f);
                Minecraft.getInstance().font.drawInBatch(loading, 0, 0, 0xFF333333, false, poses.last().pose(), buffers,
                        net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            }
            poses.popPose();
        }
        poses.popPose();
    }
    private static void disk(VertexConsumer out, Matrix4f pose, float radius, float z, int color, int light) {
        for (int i = 0; i < SEGMENTS; i++) {
            double a = i * Math.PI * 2 / SEGMENTS, b = (i + 1) * Math.PI * 2 / SEGMENTS;
            float ax = (float) Math.cos(a), ay = (float) Math.sin(a), bx = (float) Math.cos(b), by = (float) Math.sin(b);
            for (int reverse = 0; reverse < 2; reverse++) {
                vertex(out, pose, 0, NetworkMapBlock.CENTER_Y, z, 0.5f, 0.5f, color, light);
                float x = reverse == 0 ? ax : bx, y = reverse == 0 ? ay : by;
                vertex(out, pose, x * radius, NetworkMapBlock.CENTER_Y + y * radius, z, (x + 1) / 2, (1 - y) / 2, color, light);
                x = reverse == 0 ? bx : ax; y = reverse == 0 ? by : ay;
                vertex(out, pose, x * radius, NetworkMapBlock.CENTER_Y + y * radius, z, (x + 1) / 2, (1 - y) / 2, color, light);
                vertex(out, pose, x * radius, NetworkMapBlock.CENTER_Y + y * radius, z, (x + 1) / 2, (1 - y) / 2, color, light);
            }
        }
    }
    private static void box(VertexConsumer out, Matrix4f p, float x0, float y0, float z0, float x1, float y1, float z1, int light) {
        int c = 0xFF292B30;
        quad(out,p,x0,y0,z0,x1,y0,z0,x1,y1,z0,x0,y1,z0,c,light);
        quad(out,p,x0,y0,z1,x1,y0,z1,x1,y1,z1,x0,y1,z1,c,light);
        quad(out,p,x0,y0,z0,x0,y0,z1,x0,y1,z1,x0,y1,z0,c,light);
        quad(out,p,x1,y0,z0,x1,y0,z1,x1,y1,z1,x1,y1,z0,c,light);
        quad(out,p,x0,y0,z0,x1,y0,z0,x1,y0,z1,x0,y0,z1,c,light);
        quad(out,p,x0,y1,z0,x1,y1,z0,x1,y1,z1,x0,y1,z1,c,light);
    }
    private static void quad(VertexConsumer out, Matrix4f p, float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz,float dx,float dy,float dz,int color,int light) {
        vertex(out,p,ax,ay,az,0,0,color,light); vertex(out,p,bx,by,bz,1,0,color,light);
        vertex(out,p,cx,cy,cz,1,1,color,light); vertex(out,p,dx,dy,dz,0,1,color,light);
        vertex(out,p,dx,dy,dz,0,1,color,light); vertex(out,p,cx,cy,cz,1,1,color,light);
        vertex(out,p,bx,by,bz,1,0,color,light); vertex(out,p,ax,ay,az,0,0,color,light);
    }
    private static void vertex(VertexConsumer out, Matrix4f p, float x,float y,float z,float u,float v,int c,int light) {
        out.vertex(p,x,y,z).color((c>>>16)&255,(c>>>8)&255,c&255,(c>>>24)&255).uv(u,v).uv2(light).endVertex();
    }
    @Override public boolean shouldRenderOffScreen(NetworkMapBlockEntity entity) { return true; }
}
