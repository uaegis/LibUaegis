package com.uaegis;

import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.excelsecu.slotapi.EsDevice;
import com.excelsecu.slotapi.EsDeviceManager;
import com.excelsecu.slotapi.EsEvent;
import com.excelsecu.slotapi.EsEventListener;
import com.excelsecu.slotapi.EsException;
import com.utils.AsyncExecutor;
import com.utils.Client;
import com.utils.InputDialog;
import com.utils.NoButtonMessageDialog;
import com.utils.RSACoder;

import java.util.UUID;

public class UaegisActivity extends BaseActivity {
    public static final String RESULT_EXTRA = "result";
    private static final int MAX_PWD_LENGTH = 12;
    private static final int MIN_PWD_LENGTH = 6;
    private static final int TIME = 300;

    //U盾
    private EsDeviceManager manager;
    private EsDevice device;
    private int deviceType = EsDevice.TYPE_BLUETOOTH;
    private boolean isConnected = false;

    //layout
    private Button switchBtn;
    private Button stateBtn;
    private Button certainBtn;

    //state
    private static final int notConnected = 3;
    private static final int connecting = 2;
    private static final int connected = 1;

    private static final UUID uuid = UUID.randomUUID();
    private static final String VERITY_MSG = "<?xml version=\"1.0\" encoding=\"utf-8\"?><T><D><M><k>ID:</k><v>"+uuid.toString()+"</v></M></D></T>";
    private Intent intent;

