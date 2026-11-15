package dev.ghost.pvp.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Static rendering helpers.
 * All 3D methods expect to be called inside a RenderWorldEvent callback.
 */
public final class RenderUtil {

    private RenderUtil() {}

    // ── 3D Box (ESP outline) ───────────────────────────────────────────────────

    public static void drawBox(PoseStack ps, AABB box, int r, int g, int b, int a, float lineWidth) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.lineWidth(lineWidth);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        var buf = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = ps.last().pose();

        float x0=(float)box.minX, x1=(float)box.maxX;
        float y0=(float)box.minY, y1=(float)box.maxY;
        float z0=(float)box.minZ, z1=(float)box.maxZ;
        float fr=r/255f, fg=g/255f, fb=b/255f, fa=a/255f;

        addLine(buf,mat,x0,y0,z0,x1,y0,z0,fr,fg,fb,fa); addLine(buf,mat,x1,y0,z0,x1,y0,z1,fr,fg,fb,fa);
        addLine(buf,mat,x1,y0,z1,x0,y0,z1,fr,fg,fb,fa); addLine(buf,mat,x0,y0,z1,x0,y0,z0,fr,fg,fb,fa);
        addLine(buf,mat,x0,y1,z0,x1,y1,z0,fr,fg,fb,fa); addLine(buf,mat,x1,y1,z0,x1,y1,z1,fr,fg,fb,fa);
        addLine(buf,mat,x1,y1,z1,x0,y1,z1,fr,fg,fb,fa); addLine(buf,mat,x0,y1,z1,x0,y1,z0,fr,fg,fb,fa);
        addLine(buf,mat,x0,y0,z0,x0,y1,z0,fr,fg,fb,fa); addLine(buf,mat,x1,y0,z0,x1,y1,z0,fr,fg,fb,fa);
        addLine(buf,mat,x1,y0,z1,x1,y1,z1,fr,fg,fb,fa); addLine(buf,mat,x0,y0,z1,x0,y1,z1,fr,fg,fb,fa);

        BufferUploader.drawWithShader(buf.buildOrThrow());
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static void drawFilledBox(PoseStack ps, AABB box, int r, int g, int b, int a) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        var buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = ps.last().pose();
        float x0=(float)box.minX,x1=(float)box.maxX,y0=(float)box.minY,y1=(float)box.maxY,z0=(float)box.minZ,z1=(float)box.maxZ;
        float fr=r/255f,fg=g/255f,fb=b/255f,fa=a/255f;

        // 6 faces
        buf.addVertex(mat,x0,y0,z0).setColor(fr,fg,fb,fa); buf.addVertex(mat,x1,y0,z0).setColor(fr,fg,fb,fa);
        buf.addVertex(mat,x1,y0,z1).setColor(fr,fg,fb,fa); buf.addVertex(mat,x0,y0,z1).setColor(fr,fg,fb,fa);
        buf.addVertex(mat,x0,y1,z0).setColor(fr,fg,fb,fa); buf.addVertex(mat,x0,y1,z1).setColor(fr,fg,fb,fa);
        buf.addVertex(mat,x1,y1,z1).setColor(fr,fg,fb,fa); buf.addVertex(mat,x1,y1,z0).setColor(fr,fg,fb,fa);
        buf.addVertex(mat,x0,y0,z0).setColor(fr,fg,fb,fa); buf.addVertex(mat,x0,y1,z0).setColor(fr,fg,fb,fa);
        buf.addVertex(mat,x1,y1,z0).setColor(fr,fg,fb,fa); buf.addVertex(mat,x1,y0,z0).setColor(fr,fg,fb,fa);
        buf.addVertex(mat,x1,y0,z1).setColor(fr,fg,fb,fa); buf.addVertex(mat,x1,y1,z1).setColor(fr,fg,fb,fa);
        buf.addVertex(mat,x0,y1,z1).setColor(fr,fg,fb,fa); buf.addVertex(mat,x0,y0,z1).setColor(fr,fg,fb,fa);
        buf.addVertex(mat,x0,y0,z1).setColor(fr,fg,fb,fa); buf.addVertex(mat,x0,y1,z1).setColor(fr,fg,fb,fa);
        buf.addVertex(mat,x0,y1,z0).setColor(fr,fg,fb,fa); buf.addVertex(mat,x0,y0,z0).setColor(fr,fg,fb,fa);
        buf.addVertex(mat,x1,y0,z0).setColor(fr,fg,fb,fa); buf.addVertex(mat,x1,y1,z0).setColor(fr,fg,fb,fa);
        buf.addVertex(mat,x1,y1,z1).setColor(fr,fg,fb,fa); buf.addVertex(mat,x1,y0,z1).setColor(fr,fg,fb,fa);

        BufferUploader.drawWithShader(buf.buildOrThrow());
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static void drawTracer(PoseStack ps, Vec3 from, Vec3 to, int r, int g, int b, int a, float lineWidth) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.lineWidth(lineWidth);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        var buf = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = ps.last().pose();
        float fr=r/255f,fg=g/255f,fb=b/255f,fa=a/255f;
        buf.addVertex(mat,(float)from.x,(float)from.y,(float)from.z).setColor(fr,fg,fb,fa);
        buf.addVertex(mat,(float)to.x,  (float)to.y,  (float)to.z).setColor(fr,fg,fb,fa);

        BufferUploader.drawWithShader(buf.buildOrThrow());
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    /**
     * Projects a world position to GUI-scaled screen coordinates.
     * Returns null if the position is behind the camera.
     * Uses the active projection/modelview matrices from RenderSystem.
     */
    public static float[] worldToScreen(Vec3 worldPos) {
        var mc     = Globals.mc;
        var camera = mc.gameRenderer.getCamera();
        var camPos = camera.getPosition();

        // Get matrices from RenderSystem state (set during level render)
        Matrix4f proj = RenderSystem.getProjectionMatrix();
        Matrix4f view = new Matrix4f(RenderSystem.getModelViewMatrix());

        double rx = worldPos.x - camPos.x;
        double ry = worldPos.y - camPos.y;
        double rz = worldPos.z - camPos.z;

        Vector4f clip = new Vector4f((float)rx, (float)ry, (float)rz, 1.0f);
        clip.mul(view).mul(proj);

        if (clip.w <= 0) return null;

        float ndcX = clip.x / clip.w;
        float ndcY = clip.y / clip.w;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        return new float[]{
            (ndcX + 1f) / 2f * sw,
            (1f - ndcY) / 2f * sh
        };
    }

    private static void addLine(BufferBuilder b, Matrix4f m,
            float x0,float y0,float z0,float x1,float y1,float z1,
            float r,float g,float fc,float a) {
        b.addVertex(m,x0,y0,z0).setColor(r,g,fc,a);
        b.addVertex(m,x1,y1,z1).setColor(r,g,fc,a);
    }

    public static int[] colorFromHex(int hex) {
        return new int[]{ (hex>>16)&0xFF, (hex>>8)&0xFF, hex&0xFF };
    }
}

