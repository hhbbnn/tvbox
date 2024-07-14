package com.github.tvbox.osc.cache;

import android.text.TextUtils;

import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.bean.VideoInfo;
import com.github.tvbox.osc.constans.SystemConstants;
import com.github.tvbox.osc.data.AppDataManager;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

/**
 * @author pj567
 * @date :2021/1/7
 * @description:
 */
public class RoomDataManger {
    static ExclusionStrategy vodInfoStrategy = new ExclusionStrategy() {
        @Override
        public boolean shouldSkipField(FieldAttributes field) {
            if (field.getDeclaringClass() == VideoInfo.class && field.getName().equals("seriesFlags")) {
                return true;
            }
            if (field.getDeclaringClass() == VideoInfo.class && field.getName().equals("seriesMap")) {
                return true;
            }
            return false;
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }
    };

    private static Gson getVodInfoGson() {
        return new GsonBuilder().addSerializationExclusionStrategy(vodInfoStrategy).create();
    }

    public static void insertVodRecord(String sourceKey, VideoInfo videoInfo) {
        VodRecord record = AppDataManager.get().getVodRecordDao().getVodRecord(sourceKey, videoInfo.id);
        if (record == null) {
            record = new VodRecord();
        }
        record.sourceKey = sourceKey;
        record.vodId = videoInfo.id;
        record.updateTime = System.currentTimeMillis();
        record.dataJson = getVodInfoGson().toJson(videoInfo);
        AppDataManager.get().getVodRecordDao().insert(record);
    }

    public static VideoInfo getVodInfo(String sourceKey, String vodId) {
        VodRecord record = AppDataManager.get().getVodRecordDao().getVodRecord(sourceKey, vodId);
        try {
            if (record != null && record.dataJson != null && !TextUtils.isEmpty(record.dataJson)) {
                VideoInfo videoInfo = getVodInfoGson().fromJson(record.dataJson, new TypeToken<VideoInfo>() {
                }.getType());
                if (videoInfo.name == null)
                    return null;
                return videoInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void deleteVodRecord(String sourceKey, VideoInfo videoInfo) {
        VodRecord record = AppDataManager.get().getVodRecordDao().getVodRecord(sourceKey, videoInfo.id);
        if (record != null) {
            AppDataManager.get().getVodRecordDao().delete(record);
        }
    }

    public static List<VideoInfo> getAllVodRecord(int limit) {
        // 超出历史记录
        if (AppDataManager.get().getVodRecordDao().getCount() > SystemConstants.History.MAX) {
            AppDataManager.get().getVodRecordDao().reserver(SystemConstants.History.MAX);
        }

        List<VideoInfo> videoInfoList = new ArrayList<>();
        List<VodRecord> recordList = AppDataManager.get().getVodRecordDao().getAll(limit);
        if (recordList != null) {
            for (VodRecord record : recordList) {
                VideoInfo info = null;
                if (record.dataJson != null && !TextUtils.isEmpty(record.dataJson)) {
                    info = getVodInfoGson().fromJson(record.dataJson, new TypeToken<VideoInfo>() {
                    }.getType());
                    info.sourceKey = record.sourceKey;
                    SourceBean sourceBean = ApiConfig.get().getSource(info.sourceKey);
                    if (sourceBean == null || info.name == null)
                        info = null;
                }

                if (info != null)
                    videoInfoList.add(info);
            }
        }
        return videoInfoList;
    }

    public static void insertVodCollect(String sourceKey, VideoInfo videoInfo) {
        VodCollect record = AppDataManager.get().getVodCollectDao().getVodCollect(sourceKey, videoInfo.id);
        if (record != null) {
            return;
        }
        record = new VodCollect();
        record.sourceKey = sourceKey;
        record.vodId = videoInfo.id;
        record.updateTime = System.currentTimeMillis();
        record.name = videoInfo.name;
        record.pic = videoInfo.pic;
        AppDataManager.get().getVodCollectDao().insert(record);
    }

    public static void deleteVodCollect(int id) {
        AppDataManager.get().getVodCollectDao().delete(id);
    }

    public static void deleteVodCollect(String sourceKey, VideoInfo videoInfo) {
        VodCollect record = AppDataManager.get().getVodCollectDao().getVodCollect(sourceKey, videoInfo.id);
        if (record != null) {
            AppDataManager.get().getVodCollectDao().delete(record);
        }
    }

    public static void deleteVodCollectAll() {
        AppDataManager.get().getVodCollectDao().deleteAll();
    }

    public static void deleteVodRecordAll() {
        AppDataManager.get().getVodRecordDao().deleteAll();
    }

    public static boolean isVodCollect(String sourceKey, String vodId) {
        VodCollect record = AppDataManager.get().getVodCollectDao().getVodCollect(sourceKey, vodId);
        return record != null;
    }

    public static List<VodCollect> getAllVodCollect() {
        return AppDataManager.get().getVodCollectDao().getAll();
    }
}