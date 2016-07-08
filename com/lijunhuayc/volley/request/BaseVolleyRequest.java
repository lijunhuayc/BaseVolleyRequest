package com.lijunhuayc.volley.request;

import android.content.Context;
import android.content.Intent;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.echi.train.app.MyApplication;
import com.echi.train.model.BaseObject;
import com.echi.train.ui.activity.LoginActivity;
import com.echi.train.ui.fragment.HTabPersonalFragment;
import com.echi.train.utils.Timber;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Desc: 通用Request 类
 * Created by ${junhua.li} on 2016/03/23 18:05.
 * Email: jli@bpexps.com
 */
public class BaseVolleyRequest<T> extends Request<T> {
    private String TAG = getClass().getSimpleName();
    private final Gson gson = new Gson();
    private Class<T> clazz;//解析目标类, 必须与接口数据结构相同
    private Listener<T> listener;
    private static Map<String, String> defaultHeader = new HashMap<String, String>();//全局请求头信息
    private Map<String, String> mHeader = new HashMap<String, String>();//当前请求头信息
    private Map<String, String> params = new HashMap<String, String>();//POST 参数[Map格式]
    private String mRequestBody;    //POST 参数[json格式]
    private ParamType paramType = ParamType.JSON;//POST 参数格式

    public enum ParamType {
        KEY_VALUE, JSON
    }

    static {
        //TODO ... 设置访问自己服务器时必须传递的参数，密钥等
//        defaultHeader.put("APP-Key", "LBS-AAA");
//        defaultHeader.put("APP-Secret", "ad12msa234das232in");
    }

    /**
     * @param url
     * @param clazz         我们最终的转化类型
     * @param appendHeader  请求附带的头信息
     * @param listener
     * @param appendHeader  附加头数据
     * @param errorListener
     */
    public BaseVolleyRequest(int method, String url, Class<T> clazz, Map<String, String> appendHeader, Listener<T> listener, ErrorListener errorListener) {
        this(method, url, errorListener);
        mHeader = new HashMap<>(defaultHeader);
        setClazz(clazz);
        setListener(listener);
        addHeaders(appendHeader);
        VolleyLog.d("request method=%s", conversionMethodStr(method));
//        VolleyLog.d("request url=%s", url);
        setRetryPolicy(new DefaultRetryPolicy(10 * 1000, 2, 1.0f));//超市时间10s, 最大重连次数2次
    }

    @Override
    public String getBodyContentType() {
        return "application/json; charset=" + this.getParamsEncoding();
    }

    public BaseVolleyRequest(int method, String url, ErrorListener errorListener) {
        super(method, url, errorListener);
    }

    public BaseVolleyRequest(int method, String url, Class<T> clazz, Listener<T> listener, ErrorListener errorListener) {
        this(method, url, clazz, null, listener, errorListener);
    }

    public BaseVolleyRequest(String url, ErrorListener errorListener) {
        this(Method.GET, url, errorListener);
    }

    public BaseVolleyRequest(String url, Class<T> clazz, ErrorListener errorListener) {
        this(url, clazz, null, null, errorListener);
    }

    public BaseVolleyRequest(String url, Class<T> clazz, Listener<T> listener, ErrorListener errorListener) {
        this(url, clazz, null, listener, errorListener);
    }

    public BaseVolleyRequest(String url, Class<T> clazz, Map<String, String> appendHeader, Listener<T> listener, ErrorListener errorListener) {
        this(Method.GET, url, clazz, appendHeader, listener, errorListener);
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        // 默认返回 return Collections.emptyMap();
        return mHeader;
    }

    /**
     * 添加请求头信息
     *
     * @param mHeader
     */
    public BaseVolleyRequest<T> addHeaders(Map<String, String> mHeader) {
        if (null != mHeader) {
            this.mHeader.putAll(mHeader);
        }
        return this;
    }

    /**
     * 设置 POST 参数
     */
    public BaseVolleyRequest<T> setParams(Map<String, String> params) {
        if (null != params) {
            this.params = params;
            this.mRequestBody = gson.toJson(params);
        }
        return this;
    }

