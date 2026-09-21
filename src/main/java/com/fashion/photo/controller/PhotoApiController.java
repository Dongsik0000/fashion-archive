package com.fashion.photo.controller;

import com.fashion.cmmn.storage.LocalStorage;
import com.fashion.cmmn.util.Constants;
import com.fashion.cmmn.util.Response;
import com.fashion.cmmn.util.Validation;
import com.fashion.photo.service.PhotoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Controller
public class PhotoApiController {

    private static final Logger logger = LoggerFactory.getLogger(PhotoApiController.class);

    @Resource(name = "photoService")
    private PhotoService photoService;

    @Autowired
    private LocalStorage storage;

    @Autowired
    private TransactionTemplate transactionTemplate;

    /* ---------- 조회 (공개) ---------- */

    // 사진 목록. slug가 없으면 전체
    @ResponseBody
    @PostMapping("/api/photos/list")
    public Response list(@RequestBody HashMap<String, Object> param) {
        try {
            Object slugObj = param.get("slug");
            String slug = (slugObj == null || slugObj.toString().isEmpty()) ? null : slugObj.toString();
            return Response.of(Constants.SUCCESS, photoService.selectPhotoList(slug));
        } catch (Exception e) {
            logger.error("사진 목록 조회 중 오류", e);
            return Response.of(Constants.FAIL, "사진 목록을 불러오지 못했습니다.", null);
        }
    }

    // 사진 상세: 사진 + 카테고리 id + 라벨별 링크
    @ResponseBody
    @PostMapping("/api/photos/detail")
    public Response detail(@RequestBody HashMap<String, Object> param) {
        try {
            long id = Long.parseLong(String.valueOf(param.get("id")));
            Map<String, Object> photo = photoService.selectPhoto(id);
            if (photo == null) {
                return Response.of(Constants.FAIL, "사진을 찾을 수 없습니다.", null);
            }
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("photo", photo);
            data.put("categoryIds", photoService.selectPhotoCategoryIds(id));
            data.put("linkGroups", groupLinks(photoService.selectLinkList(id)));
            return Response.of(Constants.SUCCESS, data);
        } catch (NumberFormatException e) {
            return Response.of(Constants.FAIL, "잘못된 요청입니다.", null);
        } catch (Exception e) {
            logger.error("사진 상세 조회 중 오류", e);
            return Response.of(Constants.FAIL, "사진을 불러오지 못했습니다.", null);
        }
    }

    /* ---------- 사진 등록/수정/삭제 (로그인 필요: /api/admin/** 는 LoginInterceptor가 막는다) ---------- */

    // 등록. multipart: file, title, memo, categoryIds. 흐름: 검증 → Storage 업로드 → (트랜잭션) INSERT ×2
    @ResponseBody
    @PostMapping("/api/admin/photos")
    public Response create(@RequestParam("file") MultipartFile file,
                           @RequestParam(value = "title", required = false) String title,
                           @RequestParam(value = "memo", required = false) String memo,
                           @RequestParam(value = "categoryIds", required = false) List<Integer> categoryIds,
                           HttpSession session) {
        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("사진 파일을 선택해주세요.");
            }
            byte[] bytes = file.getBytes();
            String ext = Validation.imageExtension(Arrays.copyOf(bytes, 12));   // 확장자가 아니라 파일 내용으로 판별
            String cleanTitle = Validation.requireText(title, "제목", 100);
            String cleanMemo = Validation.optionalText(memo, "메모", 2000);
            List<Integer> ids = requireCategories(categoryIds);
            long ownerId = (Long) session.getAttribute(Constants.SESSION_USER_ID);

            String imageUrl = storage.upload(bytes, ext);

            Map<String, Object> photo = new HashMap<String, Object>();
            photo.put("ownerId", ownerId);
            photo.put("title", cleanTitle);
            photo.put("memo", cleanMemo);
            photo.put("imageUrl", imageUrl);

