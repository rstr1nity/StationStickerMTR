package com.tr1nity.stationsticker.client.display;

import net.minecraft.resources.ResourceLocation;
import org.mtr.mod.client.CustomResourceLoader;
import org.mtr.mod.resource.SignResource;

import javax.annotation.Nullable;
import java.util.List;

// Значки табличек MTR (окно Railway Sign): встроенные и из ресурспаков/аддонов. Значки MTR - светлые символы
// для тёмной таблички, поэтому рисуем их, как MTR: на квадрате фона значка, а без своего фона - на тёмном
public final class MtrSigns {

    private static final int DEFAULT_BACKGROUND = 0xFF1A1A1A;

    public record Sign(String id, ResourceLocation texture, int background, boolean flip) {
    }

    private MtrSigns() {
    }

    public static List<String> ids() {
        return CustomResourceLoader.getSortedSignIds();
    }

    @Nullable
    public static Sign get(String id) {
        if (id.isEmpty()) return null;
        Sign[] found = new Sign[1];
        CustomResourceLoader.getSignById(id, (SignResource sign) -> {
            int background = sign.getBackgroundColor() == 0 ? DEFAULT_BACKGROUND : 0xFF000000 | sign.getBackgroundColor();
            found[0] = new Sign(id, sign.getTexture().data, background, sign.getFlipTexture());
        });
        return found[0];
    }

    // Квадратный значок со стороной size, левый верхний угол в (x, y)
    public static void draw(SlideCanvas canvas, String id, float x, float y, float size) {
        Sign sign = get(id);
        if (sign == null) return;
        canvas.backgroundRect(x, y, size, size, sign.background());
        canvas.texture(sign.texture(), x, y, size, size, sign.flip());
    }
}