    private Handler UI_THREAD_HANDLER = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            updateState(msg.what, msg.obj);
        }
    };


    //executor
    private AsyncExecutor<String> getSNAsyncExecutor = new AsyncExecutor<String>() {
        @Override
        public String asyncExecute() {
            if(!isConnected){
                return null;
            }
            try {
                String str = device.getMediaId();
                return str;
            } catch (EsException e) {
                e.printStackTrace();
                showTips(String.format("获取Sn错误:0x%08", new Object[]{Integer.valueOf(e.getErrorCode())}));
            }
            return "";
        }

        @Override
        public void executeComplete(String obj) {
            if (obj != null){
                stateBtn.setText(String.format("设备已连接[SN:%s]", new Object[]{obj}));
            }
        }

        @Override
        public void executePrepare() {
        }
    };


    private class VerityExecutor implements AsyncExecutor<String>{
        private NoButtonMessageDialog dialog = new NoButtonMessageDialog(UaegisActivity.this);
        private String psw;
        private String msg;
        public VerityExecutor(String psw, String msg){
            this.psw = psw;
            this.msg = msg;
        }
        @Override
        public String asyncExecute() {

            if (!isConnected) {
                return null;
            }
            try {
                int[] certs = device.getKeyPairList(EsDevice.KEY_SPEC_SIGNATURE);
                if (certs.length < 1){
                    return "未找到证书";
                }
                if (!doVerityPsw(psw)){
                    int[] infos = device.getPinInfo(EsDevice.PIN_TYPE_USER);
                    return "校验密码错误，剩余: " + infos[0] + "次";
                }

                byte[] signature = device.signMessage(certs[0], EsDevice.HASH_ALG_SHA256, msg, "utf-8");
                dialog.cancel();
                byte[] certbyte = device.readCert(certs[0]);

                //发送加密后的signature+明文+数字证书到服务器

                return  Client.doVerity(msg.getBytes(), signature, certbyte);
            } catch (EsException e) {
                e.printStackTrace();
            }
            return "";
        }

        @Override
        public void executeComplete(String obj) {
            dialog.cancel();
            if(null != obj){
                /*
                Intent intent = new Intent();
                intent.putExtra(RESULT_EXTRA, obj);
                setResult(RESULT_OK, intent);
                finish();*/
                if (null != intent){
                    intent.putExtra(RESULT_EXTRA, obj);
                    setResult(RESULT_OK, intent);
                    UaegisActivity.this.finish();
                }
                //showTips(obj);
            }
        }

        @Override
        public void executePrepare() {
            dialog.setMessage("将要进行签名操作，请点击设备上的“确认”按钮");
            dialog.show();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uaegis);
        intent = getIntent();
        init();
        registerListener();
    }



    private void init(){
        switchBtn = (Button) findViewById(R.id.uaegis_main_btn_switch);
        stateBtn = (Button) findViewById(R.id.uaegis_main_btn_conn_state);
        certainBtn = (Button) findViewById(R.id.uaegis_main_btn_verity_certain);
        manager = EsDeviceManager.getInstance(this);
        manager.setListener(deviceType, esEventListener);
        updateState(3, null);
        manager.start(deviceType, TIME);
    }

    private void registerListener() {
        switchBtn.setOnClickListener(clickListener);
        stateBtn.setOnClickListener(clickListener);
        certainBtn.setOnClickListener(clickListener);
    }

    //verity psw
    private boolean doVerityPsw(String psw) {
        if (!isConnected){
            return false;
        }
        try {
            int[] infos = device.getPinInfo(EsDevice.PIN_TYPE_USER);
            if (infos[0] < 1){
                showMessageDialog(null, "设备已锁定");
                return false;
            }
            if (infos[0] < 2 ){
                showTips("请按设备上的<确认>按键");
            }
            boolean flag = device.verifyPin(EsDevice.PIN_TYPE_USER, psw);
            return flag;
        } catch (EsException e) {
            e.printStackTrace();
            return false;
        }
    }



    private void showTips(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void updateState(int state, Object object){
        EsEvent event = (EsEvent) object;
        String str = "蓝牙";
        if (state == notConnected){
            isConnected = false;
            enableButtons(false);
            stateBtn.setText("未连接设备");
        }else if (state == connecting){
            stateBtn.setText("正在连接设备");
            enableButtons(false);
        }else{
            isConnected = true;
            device = manager.getDevice(event.getDeviceId());
            enableButtons(true);
            stateBtn.setText("设备已连接");
            asyncExecute(getSNAsyncExecutor);
            if (deviceType != EsDevice.TYPE_BLUETOOTH) {
                str = "音频";
            }

            setTitle(getString(R.string.uaegis_title_name, new Object[]{str}));
        }
    }
    private void enableButtons(boolean enabled) {
        stateBtn.setEnabled(enabled);
        certainBtn.setEnabled(enabled);
    }

    private EsEventListener esEventListener = new EsEventListener() {
        @Override
        public void onEvent(EsEvent event) {
            Message msg = new Message();
            msg.obj = event;
            switch (event.getType()) {
                case EsEvent.TYPE_DEVICE_CONNECTED:
                    device = manager.getDevice(event.getDeviceId());
                    showTips("设备已连接");
                    msg.what = connected;
                    break;
                case EsEvent.TYPE_DEVICE_CONNECTING:
                    showTips("设备接入，正在连接");
                    msg.what = connecting;
                    break;
                case EsEvent.TYPE_DEVICE_DISCONNECTED:
                    if (device.getId() == event.getDeviceId()) {
                        showTips("设备已断开连接");
                        msg.what = notConnected;
                    }
                    break;
                case EsEvent.TYPE_ERROR:
                    if (event.getErrorCode() == EsException.ERROR_DRV_BLUETOOTH_DISCOVERABLE_TIMEOUT) {
                        showTips("蓝牙可被发现超时，仅已配对的设备可以连上");
                    } else {
                        showTips("连接出错");
                    }
                    msg.what = notConnected;
                    break;
                default:
                    break;
            }
            UI_THREAD_HANDLER.sendMessage(msg);
        }
    };

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int i = v.getId();
            if (i == R.id.uaegis_main_btn_switch) {
                openOptionsMenu();

            } else if (i == R.id.uaegis_main_btn_verity_certain) {
                showInputDialog("签名", "请输入密码", "", "请输入6-12个数字或字母", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface paramAnonymousDialogInterface, int paramAnonymousInt) {
                        if (((paramAnonymousDialogInterface instanceof InputDialog)) && (paramAnonymousInt == -1)) {
                            String str = ((InputDialog) paramAnonymousDialogInterface).getText();
                            if ((checkPassword(str.trim())) && (isConnected)) {
                                doVerity(str.trim(), VERITY_MSG);
                            }
                        }
                    }
                });

            } else {
            }
        }
    };

    private void doVerity(String psw, String msg) {
        asyncExecute(new VerityExecutor(psw, msg));
    }

    private boolean checkPassword(String psw) {
        int i = psw.length();
        boolean bool;
        String str;
        if (i < MIN_PWD_LENGTH) {
            bool = false;
            str = "密码长度不能小于6";
        } else if (i > MAX_PWD_LENGTH){
            str = "密码长度不能大于12";
            bool = false;
        }else {
            bool = true;
            str = null;
        }
        if (!bool){
            showMessageDialog(null, str);
        }
        return bool;
    }
    public void showMessageDialog(String title, String msg) {
        Builder builder = new Builder(this);
        if(title == null) {
            title = "信息";
        }

        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setPositiveButton("确定", null);
        builder.create().show();
    }

    public void showInputDialog(String title, String msg, String text,
                                String hidedText, android.content.DialogInterface.OnClickListener listener) {
        InputDialog dialog = new InputDialog(this);
        dialog.setTitle(title);
        dialog.setMessage(msg);
        dialog.setText(text);
        dialog.setHint(hidedText);
        dialog.setListener(listener);
        dialog.show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.uaegis_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.uaegis_action_start) {
            manager.start(deviceType, TIME);
            showTips("监听已打开，等待接入");

        } else if (i == R.id.uaegis_action_stop) {
            manager.stop(deviceType);
            showTips("监听已关闭");

        } else if (i == R.id.uaegis_action_switch) {
            if (device != null) {
                manager.stop(deviceType);
                device = null;
            }
            String str = "";
            if (deviceType == EsDevice.TYPE_BLUETOOTH) {
                deviceType = EsDevice.TYPE_AUDIO;
                item.setTitle(getString(R.string.uaegis_action_switch, new Object[]{"蓝牙"}));
                str = "音频";
            } else {
                deviceType = EsDevice.TYPE_BLUETOOTH;
                item.setTitle(getString(R.string.uaegis_action_switch, new Object[]{"音频"}));
                str = "蓝牙";
            }
            showTips("已切换至" + str + "模式");
            manager = EsDeviceManager.getInstance(this);
            manager.setListener(deviceType, esEventListener);


        } else {
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.getItem(0);
        if(deviceType != EsDevice.TYPE_BLUETOOTH){
            item.setTitle(getString(R.string.uaegis_action_switch, new Object[]{"蓝牙"}));
        }else {
            item.setTitle(getString(R.string.uaegis_action_switch, new Object[]{"音频"}));
        }
        return super.onPrepareOptionsMenu(menu);
    }

}
