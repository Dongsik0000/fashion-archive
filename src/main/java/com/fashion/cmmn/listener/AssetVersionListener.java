package com.fashion.cmmn.listener;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

// 정적 파일 URL에 붙일 버전(기동 시각). 배포 후 브라우저가 예전 css/js를 캐시에서 쓰는 것을 막는다
public class AssetVersionListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        sce.getServletContext().setAttribute("assetVersion", String.valueOf(System.currentTimeMillis()));
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