    /**
     * 设置POST 参数
     *
     * @param mRequestBody
     */
    public BaseVolleyRequest<T> setRequestBody(String mRequestBody) {
        if (null != mRequestBody) {
            this.mRequestBody = mRequestBody;
            this.params = gson.fromJson(mRequestBody, Map.class);
        }
        return this;
    }

    private String getRequestBody() {
        return mRequestBody;
    }

    /**
     * 设置 POST 参数的格式为 ParamType.JSON(default) or KEY_VIEW {@link ParamType}
     *
     * @param paramType
     */
    public BaseVolleyRequest<T> setParamType(ParamType paramType) {
        this.paramType = paramType;
        return this;
    }

    public BaseVolleyRequest<T> setClazz(Class<T> clazz) {
        this.clazz = clazz;
        return this;
    }

    public BaseVolleyRequest<T> setListener(Listener<T> listener) {
        this.listener = listener;
        return this;
    }

    @Override
    protected Map<String, String> getParams() {
        return this.params;
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
//        VolleyLog.d("getParams=%s\n getRequestBody=%s", getParams(), getRequestBody());
        if (paramType == ParamType.JSON) {
            try {
                return mRequestBody == null ? null : mRequestBody.getBytes(getParamsEncoding());
            } catch (UnsupportedEncodingException var2) {
                VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", new Object[]{mRequestBody, getParamsEncoding()});
                return null;
            }
        } else {
            return super.getBody();
        }
    }

    @Override
    protected String getParamsEncoding() {
        return "UTF-8";
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        String jsonStr;
        try {
            jsonStr = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
        } catch (UnsupportedEncodingException e) {
            jsonStr = new String(response.data);
        }
        VolleyLog.d("request response.data = %s", jsonStr);

        try {
            if (null == clazz || clazz == String.class) {
                return Response.success((T) jsonStr, HttpHeaderParser.parseCacheHeaders(response));
            } else if (clazz == JSONObject.class) {
                return Response.success((T) new JSONObject(jsonStr), HttpHeaderParser.parseCacheHeaders(response));
            } else if (clazz == JSONArray.class) {
                return Response.success((T) new JSONArray(jsonStr), HttpHeaderParser.parseCacheHeaders(response));
            } else {
                return Response.success(gson.fromJson(jsonStr, clazz), HttpHeaderParser.parseCacheHeaders(response));
            }
        } catch (JSONException var4) {
            return Response.error(new ParseError(var4));
        } catch (Exception e) {
            return Response.error(new ParseError(e));
        }
    }

    /**
     * 过滤 token失效的处理
     *
     * @param response
     * @return
     */
    private boolean interceptResult(T response) {
        if (!getUrl().equals(HTabPersonalFragment.buildPersonalDataCacheKey()) && response instanceof BaseObject) {
            BaseObject bo = (BaseObject) response;
            if (bo.getErr_code() == 10001) {
                Timber.d("interceptResult: token已失效, 需要重新登录");
                Context mContext = MyApplication.getApplication().getApplicationContext();
                Intent intent = new Intent(mContext, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void deliverResponse(T response) {
        if (interceptResult(response)) {
            return;
        }
        listener.onResponse(response);
    }

    public static String conversionMethodStr(int method) {
        String methodStr = "not a request Method";
        switch (method) {
            case Method.GET:
                methodStr = "Method.GET";
                break;
            case Method.DELETE:
                methodStr = "Method.DELETE";
                break;
            case Method.DEPRECATED_GET_OR_POST:
                methodStr = "Method.DEPRECATED_GET_OR_POST";
                break;
            case Method.POST:
                methodStr = "Method.POST";
                break;
            case Method.PUT:
                methodStr = "Method.PUT";
                break;
            case Method.HEAD:
                methodStr = "Method.HEAD";
                break;
            case Method.OPTIONS:
                methodStr = "Method.OPTIONS";
                break;
            case Method.PATCH:
                methodStr = "Method.PATCH";
                break;
        }
        return methodStr;
    }

}