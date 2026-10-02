package com.example.skillcards.network;

/**
 * 客户端持有的全局效果层数镜像（双端通用：只存 int，不引用任何客户端类）。
 * HUD 图层（client 包）从这里读取数值渲染。
 */
public final class GlobalFxState {
    private GlobalFxState() {}

    private static volatile int dance;
    private static volatile int kuangjian;
    private static volatile int scorch;

    public static void receive(GlobalFxPayload payload) {
        dance = payload.dance();
        kuangjian = payload.kuangjian();
        scorch = payload.scorch();
    }

    public static int dance() {
        return dance;
    }

    public static int kuangjian() {
        return kuangjian;
    }

    public static int scorch() {
        return scorch;
    }
}
