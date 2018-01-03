package com.dq.robot;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class WeiXinService extends AccessibilityService {

    private static final String TAG = "WeiXinService";

    private static WeiXinService service;

    private Handler mHandler = null;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "service destory");
        service = null;
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "service interrupt");
        Toast.makeText(this, "中断服务", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        service = this;
        Toast.makeText(this, "已连接服务", Toast.LENGTH_SHORT).show();
    }

    //收到消息，发送动作
    private boolean isSend = false;

    //是否在输入
    private boolean isInputEnter = false;

    //是否在设置备注
    private boolean isSetBz = false;

    //加好友
    private boolean isAdd = false;

    //第一次发消息
    private boolean isFirst = false;

    //第一次发送
    private boolean isFirstSend = false;

    //是否主动发送消息
    private boolean isPushMsg = false;

    //是否输入了昵称
    private boolean isPushCode = false;

    private boolean isBack = false;

    //微信号
    private String wxNoStr;

    //最新一条微信消息
    private String wxMsgStr;

    private boolean isAddFriendCheck = false;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (BuildConfig.DEBUG) {
            //Log.e(TAG, "事件--->" + event);
        }

        String pkn = String.valueOf(event.getPackageName());
        if ("com.tencent.mm".equals(pkn)) {
            final AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
            if (nodeInfo == null) {
                Log.w(TAG, "rootWindow为空");
                return;
            }

            if (isBack) {
                String homeWx = "com.tencent.mm:id/bw6";
                AccessibilityNodeInfo targetHomeWx = AccessibilityHelper.findNodeInfosById(nodeInfo, homeWx);
                if (targetHomeWx == null || targetHomeWx.getText() == null ||
                        !"微信".equals(targetHomeWx.getText().toString().trim())) {
                    execShellCmd(String.format("input tap  %s %s", getRandom(5, 80), getRandom(80, 130)));
                    return;
                } else {
                    AccessibilityHelper.performClick(targetHomeWx);
                    isBack = false;
                }
            }

            String dialogTitleId = "com.tencent.mm:id/bvo";
            AccessibilityNodeInfo targetTitle = AccessibilityHelper.findNodeInfosById(nodeInfo, dialogTitleId);
            String dialogCancelId = "com.tencent.mm:id/aer";
            AccessibilityNodeInfo targetCancel = AccessibilityHelper.findNodeInfosById(nodeInfo, dialogCancelId);
            String dialogConfirmId = "com.tencent.mm:id/aes";
            AccessibilityNodeInfo targetConfirm = AccessibilityHelper.findNodeInfosById(nodeInfo, dialogConfirmId);
            if (targetCancel != null && targetConfirm != null && targetTitle != null &&
                    targetTitle.getText() != null && targetTitle.getText().toString().trim().contains("更新")) {
                if (targetCancel.getText().toString().trim().contains("取消")) {
                    AccessibilityHelper.performClick(targetCancel);
                } else if (targetConfirm.getText().toString().trim().contains("是")) {
                    AccessibilityHelper.performClick(targetConfirm);
                }
                return;
            }

            if (isSend) {
                //聊天界面---右上角图片
                String chattingId = "com.tencent.mm:id/fk";
                AccessibilityNodeInfo targetChatting = AccessibilityHelper.findNodeInfosById(nodeInfo, chattingId);
                //详细资料界面---微信号
                String wxNoId = "com.tencent.mm:id/agb";
                AccessibilityNodeInfo targetWxNo = AccessibilityHelper.findNodeInfosById(nodeInfo, wxNoId);
                //详细资料界面---设置备注
                String wxSetBzId = "com.tencent.mm:id/agr";
                AccessibilityNodeInfo targetSetBz = AccessibilityHelper.findNodeInfosById(nodeInfo, wxSetBzId);
                //详细资料界面---昵称
                String wxNickId = "com.tencent.mm:id/agl";
                AccessibilityNodeInfo targetNick = AccessibilityHelper.findNodeInfosById(nodeInfo, wxNickId);
                //详细资料界面---备注
                String wxBzId = "com.tencent.mm:id/ms";
                AccessibilityNodeInfo targetBz = AccessibilityHelper.findNodeInfosById(nodeInfo, wxBzId);
                //设置备注界面---设置备注输入框
                String setEtId = "com.tencent.mm:id/ahu";
                AccessibilityNodeInfo targetSetEt = AccessibilityHelper.findNodeInfosById(nodeInfo, setEtId);
                if (targetChatting != null) {
                    String wxSendId = "com.tencent.mm:id/a5k";
                    AccessibilityNodeInfo targetSend = AccessibilityHelper.findNodeInfosById(nodeInfo, wxSendId);
                    if (StringUtil.isBlank(wxNoStr)) {
                        //聊天界面对方头像
                        String chattingOtherId = "com.tencent.mm:id/ih";
                        AccessibilityNodeInfo targetChattingOther = AccessibilityHelper.findNodeInfosByIdDesc(nodeInfo, chattingOtherId);
                        if (targetChattingOther != null) {
                            //可以看到头像
                            AccessibilityHelper.performClick(targetChattingOther);
                        } else {
                            //看不到头像
                        }
                    } else if (!isInputEnter) {
                        isInputEnter = true;
                        String otherMsgId = "com.tencent.mm:id/ij";
                        AccessibilityNodeInfo targetMsg = AccessibilityHelper.findNodeInfosByIdDesc(nodeInfo, otherMsgId);
                        if (targetMsg != null && targetMsg.getText() != null) {
                            wxMsgStr = targetMsg.getText().toString().trim();
                            Log.e(TAG, "other:" + wxMsgStr);
                        } else {
                            showToast("对方没有发送任何文字消息");
                            isSend = false;
                            isBack = true;
                            execShellCmd(String.format("input tap  %s %s", getRandom(5, 80), getRandom(80, 130)));
                            return;
                        }
                        //给对方发消息
                        getHandler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                String sendEtId = "com.tencent.mm:id/a5e";
                                AccessibilityNodeInfo targetSendEt = AccessibilityHelper.findNodeInfosById(nodeInfo, sendEtId);
                                AccessibilityHelper.performClick(targetSendEt);
                                //execShellCmd(String.format("input tap  %s %s", getRandom(270, 540), getRandom(1800, 1890)));
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                                if (StringUtil.isBlank(wxMsgStr)) {
                                    return;
                                }
                                exeCmd(String.format("am broadcast -a ADB_INPUT_TEXT --es msg \"%s\"", "内测数据"));
                            }
                        }, 100);
                    } else if (targetSend != null) {
                        AccessibilityHelper.performClick(targetSend);
                        getHandler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                execShellCmd(String.format("input tap  %s %s", getRandom(5, 80), getRandom(80, 130)));
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                isSend = false;
                            }
                        }, 500);
                    }
                } else if (targetWxNo != null) {
                    if (StringUtil.isBlank(wxNoStr)) {
                        wxNoStr = targetWxNo.getText().toString().trim().replace("微信号: ", "");
                        isInputEnter = false;
                        Log.e(TAG, "wxNoStr:" + wxNoStr);
                        if ("weixin".equals(wxNoStr)) {
                            isSend = false;
                            isBack = true;
                        }
                        execShellCmd(String.format("input tap  %s %s", getRandom(5, 80), getRandom(80, 130)));
                    }
                } else if (targetNick != null && targetBz != null) {
                    if (StringUtil.isBlank(wxNoStr)) {
                        wxNoStr = targetBz.getText().toString().trim().replace("微信号: ", "");
                        isInputEnter = false;
                        Log.e(TAG, "wxNoStr:" + wxNoStr);
                        if ("weixin".equals(wxNoStr)) {
                            isSend = false;
                            isBack = true;
                        }
                        execShellCmd(String.format("input tap  %s %s", getRandom(5, 80), getRandom(80, 130)));
                    }
                } else if (targetSetBz != null) {
                    //没有微信号，设置备注作为唯一标示
                    isSetBz = true;
                    AccessibilityHelper.performClick(targetSetBz);
                } else if (targetSetEt != null) {
                    if (isSetBz) {
                        isSetBz = false;
                    } else {
                        return;
                    }
                    getHandler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            execShellCmd(String.format("input tap  %s %s", getRandom(300, 800), getRandom(377, 485)));
                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            execShellCmd(String.format("input tap  %s %s", getRandom(930, 980), getRandom(377, 485)));
                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            exeCmd(String.format("am broadcast -a ADB_INPUT_TEXT --es msg \"%s\"", System.currentTimeMillis() + ""));
                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            execShellCmd(String.format("input tap  %s %s", getRandom(876, 1056), getRandom(87, 177)));
                        }
                    }, 300);
                } else {
                    String wxNewId = "android:id/text1";
                    AccessibilityNodeInfo targetNew = AccessibilityHelper.findNodeInfosById(nodeInfo, wxNewId);
                    if (targetNew != null && targetNew.getText() != null && targetNew.getText().toString().trim().equals("腾讯新闻")) {
                        isSend = false;
                        execShellCmd(String.format("input tap  %s %s", getRandom(5, 80), getRandom(80, 130)));
                    }
                }
            } else if (isAdd) {
                //新的朋友小红点
                String newFriendRedId = "com.tencent.mm:id/ayc";
                AccessibilityNodeInfo targetNewFriendRed = AccessibilityHelper.findNodeInfosById(nodeInfo, newFriendRedId);
                //朋友圈小红点
                String circleRedId = "com.tencent.mm:id/bwk";
                AccessibilityNodeInfo targetCircleRed = AccessibilityHelper.findNodeInfosById(nodeInfo, circleRedId);
                //新的朋友icon
                String newFriendImgId = "com.tencent.mm:id/i7";
                AccessibilityNodeInfo targetNewFriendImg = AccessibilityHelper.findNodeInfosById(nodeInfo, newFriendImgId);
                //同意按钮
                String agreeId = "com.tencent.mm:id/aym";
                AccessibilityNodeInfo targetAgree = AccessibilityHelper.findNodeInfosById(nodeInfo, agreeId);
                List<AccessibilityNodeInfo> targetAgreeList = AccessibilityHelper.findNodeInfosByIds(nodeInfo, agreeId);
                //详细资料
                String infoId = "android:id/text1";
                AccessibilityNodeInfo targetInfo = AccessibilityHelper.findNodeInfosById(nodeInfo, infoId);
                //weixin
                String weixinId = "com.tencent.mm:id/bw6";
                AccessibilityNodeInfo targetWeixin = AccessibilityHelper.findNodeInfosById(nodeInfo, weixinId);
                //聊天界面---右上角图片
                String chattingId = "com.tencent.mm:id/fk";
                AccessibilityNodeInfo targetChatting = AccessibilityHelper.findNodeInfosById(nodeInfo, chattingId);
                //验证界面---完成按钮
                String doneId = "com.tencent.mm:id/go";
                AccessibilityNodeInfo targetDone = AccessibilityHelper.findNodeInfosById(nodeInfo, doneId);
                //信息界面---通过验证按钮
                String checkId = "com.tencent.mm:id/afx";
                AccessibilityNodeInfo targetCheck = AccessibilityHelper.findNodeInfosById(nodeInfo, checkId);
                if (targetNewFriendRed != null) {
                    AccessibilityHelper.performClick(targetNewFriendRed);
                } else if (targetCheck != null) {
                    AccessibilityHelper.performClick(targetCheck);
                } else if (targetNewFriendImg != null) {
                    AccessibilityHelper.performClick(targetNewFriendImg);
                } else if (targetAgreeList != null) {
                    for (int i = 0; i < targetAgreeList.size(); i++) {
                        if (targetAgreeList.get(i) != null && targetAgreeList.get(i).getText() != null &&
                                "接受".equals(targetAgreeList.get(i).getText().toString().trim())) {
                            AccessibilityHelper.performClick(targetAgreeList.get(i));
                            break;
                        }
                    }
                } else if (targetDone != null) {
                    isAdd = false;
                    AccessibilityHelper.performClick(targetDone);
                } else if (targetCircleRed != null) {
                    isAdd = false;
                    AccessibilityHelper.performClick(targetCircleRed);
                }
            } else if (isFirst) {
                //聊天界面
                String chattingId = "com.tencent.mm:id/a5e";
                AccessibilityNodeInfo targetChatting = AccessibilityHelper.findNodeInfosById(nodeInfo, chattingId);
                String collImgId = "com.tencent.mm:id/ao7";
                AccessibilityNodeInfo targetCollImg = AccessibilityHelper.findNodeInfosById(nodeInfo, collImgId);
                String collSendId = "com.tencent.mm:id/a_r";
                AccessibilityNodeInfo targetCollSend = AccessibilityHelper.findNodeInfosById(nodeInfo, collSendId);
                if (targetChatting != null) {
                    if (isFirstSend) {
                        return;
                    }
                    isFirstSend = true;
                    String sendEtId = "com.tencent.mm:id/a5e";
                    AccessibilityNodeInfo targetSendEt = AccessibilityHelper.findNodeInfosById(nodeInfo, sendEtId);
                    AccessibilityHelper.performClick(targetSendEt);
                    getHandler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            String msg = "你好，很高兴认识你。";
                            exeCmd(String.format("am broadcast -a ADB_INPUT_TEXT --es msg \"%s\"", msg));
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            String wxSendId = "com.tencent.mm:id/a5k";
                            AccessibilityNodeInfo targetSend = AccessibilityHelper.findNodeInfosById(nodeInfo, wxSendId);
                            if (targetSend != null) {
                                AccessibilityHelper.performClick(targetSend);
                            }
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            execShellCmd(String.format("input tap  %s %s", getRandom(5, 80), getRandom(80, 130)));
                            isFirst = false;
                            isFirstSend = false;
                            isBack = true;
                            //发送图片
//                            String moreAddId = "com.tencent.mm:id/a2e";
//                            AccessibilityNodeInfo targetMoreAdd = AccessibilityHelper.findNodeInfosById(nodeInfo, moreAddId);
//                            if (targetMoreAdd != null) {
//                                AccessibilityHelper.performClick(targetMoreAdd);
//                            }
//                            try {
//                                Thread.sleep(1000);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                            String collectionId = "com.tencent.mm:id/ky";
//                            List<AccessibilityNodeInfo> targetCollection = AccessibilityHelper.findNodeInfosByIds(nodeInfo, collectionId);
//                            if (targetCollection != null) {
//                                for (int i = 0; i < targetCollection.size(); i++) {
//                                    if (targetCollection.get(i) != null && targetCollection.get(i).getText() != null &&
//                                            targetCollection.get(i).getText().toString().trim().contains("我的收藏")) {
//                                        AccessibilityHelper.performClick(targetCollection.get(i));
//                                    }
//                                }
//                            }
                        }
                    }, 500);
                } else if (targetCollImg != null) {
                    AccessibilityHelper.performClick(targetCollImg);
                } else if (targetCollSend != null && targetCollSend.getText() != null &&
                        targetCollSend.getText().toString().trim().contains("发送")) {
                    AccessibilityHelper.performClick(targetCollSend);
                    isFirst = false;
                    isFirstSend = false;
                    isBack = true;
                }
            } else if (isPushMsg) {
                String searchEtId = "com.tencent.mm:id/h2";
                AccessibilityNodeInfo targetSearchEt = AccessibilityHelper.findNodeInfosById(nodeInfo, searchEtId);
                String recordId = "com.tencent.mm:id/ja";
                AccessibilityNodeInfo targetRecord = AccessibilityHelper.findNodeInfosById(nodeInfo, recordId);
                String sosoId = "com.tencent.mm:id/b23";
                AccessibilityNodeInfo targetSoso = AccessibilityHelper.findNodeInfosById(nodeInfo, sosoId);
                //聊天界面---右上角图片
                String chattingId = "com.tencent.mm:id/a5e";
                AccessibilityNodeInfo targetChatting = AccessibilityHelper.findNodeInfosById(nodeInfo, chattingId);
                String wxSendId = "com.tencent.mm:id/a5k";
                AccessibilityNodeInfo targetSend = AccessibilityHelper.findNodeInfosById(nodeInfo, wxSendId);
                if (!isPushCode && targetSearchEt != null) {
                    isPushCode = true;
                    AccessibilityHelper.performClick(targetSearchEt);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    exeCmd(String.format("am broadcast -a ADB_INPUT_TEXT --es msg \"%s\"", "Hmj9406"));
                } else if (targetRecord != null) {
                    isInputEnter = false;
                    AccessibilityHelper.performClick(targetRecord);
                } else if (targetSoso != null) {
                    isPushMsg = false;
                    execShellCmd(String.format("input tap  %s %s", getRandom(5, 80), getRandom(80, 130)));
                } else if (targetChatting != null) {
                    if (!isInputEnter) {
                        isInputEnter = true;
                        //给对方发消息
                        getHandler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                String sendEtId = "com.tencent.mm:id/a5e";
                                AccessibilityNodeInfo targetSendEt = AccessibilityHelper.findNodeInfosById(nodeInfo, sendEtId);
                                AccessibilityHelper.performClick(targetSendEt);
                                //execShellCmd(String.format("input tap  %s %s", getRandom(270, 540), getRandom(1800, 1890)));
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                exeCmd(String.format("am broadcast -a ADB_INPUT_TEXT --es msg \"%s\"", "你好！！！"));
                            }
                        }, 100);
                    } else if (targetSend != null) {
                        AccessibilityHelper.performClick(targetSend);
                        isPushMsg = false;
                        isInputEnter = false;
                        getHandler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                isBack = true;
                                execShellCmd(String.format("input tap  %s %s", getRandom(5, 80), getRandom(80, 130)));
                            }
                        }, 500);
                    }
                } else {
//                    isPushMsg = false;
//                    execShellCmd(String.format("input tap  %s %s", getRandom(5, 80), getRandom(80, 130)));
                }
            } else if (isAddFriendCheck) {
                //通讯录新的朋友
                String friendLayoutId = "com.tencent.mm:id/aya";
                AccessibilityNodeInfo targetFriendLayout = AccessibilityHelper.findNodeInfosById(nodeInfo, friendLayoutId);
                //listview
                String listViewId = "com.tencent.mm:id/ayr";
                AccessibilityNodeInfo targetListView = AccessibilityHelper.findNodeInfosById(nodeInfo, listViewId);
                //listview
                String noneId = "com.tencent.mm:id/ayv";
                AccessibilityNodeInfo targetNone = AccessibilityHelper.findNodeInfosById(nodeInfo, noneId);
                if (targetFriendLayout != null) {
                    AccessibilityHelper.performClick(targetFriendLayout);
                } else if (targetListView != null) {
                    //同意按钮
                    String agreeId = "com.tencent.mm:id/aym";
                    AccessibilityNodeInfo targetAgree = AccessibilityHelper.findNodeInfosById(nodeInfo, agreeId);
                    if (targetAgree != null) {
                        isAddFriendCheck = false;
                        isAdd = true;
                        AccessibilityHelper.performClick(targetAgree);
                    } else {
                        if (!AccessibilityHelper.scroll(targetListView)) {
                            isAddFriendCheck = false;
                            isBack = true;
                            execShellCmd(String.format("input tap  %s %s", getRandom(5, 80), getRandom(80, 130)));
                        }
                    }
                } else if (targetNone != null) {
                    isAddFriendCheck = false;
                    isBack = true;
                    execShellCmd(String.format("input tap  %s %s", getRandom(5, 80), getRandom(80, 130)));
                }
            } else {
                //列表小红点
                String listRedId = "com.tencent.mm:id/i8";
                final AccessibilityNodeInfo targetListRed = AccessibilityHelper.findNodeInfosById(nodeInfo, listRedId);
                //底部小红点
                String bottomRedId = "com.tencent.mm:id/bw4";
                final AccessibilityNodeInfo targetBottomRed = AccessibilityHelper.findNodeInfosById(nodeInfo, bottomRedId);
                if (targetListRed != null) {
                    Log.e(TAG, "有列表小红点了");
                    //有列表小红点
                    getHandler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.e(TAG, "列表小红点 点击了");
                            wxNoStr = null;
                            wxMsgStr = null;
                            AccessibilityHelper.performClick(targetListRed);
                            isSend = true;
                        }
                    }, 800);
                } else if (targetBottomRed != null) {
                    Log.e(TAG, "有底部小红点了");
                    Rect outBounds = new Rect();
                    targetBottomRed.getBoundsInScreen(outBounds);
                    Rect mStandarList = new Rect(150, 1770, 220, 1830);
                    Rect mStandarAdd = new Rect(420, 1770, 490, 1830);
                    if (mStandarList.contains(outBounds)) {
                        //聊天底部小红点
                        getHandler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                AccessibilityHelper.performClick(targetBottomRed);
                                try {
                                    Thread.sleep(getRandomInt(100, 200));
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                AccessibilityHelper.performClick(targetBottomRed);
                            }
                        }, 100);
                    } else if (mStandarAdd.contains(outBounds)) {
                        //加好友底部小红点
                        getHandler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
//                            newFriendNum = 0;
//                            newFriendIndex = 0;
                                isAdd = true;
                                AccessibilityHelper.performClick(targetBottomRed);
                            }
                        }, 800);
                    }
                } else {
                    //详细资料界面
                    String settingNameId = "com.tencent.mm:id/agb";
                    AccessibilityNodeInfo targetSettingName = AccessibilityHelper.findNodeInfosById(nodeInfo, settingNameId);
                    String sendMsgId = "com.tencent.mm:id/ag6";
                    AccessibilityNodeInfo targetSendMsg = AccessibilityHelper.findNodeInfosById(nodeInfo, sendMsgId);
                    if (targetSettingName == null && targetSendMsg != null) {
                        isFirst = true;
                        AccessibilityHelper.performClick(targetSendMsg);
                    }
                }
            }
        }
    }

    public String getRandom(int min, int max) {
        Random random = new Random();
        int s = random.nextInt(max) % (max - min + 1) + min;
        return String.valueOf(s);
    }

    public int getRandomInt(int min, int max) {
        Random random = new Random();
        int s = random.nextInt(max) % (max - min + 1) + min;
        return s;
    }

    private void showToast(String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    /**
     * 执行shell命令
     *
     * @param cmd
     */
    private void execShellCmd(String cmd) {
        try {
            // 申请获取root权限，这一步很重要，不然会没有作用
            Process process = Runtime.getRuntime().exec("su");
            // 获取输出流
            OutputStream outputStream = process.getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(
                    outputStream);
            dataOutputStream.writeBytes(cmd);
            dataOutputStream.flush();
            dataOutputStream.close();
            outputStream.close();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Shell命令封装类
     *
     * @param cmd Shell命令
     */
    public static void exeCmd(String cmd) {
        try {
            Process process = Runtime.getRuntime().exec("su");

            OutputStream outputStream = process.getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(
                    outputStream);
            byte[] t_utf8 = (cmd + "\n").getBytes("UTF-8");
            dataOutputStream.write(t_utf8);
            dataOutputStream.flush();
            dataOutputStream.close();
            outputStream.close();
        } catch (Throwable t) {
            Log.e("test", "execCommonShell[ " + cmd + " ] error.", t);
        }
    }

    private void clickId(AccessibilityNodeInfo nodeInfo, String id) {
        AccessibilityNodeInfo targetHomeAdd = AccessibilityHelper.findNodeInfosById(nodeInfo, id);
        if (targetHomeAdd != null) {
            Toast.makeText(this, id + " is find", Toast.LENGTH_SHORT).show();
            AccessibilityHelper.performClick(targetHomeAdd);
        }
    }

    private void clickIds(AccessibilityNodeInfo nodeInfo, String id) {
        List<AccessibilityNodeInfo> targetHomeAdd = AccessibilityHelper.findNodeInfosByIds(nodeInfo, id);
        if (targetHomeAdd != null) {
            for (int i = 0; i < targetHomeAdd.size(); i++) {
                if (targetHomeAdd.get(i) != null)
                    AccessibilityHelper.performClick(targetHomeAdd.get(i));
            }
        }
    }

    private Handler getHandler() {
        if (mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper());
        }
        return mHandler;
    }

    /**
     * 判断当前服务是否正在运行
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static boolean isRunning() {
        if (service == null) {
            return false;
        }
        AccessibilityManager accessibilityManager = (AccessibilityManager) service.getSystemService(Context.ACCESSIBILITY_SERVICE);
        AccessibilityServiceInfo info = service.getServiceInfo();
        if (info == null) {
            return false;
        }
        List<AccessibilityServiceInfo> list = accessibilityManager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_GENERIC);
        Iterator<AccessibilityServiceInfo> iterator = list.iterator();

        boolean isConnect = false;
        while (iterator.hasNext()) {
            AccessibilityServiceInfo i = iterator.next();
            if (i.getId().equals(info.getId())) {
                isConnect = true;
                break;
            }
        }
        if (!isConnect) {
            return false;
        }
        return true;
    }

    /**
     * 快速读取通知栏服务是否启动
     */
    public static boolean isNotificationServiceRunning() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return false;
        }
        //部份手机没有NotificationService服务
        return false;
    }


}
