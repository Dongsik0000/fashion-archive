package com.fashion.photo.controller;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PhotoApiControllerTest {

    // DB 결과 한 행을 흉내 낸다. 키는 컬럼명 그대로
    private static Map<String, Object> link(long id, String label) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("id", id);
        m.put("item_label", label);
        m.put("url", "https://example.com/" + id);
        return m;
    }

    @Test
    void groupLinks_groupsByLabelKeepingFirstSeenOrder() {
        List<Map<String, Object>> links = Arrays.asList(
                link(1, "아우터"), link(2, "바지"), link(3, "아우터"), link(4, "신발"), link(5, "바지"));

        Map<String, List<Map<String, Object>>> groups = PhotoApiController.groupLinks(links);

        assertEquals(Arrays.asList("아우터", "바지", "신발"), new ArrayList<String>(groups.keySet()));
        assertEquals(1L, groups.get("아우터").get(0).get("id"));
        assertEquals(3L, groups.get("아우터").get(1).get("id"));
        assertEquals(2, groups.get("바지").size());
        assertEquals(1, groups.get("신발").size());
    }

    @Test
    void groupLinks_emptyInputGivesEmptyMap() {
        assertTrue(PhotoApiController.groupLinks(Collections.<Map<String, Object>>emptyList()).isEmpty());
    }
}
