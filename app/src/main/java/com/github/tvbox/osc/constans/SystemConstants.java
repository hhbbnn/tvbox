package com.github.tvbox.osc.constans;

/**
 * 系统常量
 */
public class SystemConstants {

    public static class Source {
        // XML 类型
        public static final int TYPE_XML = 0;
        // JSON 类型
        public static final int TYPE_JSON = 1;
        // Spider 类型
        public static final int TYPE_SPIDER = 3;
        // 扩展数据
        public static final int TYPE_EXPAND = 4;
    }

    public static class History {
        // 最大保留历史观看记录
        public static final int MAX = 35;
    }

    public static class Setting {

        public enum HomeRecType {
            DOUBAN(0, "豆瓣热播"),
            SOURCE_REC(1, "推荐"),
            HISTORY(2, "观看历史");

            private final int code;
            private final String name;

            HomeRecType(int code, String name) {
                this.code = code;
                this.name = name;
            }

            public int getCode() {
                return code;
            }

            public String getName() {
                return name;
            }

            // 根据类型码获取枚举实例
            public static HomeRecType getByCode(int code) {
                for (HomeRecType type : values()) {
                    if (type.getCode() == code) {
                        return type;
                    }
                }
                throw new IllegalArgumentException("Invalid code: " + code);
            }

            // 直接根据类型码获取名称，类似于原始方法的功能
            public static String getHomeRecName(int type) {
                return getByCode(type).getName();
            }
        }
    }
}
