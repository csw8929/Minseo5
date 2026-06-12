package com.example.minseo5.util;

public class CategoryColors {

    // 파스텔 톤 컬러풀 팔레트 (시안 규격). 통계 도넛과 리스트 타일이 같은 색을 쓰도록 공유.
    private static final int[] PALETTE = {
            0xFF66BB6A, 0xFF4FC3F7, 0xFFFFB74D, 0xFFF06292, 0xFFBA68C8,
            0xFFFFD54F, 0xFF4DB6AC, 0xFFA1887F, 0xFF90A4AE, 0xFFFF8A65
    };

    public static int colorFor(String category) {
        String key = category != null ? category : "";
        return PALETTE[Math.floorMod(key.hashCode(), PALETTE.length)];
    }

    public static int[] palette() {
        return PALETTE;
    }
}