            long id;
            try {
                // INSERT 둘 중 하나라도 실패하면 둘 다 롤백
                id = transactionTemplate.execute(status -> {
                    photoService.insertPhoto(photo);
                    long newId = ((Number) photo.get("id")).longValue();
                    photoService.insertPhotoCategories(newId, ids);
                    return newId;
                });
            } catch (RuntimeException e) {
                storage.delete(imageUrl);   // DB에 못 남겼으니 방금 올린 파일도 정리
                throw e;
            }
            return Response.of(Constants.SUCCESS, Collections.singletonMap("id", id));
        } catch (IllegalArgumentException e) {
            return Response.of(Constants.FAIL, e.getMessage(), null);
        } catch (Exception e) {
            logger.error("사진 등록 중 오류", e);
            return Response.of(Constants.FAIL, "사진을 올리지 못했습니다.", null);
        }
    }

    // 수정. JSON: title, memo, categoryIds[]
    @ResponseBody
    @PostMapping("/api/admin/photos/{id}")
    public Response update(@PathVariable long id, @RequestBody HashMap<String, Object> param) {
        try {
            String title = Validation.requireText(param.get("title"), "제목", 100);
            String memo = Validation.optionalText(param.get("memo"), "메모", 2000);
            List<Integer> ids = requireCategories(toIntList(param.get("categoryIds")));

            Map<String, Object> photo = new HashMap<String, Object>();
            photo.put("id", id);
            photo.put("title", title);
            photo.put("memo", memo);

            boolean found = transactionTemplate.execute(status -> {
                if (photoService.updatePhoto(photo) == 0) {
                    return false;
                }
                photoService.deletePhotoCategories(id);
                photoService.insertPhotoCategories(id, ids);
                return true;
            });
            if (!found) {
                return Response.of(Constants.FAIL, "사진을 찾을 수 없습니다.", null);
            }
            return Response.of(Constants.SUCCESS);
        } catch (IllegalArgumentException e) {
            return Response.of(Constants.FAIL, e.getMessage(), null);
        } catch (Exception e) {
            logger.error("사진 수정 중 오류", e);
            return Response.of(Constants.FAIL, "사진을 수정하지 못했습니다.", null);
        }
    }

    // 삭제. DB(CASCADE)가 먼저, 파일은 나중에. 파일 삭제 실패는 고아 파일로 남을 뿐 화면에는 영향 없음
    @ResponseBody
    @PostMapping("/api/admin/photos/{id}/delete")
    public Response delete(@PathVariable long id) {
        try {
            Map<String, Object> photo = photoService.selectPhoto(id);
            if (photo == null) {
                return Response.of(Constants.FAIL, "사진을 찾을 수 없습니다.", null);
            }
            photoService.deletePhoto(id);
            storage.delete((String) photo.get("image_url"));
            return Response.of(Constants.SUCCESS);
        } catch (Exception e) {
            logger.error("사진 삭제 중 오류", e);
            return Response.of(Constants.FAIL, "사진을 삭제하지 못했습니다.", null);
        }
    }

    /* ---------- 제품 링크 추가/수정/삭제 ---------- */

    // 추가. JSON: itemLabel, url, title, note
    @ResponseBody
    @PostMapping("/api/admin/photos/{photoId}/links")
    public Response addLink(@PathVariable long photoId, @RequestBody HashMap<String, Object> param) {
        try {
            Map<String, Object> link = linkParam(param);
            if (photoService.selectPhoto(photoId) == null) {
                return Response.of(Constants.FAIL, "사진을 찾을 수 없습니다.", null);
            }
            link.put("photoId", photoId);
            photoService.insertLink(link);
            return Response.of(Constants.SUCCESS);
        } catch (IllegalArgumentException e) {
            return Response.of(Constants.FAIL, e.getMessage(), null);
        } catch (Exception e) {
            logger.error("링크 추가 중 오류", e);
            return Response.of(Constants.FAIL, "링크를 추가하지 못했습니다.", null);
        }
    }

    // 수정. JSON: itemLabel, url, title, note
    @ResponseBody
    @PostMapping("/api/admin/links/{linkId}")
    public Response updateLink(@PathVariable long linkId, @RequestBody HashMap<String, Object> param) {
        try {
            Map<String, Object> link = linkParam(param);
            link.put("id", linkId);
            if (photoService.updateLink(link) == 0) {
                return Response.of(Constants.FAIL, "링크를 찾을 수 없습니다.", null);
            }
            return Response.of(Constants.SUCCESS);
        } catch (IllegalArgumentException e) {
            return Response.of(Constants.FAIL, e.getMessage(), null);
        } catch (Exception e) {
            logger.error("링크 수정 중 오류", e);
            return Response.of(Constants.FAIL, "링크를 수정하지 못했습니다.", null);
        }
    }

    @ResponseBody
    @PostMapping("/api/admin/links/{linkId}/delete")
    public Response deleteLink(@PathVariable long linkId) {
        try {
            if (photoService.deleteLink(linkId) == 0) {
                return Response.of(Constants.FAIL, "링크를 찾을 수 없습니다.", null);
            }
            return Response.of(Constants.SUCCESS);
        } catch (Exception e) {
            logger.error("링크 삭제 중 오류", e);
            return Response.of(Constants.FAIL, "링크를 삭제하지 못했습니다.", null);
        }
    }

    /* ---------- 도우미 ---------- */

    // 링크를 item_label별로 묶는다. 라벨이 처음 나온 순서(= 등록 순서)를 유지하기 위해 LinkedHashMap
    public static Map<String, List<Map<String, Object>>> groupLinks(List<Map<String, Object>> links) {
        Map<String, List<Map<String, Object>>> groups = new LinkedHashMap<String, List<Map<String, Object>>>();
        for (Map<String, Object> link : links) {
            String label = String.valueOf(link.get("item_label"));
            if (!groups.containsKey(label)) {
                groups.put(label, new ArrayList<Map<String, Object>>());
            }
            groups.get(label).add(link);
        }
        return groups;
    }

    // 링크 입력값 검증. javascript: 같은 URL은 여기서 걸러진다
    private static Map<String, Object> linkParam(Map<String, Object> param) {
        Map<String, Object> link = new HashMap<String, Object>();
        link.put("itemLabel", Validation.requireText(param.get("itemLabel"), "아이템", 30));
        link.put("url", Validation.requireHttpUrl(param.get("url")));
        link.put("title", Validation.optionalText(param.get("title"), "제품 이름", 100));
        link.put("note", Validation.optionalText(param.get("note"), "메모", 500));
        return link;
    }

    // 최소 1개, 중복 제거
    private static List<Integer> requireCategories(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("카테고리를 하나 이상 선택해주세요.");
        }
        return new ArrayList<Integer>(new LinkedHashSet<Integer>(ids));
    }

    // JSON 배열은 List<Object>(Integer 또는 String)로 들어온다
    private static List<Integer> toIntList(Object value) {
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        List<Integer> out = new ArrayList<Integer>();
        for (Object o : (List<?>) value) {
            out.add(Integer.parseInt(String.valueOf(o)));
        }
        return out;
    }
}
