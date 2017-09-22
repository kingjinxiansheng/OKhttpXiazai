package com.example.demo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final static long CONNECT_TIMEOUT = 60;//超时时间，秒
    private final static long READ_TIMEOUT = 60;//读取时间，秒
    private final static long WRITE_TIMEOUT = 60;//写入时间，秒
    private Button bt;
    private String DOWNLOAD_URL = "http://dldir1.qq.com/weixin/android/weixin657android1040.apk";
    private FileOutputStream mDstOutputStream;
    private long l;
    private int mCurrentDownload;
    private static final int DELTA = 1024 * 1024;
    private String xiazaiurl = Environment.getExternalStorageDirectory().getAbsolutePath()
            + File.separator + "icheny" + File.separator + "/yixin.apk";

    private enum DownloadState {
        PENDING,
        DOWNLOADING,
        PAUSE,
        DONE
    }

    private DownloadState mState = DownloadState.PENDING;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        bt = (Button) findViewById(R.id.bt);
        bt.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt:
                xiazai();
                break;
        }
    }

    private void xiazai() {
        getFileLength(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    //获取下载文件长度
                    l = response.body().contentLength();
                    //设置下载位置
                    mDstOutputStream = new FileOutputStream(xiazaiurl);
                    downloadRange(0);
                }
            }
        });

    }

    private void downloadRange(final int i) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                Request request = new Request.Builder()
                        .url(DOWNLOAD_URL)
                        .addHeader("range", "bytes=" + i + "-" + Math.min(i + DELTA, l)).build();

                getClient().newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {

                        final byte[] bytes = response.body().bytes();
                        mDstOutputStream.write(bytes);
                        mCurrentDownload += bytes.length;
                        if (mCurrentDownload >= l) {
                            mDstOutputStream.flush();
                            mDstOutputStream.close();
                            anzhuang();

                            return;
                        }

                        downloadRange(mCurrentDownload);

                    }
                });
            }
        }.start();

    }
//下载完成调用隐式跳转到安装页面
    private void anzhuang() {
        File file = new File(xiazaiurl);
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file),
                "application/vnd.android.package-archive");
        startActivity(intent);
    }

    private void getFileLength(Callback callback) {
        Request request = new Request.Builder()
                .url(DOWNLOAD_URL)
                .method("HEAD", null).build();

        getClient().newCall(request).enqueue(callback);
    }

    public OkHttpClient getClient() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS).build();
        return client;
    }
}
