/*
 * Copyright 2026 deekoz2
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dsh.scserver;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class MainActivity extends Activity {

    private static final String DIR = "/sdcard/server";
    private TextView statusView, hintView, logView;
    private EditText pkgEdit;
    private Handler handler = new Handler();
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("cfg", MODE_PRIVATE);
        buildUi();
        tick();
        handler.postDelayed(ticker, 2000);
    }

    private Runnable ticker = new Runnable() {
        public void run() { tick(); handler.postDelayed(this, 2000); }
    };

    private Button mkBtn(String label, int color) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(18);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(color);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(12, 8, 12, 8);
        b.setLayoutParams(lp);
        return b;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 40, 32, 24);
        root.setBackgroundColor(Color.parseColor("#141A26"));

        TextView title = new TextView(this);
        title.setText("生存战争服务端");
        title.setTextSize(26); title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#DEBA6C"));
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("ValleyServer 同款思路 · SurvivalcraftNet · arm64");
        sub.setTextSize(13); sub.setTextColor(Color.parseColor("#969CAA"));
        root.addView(sub);

        statusView = new TextView(this);
        statusView.setTextSize(40); statusView.setTypeface(null, Typeface.BOLD);
        statusView.setPadding(0, 40, 0, 8);
        root.addView(statusView);

        hintView = new TextView(this);
        hintView.setTextSize(14); hintView.setTextColor(Color.parseColor("#969CAA"));
        root.addView(hintView);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button start = mkBtn("▶ 启动服务器", Color.parseColor("#2E7D46"));
        Button stop  = mkBtn("■ 停止服务器", Color.parseColor("#9E2B25"));
        start.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { sendCmd("start"); } });
        stop.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { sendCmd("stop"); } });
        row.addView(start); row.addView(stop);
        root.addView(row);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        Button refresh = mkBtn("↻ 刷新状态", Color.parseColor("#3A4A66"));
        Button grant   = mkBtn("授文件权限", Color.parseColor("#3A4A66"));
        refresh.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { tick(); } });
        grant.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { askAllFiles(); } });
        row2.addView(refresh); row2.addView(grant);
        root.addView(row2);

        TextView logTitle = new TextView(this);
        logTitle.setText("运行日志（最近 24 行）");
        logTitle.setTextSize(14); logTitle.setTextColor(Color.parseColor("#969CAA"));
        logTitle.setPadding(0, 24, 0, 6);
        root.addView(logTitle);

        ScrollView sc = new ScrollView(this);
        sc.setFillViewport(true);
        logView = new TextView(this);
        logView.setTextSize(12);
        logView.setTypeface(Typeface.MONOSPACE);
        logView.setTextColor(Color.parseColor("#BFE8BF"));
        logView.setBackgroundColor(Color.parseColor("#0D1117"));
        logView.setPadding(16, 12, 16, 12);
        sc.addView(logView);
        root.addView(sc, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout row3 = new LinearLayout(this);
        row3.setOrientation(LinearLayout.HORIZONTAL);
        row3.setGravity(Gravity.CENTER_VERTICAL);
        TextView lb = new TextView(this);
        lb.setText("Termux 包名"); lb.setTextSize(14); lb.setTextColor(Color.parseColor("#969CAA"));
        lb.setPadding(0, 0, 16, 0);
        pkgEdit = new EditText(this);
        pkgEdit.setInputType(InputType.TYPE_CLASS_TEXT);
        pkgEdit.setText(prefs.getString("pkg", "com.termux"));
        pkgEdit.setTextSize(14);
        LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        pkgEdit.setLayoutParams(pl);
        Button help = mkBtn("Termux 配置说明", Color.parseColor("#3A4A66"));
        help.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showGuide(); } });
        row3.addView(lb); row3.addView(pkgEdit); row3.addView(help);
        root.addView(row3);

        setContentView(root);
    }

    private String getPkg() {
        String p = pkgEdit.getText().toString().trim();
        if (p.length() == 0) p = "com.termux";
        prefs.edit().putString("pkg", p).apply();
        return p;
    }

    private void sendCmd(String cmd) {
        String pkg = getPkg();
        Intent i = new Intent();
        i.setClassName(pkg, "com.termux.app.RunCommandService");
        i.setAction("com.termux.RUN_COMMAND");
        i.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/" + pkg + "/files/usr/bin/sh");
        i.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{ DIR + "/sc-ctl.sh", cmd });
        i.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/");
        i.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
        try {
            startService(i);
            Toast.makeText(this, "已发送：" + cmd, Toast.LENGTH_SHORT).show();
            handler.postDelayed(new Runnable() { public void run() { tick(); } }, 1500);
        } catch (Exception e) {
            Toast.makeText(this, "发送失败：检查包名 / ZeroTermux 是否装好", Toast.LENGTH_LONG).show();
        }
    }

    private boolean allFilesGranted() {
        try {
            Object r = Environment.class.getMethod("isExternalStorageManager").invoke(null);
            return ((Boolean) r).booleanValue();
        } catch (Throwable t) {
            return true; // Android 10 及以下按老规则
        }
    }

    private void askAllFiles() {
        try {
            Intent i = new Intent("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION",
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } catch (Exception e) {
            try {
                startActivity(new Intent("android.settings.MANAGE_ALL_FILES_ACCESS_PERMISSION"));
            } catch (Exception e2) {
                Toast.makeText(this, "请在系统设置里手动授予「所有文件访问」", Toast.LENGTH_LONG).show();
            }
        }
    }

    private String readSmall(File f) {
        try {
            if (!f.exists()) return null;
            BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int n;
            while ((n = r.read(buf)) > 0) sb.append(buf, 0, n);
            r.close();
            return sb.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    private void tick() {
        if (!allFilesGranted()) {
            statusView.setText("需要授权");
            statusView.setTextColor(Color.parseColor("#E8C468"));
            hintView.setText("点底部「授文件权限」，允许「所有文件访问」后回来");
            logView.setText("");
            return;
        }
        String st = readSmall(new File(DIR + "/.sc-status"));
        if (st == null) {
            statusView.setText("未配置");
            statusView.setTextColor(Color.parseColor("#969CAA"));
            hintView.setText("找不到 " + DIR + "/.sc-status —— 确认服务端目录、以及 Termux 配置说明第 3 步");
        } else if (st.startsWith("RUNNING")) {
            statusView.setText("● 运行中");
            statusView.setTextColor(Color.parseColor("#7BC77B"));
            hintView.setText("客户端连接：本机端口 20000 附近（以服务端日志为准）");
        } else if (st.startsWith("STARTING")) {
            statusView.setText("◌ 启动中…");
            statusView.setTextColor(Color.parseColor("#E8C468"));
            hintView.setText("正在拉起服务端");
        } else if (st.startsWith("ERROR")) {
            statusView.setText("⚠ 配置错误");
            statusView.setTextColor(Color.parseColor("#E86A60"));
            hintView.setText(st.contains("no-dotnet")
                    ? "ZeroTermux 里没找到 dotnet —— 看「Termux 配置说明」"
                    : st.trim());
        } else {
            statusView.setText("○ 已停止");
            statusView.setTextColor(Color.parseColor("#969CAA"));
            hintView.setText("点「启动服务器」开机");
        }
        String log = readSmall(new File(DIR + "/.sc-log-tail"));
        logView.setText(log == null ? "（暂无日志）" : log);
    }

    private void showGuide() {
        String g = "【ZeroTermux 一次性配置】\n\n"
            + "① 允许外部调用（必须）：\n"
            + "   echo 'allow-external-apps=true' >> ~/.termux/termux.properties\n"
            + "   然后完全退出并重开 ZeroTermux\n\n"
            + "② 存储权限：\n"
            + "   termux-setup-storage   （弹窗点允许）\n\n"
            + "③ 安装 .NET 10（Termux 官方源就有）：\n"
            + "     pkg update && pkg install dotnet-sdk-10.0\n\n"
            + "④ 测试：\n"
            + "   sh /sdcard/server/sc-ctl.sh status\n"
            + "   sh /sdcard/server/sc-ctl.sh start\n\n"
            + "⑤ 回到本 App：包名填 com.termux\n"
            + "（ZeroTermux 若是别的包名，填它的实际包名）\n\n"
            + "点「确定」把以上命令复制到剪贴板";
        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this);
        b.setTitle("配置说明");
        b.setMessage(g);
        b.setPositiveButton("复制", new android.content.DialogInterface.OnClickListener() {
            public void onClick(android.content.DialogInterface di, int w) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("sc-setup", g));
                Toast.makeText(MainActivity.this, "已复制", Toast.LENGTH_SHORT).show();
            }
        });
        b.setNegativeButton("关闭", null);
        b.show();
    }
}
