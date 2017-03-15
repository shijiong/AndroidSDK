package com.chinamobile.iot.onenet;

import android.app.Application;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;

import com.chinamobile.iot.onenet.http.HttpExecutor;
import com.chinamobile.iot.onenet.module.DataPoint;
import com.chinamobile.iot.onenet.module.DataStream;
import com.chinamobile.iot.onenet.module.Device;
import com.chinamobile.iot.onenet.module.Trigger;
import com.chinamobile.iot.onenet.util.Assertions;
import com.chinamobile.iot.onenet.util.Meta;
import com.chinamobile.iot.onenet.util.OneNetLogger;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class OneNetApi {

    public static final String LOG_TAG = "OneNetApi";

    private static String sApiKey;
    static boolean sDebug;
    private static HttpExecutor sHttpExecutor;

    public static void init(Application application, boolean debug) {
        try {
            sApiKey = Meta.readApiKey(application);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        sDebug = debug;
        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();
        if (sDebug) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(new OneNetLogger());
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            okHttpClientBuilder.addNetworkInterceptor(loggingInterceptor);
        }
        okHttpClientBuilder.addInterceptor(sApiKeyInterceptor);
        sHttpExecutor = new HttpExecutor(okHttpClientBuilder.build());
    }

    private static Interceptor sApiKeyInterceptor = new Interceptor() {

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request.Builder builder = chain.request().newBuilder();
            builder.addHeader("api-key", sApiKey);
            if (TextUtils.isEmpty(sApiKey)) {
                Log.e(LOG_TAG, "api-key is messing, please config in the meta-data or call setApiKey()");
            }
            return chain.proceed(builder.build());
        }
    };

    public static void setApiKey(String apiKey) {
        sApiKey = apiKey;
    }

    private static boolean isInitialized() {
        return sHttpExecutor != null;
    }

    private static void assertInitialized() {
        Assertions.assertCondition(isInitialized(), "You should call OneNetApi.init() in your Application!");
    }

    private static void get(String url, OneNetApiCallback callback) {
        sHttpExecutor.get(url, new OneNetApiCallbackAdapter(callback));
    }

    private static void post(String url, String requestBodyString, OneNetApiCallback callback) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestBodyString);
        sHttpExecutor.post(url, requestBody, new OneNetApiCallbackAdapter(callback));
    }

    private static void post(String url, File file, OneNetApiCallback callback) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/octet-stream"), file);
        sHttpExecutor.post(url, requestBody, new OneNetApiCallbackAdapter(callback));
    }

    private static void post(String url, byte[] content, OneNetApiCallback callback) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/octet-stream"), content);
        sHttpExecutor.post(url, requestBody, new OneNetApiCallbackAdapter(callback));
    }

    private static void put(String url, String requestBodyString, OneNetApiCallback callback) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestBodyString);
        sHttpExecutor.put(url, requestBody, new OneNetApiCallbackAdapter(callback));
    }

    private static void delete(String url, OneNetApiCallback callback) {
        sHttpExecutor.delete(url, new OneNetApiCallbackAdapter(callback));
    }

    /******************** 设备相关api ********************/

    /**
     * 注册设备
     *
     * @param registerCode      设备注册码
     * @param requestBodyString HTTP内容 详见<a href="http://www.heclouds.com/doc/art262.html#68">
     *                          http://www.heclouds.com/doc/art262.html#68</a>
     * @param callback          回调函数
     */
    public static void registerDevice(String registerCode, String requestBodyString, OneNetApiCallback callback) {
        assertInitialized();
        post(Device.urlForRegistering(registerCode), requestBodyString, callback);
    }

    /**
     * 新增设备
     *
     * @param requestBodyString HTTP内容 详见<a href="http://www.heclouds.com/doc/art262.html#68">
     *                          http://www.heclouds.com/doc/art262.html#68</a>
     * @param callback          回调函数
     */
    public static void addDevice(String requestBodyString, OneNetApiCallback callback) {
        assertInitialized();
        post(Device.urlForAdding(), requestBodyString, callback);
    }

    /**
     * 更新设备
     *
     * @param deviceId          设备ID
     * @param requestBodyString HTTP内容 详见<a href="http://www.heclouds.com/doc/art262.html#68">
     *                          http://www.heclouds.com/doc/art262.html#68</a>
     * @param callback          回调函数
     */
    public static void updateDevice(String deviceId, String requestBodyString, OneNetApiCallback callback) {
        assertInitialized();
        put(Device.urlForUpdating(deviceId), requestBodyString, callback);
    }

    /**
     * 精确查询单个设备
     *
     * @param deviceId 设备ID
     * @param callback 回调函数
     */
    public static void querySingleDevice(String deviceId, OneNetApiCallback callback) {
        assertInitialized();
        get(Device.urlForQueryingSingle(deviceId), callback);
    }

    /**
     * 模糊查询设备
     *
     * @param params   URL参数 详见<a href="http://www.heclouds.com/doc/art262.html#68">
     *                 http://www.heclouds.com/doc/art262.html#68</a>
     * @param callback 回调函数
     */
    public static void fuzzyQueryDevices(Map<String, String> params, OneNetApiCallback callback) {
        assertInitialized();
        get(Device.urlForfuzzyQuerying(params), callback);
    }

    /**
     * 删除设备
     *
     * @param deviceId 设备ID
     * @param callback 回调函数
     */
    public static void deleteDevice(String deviceId, OneNetApiCallback callback) {
        assertInitialized();
        delete(Device.urlForDeleting(deviceId), callback);
    }

    /******************** END ********************/

    /******************** 数据流相关api ********************/

    /**
     * 新增数据流
     *
     * @param deviceId          设备ID
     * @param requestBodyString HTTP内容 详见 <a href="http://www.heclouds.com/doc/art261.html#68">
     *                          http://www.heclouds.com/doc/art261.html#68</a>
     * @param callback          回调函数
     */
    public static void addDataStream(String deviceId, String requestBodyString, OneNetApiCallback callback) {
        assertInitialized();
        post(DataStream.urlForAdding(deviceId), requestBodyString, callback);
    }

    /**
     * 更新数据流
     *
     * @param deviceId          设备ID
     * @param dataStreamId      数据流ID
     * @param requestBodyString HTTP内容 详见 <a href="http://www.heclouds.com/doc/art261.html#68">
     *                          http://www.heclouds.com/doc/art261.html#68</a>
     * @param callback          回调函数
     */
    public static void updateDataStream(String deviceId, String dataStreamId, String requestBodyString, OneNetApiCallback callback) {
        assertInitialized();
        put(DataStream.urlForUpdating(deviceId, dataStreamId), requestBodyString, callback);
    }

    /**
     * 查询单个数据流
     *
     * @param deviceId     设备ID
     * @param dataStreamId 数据流ID
     * @param callback     回调函数
     */
    public static void querySingleDataStream(String deviceId, String dataStreamId, OneNetApiCallback callback) {
        assertInitialized();
        get(DataStream.urlForQueryingSingle(deviceId, dataStreamId), callback);
    }

    /**
     * 查询多个数据流
     *
     * @param deviceId 设备ID
     * @param callback 回调函数
     */
    public static void queryMultiDataStreams(String deviceId, OneNetApiCallback callback) {
        assertInitialized();
        get(DataStream.urlForQueryingMulti(deviceId), callback);
    }

    /**
     * 删除数据流
     *
     * @param deviceId     设备ID
     * @param dataStreamId 数据流ID
     * @param callback     回调函数
     */
    public static void deleteDatastream(String deviceId, String dataStreamId, OneNetApiCallback callback) {
        assertInitialized();
        delete(DataStream.urlForDeleting(deviceId, dataStreamId), callback);
    }

    /******************** END ********************/

    /******************** 数据点相关api ********************/

    /**
     * 新增数据点(数据点类型为3)
     *
     * @param deviceId          设备ID
     * @param requestBodyString HTTP内容 详见<a href="http://www.heclouds.com/doc/art260.html#68">
     *                          http://www.heclouds.com/doc/art260.html#68</a>
     * @param callback          回调函数
     */
    public static void addDataPoints(String deviceId, String requestBodyString, OneNetApiCallback callback) {
        assertInitialized();
        post(DataPoint.urlForAdding(deviceId, null), requestBodyString, callback);
    }

    /**
     * 新增数据点
     *
     * @param deviceId          设备ID
     * @param type              数据点类型
     * @param requestBodyString HTTP内容 详见<a href="http://www.heclouds.com/doc/art260.html#68">
     *                          http://www.heclouds.com/doc/art260.html#68</a>
     * @param callback          回调函数
     */
    public static void addDataPoints(String deviceId, String type, String requestBodyString, OneNetApiCallback callback) {
        assertInitialized();
        post(DataPoint.urlForAdding(deviceId, type), requestBodyString, callback);
    }

    /**
     * 查询数据点
     *
     * @param deviceId 设备ID
     * @param params   URL参数 详见<a href="http://www.heclouds.com/doc/art260.html#68">
     *                 http://www.heclouds.com/doc/art260.html#68</a>
     * @param callback 回调函数
     */
    public static void queryDataPoints(String deviceId, Map<String, String> params, OneNetApiCallback callback) {
        assertInitialized();
        get(DataPoint.urlForQuerying(deviceId, params), callback);
    }

    /******************** END ********************/

    /******************** 触发器相关api ********************/

    public static void addTrigger(String requestBodyString, OneNetApiCallback callback) {
        assertInitialized();
        post(Trigger.urlForAdding(), requestBodyString, callback);
    }

    public static void updateTrigger(String triggerId, String requestBodyString, OneNetApiCallback callback) {
        assertInitialized();
        put(Trigger.urlForUpdating(triggerId), requestBodyString, callback);
    }

    public static void querySingleTrigger(String triggerId, OneNetApiCallback callback) {
        assertInitialized();
        get(Trigger.urlForQueryingSingle(triggerId), callback);
    }

    public static void fuzzyQueryTriggers(String name, int page, int perPager, OneNetApiCallback callback) {
        assertInitialized();
        get(Trigger.urlForfuzzyQuerying(name, page, perPager), callback);
    }

    public static void deleteTrigger(String triggerId, OneNetApiCallback callback) {
        assertInitialized();
        delete(Trigger.urlForDeleting(triggerId), callback);
    }

    /******************** END ********************/

    /******************** 二进制相关api ********************/

    /******************** END ********************/
}